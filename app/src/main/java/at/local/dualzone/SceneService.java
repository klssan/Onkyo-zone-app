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
 boolean running,checking;int activeId=-1,token;Thread worker;volatile Socket activeSocket;
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent i,int flags,int startId){
  int id=i==null?-1:i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
  String action=i==null?"":i.getAction();boolean checkOnly=SceneWidget.CHECK.equals(action);
  if(!running){
   NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Onkyo-Szenen",NotificationManager.IMPORTANCE_LOW));
   Notification n=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(checkOnly?"Onkyo-Verbindung prüfen":"Onkyo-Szene").setContentText(checkOnly?"Status wird abgefragt":"Receiver wird gesteuert").setOngoing(true).build();
   if(Build.VERSION.SDK_INT>=34)startForeground(520,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE);else startForeground(520,n);
  }
  if(!SceneWidget.owns(this,id)||!checkOnly&&!SceneWidget.RUN.equals(action)){if(!running)stopSelf();return START_NOT_STICKY;}
  if(running){if(id!=activeId)SceneWidget.notice(this,id,"Andere Aktion läuft · erneut tippen");return START_NOT_STICKY;}
  Scene scene=SceneStore.get(this,id);if(scene==null){SceneWidget.refresh(this,id);stopSelf();return START_NOT_STICKY;}
  if(!checkOnly&&!SceneStore.begin(this,id)){SceneWidget.notice(this,id,"Speichern fehlgeschlagen · nicht gestartet");stopSelf();return START_NOT_STICKY;}
  running=true;checking=checkOnly;activeId=id;final int job=++token;
  SceneWidget.observeNetwork(this);
  SceneWidget.phase(this,id,checkOnly?"Verbindung wird geprüft …":"Szene wird vorbereitet …");
  Diagnostics.open(getFilesDir());Diagnostics.event("WIDGET","start check="+checkOnly+" demo="+scene.demo);
  worker=new Thread(()->{
   String result;
   try{
    if(scene.demo){scene.validate();Diagnostics.event("WIDGET","demo; no network commands");result="Demo · keine Befehle";}
    else{int count=runScene(scene,checkOnly,job,id);Diagnostics.event("WIDGET","observed actions="+count);result="Bestätigt";}
   }catch(SceneExecutor.SceneException e){
    Diagnostics.event("WIDGET","not confirmed; observed actions="+e.confirmed);
    result=(e.confirmed>0?"Teilweise ("+e.confirmed+")":"Nicht bestätigt")+" · "+safeSceneError(e.getMessage());
   }catch(LocalFailure e){Diagnostics.event("WIDGET","preflight blocked");result="Nicht ausgeführt · "+e.getMessage();}
   catch(Exception e){Diagnostics.error("WIDGET_ERROR",e);result="Nicht bestätigt · Receiver prüfen";}
   final String message=result;
   main.post(()->{
    if(!running||token!=job||activeId!=id)return;
    if(!checkOnly&&SceneWidget.owns(this,id))SceneStore.complete(this,id,message);
    running=false;activeId=-1;activeSocket=null;SceneWidget.phase(this,id,null);SceneWidget.refreshAll(this);
    stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
   });
  },"scene-control");worker.start();return START_NOT_STICKY;
 }
 static String safeSceneError(String message){
  if(message==null)return "Receiver antwortet nicht";
  if(message.matches("(Status fehlt|Nicht bestätigt|Nicht verfügbar): [A-Z0-9]{3}"))return message;
  if(Arrays.asList("Zonenstatus unbekannt","Zone ausgeschaltet","Quelle unbekannt","SOURCE folgt Hauptzone","Gemeinsame Medienquelle","Gemeinsamer FM/AM-Tuner").contains(message))return message;
  return "Receiver antwortet nicht";
 }
 void phase(int job,int id,String value){main.post(()->{if(running&&token==job&&activeId==id)SceneWidget.phase(this,id,value);});}
 void probe(int job,String code,long handle,String target){main.post(()->{if(running&&token==job&&target.equals(SceneStore.target(this))){SceneStore.probe(this,code,handle,target);SceneWidget.refreshAll(this);}});}
 static final class LocalFailure extends IOException{final String code;LocalFailure(String message,String code){super(message);this.code=code;}}
 int runScene(Scene scene,boolean checkOnly,int job,int id)throws Exception{
  ConnectivityManager cm=getSystemService(ConnectivityManager.class);Network network=SceneWidget.lan(this);
  long handle=network==null?0:network.getNetworkHandle();
  SharedPreferences config=getSharedPreferences("config",MODE_PRIVATE);boolean manual=config.getBoolean("manual",false);String ip=config.getString("ip",""),deviceId=config.getString("deviceId","");
  String target=SceneStore.target(this),failure=WidgetStatus.DISCOVERY_ERROR;
  try{
   if(network==null)throw new LocalFailure("Kein WLAN/LAN",WidgetStatus.NO_LAN);
   if(!manual){
    phase(job,id,"Onkyo wird gesucht …");
    ArrayList<InetAddress> targets=new ArrayList<>();LinkProperties props=cm.getLinkProperties(network);
    if(props!=null)for(LinkAddress l:props.getLinkAddresses())if(l.getAddress() instanceof Inet4Address)targets.add(Discovery.broadcast(l.getAddress(),l.getPrefixLength()));
    List<Discovery.Device> found;try(DatagramSocket socket=new DatagramSocket(null)){network.bindSocket(socket);socket.bind(new InetSocketAddress(0));found=Discovery.find(socket,targets,1800);}
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
     Capabilities profile=null;
     try{profile=Capabilities.parse(wire.query("NRI"));}catch(Exception e){Diagnostics.error("WIDGET_PROFILE_UNAVAILABLE",e);}
     if(profile!=null&&!profile.model.equalsIgnoreCase("TX-NR656"))throw new LocalFailure("Falsches Receiver-Modell",WidgetStatus.WRONG_MODEL);
     if(profile==null&&manual)throw new LocalFailure("Geräteprofil nicht bestätigt",WidgetStatus.PROFILE_UNKNOWN);
     SceneExecutor.checkReceiver(wire);probe(job,WidgetStatus.REACHABLE,handle,target);
     if(checkOnly)return 0; // Read-only check: no source, volume or power writes.
     if(!target.equals(SceneStore.target(this)))throw new LocalFailure("Receiver-Auswahl geändert",null);
     if(profile!=null)for(int z=0;z<2;z++){Scene.Zone setting=scene.zones[z];
      if(profile.hasSelectors&&!setting.source.isEmpty()&&!profile.sources(z).containsKey(setting.source))throw new LocalFailure("Quelle nicht unterstützt",null);
      if(profile.hasServices&&!setting.service.isEmpty()&&!profile.services(z).containsKey(setting.service))throw new LocalFailure("NET-Dienst nicht unterstützt",null);
     }
     phase(job,id,"Szene wird ausgeführt …");
     int count=SceneExecutor.execute(scene,wire);probe(job,WidgetStatus.REACHABLE,handle,target);return count;
    }
   }finally{activeSocket=null;}
  }catch(SceneExecutor.SceneException e){throw e;}
   catch(LocalFailure e){if(e.code!=null)probe(job,e.code,handle,target);throw e;}
   catch(Exception e){
    Diagnostics.error("WIDGET_LINK_ERROR",e);probe(job,failure,handle,target);
    throw new LocalFailure(failure.equals(WidgetStatus.DISCOVERY_ERROR)?"Gerätesuche fehlgeschlagen":failure.equals(WidgetStatus.UNREACHABLE)?"Onkyo nicht erreichbar":"Keine Statusantwort",failure);
   }
 }
 static boolean privateIp(String ip){String[] p=ip.split("\\.",-1);if(p.length!=4)return false;int[] a=new int[4];try{for(int i=0;i<4;i++){if(!p[i].matches("[0-9]{1,3}"))return false;a[i]=Integer.parseInt(p[i]);if(a[i]>255)return false;}return a[0]==10||a[0]==192&&a[1]==168||a[0]==172&&a[1]>=16&&a[1]<=31;}catch(NumberFormatException e){return false;}}
 void cancelWork(){if(worker!=null)worker.interrupt();try{if(activeSocket!=null)activeSocket.close();}catch(IOException ignored){}}
 @Override public void onTimeout(int startId){
  ++token;cancelWork();int id=activeId;boolean wasChecking=checking;running=false;activeId=-1;
  if(id!=-1){if(!wasChecking&&SceneWidget.owns(this,id))SceneStore.complete(this,id,"Zeitlimit · nicht bestätigt");SceneWidget.phase(this,id,null);}
  stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
 }
 @Override public void onDestroy(){if(running){++token;cancelWork();int id=activeId;running=false;activeId=-1;if(id!=-1)SceneWidget.phase(this,id,null);}super.onDestroy();}
}
