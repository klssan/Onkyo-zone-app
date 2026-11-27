package at.local.dualzone;

import com.wireguard.config.*;
import java.net.InetAddress;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Only a single private receiver host route. No DNS or default route; only this app. */
final class RemoteConfig {
 final String ip;
 final Config config;
 RemoteConfig(String ip,Config config){this.ip=ip;this.config=config;}
 static RemoteConfig restrict(InputStream input,String target,String packageName)throws Exception {
  if(!SceneService.privateIp(target))throw new IOException("Private Onkyo-IP erforderlich");
  Config raw=Config.parse(input);
  if(raw.getPeers().size()!=1||raw.getInterface().getAddresses().isEmpty())throw new IOException("Einzelgerät-Konfiguration erforderlich");
  Peer peer=raw.getPeers().get(0);
  if(!peer.getEndpoint().isPresent())throw new IOException("FRITZ!Box-Endpunkt fehlt");
  byte[] host=InetAddress.getByName(target).getAddress();boolean covered=false;
  for(InetNetwork route:peer.getAllowedIps()){
   byte[] address=route.getAddress().getAddress();
   if(address.length!=4)continue;
   int bits=route.getMask();boolean equal=true;
   for(int i=0;i<bits;i++)if(((host[i/8]^address[i/8])&(0x80>>(i%8)))!=0){equal=false;break;}
   if(equal)covered=true;
  }
  if(!covered)throw new IOException("Onkyo-IP liegt außerhalb der VPN-Freigabe");
  com.wireguard.config.Interface.Builder ib=new com.wireguard.config.Interface.Builder()
   .setKeyPair(raw.getInterface().getKeyPair()).addAddresses(raw.getInterface().getAddresses()).includeApplication(packageName);
  if(raw.getInterface().getMtu().isPresent())ib.setMtu(raw.getInterface().getMtu().get());
  Peer.Builder pb=new Peer.Builder().setPublicKey(peer.getPublicKey()).setEndpoint(peer.getEndpoint().get()).addAllowedIp(InetNetwork.parse(target+"/32"));
  if(peer.getPreSharedKey().isPresent())pb.setPreSharedKey(peer.getPreSharedKey().get());
  // Avoid unnecessary keepalive traffic. Handshake starts on the first receiver probe.
  return new RemoteConfig(target,new Config.Builder().setInterface(ib.build()).addPeer(pb.build()).build());
 }
 byte[] encode(){return (ip+"\n"+config.toWgQuickString()).getBytes(StandardCharsets.UTF_8);}
 static RemoteConfig decode(byte[] bytes,String packageName)throws Exception{
  String text=new String(bytes,StandardCharsets.UTF_8);int n=text.indexOf('\n');
  if(n<1)throw new IOException("Konfiguration fehlt");
  return restrict(new ByteArrayInputStream(text.substring(n+1).getBytes(StandardCharsets.UTF_8)),text.substring(0,n),packageName);
 }
}
