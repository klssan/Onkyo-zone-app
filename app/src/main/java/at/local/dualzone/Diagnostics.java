package at.local.dualzone;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
final class Diagnostics {
 private static final int LIMIT=128*1024;
 static final long MAX_AGE_MS=72L*60*60*1000;
 private static final long MIN_FREE_BYTES=16L*1024*1024;
 private static long lastPrune;
 private static File current,previous;
 private static long seq,epoch=System.nanoTime();
 private static final Set<String> SAFE=new HashSet<>(Arrays.asList("PWR","ZPW","SLI","SLZ","MVL","ZVL","AMT","ZMT","LMD","TFR","CTL","SWL","ZTN","ZBL","PRS","PRZ","TUN","TUZ","NSV","NMS","NRI"));
 static synchronized void open(File dir){if(current==null){current=new File(dir,"diagnostics.log");previous=new File(dir,"diagnostics.previous.log");}try{prune(System.currentTimeMillis());}catch(IOException ignored){}}
 // Files are expired by the FIRST entry, not modification time. A continuously
 // appended file therefore cannot retain old entries indefinitely.
 static synchronized void prune(long now)throws IOException{
  if(current==null)return;lastPrune=now;
  if(current.getParentFile().getUsableSpace()<MIN_FREE_BYTES){clear();return;}
  for(File f:new File[]{current,previous}){
   if(!f.isFile())continue;
   if(f.length()>LIMIT){Files.deleteIfExists(f.toPath());continue;}
   long oldest=0;
   try(BufferedReader in=new BufferedReader(new InputStreamReader(new FileInputStream(f),StandardCharsets.UTF_8))){
    String line=in.readLine();if(line!=null){int split=line.indexOf(' ');if(split>0)oldest=Instant.parse(line.substring(0,split)).toEpochMilli();}
   }catch(java.time.format.DateTimeParseException ignored){}
   if(oldest<=0||oldest>now+60000||now-oldest>=MAX_AGE_MS)Files.deleteIfExists(f.toPath());
  }
 }
 static boolean due(long now){return now<lastPrune||now-lastPrune>=60000;}
 static synchronized void event(String tag,String data){
  if(current==null)return;
  try{long now=System.currentTimeMillis();if(due(now))prune(now);if(current.getParentFile().getUsableSpace()<MIN_FREE_BYTES){clear();return;}}catch(IOException ignored){return;}
  String clean=data.replace('\n',' ').replace('\r',' ');if(clean.length()>4096)clean=clean.substring(0,4096);
  String line=Instant.now()+" #"+(++seq)+" +"+((System.nanoTime()-epoch)/1000000)+"ms "+tag+" "+clean+"\n";
  byte[] bytes=line.getBytes(StandardCharsets.UTF_8);
  try{if(current.length()+bytes.length>LIMIT){Files.deleteIfExists(previous.toPath());Files.move(current.toPath(),previous.toPath());}try(FileOutputStream out=new FileOutputStream(current,true)){out.write(bytes);}}catch(IOException ignored){}
 }
 static String wire(String s){
  if(s==null||s.length()<3)return "short frame";
  String k=s.substring(0,3),v=s.substring(3);
  if(!SAFE.contains(k))return "other frame length="+s.length();
  if(k.equals("NRI"))return v.equals("QSTN")?"NRIQSTN":"NRI payload length="+v.length();
  if(k.equals("NSV"))return v.matches("[0-9A-Fa-f]{2}0")?k+v:"NSV payload omitted";
  if(k.equals("NMS"))return v.matches("[A-Za-z0-9]{9}")?k+v:"NMS payload omitted length="+v.length();
  if(v.matches("QSTN|N/A|UP|DOWN|UP1|DOWN1|BUP|BDOWN|TUP|TDOWN|MOVIE|MUSIC|GAME|[0-9A-Fa-f+\\-]{1,6}|B[0-9A-Fa-f+\\-]{2}T[0-9A-Fa-f+\\-]{2}"))return k+v;
  return k+" payload omitted length="+v.length();
 }
 static void command(String tag,String s){event(tag,wire(s));}
 static void error(String tag,Throwable t){event(tag,t.getClass().getSimpleName());}
 static synchronized String snapshot()throws IOException{
  prune(System.currentTimeMillis());
  StringBuilder b=new StringBuilder("DualZone Remote diagnostic format 2\nLocal logs; newest entries last. IP/MAC, SSID, credentials, custom names, media metadata and raw XML are omitted. Times and listening actions remain visible. Correlation is observational, not an ISCP transaction ID.\n");
  if(previous!=null&&previous.isFile())b.append(new String(Files.readAllBytes(previous.toPath()),StandardCharsets.UTF_8));
  if(current!=null&&current.isFile())b.append(new String(Files.readAllBytes(current.toPath()),StandardCharsets.UTF_8));return b.toString();
 }
 static synchronized void clear()throws IOException{if(previous!=null)Files.deleteIfExists(previous.toPath());if(current!=null)Files.deleteIfExists(current.toPath());}
}
