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
 void query(){expire();if(!pollPending.compareAndSet(false,true))return;submit(true,new String[]{"PWRQSTN","ZPWQSTN","SLIQSTN","SLZQSTN","MVLQSTN","ZVLQSTN","NMSQSTN","AMTQSTN","ZMTQSTN"});}
 void send(String... commands){submit(false,commands);}
 private void submit(boolean poll,String[] commands){
  if(closed){if(poll)pollPending.set(false);return;}
  try{writer.execute(()->{
   try{Socket s=socket;if(s==null||!s.isConnected()||s.isClosed())throw new IOException();
    for(String c:commands){if(closed)return;byte[] frame=Protocol.encode(c);track(c);Diagnostics.command("TX",c);s.getOutputStream().write(frame);s.getOutputStream().flush();Thread.sleep(120);}
   }catch(Exception e){if(!closed){Diagnostics.error("TCP_WRITE_ERROR",e);listener.status("Senden fehlgeschlagen",false);close();}}finally{if(poll)pollPending.set(false);}
  });}catch(RejectedExecutionException e){if(poll)pollPending.set(false);if(!closed){Diagnostics.event("QUEUE","command queue full");listener.status("Zu viele Befehle – Verbindung wird erneuert",false);close();}}
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
 public void close(){if(closed)return;closed=true;try{if(socket!=null)socket.close();}catch(IOException ignored){}writer.shutdownNow();synchronized(this){for(Pending p:pending.values())Diagnostics.command("CONNECTION_CLOSED_PENDING",p.command);pending.clear();}Diagnostics.event("TCP","closed");}
}
