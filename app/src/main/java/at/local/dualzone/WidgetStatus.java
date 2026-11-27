package at.local.dualzone;
import java.text.SimpleDateFormat;
import java.util.*;
final class WidgetStatus {
 static final String UNKNOWN="unknown",REACHABLE="reachable",NOT_FOUND="not_found",NO_LAN="no_lan",UNREACHABLE="unreachable",NO_REPLY="no_reply",CHOOSE="choose",WRONG_MODEL="wrong_model",DISCOVERY_ERROR="discovery_error",PROFILE_UNKNOWN="profile_unknown";
 static final long FRESH_MS=120000;
 static final int NEUTRAL=0,GOOD=1,WARN=2,ERROR=3;
 static final class Line {final String text;final int tone;Line(String t,int c){text=t;tone=c;}}
 static String when(long at){return at<=0?"":new SimpleDateFormat("dd.MM. HH:mm",Locale.GERMANY).format(new Date(at));}
 static Line connection(boolean demo,boolean lan,String code,long at,boolean matches,long now,String phase){
  if(demo)return new Line(phase==null?"Demo · keine Receiver-Verbindung":phase,NEUTRAL);
  if(!lan)return new Line("Kein WLAN/LAN",ERROR);
  if(phase!=null&&!phase.isEmpty())return new Line(phase,NEUTRAL);
  if(!matches||at<=0||at>now+60000||code.equals(UNKNOWN)||code.equals(NO_LAN))return new Line("Onkyo ungeprüft · prüfen ↻",NEUTRAL);
  String label;
  switch(code){
   case REACHABLE:label="erreichbar";break;
   case NOT_FOUND:label="nicht gefunden";break;
   case UNREACHABLE:label="nicht erreichbar";break;
   case NO_REPLY:label="keine Statusantwort";break;
   case CHOOSE:label="Gerät auswählen";break;
   case WRONG_MODEL:label="falsches Gerät";break;
   case PROFILE_UNKNOWN:label="Geräteprofil unklar";break;
   case DISCOVERY_ERROR:label="Suche fehlgeschlagen";break;
   default:return new Line("Onkyo ungeprüft · prüfen ↻",NEUTRAL);
  }
  boolean fresh=now-at<=FRESH_MS;
  int color=fresh?(code.equals(REACHABLE)?GOOD:WARN):NEUTRAL;
  return new Line("Check "+when(at)+": "+label,color);
 }
 static Line compact(Line connection,Line execution){
  String c=connection.text,e=execution.text;
  if(c.equals("Kein WLAN/LAN")||c.startsWith("Offline"))return new Line("Offline",ERROR);
  if(c.startsWith("Kein Internet"))return new Line("Kein Internet",ERROR);
  if(c.contains("VPN-Freigabe"))return new Line("VPN erlauben",WARN);
  if(c.contains("Netzwerk gewechselt")||c.contains("neuer Versuch"))return new Line("Verb. fehlt",WARN);
  if(c.equals("Nicht verbunden"))return new Line("Verbinden",NEUTRAL);
  if(c.startsWith("Demo"))return new Line("Demo",NEUTRAL);
  if(c.contains("…"))return new Line("…",NEUTRAL);
  if(c.contains("nicht gefunden")||c.contains("nicht erreichbar"))return new Line("Onkyo offline",WARN);
  if(c.contains("keine Statusantwort")||c.contains("fehlgeschlagen")||c.contains("falsches Gerät")||c.contains("unklar"))return new Line("Fehler",WARN);
  if(c.contains("auswählen"))return new Line("Gerät wählen",WARN);
  if(e.contains("Zone ausgeschaltet"))return new Line("Zone aus",WARN);
  if(e.contains("Gemeinsame Medienquelle")||e.contains("Gemeinsamer FM/AM-Tuner")||e.contains("SOURCE folgt Wohnzimmer"))return new Line("Quelle geteilt",WARN);
  if(e.contains("Teilweise"))return new Line("Teilweise",WARN);
  if(e.contains("nicht bestätigt")||e.contains("Nicht bestätigt")||e.contains("Nicht ausgeführt")||e.contains("Zeitlimit"))return new Line("Fehler",WARN);
  if(c.startsWith("Onkyo ungeprüft"))return new Line("Prüfen",NEUTRAL);
  if(e.contains("Bestätigt · An")||e.contains("Bestätigt · Aus")){
   String time=e.matches("Lauf [0-9]{2}\\.[0-9]{2}\\. [0-9]{2}:[0-9]{2}:.*")?e.substring(12,17):"";
   // Last confirmed action, never a live claim; always include time if available.
   return new Line((e.contains("Bestätigt · An")?"An ":"Aus ")+time,connection.tone==GOOD?GOOD:NEUTRAL);
  }
  if(e.contains("Bestätigt"))return new Line("OK · letzter Lauf",connection.tone==GOOD?GOOD:NEUTRAL);
  return new Line("Bereit",NEUTRAL);
 }
 static Line execution(String message,long at,boolean pending,long pendingAt,boolean running){
  if(pending&&!running)return new Line("Versuch "+when(pendingAt)+": nicht bestätigt",WARN);
  if(message==null||message.isEmpty()||at<=0)return new Line("Noch kein Ausführungsergebnis",NEUTRAL);
  int tone=message.startsWith("Bestätigt")?GOOD:message.startsWith("Demo")?NEUTRAL:WARN;
  return new Line("Lauf "+when(at)+": "+message,tone);
 }
}
