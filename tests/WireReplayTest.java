package at.local.dualzone;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public class WireReplayTest {
 static byte[] incoming(String text){byte[] p=("!1"+text+"\u001a\r\n").getBytes(StandardCharsets.UTF_8);return ByteBuffer.allocate(16+p.length).putInt(0x49534350).putInt(16).putInt(p.length).put((byte)1).put(new byte[3]).put(p).array();}
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  byte[] data=incoming("MVL28");check(Protocol.read(new DataInputStream(new ByteArrayInputStream(data))).equals("MVL28"));
  byte[] data2=incoming("SLZ80");ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(data);out.write(data2);
  InputStream fragmented=new FilterInputStream(new ByteArrayInputStream(out.toByteArray())){public int read(byte[] b,int o,int n)throws IOException{return super.read(b,o,Math.min(1,n));}};
  DataInputStream in=new DataInputStream(fragmented);check(Protocol.read(in).equals("MVL28"));check(Protocol.read(in).equals("SLZ80"));
  try{Protocol.read(new DataInputStream(new ByteArrayInputStream(Arrays.copyOf(data,20))));throw new AssertionError();}catch(EOFException ok){}
  byte[] discovery=incoming("ECNTX-NR656/60128/XX/ABCDEF123456");check(Discovery.parse(discovery,discovery.length,InetAddress.getByName("192.168.1.2")).model.equals("TX-NR656"));
  for(int a=0;a<=50;a++)for(int b=0;b<=50;b++)for(int d=-55;d<=55;d++){int[] v=Protocol.linked(a,b,d,50);check(v[0]>=0&&v[0]<=50&&v[1]>=0&&v[1]<=50&&v[0]-v[1]==a-b);}
  System.out.println("Independent EOF frames, fragmented TCP, truncated frames, discovery reply, 288711 linked-volume cases passed");
 }
}
