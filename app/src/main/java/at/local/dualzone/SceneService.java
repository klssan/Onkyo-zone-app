package at.local.dualzone;
import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import android.net.*;
import java.net.*;
import java.io.*;
import java.util.*;
public final class SceneService extends Service {
 static final String CHANNEL="scene_control";
 final Handler main=new Handler(Looper.getMainLooper());
 boolean running,checking,radio;String radioAction="";final java.util.concurrent.atomic.AtomicInteger volumePending=new java.util.concurrent.atomic.AtomicInteger();int activeId=-1,token;Thread worker;volatile Socket activeSocket;
 static boolean volumeAction(String a){return RadioWidget.PLUS.equals(a)||RadioWidget.MINUS.equals(a);}
 int volumeDelta(String a){return RadioWidget.PLUS.equals(a)?1:-1;}
 void enqueueVolume(String a){int d=volumeDelta(a);volumePending.updateAndGet(v->Math.max(-12,Math.min(12,v+d)));}
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent i,int flags,int startId){
  int id=i==null?-1:i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
  String action=i==null?"":i.getAction();boolean isRadio=RadioWidget.action(action);boolean checkOnly=SceneWidget.CHECK.equals(action)||RadioWidget.CHECK.equals(action);
  if(!running){
   NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Onkyo dahoam Szenen",NotificationManager.IMPORTANCE_LOW));
   Notification n=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.dz_notification).setContentTitle(checkOnly?"Onkyo dahoam Verbindung prüfen":"Onkyo dahoam Szene").setContentText(checkOnly?"Status wird abgefragt":"Receiver wird gesteuert").setOngoing(true).build();
   if(Build.VERSION.SDK_INT>=34)startForeground(520,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE);else startForeground(520,n);
  }
  if(isRadio?!RadioWidget.owns(this,id):!SceneWidget.owns(this,id)||!checkOnly&&!SceneWidget.RUN.equals(action)){if(!running)stopSelf();return START_NOT_STICKY;}
  if(running){if(isRadio&&radio&&volumeAction(action)&&volumeAction(radioAction)&&RadioWidget.isRadio(this,id)==RadioWidget.isRadio(this,activeId)){enqueueVolume(action);}else if(isRadio){RadioWidget.phase(this,"Aktion läuft …");}else if(id!=activeId)SceneWidget.notice(this,id,"Andere Aktion läuft · erneut tippen");return START_NOT_STICKY;}
  Scene scene=isRadio?null:SceneStore.get(this,id);if(!isRadio&&scene==null){SceneWidget.refresh(this,id);stopSelf();return START_NOT_STICKY;}
  if(!isRadio&&!checkOnly&&!SceneStore.begin(this,id)){SceneWidget.notice(this,id,"Speichern fehlgeschlagen · nicht gestartet");stopSelf();return START_NOT_STICKY;}
  running=true;checking=checkOnly;radio=isRadio;radioAction=action;volumePending.set(0);if(volumeAction(action))enqueueVolume(action);activeId=id;final int job=++token;
  SceneWidget.observeNetwork(this);
  if(isRadio)RadioWidget.phase(this,"Verbinden …");else SceneWidget.phase(this,id,checkOnly?"Verbindung wird geprüft …":"Szene wird vorbereitet …");
  Diagnostics.open(getFilesDir());Diagnostics.event("WIDGET","start action="+(isRadio?action:checkOnly?"CHECK":"SCENE")+" demo="+(!isRadio&&scene.demo));
  worker=new Thread(()->{
   String result;
   try{
    if(isRadio){if(volumeAction(action))runVolume(job,id);else runScene(null,checkOnly,job,id);result="Bestätigt";}else if(scene.demo){scene.validate();Diagnostics.event("WIDGET","demo; no network commands");result="Demo · keine Befehle";}
    else{int count=runScene(scene,checkOnly,job,id);Diagnostics.event("WIDGET","observed actions="+count);result=count==-1?"Bestätigt · Aus":scene.radioToggleZone()>=0?"Bestätigt · An":"Bestätigt";}
   }catch(SceneExecutor.SceneException e){
    Diagnostics.event("WIDGET","not confirmed; observed actions="+e.confirmed+" reason="+safeSceneError(e.getMessage()));
    result=(e.confirmed>0?"Teilweise ("+e.confirmed+")":"Nicht bestätigt")+" · "+safeSceneError(e.getMessage());
   }catch(LocalFailure e){Diagnostics.event("WIDGET","preflight blocked reason="+e.getMessage());result="Nicht ausgeführt · "+e.getMessage();}
   catch(Exception e){Diagnostics.error("WIDGET_ERROR",e);result="Nicht bestätigt · "+(volumeAction(action)?safeSceneError(e.getMessage()):"Receiver prüfen");}
   final String message=result;
   main.post(()->{
    if(!running||token!=job||activeId!=id)return;
    if(isRadio){if(!message.startsWith("Bestätigt")){volumePending.set(0);RadioWidget.failure(this,message);}RadioWidget.phase(this,"");}else if(!checkOnly&&SceneWidget.owns(this,id))SceneStore.complete(this,id,message);
    running=false;activeId=-1;activeSocket=null;SceneWidget.phase(this,id,null);SceneWidget.refreshAll(this);
    int remaining=volumePending.getAndSet(0);
    if(isRadio&&volumeAction(action)&&message.startsWith("Bestätigt")&&remaining!=0){
     String next=remaining>0?RadioWidget.PLUS:RadioWidget.MINUS;
     onStartCommand(new Intent(this,SceneService.class).setAction(next).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id),0,startId);
     volumePending.addAndGet(remaining-(remaining>0?1:-1));return;
    }
    stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
   });
  },"scene-control");worker.start();return START_NOT_STICKY;
 }
 void runVolume(int job,int id)throws Exception{
  Network network=SceneWidget.lan(this);if(network==null)throw new LocalFailure("Kein WLAN/LAN",WidgetStatus.NO_LAN);
  SharedPreferences config=getSharedPreferences("config",MODE_PRIVATE);String ip=config.getString("ip","");boolean manual=config.getBoolean("manual",false);
  String target=SceneStore.target(this);Capabilities profile=VolumeProfile.get(this,network,ip);
  Socket socket=null;
  try{
   // A recently confirmed IP on the same network avoids a broadcast for every tap.
   if(profile!=null||manual){
    if(privateIp(ip))try{socket=network.getSocketFactory().createSocket();activeSocket=socket;socket.connect(new InetSocketAddress(ip,60128),900);}
    catch(IOException e){if(socket!=null)socket.close();socket=null;profile=null;VolumeProfile.clear(this);}
   }
   if(socket==null&&!manual){
    phase(job,id,"Onkyo wird gesucht …");
    ArrayList<InetAddress> targets=new ArrayList<>();LinkProperties lp=getSystemService(ConnectivityManager.class).getLinkProperties(network);
    if(lp!=null)for(LinkAddress l:lp.getLinkAddresses())if(l.getAddress() instanceof Inet4Address)targets.add(Discovery.broadcast(l.getAddress(),l.getPrefixLength()));
    String deviceId=config.getString("deviceId","");ArrayList<Discovery.Device> matches=new ArrayList<>();
    try(DatagramSocket udp=new DatagramSocket(null)){network.bindSocket(udp);udp.bind(new InetSocketAddress(0));for(Discovery.Device d:Discovery.find(udp,targets,1800,deviceId))if(d.model.equalsIgnoreCase("TX-NR656")&&d.port==60128&&(deviceId.isEmpty()||deviceId.equals(d.id)))matches.add(d);}
    if(matches.size()!=1)throw new LocalFailure(matches.isEmpty()?"Onkyo nicht gefunden":"Receiver in App auswählen",WidgetStatus.NOT_FOUND);
    if(!target.equals(SceneStore.target(this)))throw new LocalFailure("Receiver-Auswahl geändert",null);
    Discovery.Device selected=matches.get(0);ip=selected.ip;config.edit().putString("ip",ip).putString("deviceId",selected.id).apply();target=SceneStore.target(this);
    socket=network.getSocketFactory().createSocket();activeSocket=socket;socket.connect(new InetSocketAddress(ip,60128),2500);
   }
   if(socket==null)throw new LocalFailure("Onkyo nicht erreichbar",WidgetStatus.UNREACHABLE);
   socket.setTcpNoDelay(true);
   try(SceneExecutor.SocketWire wire=new SceneExecutor.SocketWire(socket,25000)){
    if(profile==null){String raw=wire.query("NRI");profile=Capabilities.parse(raw);if(!profile.model.equalsIgnoreCase("TX-NR656"))throw new LocalFailure("Falsches Receiver-Modell",WidgetStatus.WRONG_MODEL);VolumeProfile.save(this,network,ip,raw);}
    boolean diningOnly=RadioWidget.isRadio(this,id);
    RadioControl.State state=diningOnly?RadioControl.read(wire,profile):RadioControl.readVolumes(wire,profile);
    phase(job,id,"Lautstärke …");long until=SystemClock.elapsedRealtime()+12000;
    while(SystemClock.elapsedRealtime()<until&&!Thread.currentThread().isInterrupted()){
     if(!target.equals(SceneStore.target(this))||!network.equals(SceneWidget.lan(this)))throw new LocalFailure("Verbindung geändert",null);
     int delta=volumePending.getAndSet(0);
     if(delta==0){Thread.sleep(250);delta=volumePending.getAndSet(0);if(delta==0)break;}
     // Coalesce rapid presses to one bounded, confirmed write per active zone.
     if(diningOnly){RadioControl.adjustDining(state,wire,delta);RadioWidget.store(this,state,profile,network);}
     else{RadioControl.adjust(state,wire,delta);RadioWidget.storeVolumes(this,state,network);}
    }
    if(!state.on[0]&&!state.on[1]){if(diningOnly)RadioWidget.store(this,state,profile,network);else RadioWidget.storeVolumes(this,state,network);}
    probe(job,WidgetStatus.REACHABLE,network.getNetworkHandle(),target);
   }
  }catch(Exception e){VolumeProfile.clear(this);Diagnostics.error("WIDGET_VOLUME_ERROR",e);throw e;}
  finally{if(socket!=null)try{socket.close();}catch(IOException ignored){}activeSocket=null;}
 }
 static String safeSceneError(String message){
  if(message==null)return "Receiver antwortet nicht";
  if(message.matches("(Status fehlt|Nicht bestätigt|Nicht verfügbar): [A-Z0-9]{3}"))return message;
  if(Arrays.asList("Zonenstatus unbekannt","Zone ausgeschaltet","Quelle unbekannt","SOURCE folgt Wohnzimmer","Gemeinsame Medienquelle","Gemeinsamer FM/AM-Tuner","Lautstärke über Geräte-Maximum","Radiospeicherplatz unbekannt","Lautstärke nicht verfügbar").contains(message))return message;
  return "Receiver antwortet nicht";
 }
 void phase(int job,int id,String value){main.post(()->{if(running&&token==job&&activeId==id){if(radio)RadioWidget.phase(this,value);else SceneWidget.phase(this,id,value);}});}
 void probe(int job,String code,long handle,String target){main.post(()->{if(running&&token==job&&target.equals(SceneStore.target(this))){SceneStore.probe(this,code,handle,target);SceneWidget.refreshAll(this);}});}
 static final class LocalFailure extends IOException{final String code;LocalFailure(String message,String code){super(message);this.code=code;}}
 int runScene(Scene scene,boolean checkOnly,int job,int id)throws Exception{
  ConnectivityManager cm=getSystemService(ConnectivityManager.class);boolean remote=RemoteVpn.selected(this);
  Network network=SceneWidget.lan(this);
  if(remote&&network==null){phase(job,id,"Verbindung wird aufgebaut …");try{network=RemoteVpn.ensure(this,true,false);}catch(Exception e){throw new LocalFailure(RemoteVpn.status(this),WidgetStatus.UNREACHABLE);}}
  if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
  long handle=network==null?0:network.getNetworkHandle();
  SharedPreferences config=getSharedPreferences("config",MODE_PRIVATE);boolean manual=config.getBoolean("manual",false);String ip=config.getString("ip",""),deviceId=config.getString("deviceId","");
  if(remote){manual=true;ip=RemoteVpn.target(this);}
  String target=SceneStore.target(this),failure=WidgetStatus.DISCOVERY_ERROR;
  try{
   if(network==null)throw new LocalFailure(remote?RemoteVpn.status(this):"Kein WLAN/LAN",WidgetStatus.NO_LAN);
   if(!manual){
    phase(job,id,"Onkyo wird gesucht …");
    ArrayList<InetAddress> targets=new ArrayList<>();LinkProperties props=cm.getLinkProperties(network);
    if(props!=null)for(LinkAddress l:props.getLinkAddresses())if(l.getAddress() instanceof Inet4Address)targets.add(Discovery.broadcast(l.getAddress(),l.getPrefixLength()));
    List<Discovery.Device> found;try(DatagramSocket socket=new DatagramSocket(null)){network.bindSocket(socket);socket.bind(new InetSocketAddress(0));found=Discovery.find(socket,targets,1800,deviceId);}
    ArrayList<Discovery.Device> matches=new ArrayList<>();
    for(Discovery.Device d:found)if(d.model.equalsIgnoreCase("TX-NR656")&&d.port==60128&&(deviceId.isEmpty()||deviceId.equals(d.id)))matches.add(d);
    if(matches.size()!=1)throw new LocalFailure(matches.isEmpty()?"Onkyo nicht gefunden":"Receiver in App auswählen",matches.isEmpty()?WidgetStatus.NOT_FOUND:WidgetStatus.CHOOSE);
    Discovery.Device selected=matches.get(0);ip=selected.ip;
    if(!target.equals(SceneStore.target(this)))throw new LocalFailure("Receiver-Auswahl geändert",null);
    config.edit().putString("ip",ip).putString("deviceId",selected.id).apply();target=SceneStore.target(this);
   }
   if(!privateIp(ip))throw new LocalFailure("Receiver in App verbinden",WidgetStatus.CHOOSE);
   if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
   phase(job,id,"Verbindung wird aufgebaut …");failure=WidgetStatus.UNREACHABLE;
   try(Socket socket=network.getSocketFactory().createSocket()){
    activeSocket=socket;socket.connect(new InetSocketAddress(ip,60128),3000);socket.setTcpNoDelay(true);failure=WidgetStatus.NO_REPLY;
    phase(job,id,"Gerätestatus wird geprüft …");
    try(SceneExecutor.SocketWire wire=new SceneExecutor.SocketWire(socket,30000)){
     Capabilities profile=radio&&!checkOnly?VolumeProfile.get(this,network,ip):null;
     try{if(profile==null){String raw=wire.query("NRI");profile=Capabilities.parse(raw);if(profile.model.equalsIgnoreCase("TX-NR656"))VolumeProfile.save(this,network,ip,raw);}}catch(Exception e){Diagnostics.error("WIDGET_PROFILE_UNAVAILABLE",e);}
     if(profile!=null&&!profile.model.equalsIgnoreCase("TX-NR656"))throw new LocalFailure("Falsches Receiver-Modell",WidgetStatus.WRONG_MODEL);
     if(profile==null&&manual)throw new LocalFailure("Geräteprofil nicht bestätigt",WidgetStatus.PROFILE_UNKNOWN);
     if(!radio)SceneExecutor.checkReceiver(wire);probe(job,WidgetStatus.REACHABLE,handle,target);
     if(radio){
      if(profile==null)throw new LocalFailure("Geräteprofil nicht bestätigt",WidgetStatus.PROFILE_UNKNOWN);
      RadioControl.State state=RadioControl.read(wire,profile);
      if(!target.equals(SceneStore.target(this))||remote&&!network.equals(RemoteVpn.network(this)))throw new LocalFailure("Verbindung geändert · erneut tippen",null);
      int desired=-1;
      switch(radioAction){
       case RadioWidget.TOGGLE:
        if(state.fm(1)){RadioControl.toggle(state,wire,profile,1);}
        else{desired=state.tuner()<0?RadioWidget.lastPreset(this):-1;RadioControl.startFM(state,wire,profile,1);}
        state=RadioControl.read(wire,profile);
        if(desired>0&&state.fm(1)){RadioControl.selectPreset(state,wire,profile,1,desired);RadioControl.readTuner(state,wire);}
        break;
       case RadioWidget.UP:RadioControl.stepDining(state,wire,profile);RadioControl.readTuner(state,wire);break;
       default:break;
      }
      RadioWidget.store(this,state,profile,network);return 0;
     }
     if(checkOnly)return 0; // Read-only check: no source, volume or power writes.
     if(!target.equals(SceneStore.target(this))||remote&&!network.equals(RemoteVpn.network(this)))throw new LocalFailure("Verbindung geändert · erneut tippen",null);
     if(scene.radioToggleZone()>=0){phase(job,id,"Radio wird geprüft …");try{if(SceneExecutor.toggleRadioOff(scene,wire)){probe(job,WidgetStatus.REACHABLE,handle,target);return -1;}}catch(IOException e){throw new SceneExecutor.SceneException(e.getMessage(),0);}}
     if(profile!=null)for(int z=0;z<2;z++){Scene.Zone setting=scene.zones[z];
      if(profile.hasSelectors&&!setting.source.isEmpty()&&!profile.sources(z).containsKey(setting.source))throw new LocalFailure("Quelle nicht unterstützt",null);
      if(profile.hasServices&&!setting.service.isEmpty()&&!profile.services(z).containsKey(setting.service))throw new LocalFailure("NET-Dienst nicht unterstützt",null);
     }
     phase(job,id,"Szene wird ausgeführt …");
     int count=SceneExecutor.execute(scene,wire,profile);probe(job,WidgetStatus.REACHABLE,handle,target);return count;
    }
   }finally{activeSocket=null;}
  }catch(SceneExecutor.SceneException e){throw e;}
   catch(LocalFailure e){if(e.code!=null)probe(job,e.code,handle,target);throw e;}
   catch(Exception e){
    Diagnostics.error("WIDGET_LINK_ERROR",e);if(remote)RemoteVpn.invalidate(this,network);probe(job,failure,handle,target);
    throw new LocalFailure(failure.equals(WidgetStatus.DISCOVERY_ERROR)?"Gerätesuche fehlgeschlagen":failure.equals(WidgetStatus.UNREACHABLE)?"Onkyo nicht erreichbar":"Keine Statusantwort",failure);
   }
 }
 static boolean privateIp(String ip){String[] p=ip.split("\\.",-1);if(p.length!=4)return false;int[] a=new int[4];try{for(int i=0;i<4;i++){if(!p[i].matches("[0-9]{1,3}"))return false;a[i]=Integer.parseInt(p[i]);if(a[i]>255)return false;}return a[0]==10||a[0]==192&&a[1]==168||a[0]==172&&a[1]>=16&&a[1]<=31;}catch(NumberFormatException e){return false;}}
 void cancelWork(){if(worker!=null)worker.interrupt();try{if(activeSocket!=null)activeSocket.close();}catch(IOException ignored){}}
 @Override public void onTimeout(int startId){
  ++token;cancelWork();int id=activeId;boolean wasChecking=checking;running=false;activeId=-1;
  if(radio){RadioWidget.failure(this,"Zeitlimit · erneut tippen");RadioWidget.phase(this,"");}if(id!=-1){if(!wasChecking&&SceneWidget.owns(this,id))SceneStore.complete(this,id,"Zeitlimit · nicht bestätigt");SceneWidget.phase(this,id,null);}
  stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
 }
 @Override public void onDestroy(){if(running){if(radio){RadioWidget.failure(this,"Unterbrochen · erneut tippen");RadioWidget.phase(this,"");}++token;cancelWork();int id=activeId;running=false;activeId=-1;if(id!=-1)SceneWidget.phase(this,id,null);}super.onDestroy();}
}
