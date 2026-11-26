package at.local.dualzone;
import java.util.*;
/** Immutable, validated scene. Empty source/service and -1 volume/preset mean unchanged. */
final class Scene {
 static final int KEEP=0,ON=1,OFF=2,CAP=50;
 static final String[] SOURCE_CODES={"","10","01","02","11","05","03","12","23","22","24","25","2B","2E","80"};
 static final String[] SOURCE_NAMES={"Unverändert","BD/DVD","CBL/SAT","GAME","STRM BOX","PC","AUX","TV","CD","PHONO","FM","AM","NET","Bluetooth","SOURCE"};
 static final String[] NET_CODES={"","0E","0A","00","F0","12","1B"};
 static final String[] NET_NAMES={"Unverändert","TuneIn Radio","Spotify","Music Server","USB","Deezer","TIDAL"};
 static final class Zone {
  final int power,volume,preset;final String source,service;
  Zone(int p,String s,int v,int r,String n){power=p;source=s;volume=v;preset=r;service=n;}
  boolean changes(){return power!=KEEP||!source.isEmpty()||volume>=0||preset>=1||!service.isEmpty();}
  boolean settings(){return !source.isEmpty()||volume>=0||preset>=1||!service.isEmpty();}
 }
 final String name,icon;final Zone[] zones;final boolean share,demo;
 Scene(String name,String icon,Zone main,Zone second,boolean share,boolean demo){this.name=name;this.icon=icon;zones=new Zone[]{main,second};this.share=share;this.demo=demo;validate();}
 void validate(){
  if(name.trim().isEmpty()||name.length()>40||!Arrays.asList("Radio","Musik","TV","Haus","Power").contains(icon))throw new IllegalArgumentException("Name oder Symbol ungültig");
  if(!zones[0].changes()&&!zones[1].changes())throw new IllegalArgumentException("Mindestens eine Einstellung wählen");
  for(int i=0;i<2;i++){Zone z=zones[i];if(z.power<KEEP||z.power>OFF||z.volume < -1||z.volume>CAP||z.preset< -1||z.preset==0||z.preset>40||!Arrays.asList(SOURCE_CODES).contains(z.source)||!Arrays.asList(NET_CODES).contains(z.service))throw new IllegalArgumentException("Ungültige Einstellung");
   if(i==0&&z.source.equals("80")||i==1&&z.source.equals("05"))throw new IllegalArgumentException("Quelle in dieser Zone nicht verfügbar");
   if(z.power==OFF&&z.settings())throw new IllegalArgumentException("Bei Ausschalten keine weiteren Einstellungen wählen");
   if(z.preset>=1&&!z.source.equals("24")&&!z.source.equals("25"))throw new IllegalArgumentException("Speicherplatz benötigt FM oder AM");
   if(!z.service.isEmpty()&&!z.source.equals("2B"))throw new IllegalArgumentException("NET-Dienst benötigt NET");
  }
  Zone a=zones[0],b=zones[1];if(a.source.equals("24")&&b.source.equals("25")||a.source.equals("25")&&b.source.equals("24"))throw new IllegalArgumentException("Nur ein gemeinsamer FM/AM-Tuner");if(a.preset>=1&&b.preset>=1&&(a.preset!=b.preset||!a.source.equals(b.source)))throw new IllegalArgumentException("Nur ein gemeinsamer Radiosender");
  if(!a.service.isEmpty()&&!b.service.isEmpty()&&!a.service.equals(b.service))throw new IllegalArgumentException("Nur ein gemeinsamer NET-Dienst");
 }
 String summary(){ArrayList<String> items=new ArrayList<>();for(int i=0;i<2;i++){Zone z=zones[i];if(!z.changes())continue;String x=i==0?"Haupt":"Zone 2";if(z.power==ON)x+=" · An";if(z.power==OFF)x+=" · Aus";if(!z.source.isEmpty())x+=" · "+SOURCE_NAMES[Arrays.asList(SOURCE_CODES).indexOf(z.source)];if(z.preset>0)x+=" "+z.preset;if(!z.service.isEmpty())x+=" · "+NET_NAMES[Arrays.asList(NET_CODES).indexOf(z.service)];if(z.volume>=0)x+=" · "+z.volume;items.add(x);}return String.join(" / ",items);}
}
