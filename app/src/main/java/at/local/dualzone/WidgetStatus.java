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
 static Line execution(String message,long at,boolean pending,long pendingAt,boolean running){
  if(pending&&!running)return new Line("Versuch "+when(pendingAt)+": nicht bestätigt",WARN);
  if(message==null||message.isEmpty()||at<=0)return new Line("Noch kein Ausführungsergebnis",NEUTRAL);
  int tone=message.startsWith("Bestätigt")?GOOD:message.startsWith("Demo")?NEUTRAL:WARN;
  return new Line("Lauf "+when(at)+": "+message,tone);
 }
}
