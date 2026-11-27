package at.local.dualzone;
import java.net.*;
import java.util.*;
public class DiscoveryTest {
 public static void main(String[] args)throws Exception{
  byte[] b=Discovery.packet("!1ECNTX-NR656/60128/XX/ABCDEF123456");
  Discovery.Device d=Discovery.parse(b,b.length,InetAddress.getByName("192.168.1.23"));
  if(d==null||!d.ip.equals("192.168.1.23")||!d.model.equals("TX-NR656")||!d.id.equals("ABCDEF123456")||d.port!=60128)throw new AssertionError();
  if(!Discovery.broadcast(InetAddress.getByName("192.168.17.23"),23).getHostAddress().equals("192.168.17.255"))throw new AssertionError();
  byte[] q=Discovery.packet("!xECNQSTN");if(q.length!=27||q[17]!='x')throw new AssertionError();
  byte[] bad=Discovery.packet("!1ECNTX-NR656/99999/XX");if(Discovery.parse(bad,bad.length,InetAddress.getLoopbackAddress())!=null)throw new AssertionError();
  if(!Discovery.known(d,"ABCDEF123456")||Discovery.known(d,"")||Discovery.known(d,"different"))throw new AssertionError("known identity");
  System.out.println("Discovery tests passed");
 }
}
