package at.local.dualzone;
import android.content.*;
import android.net.*;
import android.os.*;
import com.wireguard.android.backend.GoBackend;
import com.wireguard.android.backend.Tunnel;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** All connection work serialized on worker threads. Failed writes are never replayed. */
final class RemoteVpn {
 static volatile boolean up;
 static volatile String target="",state="Nicht verbunden",verifiedLink="";
 static volatile Network route;
 static volatile boolean viaVpn;
 static GoBackend backend;
 static final RetryPolicy retry=new RetryPolicy();
 static final AtomicBoolean scheduled=new AtomicBoolean();
 static final Tunnel tunnel=new Tunnel(){
  @Override public String getName(){return "onkyo-dahoam";}
  @Override public void onStateChange(State s){up=s==State.UP;if(!up&&viaVpn){route=null;state="VPN getrennt · neuer Versuch";}}
 };
 static SharedPreferences settings(Context c){return c.getSharedPreferences("remote",Context.MODE_PRIVATE);}
 static boolean selected(Context c){return settings(c).getBoolean("enabled",false)&&RemoteVault.exists(c);}
 static String target(Context c){return settings(c).getString("target","");}
 static boolean vpn(Context c){return network(c)!=null&&viaVpn;}
 static String link(Context c){
  ConnectivityManager cm=c.getSystemService(ConnectivityManager.class);if(cm==null)return "";
  ArrayList<String> values=new ArrayList<>();
  for(Network n:cm.getAllNetworks()){
   NetworkCapabilities caps=cm.getNetworkCapabilities(n);
   if(caps==null||caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)||!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN))continue;
   if(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)||caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    values.add(n.getNetworkHandle()+":"+caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
  }
  Collections.sort(values);return String.join(",",values);
 }
 static boolean hasInternet(Context c){
  ConnectivityManager cm=c.getSystemService(ConnectivityManager.class);if(cm==null)return false;
  for(Network n:cm.getAllNetworks()){
   NetworkCapabilities caps=cm.getNetworkCapabilities(n);
   if(caps!=null&&caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)&&caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)&&caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))return true;
  }
  return false;
 }
 static Network network(Context c){
  Network n=route;if(!selected(c)||n==null||!verifiedLink.equals(link(c)))return null;
  ConnectivityManager cm=c.getSystemService(ConnectivityManager.class);
  if(cm==null||cm.getNetworkCapabilities(n)==null||viaVpn&&(!up||VpnService.prepare(c)!=null))return null;
  return n;
 }
 static Network findVpn(Context c,String ip){
  if(!up||!SceneService.privateIp(ip)||VpnService.prepare(c)!=null)return null;
  ConnectivityManager cm=c.getSystemService(ConnectivityManager.class);if(cm==null)return null;
  for(Network n:cm.getAllNetworks()){
   NetworkCapabilities cap=cm.getNetworkCapabilities(n);if(cap==null||!cap.hasTransport(NetworkCapabilities.TRANSPORT_VPN))continue;
   LinkProperties lp=cm.getLinkProperties(n);if(lp==null)continue;
   for(RouteInfo r:lp.getRoutes())if(r.getDestination().getPrefixLength()==32&&ip.equals(r.getDestination().getAddress().getHostAddress()))return n;
  }
  return null;
 }
 static Network home(Context c,String ip)throws Exception{
  // Never classify a foreign WLAN as home merely because its private subnet matches.
  String id=c.getSharedPreferences("config",Context.MODE_PRIVATE).getString("deviceId","");
  if(id.isEmpty())return null;
  ConnectivityManager cm=c.getSystemService(ConnectivityManager.class);
  for(Network n:cm.getAllNetworks()){
   NetworkCapabilities caps=cm.getNetworkCapabilities(n);
   if(caps==null||caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)||!(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)))continue;
   ArrayList<InetAddress> targets=new ArrayList<>();LinkProperties lp=cm.getLinkProperties(n);
   if(lp!=null)for(LinkAddress a:lp.getLinkAddresses())if(a.getAddress() instanceof Inet4Address)targets.add(Discovery.broadcast(a.getAddress(),a.getPrefixLength()));
   try(DatagramSocket socket=new DatagramSocket(null)){
    n.bindSocket(socket);socket.bind(new InetSocketAddress(0));
    for(Discovery.Device d:Discovery.find(socket,targets,1300))if(ip.equals(d.ip)&&id.equals(d.id)&&d.model.equalsIgnoreCase("TX-NR656")&&d.port==60128)return n;
   }catch(IOException e){Diagnostics.error("REMOTE_LOCAL_PROBE",e);}
  }
  return null;
 }
 static void probe(Network n,String ip)throws Exception{
  try(Socket s=n.getSocketFactory().createSocket()){
   s.connect(new InetSocketAddress(ip,60128),4500);
   try(SceneExecutor.SocketWire wire=new SceneExecutor.SocketWire(s,7000)){
    Capabilities profile=Capabilities.parse(wire.query("NRI"));
    if(profile==null||!profile.model.equalsIgnoreCase("TX-NR656"))throw new IOException("Falsches Receiver-Modell");
    SceneExecutor.checkReceiver(wire);
   }
  }
 }
 static void down()throws Exception{
  route=null;viaVpn=false;
  try{if(backend!=null)backend.setState(tunnel,Tunnel.State.DOWN,null);}finally{up=false;}
 }
 static void state(Context c,String s){state=s;Diagnostics.event("REMOTE_STATE",s);new Handler(Looper.getMainLooper()).post(()->SceneWidget.refreshAll(c.getApplicationContext()));}
 static synchronized Network ensure(Context c,boolean explicit,boolean forceVpn)throws Exception{
  Context app=c.getApplicationContext();
  if(!selected(app))return null;
  Network current=network(app);if(current!=null&&!forceVpn)return current;
  String key=link(app);if(!retry.ready(key,SystemClock.elapsedRealtime(),explicit))return null;
  route=null;
  if(key.isEmpty()){try{down();}catch(Exception ignored){}state(app,"Offline · kein Netzwerk");retry.failed(SystemClock.elapsedRealtime());return null;}
  try{
   state(app,"Verbindung wird geprüft …");
   RemoteConfig cfg=RemoteVault.load(app);target=cfg.ip;
   if(!forceVpn){
    Network local=home(app,cfg.ip);
    if(local!=null){
     down();probe(local,cfg.ip);
     if(!key.equals(link(app)))throw new IOException("Netzwerk gewechselt");
     verifiedLink=key;viaVpn=false;route=local;retry.reset();state(app,"Verbunden · Heimnetz");return local;
    }
   }
   if(!hasInternet(app)){down();state(app,"Kein Internet · Onkyo lokal nicht gefunden");retry.failed(SystemClock.elapsedRealtime());return null;}
   if(VpnService.prepare(app)!=null){down();state(app,"VPN-Freigabe fehlt · Einrichtung öffnen");retry.failed(SystemClock.elapsedRealtime());return null;}
   state(app,"Fernzugriff wird aufgebaut …");
   down();if(backend==null)backend=new GoBackend(app);
   backend.setState(tunnel,Tunnel.State.UP,cfg.config);
   Network n=null;
   for(int i=0;i<30&&n==null;i++){n=findVpn(app,cfg.ip);if(n==null)Thread.sleep(150);}
   if(n==null)throw new IOException("VPN-Netzwerk nicht bereit");
   state(app,"Onkyo wird über VPN geprüft …");probe(n,cfg.ip);
   if(!key.equals(link(app))||!up||VpnService.prepare(app)!=null)throw new IOException("Netzwerk gewechselt");
   verifiedLink=key;viaVpn=true;route=n;retry.reset();state(app,"Verbunden · Fernzugriff");return n;
  }catch(Exception e){
   try{down();}catch(Exception ignored){}
   Diagnostics.error("REMOTE_CONNECT_ERROR",e);retry.failed(SystemClock.elapsedRealtime());
   state(app,!hasInternet(app)?"Kein Internet":"FRITZ!Box / Onkyo nicht erreichbar · neuer Versuch");
   throw e;
  }
 }
 static void request(Context c){
  Context app=c.getApplicationContext();if(!selected(app)||network(app)!=null||!scheduled.compareAndSet(false,true))return;
  new Thread(()->{try{ensure(app,false,false);}catch(Exception ignored){}finally{scheduled.set(false);}},"remote-reconnect").start();
 }
 static synchronized void invalidate(Context c,Network failed){
  if(failed==null||!failed.equals(route))return;
  route=null;retry.failed(SystemClock.elapsedRealtime());state(c,"Verbindung verloren · neuer Versuch");
 }
 static synchronized void start(Context c)throws Exception{
  Context app=c.getApplicationContext();
  if(VpnService.prepare(app)!=null)throw new IOException("VPN-Freigabe fehlt");
  RemoteConfig cfg=RemoteVault.load(app);
  settings(app).edit().putBoolean("enabled",true).putString("target",cfg.ip).commit();
  retry.reset();if(ensure(app,true,true)==null)throw new IOException("Fernzugriff nicht bereit");
 }
 static synchronized void stop(Context c)throws Exception{
  settings(c).edit().putBoolean("enabled",false).apply();try{down();}finally{target="";retry.reset();state(c,"Fernzugriff deaktiviert");}
 }
 static String status(Context c){
  if(network(c)!=null)return viaVpn?"Verbunden · Fernzugriff":"Verbunden · Heimnetz";
  if(selected(c)){
   if(link(c).isEmpty())return "Offline · kein Netzwerk";
   if(VpnService.prepare(c)!=null)return "VPN-Freigabe fehlt · Einrichtung öffnen";
   return state.startsWith("Verbunden")?"Netzwerk gewechselt · neuer Versuch":state;
  }
  return RemoteVault.exists(c)?"Eingerichtet · deaktiviert":"Noch nicht eingerichtet";
 }
}
