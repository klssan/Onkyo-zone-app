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
 boolean running;int activeId=-1;Thread worker;volatile Socket activeSocket;
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent i,int flags,int startId){
  int id=i==null?-1:i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
  NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Onkyo-Szenen",NotificationManager.IMPORTANCE_LOW));
  Notification n=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Onkyo-Szene").setContentText("Receiver wird gesteuert").setOngoing(true).build();
  if(Build.VERSION.SDK_INT>=34)startForeground(520,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE);else startForeground(520,n);
  if(!SceneWidget.owns(this,id)){if(!running)stopSelf();return START_NOT_STICKY;}
  if(running){if(id!=activeId)SceneWidget.result(this,id,"Andere Szene läuft · erneut tippen");return START_NOT_STICKY;}
  Scene s=SceneStore.get(this,id);if(s==null){SceneWidget.result(this,id,"Bitte einrichten");stopSelf();return START_NOT_STICKY;}
  running=true;activeId=id;SceneWidget.result(this,id,"Wird ausgeführt …");
  Diagnostics.open(getFilesDir());Diagnostics.event("WIDGET","start demo="+s.demo);
  worker=new Thread(()->{
   String result;
   try{if(s.demo){s.validate();Diagnostics.event("WIDGET","demo configuration valid; no network commands");result="Demo · keine Befehle";}
    else{int count=runScene(s);Diagnostics.event("WIDGET","observed actions="+count);result="Bestätigt · "+new java.text.SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date());}
   }catch(SceneExecutor.SceneException e){Diagnostics.event("WIDGET","not confirmed; observed actions="+e.confirmed);String m=e.getMessage();result=(e.confirmed>0?"Teilweise ("+e.confirmed+") · ":"Nicht bestätigt · ")+(m!=null&&m.length()<70?m:"Receiver prüfen");}
   catch(Exception e){Diagnostics.error("WIDGET_ERROR",e);result=e instanceof LocalFailure?e.getMessage():"Nicht erreichbar · Receiver prüfen";}
   final String message=result;
   new Handler(Looper.getMainLooper()).post(()->{if(activeId!=id)return;SceneWidget.result(this,id,message);running=false;activeId=-1;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();});
  },"scene-control");worker.start();return START_NOT_STICKY;
 }
 static final class LocalFailure extends IOException{LocalFailure(String message){super(message);}}
 int runScene(Scene scene)throws Exception{
  ConnectivityManager cm=getSystemService(ConnectivityManager.class);Network network=null;for(Network n:cm.getAllNetworks()){NetworkCapabilities caps=cm.getNetworkCapabilities(n);if(caps!=null&&(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))){network=n;break;}}
  if(network==null)throw new LocalFailure("Kein WLAN/LAN");
  SharedPreferences config=getSharedPreferences("config",MODE_PRIVATE);boolean manual=config.getBoolean("manual",false);String ip=config.getString("ip",""),deviceId=config.getString("deviceId","");
  if(!manual){
   ArrayList<InetAddress> targets=new ArrayList<>();LinkProperties props=cm.getLinkProperties(network);if(props!=null)for(LinkAddress l:props.getLinkAddresses())if(l.getAddress() instanceof Inet4Address)targets.add(Discovery.broadcast(l.getAddress(),l.getPrefixLength()));
   List<Discovery.Device> found;try(DatagramSocket socket=new DatagramSocket(null)){network.bindSocket(socket);socket.bind(new InetSocketAddress(0));found=Discovery.find(socket,targets,1800);}
   ArrayList<Discovery.Device> matches=new ArrayList<>();for(Discovery.Device d:found)if(d.model.equalsIgnoreCase("TX-NR656")&&d.port==60128&&(deviceId.isEmpty()||deviceId.equals(d.id)))matches.add(d);
   if(matches.size()!=1)throw new LocalFailure(matches.isEmpty()?"Onkyo nicht gefunden":"Receiver in App auswählen");
   Discovery.Device selected=matches.get(0);ip=selected.ip;config.edit().putString("ip",ip).putString("deviceId",selected.id).apply();
  }
  if(!privateIp(ip))throw new LocalFailure("Receiver in App verbinden");
  try(Socket socket=network.getSocketFactory().createSocket()){
   activeSocket=socket;socket.connect(new InetSocketAddress(ip,60128),3000);socket.setTcpNoDelay(true);
   try(SceneExecutor.SocketWire wire=new SceneExecutor.SocketWire(socket,30000)){
    // Confirm capability masks when NRI responds; standby units may not return a profile.
    Capabilities profile=null;try{String xml=wire.query("NRI");profile=Capabilities.parse(xml);}catch(Exception e){Diagnostics.error("WIDGET_PROFILE_UNAVAILABLE",e);}
    if(profile!=null){if(!profile.model.equalsIgnoreCase("TX-NR656"))throw new LocalFailure("Falsches Receiver-Modell");
     for(int z=0;z<2;z++){Scene.Zone setting=scene.zones[z];if(profile.hasSelectors&&!setting.source.isEmpty()&&!profile.sources(z).containsKey(setting.source))throw new LocalFailure("Quelle nicht unterstützt");if(profile.hasServices&&!setting.service.isEmpty()&&!profile.services(z).containsKey(setting.service))throw new LocalFailure("NET-Dienst nicht unterstützt");}
    }else if(manual){throw new LocalFailure("Geräteprofil nicht bestätigt");}
    return SceneExecutor.execute(scene,wire);
   }
  }
 }
 static boolean privateIp(String ip){String[] p=ip.split("\\.",-1);if(p.length!=4)return false;int[] a=new int[4];try{for(int i=0;i<4;i++){if(!p[i].matches("[0-9]{1,3}"))return false;a[i]=Integer.parseInt(p[i]);if(a[i]>255)return false;}return a[0]==10||a[0]==192&&a[1]==168||a[0]==172&&a[1]>=16&&a[1]<=31;}catch(NumberFormatException e){return false;}}
 @Override public void onTimeout(int startId){if(worker!=null)worker.interrupt();try{if(activeSocket!=null)activeSocket.close();}catch(IOException ignored){}if(activeId!=-1)SceneWidget.result(this,activeId,"Zeitlimit · Status prüfen");activeId=-1;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
}
