package at.local.dualzone;
import java.nio.file.*;
public class DiagnosticsTest {
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("onkyo-log");Diagnostics.open(dir.toFile());
  Diagnostics.command("TX","MVL28");Diagnostics.command("RX","NRI<macaddress>PRIVATE123</macaddress>");Diagnostics.command("TX","NSV0A1PASSWORD");Diagnostics.command("RX","NTIPrivateSong");
  String log=Diagnostics.snapshot();if(!log.contains("MVL28")||log.contains("PRIVATE123")||log.contains("PASSWORD")||log.contains("PrivateSong"))throw new AssertionError("privacy");
  for(int i=0;i<300;i++)Diagnostics.event("TEST","x".repeat(1000));
  if(Files.size(dir.resolve("diagnostics.log"))>131072||Files.size(dir.resolve("diagnostics.previous.log"))>131072)throw new AssertionError("rotation");
  Diagnostics.clear();if(Files.exists(dir.resolve("diagnostics.log")))throw new AssertionError("clear");
  System.out.println("Diagnostics privacy, rotation and clearing passed");
 }
}
