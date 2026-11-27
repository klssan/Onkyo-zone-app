package at.local.dualzone;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
final class Receiver implements AutoCloseable {
 interface Listener {void message(String message);void status(String status,boolean online);}
 private final Listener listener;
 private final ThreadPoolExecutor writer=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(24));
 private final AtomicBoolean pollPending=new AtomicBoolean();
 private final Map<String,Pending> pending=new HashMap<>();
 private final Map<String,String> latestVolumes=new HashMap<>();
 private final Set<String> queuedVolumes=new HashSet<>();
 private volatile Socket socket;
 private volatile boolean closed;
 private static final class Pending {final String command;final long time;Pending(String command){this.command=command;time=System.nanoTime();}}
 Receiver(String ip, Listener l){this(ip,javax.net.SocketFactory.getDefault(),l);}
 Receiver(String ip,javax.net.SocketFactory factory,Listener l){
  listener=l;
  new Thread(()->{
   boolean opened=false;long start=System.nanoTime();
   try(Socket s=factory.createSocket()){
    socket=s;if(closed)return;
    s.connect(new InetSocketAddress(ip,60128),3000);s.setTcpNoDelay(true);s.setSoTimeout(15000);
    if(closed)return;opened=true;Diagnostics.event("TCP","connected ms="+((System.nanoTime()-start)/1000000));
    listener.status("Verbunden – Status wird gelesen",true);query();
    DataInputStream in=new DataInputStream(new BufferedInputStream(s.getInputStream()));
    while(!closed){String msg=Protocol.read(in);if(!msg.isEmpty()){Diagnostics.command("RX",msg);observe(msg);listener.message(msg);}else Diagnostics.event("RX","ignored non-receiver frame");}
   }catch(Exception e){if(!closed){Diagnostics.error(opened?"TCP_READ_ERROR":"TCP_CONNECT_ERROR",e);listener.status("Receiver nicht erreichbar",false);}}
   finally{close();}
  },"eISCP-reader").start();
 }
 void query(){query("PWRQSTN","ZPWQSTN");}
 void query(String... commands){expire();if(!pollPending.compareAndSet(false,true))return;submit(true,commands);}
 static String[] dashboardQueries(Boolean main,Boolean second,String a,String b){
  LinkedHashSet<String> q=new LinkedHashSet<>();q.add("PWRQSTN");q.add("ZPWQSTN");
  boolean m=Boolean.TRUE.equals(main),z=Boolean.TRUE.equals(second);
  if(m||z&&"80".equals(b))q.add("SLIQSTN");
  if(z)q.add("SLZQSTN");
  if(m){q.add("MVLQSTN");q.add("AMTQSTN");}
  if(z){q.add("ZVLQSTN");q.add("ZMTQSTN");}
  if(m&&"2B".equals(a)||z&&("2B".equals(b)||"80".equals(b)&&"2B".equals(a)))q.add("NMSQSTN");
  if(m&&("24".equals(a)||"25".equals(a))||z&&"80".equals(b)&&("24".equals(a)||"25".equals(a))){q.add("PRSQSTN");q.add("TUNQSTN");}
  if(z&&("24".equals(b)||"25".equals(b))){q.add("PRZQSTN");q.add("TUZQSTN");}
  return q.toArray(new String[0]);
 }
 void send(String... commands){submit(false,commands);}
 // At most one queued volume write per zone; a running write may have one successor.
 synchronized void sendVolume(String command){
  if(closed||!command.matches("(MVL|ZVL)[0-9A-F]{2,3}"))return;
  String key=command.substring(0,3);latestVolumes.put(key,command);if(!queuedVolumes.add(key))return;
  try{writer.execute(()->{String newest;synchronized(this){newest=latestVolumes.remove(key);queuedVolumes.remove(key);}if(newest!=null)writeCommands(false,new String[]{newest});});}
  catch(RejectedExecutionException e){queuedVolumes.remove(key);latestVolumes.remove(key);if(!closed){Diagnostics.event("QUEUE","volume queue full");listener.status("Zu viele Befehle – Verbindung wird erneuert",false);close();}}
 }
 private void submit(boolean poll,String[] commands){
  if(closed){if(poll)pollPending.set(false);return;}
  try{writer.execute(()->writeCommands(poll,commands));}catch(RejectedExecutionException e){if(poll)pollPending.set(false);if(!closed){Diagnostics.event("QUEUE","command queue full");listener.status("Zu viele Befehle – Verbindung wird erneuert",false);close();}}
 }
 private void writeCommands(boolean poll,String[] commands){
   try{Socket s=socket;if(s==null||!s.isConnected()||s.isClosed())throw new IOException();
    int index=0;for(String c:commands){if(closed)return;if(poll&&index++>=2&&!writer.getQueue().isEmpty()){Diagnostics.event("QUEUE","yield poll to queued action");break;}byte[] frame=Protocol.encode(c);track(c);Diagnostics.command("TX",c);s.getOutputStream().write(frame);s.getOutputStream().flush();Thread.sleep(120);}
   }catch(Exception e){if(!closed){Diagnostics.error("TCP_WRITE_ERROR",e);listener.status("Senden fehlgeschlagen",false);close();}}finally{if(poll)pollPending.set(false);}
 }
 private synchronized void track(String c){expire();if(c.length()<4)return;String k=c.substring(0,3);if(!c.endsWith("QSTN")||!pending.containsKey(k))pending.put(k,new Pending(c));}
 private synchronized void observe(String msg){
  expire();if(msg.length()<4)return;String k=msg.substring(0,3);Pending p=pending.get(k);if(p==null)return;
  String param=p.command.substring(3),value=msg.substring(3);
  boolean query=param.equals("QSTN"),vol=k.equals("MVL")||k.equals("ZVL"),absolute=param.matches("[0-9A-Fa-f]{2}")||vol&&param.matches("[0-9A-Fa-f]{3}"),na=value.equals("N/A");
  if(!query&&absolute&&!param.equalsIgnoreCase(value)&&!(vol&&Protocol.volumeMatches(param,value))&&!na)return;
  pending.remove(k);
  Diagnostics.event(na?"UNAVAILABLE":"OBSERVED",Diagnostics.wire(p.command)+" -> "+Diagnostics.wire(msg)+" ms="+((System.nanoTime()-p.time)/1000000));
 }
 private synchronized void expire(){long now=System.nanoTime();Iterator<Map.Entry<String,Pending>> it=pending.entrySet().iterator();while(it.hasNext()){Pending p=it.next().getValue();if(now-p.time>5000000000L){Diagnostics.command("NO_MATCHING_REPLY",p.command);it.remove();}}}
 public void close(){if(closed)return;closed=true;try{if(socket!=null)socket.close();}catch(IOException ignored){}writer.shutdownNow();synchronized(this){for(Pending p:pending.values())Diagnostics.command("CONNECTION_CLOSED_PENDING",p.command);pending.clear();latestVolumes.clear();queuedVolumes.clear();}Diagnostics.event("TCP","closed");}
}
