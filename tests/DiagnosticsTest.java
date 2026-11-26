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
  long now=System.currentTimeMillis();
  String old=java.time.Instant.ofEpochMilli(now-Diagnostics.MAX_AGE_MS-1000)+" #1 old entry\n";
  String recent=java.time.Instant.ofEpochMilli(now-1000)+" #2 recent entry\n";
  Files.writeString(dir.resolve("diagnostics.log"),old+recent);
  Files.writeString(dir.resolve("diagnostics.previous.log"),recent);
  Diagnostics.prune(now);
  if(Files.exists(dir.resolve("diagnostics.log"))||!Files.exists(dir.resolve("diagnostics.previous.log")))throw new AssertionError("retention based on oldest entry");
  Files.writeString(dir.resolve("diagnostics.log"),"invalid old log");
  Diagnostics.prune(now);
  if(Files.exists(dir.resolve("diagnostics.log")))throw new AssertionError("invalid log retention");
  Files.write(dir.resolve("diagnostics.log"),new byte[131073]);
  Diagnostics.prune(now);
  if(Files.exists(dir.resolve("diagnostics.log")))throw new AssertionError("oversized log");
  Diagnostics.clear();
  System.out.println("Diagnostics privacy, rotation, expiry, size and clearing passed");
 }
}
