package at.local.dualzone;
import android.content.Context;
import android.net.*;
import android.os.*;
import com.wireguard.android.backend.GoBackend;
import com.wireguard.android.backend.Tunnel;
import java.net.*;
import java.io.*;

/** Explicit opt-in first test build. A successful authenticated read is required before control. */
final class RemoteVpn {
 static volatile boolean requested,verified,up;
 static volatile String target="";
 static GoBackend backend;
 static final Tunnel tunnel=new Tunnel(){
  @Override public String getName(){return "onkyo-dahoam";}
  @Override public void onStateChange(State state){up=state==State.UP;if(!up)verified=false;}
 };
 static boolean selected(){return requested;}
 static Network network(Context c){
  if(!requested||!verified||!up)return null;
  return findNetwork(c,target);
 }
 static Network findNetwork(Context c,String ip){
  if(!up||!SceneService.privateIp(ip)||VpnService.prepare(c)!=null)return null;
  ConnectivityManager cm=c.getSystemService(ConnectivityManager.class);if(cm==null)return null;
  for(Network n:cm.getAllNetworks()){
   NetworkCapabilities cap=cm.getNetworkCapabilities(n);if(cap==null||!cap.hasTransport(NetworkCapabilities.TRANSPORT_VPN))continue;
   LinkProperties lp=cm.getLinkProperties(n);if(lp==null)continue;
   for(RouteInfo route:lp.getRoutes())if(route.getDestination().getPrefixLength()==32&&route.getDestination().getAddress().getHostAddress().equals(ip))return n;
  }
  return null;
 }
 static synchronized void start(Context c)throws Exception{
  Context app=c.getApplicationContext();
  if(VpnService.prepare(app)!=null)throw new IOException("Android-VPN-Berechtigung fehlt");
  RemoteConfig cfg=RemoteVault.load(app);
  requested=true;verified=false;target=cfg.ip;
  try{
   if(backend==null)backend=new GoBackend(app);
   backend.setState(tunnel,Tunnel.State.UP,cfg.config);
   Network n=null;
   for(int i=0;i<40&&n==null;i++){n=findNetwork(app,cfg.ip);if(n==null)Thread.sleep(150);}
   if(n==null)throw new IOException("VPN-Netzwerk nicht bereit");
   // No success just because the tunnel interface is up.
   try(Socket socket=n.getSocketFactory().createSocket()){
    socket.connect(new InetSocketAddress(cfg.ip,60128),8000);
    try(SceneExecutor.SocketWire wire=new SceneExecutor.SocketWire(socket,12000)){
     Capabilities profile=Capabilities.parse(wire.query("NRI"));
     if(profile==null||!profile.model.equalsIgnoreCase("TX-NR656"))throw new IOException("Onkyo-Modell nicht bestätigt");
     SceneExecutor.checkReceiver(wire);
    }
   }
   verified=true;
  }catch(Exception e){try{stop();}catch(Exception ignored){}throw e;}
 }
 static synchronized void stop()throws Exception{
  verified=false;
  try{if(backend!=null)backend.setState(tunnel,Tunnel.State.DOWN,null);}
  finally{requested=false;up=false;target="";}
 }
 static String status(Context c){
  if(network(c)!=null)return "Fernzugriff · Onkyo bestätigt";
  if(requested)return "Fernzugriff · nicht verbunden";
  return RemoteVault.exists(c)?"Eingerichtet · getrennt":"Noch nicht eingerichtet";
 }
}
