package at.local.dualzone;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
final class Receiver implements AutoCloseable {
 interface Listener {void message(String message);void status(String status,boolean online);}
 private final Listener listener;
 private final ExecutorService writer=Executors.newSingleThreadExecutor();
 private volatile Socket socket;
 private volatile boolean closed;
 Receiver(String ip, Listener l){this(ip,javax.net.SocketFactory.getDefault(),l);}
 Receiver(String ip,javax.net.SocketFactory factory,Listener l){
  listener=l;
  new Thread(()->{
   try(Socket s=factory.createSocket()){
    socket=s;if(closed)return;
    s.connect(new InetSocketAddress(ip,60128),3000);s.setTcpNoDelay(true);s.setSoTimeout(15000);
    if(closed)return;
    listener.status("Verbunden – Status wird gelesen",true);query();
    DataInputStream in=new DataInputStream(new BufferedInputStream(s.getInputStream()));
    while(!closed){String msg=Protocol.read(in);if(!msg.isEmpty())listener.message(msg);}
   }catch(Exception e){if(!closed)listener.status("Receiver nicht erreichbar",false);}
   finally{close();}
  },"eISCP-reader").start();
 }
 void query(){send("PWRQSTN","ZPWQSTN","SLIQSTN","SLZQSTN","MVLQSTN","ZVLQSTN","NMSQSTN","AMTQSTN","ZMTQSTN");}
 void send(String... commands){
  if(closed)return;
  try{writer.execute(()->{
   try{Socket s=socket;if(s==null||!s.isConnected()||s.isClosed())throw new IOException();
    for(String c:commands){if(closed)return;s.getOutputStream().write(Protocol.encode(c));s.getOutputStream().flush();Thread.sleep(120);}
   }catch(Exception e){if(!closed){listener.status("Senden fehlgeschlagen",false);close();}}
  });}catch(RejectedExecutionException ignored){}
 }
 public void close(){closed=true;try{if(socket!=null)socket.close();}catch(IOException ignored){}writer.shutdownNow();}
}
