package at.local.dualzone;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
public class ReceiverTest {
 public static void main(String[] args)throws Exception{
 if(!java.util.Arrays.equals(Receiver.dashboardQueries(false,false,"24","24"),new String[]{"PWRQSTN","ZPWQSTN"}))throw new AssertionError("standby polling");
 java.util.List<String> fm=java.util.Arrays.asList(Receiver.dashboardQueries(false,true,"24","24"));
 if(fm.contains("MVLQSTN")||fm.contains("NMSQSTN")||!fm.contains("ZVLQSTN")||!fm.contains("TUZQSTN"))throw new AssertionError("FM polling");
 java.util.List<String> follow=java.util.Arrays.asList(Receiver.dashboardQueries(false,true,"2B","80"));
 if(!follow.contains("SLIQSTN")||!follow.contains("NMSQSTN")||follow.contains("MVLQSTN"))throw new AssertionError("SOURCE polling");
 try(ServerSocket ss=new ServerSocket(60128,1,InetAddress.getByName("127.0.0.1"))){
 ExecutorService server=Executors.newSingleThreadExecutor();
 Future<?> f=server.submit(()->{try(Socket s=ss.accept()){
 s.setSoTimeout(5000);DataInputStream in=new DataInputStream(s.getInputStream());
 String[] expected={"PWRQSTN","ZPWQSTN","MVL28","ZVL1E"};
 String[] replies={"PWR01","ZPW01","MVL28","ZVL1E"};
 for(int i=0;i<expected.length;i++){String c=Protocol.read(in);if(!expected[i].equals(c))throw new AssertionError(c);s.getOutputStream().write(Protocol.encode(replies[i]));s.getOutputStream().flush();}
 }catch(Exception ex){throw new RuntimeException(ex);}});
 CountDownLatch ready=new CountDownLatch(1),result=new CountDownLatch(1);
 try(Receiver r=new Receiver("127.0.0.1",new Receiver.Listener(){public void status(String s,boolean online){}public void message(String m){if(m.equals("ZPW01"))ready.countDown();if(m.equals("ZVL1E"))result.countDown();}})){
 if(!ready.await(6,TimeUnit.SECONDS))throw new AssertionError("No initial state");r.send("MVL28","ZVL1E");if(!result.await(6,TimeUnit.SECONDS))throw new AssertionError("No linked reply");f.get(5,TimeUnit.SECONDS);
 }finally{server.shutdownNow();}
 }
 System.out.println("Receiver TCP integration test passed");
 }
}
