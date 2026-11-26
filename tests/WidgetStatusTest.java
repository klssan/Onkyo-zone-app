package at.local.dualzone;
import java.io.*;
import java.util.*;
public class WidgetStatusTest {
 static void check(boolean yes){if(!yes)throw new AssertionError();}
 static WidgetStatus.Line connection(boolean demo,boolean lan,String code,long at,boolean match,long now,String phase){return WidgetStatus.connection(demo,lan,code,at,match,now,phase);}
 public static void main(String[] args)throws Exception{
  long now=1700000000000L;
  check(connection(false,false,WidgetStatus.REACHABLE,now,true,now,null).text.equals("Kein WLAN/LAN"));
  check(connection(false,false,WidgetStatus.REACHABLE,now,true,now,"Suche").tone==WidgetStatus.ERROR);
  check(connection(true,false,WidgetStatus.REACHABLE,now,true,now,null).text.startsWith("Demo"));
  check(connection(false,true,WidgetStatus.UNKNOWN,0,false,now,null).text.contains("ungeprüft"));
  check(connection(false,true,WidgetStatus.REACHABLE,now,false,now,null).text.contains("ungeprüft"));
  check(connection(false,true,WidgetStatus.REACHABLE,now,true,now,null).tone==WidgetStatus.GOOD);
  check(connection(false,true,WidgetStatus.REACHABLE,now-120001,true,now,null).tone==WidgetStatus.NEUTRAL);
  check(connection(false,true,WidgetStatus.REACHABLE,now-120001,true,now,null).text.startsWith("Check "));
  check(connection(false,true,WidgetStatus.NO_REPLY,now,true,now,null).text.contains("keine Statusantwort"));
  check(connection(false,true,WidgetStatus.NOT_FOUND,now,true,now,null).tone==WidgetStatus.WARN);
  check(connection(false,true,WidgetStatus.REACHABLE,now,true,now,"Verbindung wird aufgebaut").text.equals("Verbindung wird aufgebaut"));
  check(WidgetStatus.execution("Bestätigt",now,false,0,false).tone==WidgetStatus.GOOD);
  check(WidgetStatus.execution("Bestätigt",now,true,now,false).text.contains("nicht bestätigt"));
  check(WidgetStatus.execution("Bestätigt",now,true,now,true).text.contains("Bestätigt"));
  check(WidgetStatus.execution("Teilweise (2)",now,false,0,false).tone==WidgetStatus.WARN);
  check(WidgetStatus.execution("",0,false,0,false).text.contains("kein Ausführungsergebnis"));
  Scene scene=new Scene("FM4","Radio",new Scene.Zone(Scene.KEEP,"",-1,-1,""),new Scene.Zone(Scene.ON,"24",40,4,""),false,false);
  check(scene.zoneSummary(0).equals("Unverändert"));
  check(scene.zoneSummary(1).equals("An · FM P04 · Vol. 40"));
  List<String> seen=new ArrayList<>();SceneExecutor.checkReceiver(new SceneExecutor.Wire(){
   public String query(String prefix){seen.add(prefix);return "00";}
   public void set(String command,String prefix,String value){throw new AssertionError("Read-only probe wrote a command");}
  });check(seen.equals(Arrays.asList("PWR")));
  System.out.println("Widget status, stale observations, offline, interrupted scenes, readable actions and read-only probe passed");
 }
}
