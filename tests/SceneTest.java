package at.local.dualzone;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
public class SceneTest {
 static Scene.Zone keep(){return new Scene.Zone(Scene.KEEP,"",-1,-1,"");}
 static Scene.Zone fm(){return new Scene.Zone(Scene.ON,"24",40,4,"");}
 static void check(boolean b){if(!b)throw new AssertionError();}
 static class Fake implements SceneExecutor.Wire{
  final Map<String,String> state=new HashMap<>();final List<String> writes=new ArrayList<>();String fail="";
  Fake(){state.put("PWR","01");state.put("ZPW","00");state.put("SLI","12");state.put("SLZ","80");}
  public String query(String k)throws IOException{return state.getOrDefault(k,"N/A");}
  public void set(String c,String p,String v)throws IOException{writes.add(c);if(p.equals(fail))throw new IOException("not confirmed");state.put(p,v);}
 }
 public static void main(String[] args)throws Exception{
  Scene scene=new Scene("FM4 Küche","Radio",keep(),fm(),false,false);Fake f=new Fake();
  check(SceneExecutor.execute(scene,f)==4);check(f.writes.equals(Arrays.asList("ZPW01","ZVL28","SLZ24","PRZ04")));check(f.state.get("PWR").equals("01"));
  Fake radio=new Fake();radio.state.put("ZPW","01");radio.state.put("SLZ","24");radio.state.put("PRZ","04");
  check(scene.radioToggleZone()==1&&SceneExecutor.toggleRadioOff(scene,radio));check(radio.writes.equals(Arrays.asList("ZPW00"))&&radio.state.get("PWR").equals("01"));
  radio.writes.clear();check(!SceneExecutor.toggleRadioOff(scene,radio)&&radio.writes.isEmpty());
  radio.state.put("ZPW","01");radio.state.put("PRZ","05");check(!SceneExecutor.toggleRadioOff(scene,radio)&&radio.writes.isEmpty());
  radio.state.put("PRZ","N/A");try{SceneExecutor.toggleRadioOff(scene,radio);throw new AssertionError("unknown preset");}catch(IOException ok){check(radio.writes.isEmpty());}
  radio.state.put("SLZ","12");check(!SceneExecutor.toggleRadioOff(scene,radio)&&radio.writes.isEmpty());
  check(scene.compactSummary().equals("ET FM4 · 40"));
  Fake shared=new Fake();shared.state.put("SLI","24");try{SceneExecutor.execute(scene,shared);throw new AssertionError("shared FM");}catch(SceneExecutor.SceneException e){check(e.confirmed==0&&shared.writes.isEmpty());}
  Scene allowed=new Scene("FM4","Radio",keep(),fm(),true,false);check(SceneExecutor.execute(allowed,shared)==4);
  Fake partial=new Fake();partial.fail="SLZ";try{SceneExecutor.execute(scene,partial);throw new AssertionError();}catch(SceneExecutor.SceneException e){check(e.confirmed==2&&partial.writes.size()==3);}
  try{new Scene("unsafe","Radio",keep(),new Scene.Zone(Scene.ON,"24",101,4,""),true,false);throw new AssertionError();}catch(IllegalArgumentException ok){}
  try{new Scene("bad","Radio",keep(),new Scene.Zone(Scene.ON,"05",20,-1,""),true,false);throw new AssertionError();}catch(IllegalArgumentException ok){}
  Scene off=new Scene("Alles aus","Power",new Scene.Zone(Scene.OFF,"",-1,-1,""),new Scene.Zone(Scene.OFF,"",-1,-1,""),false,false);Fake offWire=new Fake();check(SceneExecutor.execute(off,offWire)==2&&offWire.writes.equals(Arrays.asList("ZPW00","PWR00")));
  // Real TCP, fragmented response and matching set reply.
  try(ServerSocket ss=new ServerSocket(0,1,InetAddress.getLoopbackAddress())){
   ExecutorService pool=Executors.newSingleThreadExecutor();Future<?> server=pool.submit(()->{try(Socket s=ss.accept()){DataInputStream in=new DataInputStream(s.getInputStream());String[] cmds={"PWRQSTN","ZPWQSTN","SLIQSTN","ZPW01","ZVL28","SLZ24","PRZ04"};String[] res={"PWR01","ZPW00","SLI12","ZPW01","ZVL28","SLZ24","PRZ04"};for(int n=0;n<cmds.length;n++){check(Protocol.read(in).equals(cmds[n]));byte[] b=Protocol.encode(res[n]);s.getOutputStream().write(b,0,7);s.getOutputStream().write(b,7,b.length-7);s.getOutputStream().flush();}}catch(IOException e){throw new RuntimeException(e);}});
   try(Socket s=new Socket(InetAddress.getLoopbackAddress(),ss.getLocalPort());SceneExecutor.SocketWire wire=new SceneExecutor.SocketWire(s,10000)){check(SceneExecutor.execute(scene,wire)==4);server.get(5,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
  }
  System.out.println("Scene configuration, FM4, shared-source guard, partial failure, power-off and TCP tests passed");
 }
}
