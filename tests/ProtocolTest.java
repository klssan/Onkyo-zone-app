package at.local.dualzone;
import java.io.*;
import java.util.Arrays;
public class ProtocolTest {
 static void eq(Object a,Object b){if(!a.equals(b))throw new AssertionError(a+" != "+b);}
 public static void main(String[] args)throws Exception{
 byte[] b=Protocol.encode("MVL28");eq(b.length,25);eq(Protocol.read(new DataInputStream(new ByteArrayInputStream(b))),"MVL28");
 ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(Protocol.encode("PWR01"));out.write(Protocol.encode("SLZ80"));DataInputStream in=new DataInputStream(new ByteArrayInputStream(out.toByteArray()));eq(Protocol.read(in),"PWR01");eq(Protocol.read(in),"SLZ80");
 eq(Protocol.volume("28"),40);eq(Protocol.volume("050"),-1);eq(Protocol.hex(40),"28");
 eq(Arrays.toString(Protocol.linked(35,25,5,50)),"[40, 30]");eq(Arrays.toString(Protocol.linked(35,25,90,50)),"[50, 40]");eq(Arrays.toString(Protocol.linked(35,25,-90,50)),"[10, 0]");
 try{Protocol.read(new DataInputStream(new ByteArrayInputStream(new byte[20])));throw new AssertionError();}catch(IOException expected){}
 System.out.println("Protocol tests passed");}
}
