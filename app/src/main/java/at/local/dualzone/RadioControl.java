package at.local.dualzone;
import java.io.*;
import java.util.*;
final class RadioControl {
 static final int START_VOLUME=45;
 static final String[] POWER={"PWR","ZPW"},SOURCE={"SLI","SLZ"},VOLUME={"MVL","ZVL"},PRESET={"PRS","PRZ"},TUNE={"TUN","TUZ"};
 static final class State {
  final boolean[] on=new boolean[2];final String[] source={"",""};final int[] raw={-1,-1},scale={1,1},max={100,100};int preset=-1;String frequency="";
  String effective(int i){return i==1&&source[1].equals("80")?source[0]:source[i];}
  boolean fm(int i){return on[i]&&effective(i).equals("24");}
  int tuner(){return fm(0)||fm(1)&&source[1].equals("80")?0:fm(1)?1:-1;}
  String station(Capabilities c){
   if(tuner()<0)return "Radio aus";
   for(Capabilities.Preset p:c.presets)if(p.band==1&&p.number==preset)return (p.name.isEmpty()?"P"+String.format(Locale.ROOT,"%02d",preset):p.name)+(p.frequency.isEmpty()?"":" · "+p.frequency);
   if(frequency.matches("[0-9]{5}"))return String.format(Locale.GERMANY,"%.2f MHz",Integer.parseInt(frequency)/100.0);
   return preset>0?"FM · P"+String.format(Locale.ROOT,"%02d",preset):"FM";
  }
 }
 static String optional(SceneExecutor.Wire w,String prefix)throws IOException{
  try{return w.query(prefix);}catch(IOException e){if(e.getMessage()!=null&&(e.getMessage().equals("Nicht verfügbar: "+prefix)||e.getMessage().equals("Status fehlt: "+prefix)))return "";throw e;}
 }
 static State read(SceneExecutor.Wire w,Capabilities c)throws IOException{
  State s=new State();
  for(int i=0;i<2;i++){String power=w.query(POWER[i]);if(!power.equals("00")&&!power.equals("01"))throw new IOException("Zonenstatus unbekannt");s.on[i]=power.equals("01");}
  for(int i=0;i<2;i++){
   s.source[i]=optional(w,SOURCE[i]);if(s.on[i]&&!s.source[i].matches("[0-9A-Fa-f]{2}"))throw new IOException("Quelle unbekannt");
   s.raw[i]=s.on[i]?Protocol.volume(optional(w,VOLUME[i])):-1;s.scale[i]=c.volumeReported[i]?c.volumeScale[i]:s.raw[i]>100?2:1;
   s.max[i]=Protocol.maxVolume(c.volumeMax[i],s.scale[i],s.raw[i]);
  }
  readTuner(s,w);return s;
 }
 static void readTuner(State s,SceneExecutor.Wire w)throws IOException{
  int tuner=s.tuner();s.preset=-1;s.frequency="";
  if(tuner>=0){String v=optional(w,PRESET[tuner]);if(v.matches("[0-9A-Fa-f]{2}")){int n=Integer.parseInt(v,16);if(n>=1&&n<=40)s.preset=n;}s.frequency=optional(w,TUNE[tuner]);}
 }
 static void startFM(State s,SceneExecutor.Wire w,Capabilities c,int i)throws IOException{
  if(c.hasSelectors&&!c.sources(i).containsKey("24"))throw new IOException("FM nicht verfügbar");
  int other=1-i;
  if(s.on[other]&&(s.effective(other).equals("25")||i==0&&s.source[1].equals("80")&&!s.fm(1)))throw new IOException("Andere Zone nutzt gemeinsame Quelle");
  if(!s.on[i])w.set(POWER[i]+"01",POWER[i],"01");
  int volume=Math.min(START_VOLUME*s.scale[i],s.max[i]);String v=Protocol.hex(volume);
  if(!s.on[i]||s.raw[i]!=volume)w.set(VOLUME[i]+v,VOLUME[i],v);
  if(!s.source[i].equals("24"))w.set(SOURCE[i]+"24",SOURCE[i],"24");
 }
 static void toggle(State s,SceneExecutor.Wire w,Capabilities c,int i)throws IOException{
  if(s.fm(i)){w.set(POWER[i]+"00",POWER[i],"00");return;}
  startFM(s,w,c,i);
 }
 static void selectPreset(State s,SceneExecutor.Wire w,Capabilities c,int zone,int preset)throws IOException{
  if(preset<1||preset>40||!s.fm(zone))return;
  if(c.hasPresets){boolean found=false;for(Capabilities.Preset p:c.presets)if(p.band==1&&p.number==preset){found=true;break;}if(!found)throw new IOException("Radiospeicherplatz unbekannt");}
  int i=s.tuner();if(i<0)throw new IOException("FM nicht bestätigt");
  if(s.preset!=preset){String hex=Protocol.hex(preset);w.set(PRESET[i]+hex,PRESET[i],hex);}
 }
 static int nextPreset(Capabilities c,int current,int direction){
  TreeSet<Integer> set=new TreeSet<>();for(Capabilities.Preset p:c.presets)if(p.band==1)set.add(p.number);
  if(set.isEmpty())return -1;Integer next=direction>0?set.higher(current):set.lower(current);return next==null?(direction>0?set.first():set.last()):next;
 }
 static State step(State s,SceneExecutor.Wire w,Capabilities c,int direction)throws IOException{
  if(s.tuner()<0){startFM(s,w,c,1);s=read(w,c);}
  int i=s.tuner();if(i<0)throw new IOException("FM nicht bestätigt");
  int next=nextPreset(c,s.preset,direction);
  if(next>0){String hex=Protocol.hex(next);if(next!=s.preset)w.set(PRESET[i]+hex,PRESET[i],hex);}
  else if(!c.hasPresets&&s.preset>0){w.set(PRESET[i]+(direction>0?"UP":"DOWN"),PRESET[i],null);}
  else{w.set(TUNE[i]+(direction>0?"UP":"DOWN"),TUNE[i],null);}
  return s;
 }
 // The compact radio widget must not turn on a zone, change its input,
 // or adjust another zone when changing stations or volume.
 static void stepDining(State s,SceneExecutor.Wire w,Capabilities c)throws IOException{
  if(!s.fm(1))return;
  if(s.source[1].equals("80"))return; // do not send a main-zone command
  int next=nextPreset(c,s.preset,1);
  if(next>0){if(next!=s.preset){String hex=Protocol.hex(next);w.set(PRESET[1]+hex,PRESET[1],hex);}}
  else if(!c.hasPresets&&s.preset>0)w.set(PRESET[1]+"UP",PRESET[1],null);
  else if(!c.hasPresets)w.set(TUNE[1]+"UP",TUNE[1],null);
 }
 static void adjustDining(State s,SceneExecutor.Wire w,int direction)throws IOException{
  if(!s.fm(1))return;
  if(s.raw[1]<0)throw new IOException("Lautstärke nicht verfügbar");
  int target=Math.max(0,Math.min(s.max[1],s.raw[1]+direction*s.scale[1]));
  if(target!=s.raw[1]){String v=Protocol.hex(target);w.set(VOLUME[1]+v,VOLUME[1],v);s.raw[1]=target;}
 }
 static State readVolumes(SceneExecutor.Wire w,Capabilities c)throws IOException{
  State s=new State();
  for(int i=0;i<2;i++){String power=w.query(POWER[i]);if(!power.equals("00")&&!power.equals("01"))throw new IOException("Zonenstatus unbekannt");s.on[i]=power.equals("01");}
  for(int i=0;i<2;i++)if(s.on[i]){
   s.raw[i]=Protocol.volume(w.query(VOLUME[i]));
   if(s.raw[i]<0)throw new IOException("Lautstärke nicht verfügbar");
   s.scale[i]=c.volumeReported[i]?c.volumeScale[i]:s.raw[i]>100?2:1;s.max[i]=Protocol.maxVolume(c.volumeMax[i],s.scale[i],s.raw[i]);
  }
  return s;
 }
 static void adjust(State s,SceneExecutor.Wire w,int direction)throws IOException{
  for(int i=0;i<2;i++)if(s.on[i]&&s.raw[i]<0)throw new IOException("Lautstärke nicht verfügbar");
  int[] target=s.raw.clone();
  if(s.on[0]&&s.on[1])target=Protocol.linkedScaled(s.raw[0],s.raw[1],s.scale[0],s.scale[1],s.max[0],s.max[1],direction*2);
  else for(int i=0;i<2;i++)if(s.on[i])target[i]=Math.max(0,Math.min(s.max[i],s.raw[i]+direction*s.scale[i]));
  for(int i=0;i<2;i++)if(s.on[i]&&target[i]!=s.raw[i]){String v=Protocol.hex(target[i]);w.set(VOLUME[i]+v,VOLUME[i],v);s.raw[i]=target[i];}
 }
}
