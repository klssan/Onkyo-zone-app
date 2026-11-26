package at.local.dualzone;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
public final class Protocol {
 public static byte[] encode(String command) {
  if (!command.matches("[A-Z0-9?.+-]+")&&!command.equals("NTCP/P")&&!command.equals("NTZP/P")) throw new IllegalArgumentException("Command");
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
 public static int volume(String s){if(!s.matches("[0-9A-Fa-f]{2,3}"))return -1;int n=Integer.parseInt(s,16);return n<=200?n:-1;}
 // ISCP uses raw ticks: scale 1 for whole steps, scale 2 for half steps.
 public static int maxVolume(int reported,int scale,int observed){
  int ceiling=reported>0&&reported<=100*scale?reported:100*scale;
  return Math.max(ceiling,Math.min(100*scale,Math.max(0,observed)));
 }
 public static String volumeLabel(int raw,int scale){
  if(raw<0)return "–";return scale==2?(raw%2==0?""+(raw/2):(raw/2)+",5"):""+raw;
 }
 public static boolean volumeMatches(String a,String b){int x=volume(a);return x>=0&&x==volume(b);}
 public static int[] linkedScaled(int a,int b,int scaleA,int scaleB,int maxA,int maxB,int deltaHalf){
  if(a<0||b<0||a>maxA||b>maxB||scaleA<1||scaleA>2||scaleB<1||scaleB>2)throw new IllegalArgumentException("Volume");
  int fa=2/scaleA,fb=2/scaleB,quantum=Math.max(fa,fb);
  int lo=-Math.min(a*fa,b*fb),hi=Math.min((maxA-a)*fa,(maxB-b)*fb);
  int low=-((-lo)/quantum)*quantum,high=(hi/quantum)*quantum;
  int d=Math.max(low,Math.min(high,(int)Math.round((double)deltaHalf/quantum)*quantum));
  return new int[]{a+d/fa,b+d/fb};
 }
 public static int[] linked(int a,int b,int delta,int cap){
  if(a<0||b<0||a>cap||b>cap)throw new IllegalArgumentException("Volume");
  int d=Math.max(-Math.min(a,b),Math.min(cap-Math.max(a,b),delta));return new int[]{a+d,b+d};
 }
}
