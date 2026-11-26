package at.local.dualzone;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
public final class Protocol {
 public static byte[] encode(String command) {
  if (!command.matches("[A-Z0-9?.+-]+")) throw new IllegalArgumentException("Command");
  byte[] payload=("!1"+command+"\r\n").getBytes(StandardCharsets.US_ASCII);
  return ByteBuffer.allocate(16+payload.length).putInt(0x49534350).putInt(16).putInt(payload.length).put((byte)1).put(new byte[3]).put(payload).array();
 }
 public static String read(DataInputStream in)throws IOException {
  if(in.readInt()!=0x49534350)throw new IOException("Invalid ISCP magic");
  int h=in.readInt(), n=in.readInt(), v=in.readUnsignedByte();
  if(h<16||h>256||n<2||n>2000000||v!=1)throw new IOException("Invalid ISCP header");
  byte[] extra=new byte[h-13];in.readFully(extra);
  byte[] data=new byte[n];in.readFully(data);
  String s=new String(data,StandardCharsets.UTF_8);
  return s.startsWith("!1")?s.substring(2).replaceAll("[\\x00\\x1a\\r\\n]+$",""):"";
 }
 public static String hex(int n){return String.format(Locale.ROOT,"%02X",n);}
 public static int volume(String s){if(!s.matches("[0-9A-Fa-f]{2}"))return -1;int n=Integer.parseInt(s,16);return n<=100?n:-1;}
 public static int[] linked(int a,int b,int delta,int cap){
  if(a<0||b<0||a>cap||b>cap)throw new IllegalArgumentException("Volume");
  int d=Math.max(-Math.min(a,b),Math.min(cap-Math.max(a,b),delta));return new int[]{a+d,b+d};
 }
}
