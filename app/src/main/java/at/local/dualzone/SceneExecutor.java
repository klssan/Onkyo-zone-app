package at.local.dualzone;
import java.io.*;
import java.net.*;
import java.util.*;
/** Synchronous bounded scene transaction. No optimistic success; no automatic repeat after partial execution. */
final class SceneExecutor {
 interface Wire {String query(String prefix)throws IOException;void set(String command,String expectedPrefix,String expectedValue)throws IOException;}
 static final class SceneException extends IOException{final int confirmed;SceneException(String text,int confirmed){super(text);this.confirmed=confirmed;}}
 static int execute(Scene scene,Wire wire)throws IOException{
  scene.validate();String[] power=new String[2],source=new String[2];int confirmed=0;
  try{
   for(int i=0;i<2;i++){String pp=i==0?"PWR":"ZPW";power[i]=wire.query(pp);if(!power[i].equals("00")&&!power[i].equals("01"))throw new IOException("Zonenstatus unbekannt");}
   for(int i=0;i<2;i++){Scene.Zone z=scene.zones[i];if(z.settings()&&z.power==Scene.KEEP&&power[i].equals("00"))throw new IOException("Zone ausgeschaltet");}
   for(int i=0;i<2;i++){String sp=i==0?"SLI":"SLZ";source[i]=power[i].equals("01")?wire.query(sp):"";if(power[i].equals("01")&&!source[i].matches("[0-9A-Fa-f]{2}"))throw new IOException("Quelle unbekannt");}
   String[] wanted=source.clone();boolean[] on=new boolean[2];for(int i=0;i<2;i++){Scene.Zone z=scene.zones[i];on[i]=z.power==Scene.ON||z.power==Scene.KEEP&&power[i].equals("01");if(!z.source.isEmpty())wanted[i]=z.source;}
   // Refuse effects on an independently active zone unless explicitly allowed.
   String effective2=wanted[1].equals("80")?wanted[0]:wanted[1];
   boolean mainChanges=scene.zones[0].settings()&&(!scene.zones[0].source.isEmpty()||scene.zones[0].preset>0||!scene.zones[0].service.isEmpty());
   if(!scene.share&&on[0]&&on[1]){
    if(mainChanges&&wanted[1].equals("80")&&scene.zones[1].source.isEmpty())throw new IOException("SOURCE folgt Hauptzone");
    for(int i=0;i<2;i++){Scene.Zone z=scene.zones[i];String other=i==0?effective2:wanted[0];if(z.preset>0&&(other.equals("24")||other.equals("25"))||!z.service.isEmpty()&&other.equals("2B"))throw new IOException("Gemeinsame Medienquelle");
     if(!z.source.isEmpty()&&((z.source.equals("24")&&other.equals("25"))||(z.source.equals("25")&&other.equals("24"))))throw new IOException("Gemeinsamer FM/AM-Tuner");}
   }
   // Explicit OFF first; then power on. Never set any unselected zone control.
   for(int i=1;i>=0;i--)if(scene.zones[i].power==Scene.OFF){String p=i==0?"PWR":"ZPW";wire.set(p+"00",p,"00");confirmed++;}
   for(int i=0;i<2;i++)if(scene.zones[i].power==Scene.ON){String p=i==0?"PWR":"ZPW";wire.set(p+"01",p,"01");confirmed++;}
   // Reduce / set requested volume before selecting a source; if fixed or unsupported, stop.
   for(int i=0;i<2;i++){Scene.Zone z=scene.zones[i];if(z.volume>=0){String p=i==0?"MVL":"ZVL",v=Protocol.hex(z.volume);wire.set(p+v,p,v);confirmed++;}}
   for(int i=0;i<2;i++){Scene.Zone z=scene.zones[i];if(!z.source.isEmpty()){String p=i==0?"SLI":"SLZ";wire.set(p+z.source,p,z.source);confirmed++;}}
   for(int i=0;i<2;i++){Scene.Zone z=scene.zones[i];if(z.preset>0){String p=i==0?"PRS":"PRZ",v=Protocol.hex(z.preset);wire.set(p+v,p,v);confirmed++;}
    if(!z.service.isEmpty()){wire.set("NSV"+z.service+"0","NMS",z.service);confirmed++;}}
   return confirmed;
  }catch(IOException e){throw new SceneException(e.getMessage()==null?"Receiver antwortet nicht":e.getMessage(),confirmed);}
 }
 static final class SocketWire implements Wire,AutoCloseable {
  final Socket socket;final DataInputStream in;final long deadline;
  SocketWire(Socket s,int timeoutMs)throws IOException{socket=s;in=new DataInputStream(new BufferedInputStream(s.getInputStream()));deadline=System.nanoTime()+timeoutMs*1000000L;}
  void send(String c)throws IOException{if(Thread.currentThread().isInterrupted()||System.nanoTime()>=deadline)throw new SocketTimeoutException();Diagnostics.command("WIDGET_TX",c);socket.getOutputStream().write(Protocol.encode(c));socket.getOutputStream().flush();}
  String response(String prefix,String expected,long end)throws IOException{
   while(!Thread.currentThread().isInterrupted()&&System.nanoTime()<end){long ms=(end-System.nanoTime())/1000000L;socket.setSoTimeout((int)Math.max(1,ms));String msg=Protocol.read(in);Diagnostics.command("WIDGET_RX",msg);
    if(!msg.startsWith(prefix))continue;String v=msg.substring(3);
    if(v.equals("N/A"))throw new IOException("Nicht verfügbar: "+prefix);
    if(prefix.equals("NMS")){if(v.length()<9)continue;v=v.substring(7,9);}
    if(expected==null||expected.equalsIgnoreCase(v))return prefix.equals("NRI")?v:v.toUpperCase(Locale.ROOT);
   }throw new SocketTimeoutException();
  }
  long end(int ms){return Math.min(deadline,System.nanoTime()+ms*1000000L);}
  public String query(String prefix)throws IOException{send(prefix+"QSTN");try{return response(prefix,null,end(1800));}catch(SocketTimeoutException e){throw new IOException("Status fehlt: "+prefix);}}
  public void set(String command,String prefix,String expected)throws IOException{
   send(command);
   try{response(prefix,expected,end(prefix.equals("PWR")||prefix.equals("ZPW")?1800:900));Diagnostics.command("WIDGET_OBSERVED",command);return;}catch(SocketTimeoutException ignored){}
   // Explicit query only, never repeat a potentially applied write.
   send(prefix+"QSTN");try{response(prefix,expected,end(2200));Diagnostics.command("WIDGET_OBSERVED",command);}catch(SocketTimeoutException e){throw new IOException("Nicht bestätigt: "+prefix);}
  }
  public void close()throws IOException{socket.close();}
 }
}
