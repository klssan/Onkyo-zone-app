package at.local.dualzone;
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
final class Discovery {
 static final class Device {
  final String ip,model,id;final int port;
  Device(String ip,String model,int port,String id){this.ip=ip;this.model=model;this.port=port;this.id=id;}
 }
 static byte[] packet(String payload){byte[] b=(payload+"\r\n").getBytes(StandardCharsets.US_ASCII);return ByteBuffer.allocate(16+b.length).putInt(0x49534350).putInt(16).putInt(b.length).put((byte)1).put(new byte[3]).put(b).array();}
 static Device parse(byte[] bytes,int length,InetAddress address)throws IOException{
  String message=Protocol.read(new DataInputStream(new ByteArrayInputStream(bytes,0,length)));
  if(!message.startsWith("ECN")||!(address instanceof Inet4Address))return null;
  String[] parts=message.substring(3).split("/",-1);
  if(parts.length<3||!parts[1].matches("[0-9]{5}")||!parts[0].matches("[A-Za-z0-9 -]{1,64}"))return null;
  int port=Integer.parseInt(parts[1]);if(port<1||port>65535)return null;
  String id=parts.length>3?parts[3].trim():"";
  return new Device(address.getHostAddress(),parts[0],port,id);
 }
 static InetAddress broadcast(InetAddress address,int prefix)throws UnknownHostException{
  if(!(address instanceof Inet4Address)||prefix<0||prefix>32)throw new IllegalArgumentException("IPv4 prefix");
  byte[] b=address.getAddress();int mask=prefix==0?0:-1<<(32-prefix);
  int ip=ByteBuffer.wrap(b).getInt();return InetAddress.getByAddress(ByteBuffer.allocate(4).putInt(ip|~mask).array());
 }
 static List<Device> find(DatagramSocket socket,List<InetAddress> targets,int timeout)throws IOException{
  socket.setBroadcast(true);Diagnostics.event("UDP","discovery broadcast");LinkedHashSet<InetAddress> all=new LinkedHashSet<>(targets);all.add(InetAddress.getByName("255.255.255.255"));
  byte[] query=packet("!xECNQSTN");int sent=0;IOException failure=null;
  for(InetAddress target:all)try{socket.send(new DatagramPacket(query,query.length,target,60128));sent++;}catch(IOException e){Diagnostics.error("UDP_SEND_ERROR",e);failure=e;}
  if(sent==0&&failure!=null)throw failure;
  LinkedHashMap<String,Device> devices=new LinkedHashMap<>();long until=System.nanoTime()+timeout*1000000L;
  while(System.nanoTime()<until){int remaining=(int)((until-System.nanoTime())/1000000L);socket.setSoTimeout(Math.max(1,remaining));byte[] b=new byte[2048];DatagramPacket reply=new DatagramPacket(b,b.length);
   try{socket.receive(reply);try{Device d=parse(b,reply.getLength(),reply.getAddress());if(d!=null){devices.put(d.ip,d);Diagnostics.event("UDP_RX","valid ECN response modelMatch="+d.model.equalsIgnoreCase("TX-NR656")+" port="+d.port);}else Diagnostics.event("UDP_RX","ignored response");}catch(IOException|RuntimeException e){Diagnostics.error("UDP_PARSE_ERROR",e);}}catch(SocketTimeoutException e){break;}
  }Diagnostics.event("UDP","discovery end responses="+devices.size());return new ArrayList<>(devices.values());
 }
}
