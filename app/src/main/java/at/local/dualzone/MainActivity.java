package at.local.dualzone;
import android.app.*;
import android.os.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.*;
import android.net.*;
import android.view.*;
import android.widget.*;
import java.util.*;
public class MainActivity extends Activity {
 static final int BG=0xff101619,PANEL=0xff20292d,FG=0xffeef3f1,MUTED=0xffa8b9bb,ACCENT=0xff7bd8bc,CAP=100;
 final String[] names={"TV","CBL/SAT","GAME","BD/DVD","STRM BOX","PC","AUX","CD","PHONO","FM","AM","NET","Bluetooth"};
 final String[] codes={"12","01","02","10","11","05","03","23","22","24","25","2B","2E"};
 final String[] netNames={"NET allgemein","Spotify","TuneIn Radio","Musikserver","Favoriten","Deezer"};
 final String[] netCodes={"","0A","0E","00","01","12"};
 final Zone[] zones={new Zone(),new Zone()};
 TextView status,groupText;LinearLayout linked;Button groupMinus,groupPlus;Receiver receiver;SharedPreferences prefs;
 LinearLayout dashboard,detailPage; int detailIndex=-1; TextView detailStatus,detailVolume; Button detailPower,detailSource,detailMute,modeButton; SeekBar detailSlider; boolean detailDragging,manual,choosing; String deviceId=""; Network activeNetwork; final Map<String,String> audio=new HashMap<>(); final ArrayList<Button> detailCommands=new ArrayList<>(); final ArrayList<TextView> audioLabels=new ArrayList<>();
 boolean connected,demo,connecting,foreground;int generation;String ip="",service=""; Capabilities capabilities; String serviceCode=""; long nextProfileQuery; final Map<String,View> audioRows=new HashMap<>(); TextView profileNote; LinearLayout radioPanel,netPanel; Button radioChoice,netChoice; final int[] currentPreset={-1,-1};
 final Handler handler=new Handler(Looper.getMainLooper());
 static class Zone{Boolean power,mute;int vol=-1,pending=-1;long pendingUntil;boolean unavailable;String source="";TextView value;Button powerBtn,src,minus,plus,mediaPrev,mediaChoice,mediaPlay,mediaNext;LinearLayout mediaRow;SeekBar slider;boolean dragging;long lastDrag;}
 int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
 LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);return r;}
 LinearLayout col(){LinearLayout r=new LinearLayout(this);r.setOrientation(1);return r;}
 GradientDrawable shape(int c,int radius){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(radius));return d;}
 Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(15);b.setTextColor(FG);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(8),0,dp(8),0);b.setBackground(shape(0xff344247,10));return b;}
 void add(LinearLayout r,View v,int w,int h){r.addView(v,new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h)));}
 void weighted(LinearLayout r,View v,int h){r.addView(v,new LinearLayout.LayoutParams(0,h<0?h:dp(h),1));}
 void gap(LinearLayout r,int h){add(r,new View(this),1,h);}
 void hgap(LinearLayout r,int w){add(r,new View(this),w,1);}
 @Override public void onCreate(Bundle b){super.onCreate(b);Diagnostics.open(getFilesDir());Diagnostics.event("APP","v0.7.0 Android API="+Build.VERSION.SDK_INT+" screenDp="+getResources().getConfiguration().screenWidthDp+"x"+getResources().getConfiguration().screenHeightDp);prefs=getSharedPreferences("config",MODE_PRIVATE);
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  LinearLayout root=col();root.setBackgroundColor(BG);root.setPadding(dp(12),dp(6),dp(12),dp(6));dashboard=root;setContentView(root);
  LinearLayout head=row();ImageView brand=new ImageView(this);brand.setImageResource(R.drawable.dz_logo);brand.setContentDescription("Onkyo dahoam");add(head,brand,30,30);hgap(head,5);TextView title=label("Onkyo dahoam",17,FG);title.setContentDescription("Onkyo dahoam – TX-NR656");title.setTypeface(null,Typeface.BOLD);weighted(head,title,44);
  Button tv=btn("TV");tv.setContentDescription("Google TV öffnen");add(head,tv,48,44);hgap(head,6);tv.setOnClickListener(v->openGoogleTv());
  Button config=btn("Verbindung");add(head,config,112,44);config.setOnClickListener(v->connectionMenu());add(root,head,-1,44);
  status=label("",13,MUTED);status.setMaxLines(2);status.setOnClickListener(v->diagnosticsDialog());add(root,status,-1,34);gap(root,4);
  root.addView(zoneCard(0),new LinearLayout.LayoutParams(-1,0,1));gap(root,6);
  root.addView(zoneCard(1),new LinearLayout.LayoutParams(-1,0,1));gap(root,6);
  linked=row();linked.setPadding(dp(12),dp(4),dp(12),dp(4));linked.setBackground(shape(PANEL,14));
  groupMinus=btn("−");groupMinus.setContentDescription("Beide leiser");add(linked,groupMinus,48,44);hgap(linked,8);groupText=label("Beide Zonen",16,FG);groupText.setGravity(Gravity.CENTER);weighted(linked,groupText,44);hgap(linked,8);groupPlus=btn("+");groupPlus.setContentDescription("Beide lauter");add(linked,groupPlus,48,44);add(root,linked,-1,56);
  groupMinus.setOnClickListener(v->stepGroup(-1));groupPlus.setOnClickListener(v->stepGroup(1));
  ip=prefs.getString("ip","");manual=prefs.getBoolean("manual",false);deviceId=prefs.getString("deviceId","");demo=false;status.setText("Receiver wird gesucht …");render();
 }
 LinearLayout zoneCard(int idx){Zone z=zones[idx];LinearLayout c=col();c.setPadding(dp(12),dp(5),dp(12),dp(5));c.setBackground(shape(PANEL,14));c.setGravity(Gravity.CENTER_VERTICAL);
  LinearLayout top=row();TextView title=label(idx==0?"Wohnzimmer":"Esstisch",20,FG);title.setTypeface(null,Typeface.BOLD);weighted(top,title,44);z.powerBtn=btn("--");add(top,z.powerBtn,74,44);hgap(top,5);Button details=btn("›");details.setContentDescription(idx==0?"Details Wohnzimmer":"Details Esstisch");add(top,details,44,44);add(c,top,-1,44);gap(c,4);
  z.src=btn("Quelle");add(c,z.src,-1,44);gap(c,2);
  LinearLayout vol=row();z.minus=btn("−");z.plus=btn("+");z.minus.setContentDescription("Leiser "+(idx==0?"Wohnzimmer":"Esstisch"));z.plus.setContentDescription("Lauter "+(idx==0?"Wohnzimmer":"Esstisch"));add(vol,z.minus,40,44);z.slider=new SeekBar(this);z.slider.setMax(CAP);weighted(vol,z.slider,44);add(vol,z.plus,40,44);z.value=label("--",21,FG);z.value.setGravity(Gravity.CENTER);add(vol,z.value,52,44);add(c,vol,-1,44);
  z.minus.setOnClickListener(v->stepZone(idx,-1));z.plus.setOnClickListener(v->stepZone(idx,1));
  z.mediaRow=row();z.mediaPrev=btn("⏮");z.mediaChoice=btn("Quelle");z.mediaPlay=btn("⏯");z.mediaNext=btn("⏭");add(z.mediaRow,z.mediaPrev,44,44);weighted(z.mediaRow,z.mediaChoice,44);add(z.mediaRow,z.mediaPlay,44,44);add(z.mediaRow,z.mediaNext,44,44);add(c,z.mediaRow,-1,44);
  z.mediaPrev.setOnClickListener(v->mediaStep(idx,-1));z.mediaNext.setOnClickListener(v->mediaStep(idx,1));z.mediaPlay.setOnClickListener(v->transport(idx,"P/P"));z.mediaChoice.setOnClickListener(v->{String media=effectiveSource(idx);if(isRadio(media))radioDialog(idx);else if(media.equals("2B"))netDialog(idx);else if(media.equals("12"))openGoogleTv();});
  z.powerBtn.setOnClickListener(v->{if(z.power!=null)send((idx==0?"PWR":"ZPW")+(z.power?"00":"01"));});z.src.setOnClickListener(v->sourceDialog(idx));details.setOnClickListener(v->details(idx));
  z.slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){z.dragging=true;z.lastDrag=0;}public void onProgressChanged(SeekBar s,int n,boolean user){if(user){z.value.setText(Protocol.volumeLabel(n,scale(idx)));dragVolume(idx,n,z.dragging);}}public void onStopTrackingTouch(SeekBar s){z.dragging=false;setVolume(idx,s.getProgress());render();}});
  return c;
 }
 int zoneIndex(Zone z){return z==zones[0]?0:1;}
 int scale(int idx){if(capabilities!=null&&capabilities.volumeReported[idx])return capabilities.volumeScale[idx];return zones[idx].vol>100?2:1;}
 int maxVolume(int idx){return Protocol.maxVolume(capabilities==null?0:capabilities.volumeMax[idx],scale(idx),zones[idx].vol);}
 int shownVolume(Zone z){return z.pending>=0?z.pending:z.vol;}
 boolean canVolume(Zone z){return connected&&Boolean.TRUE.equals(z.power)&&!z.unavailable&&z.vol>=0&&z.vol<=100*scale(zoneIndex(z));}
 void dragVolume(int idx,int target,boolean dragging){Zone z=zones[idx];long now=SystemClock.elapsedRealtime();if(!dragging||now-z.lastDrag>=10){z.lastDrag=now;setVolume(idx,target);}}
 void stepZone(int idx,int direction){Zone z=zones[idx];if(canVolume(z)){setVolume(idx,Math.max(0,Math.min(maxVolume(idx),shownVolume(z)+direction*scale(idx))));render();}}
 void stepGroup(int direction){if(!canLink())return;int[] t=Protocol.linkedScaled(shownVolume(zones[0]),shownVolume(zones[1]),scale(0),scale(1),maxVolume(0),maxVolume(1),direction*2);setVolumes(t[0],t[1]);render();}
 boolean markVolume(int idx,int target){
  Zone z=zones[idx];if(!canVolume(z)||target<0||target>maxVolume(idx)){Diagnostics.event("VOLUME","blocked zone="+(idx+1)+" target="+target+" power="+z.power+" raw="+z.vol+" unavailable="+z.unavailable);return false;}
  if(target==shownVolume(z))return false;z.pending=target;z.pendingUntil=SystemClock.elapsedRealtime()+6000;
  Diagnostics.event("VOLUME","requested zone="+(idx+1)+" raw="+target+" scale="+scale(idx)+" max="+maxVolume(idx));return true;
 }
 void setVolume(int idx,int target){if(markVolume(idx,target)){String command=(idx==0?"MVL":"ZVL")+Protocol.hex(target);if(demo)send(command);else if(receiver!=null)receiver.sendVolume(command);}}
 void setVolumes(int a,int b){if(!canLink()||a<0||b<0||a>maxVolume(0)||b>maxVolume(1))return;ArrayList<String> commands=new ArrayList<>();if(markVolume(0,a))commands.add("MVL"+Protocol.hex(a));if(markVolume(1,b))commands.add("ZVL"+Protocol.hex(b));if(!commands.isEmpty()){if(demo)send(commands.toArray(new String[0]));else if(receiver!=null)for(String command:commands)receiver.sendVolume(command);}}
 void expireVolumes(){long now=SystemClock.elapsedRealtime();for(int i=0;i<2;i++){Zone z=zones[i];if(z.pending>=0&&now>=z.pendingUntil){Diagnostics.event("VOLUME","unconfirmed zone="+(i+1)+" requested="+z.pending+" observed="+z.vol);z.pending=-1;Toast.makeText(this,"Lautstärke nicht bestätigt · Zone "+(i+1),Toast.LENGTH_SHORT).show();}}}
 String volumeText(Zone z){return z.unavailable?"–":Protocol.volumeLabel(shownVolume(z),scale(zoneIndex(z)))+(z.pending>=0?"…":"");}
 void receiveVolume(Zone z,String v){int raw=Protocol.volume(v);if(raw<0){Diagnostics.event("VOLUME","invalid reply zone="+(zoneIndex(z)+1));return;}z.unavailable=false;z.vol=raw;if(z.pending==raw)z.pending=-1;}
 boolean canLink(){return canVolume(zones[0])&&canVolume(zones[1])&&zones[1].source.equals("80");}
 void render(){expireVolumes();for(Zone z:zones){boolean on=Boolean.TRUE.equals(z.power);z.powerBtn.setEnabled(connected&&z.power!=null);z.powerBtn.setText(z.power==null?"--":on?"An":"Aus");z.powerBtn.setTextColor(on?ACCENT:FG);z.src.setEnabled(connected&&on);z.src.setText(sourceName(z.source)+(z.source.equals("2B")&&!service.isEmpty()?" / "+service:"")+(z.source.equals("80")?" · "+sourceName(zones[0].source):""));z.slider.setEnabled(canVolume(z));z.minus.setEnabled(canVolume(z)&&shownVolume(z)>0);z.plus.setEnabled(canVolume(z)&&shownVolume(z)<maxVolume(zoneIndex(z)));renderMedia(zoneIndex(z));if(!z.dragging){z.slider.setMax(maxVolume(zoneIndex(z)));z.slider.setProgress(Math.max(0,shownVolume(z)));z.value.setText(volumeText(z));z.slider.setContentDescription(z.unavailable?"Lautstärke nicht verfügbar":"Lautstärke, Maximum "+Protocol.volumeLabel(maxVolume(zoneIndex(z)),scale(zoneIndex(z))));}}
  linked.setVisibility(zones[1].source.equals("80")?View.VISIBLE:View.GONE);groupMinus.setEnabled(canLink()&&shownVolume(zones[0])>=scale(0)&&shownVolume(zones[1])>=scale(1));groupPlus.setEnabled(canLink()&&shownVolume(zones[0])+scale(0)<=maxVolume(0)&&shownVolume(zones[1])+scale(1)<=maxVolume(1));groupText.setText("Beide Zonen"); renderDetails();
 }
 String sourceName(String c){if(c.equals("80"))return "SOURCE";if(capabilities!=null){String name=capabilities.sourceName(c);if(name!=null)return name;}for(int i=0;i<codes.length;i++)if(codes[i].equals(c))return names[i];return c.isEmpty()?"Quelle unbekannt":"Quelle "+c;}
 LinkedHashMap<String,String> sources(int idx){if(capabilities!=null&&capabilities.hasSelectors)return capabilities.sources(idx);LinkedHashMap<String,String> out=new LinkedHashMap<>();if(idx==1)out.put("80","SOURCE");for(int j=0;j<codes.length;j++)if(idx==0||!codes[j].equals("05"))out.put(codes[j],names[j]);return out;}
 LinkedHashMap<String,String> services(int idx){LinkedHashMap<String,String> out=new LinkedHashMap<>();out.put("","NET allgemein");if(capabilities!=null&&capabilities.hasServices)out.putAll(capabilities.services(idx));else for(int j=1;j<netCodes.length;j++)out.put(netCodes[j],netNames[j]);return out;}
 String effectiveSource(int idx){String s=zones[idx].source;return idx==1&&s.equals("80")?zones[0].source:s;}
 int effectiveZone(int idx){return idx==1&&zones[1].source.equals("80")?0:idx;}
 boolean isRadio(String code){return code.equals("24")||code.equals("25");}
 boolean zoneOn(int idx){return connected&&Boolean.TRUE.equals(zones[idx].power);}
 boolean usb(String source){return source.equals("29")||source.equals("2A")||source.equals("2C");}
 boolean playback(String source){return source.equals("2B")||source.equals("2E")||usb(source);}
 String radioCaption(int idx){int n=currentPreset[effectiveZone(idx)];String caption=radioEntries(idx).get(n);return n<1?"Sender wählen":caption==null?String.format(Locale.ROOT,"P%02d",n):caption;}
 void renderMedia(int idx){Zone z=zones[idx];String media=effectiveSource(idx);boolean follows=idx==1&&z.source.equals("80"),radio=isRadio(media),net=media.equals("2B"),play=playback(media),tv=media.equals("12");
  z.mediaRow.setVisibility(zoneOn(idx)&&(follows||radio||play||tv)?View.VISIBLE:View.GONE);
  boolean control=zoneOn(idx)&&!follows;
  z.mediaPrev.setVisibility(control&&(radio||play&&!net)?View.VISIBLE:View.GONE);
  z.mediaNext.setVisibility(control&&(radio||play)?View.VISIBLE:View.GONE);
  z.mediaPlay.setVisibility(control&&play?View.VISIBLE:View.GONE);
  z.mediaChoice.setVisibility(follows||radio||net||tv?View.VISIBLE:View.GONE);
  z.mediaChoice.setEnabled(control&&(radio&&!radioEntries(idx).isEmpty()||net||tv));
  z.mediaChoice.setText(follows?(radio?radioCaption(idx):net&&!service.isEmpty()?service:sourceName(media))+" · folgt Wohnzimmer":radio?radioCaption(idx):net?(service.isEmpty()?"NET-Dienst":service):"Google TV öffnen");
  z.mediaPrev.setContentDescription((radio?"Vorheriger Sender":"Vorheriger Titel")+" "+(idx==0?"Wohnzimmer":"Esstisch"));z.mediaNext.setContentDescription((radio?"Nächster Sender":"Nächster Titel")+" "+(idx==0?"Wohnzimmer":"Esstisch"));z.mediaPlay.setContentDescription("Play/Pause "+(idx==0?"Wohnzimmer":"Esstisch"));
  boolean tuner=capabilities==null||!capabilities.hasControls||capabilities.control("TUNER Control",effectiveZone(idx));
  z.mediaPrev.setEnabled(control&&(!radio||tuner));z.mediaNext.setEnabled(control&&(!radio||tuner));z.mediaPlay.setEnabled(control&&play);
 }
 void mediaStep(int idx,int direction){if(isRadio(effectiveSource(idx)))radioPreset(idx,direction<0?"DOWN":"UP");else transport(idx,direction<0?"TRDN":"TRUP");}
 void transport(int idx,String action){String media=effectiveSource(idx);if(!zoneOn(idx)||!playback(media)||idx==1&&zones[1].source.equals("80"))return;if(!Arrays.asList("P/P","TRUP","TRDN").contains(action))return;mediaAction(idx,media,(effectiveZone(idx)==0?"NTC":"NTZ")+action);}
 void queryDashboardMedia(){if(!connected||receiver==null)return;ArrayList<String> q=new ArrayList<>();for(int i=0;i<2;i++)if(zoneOn(i)&&isRadio(effectiveSource(i))){String cmd=effectiveZone(i)==0?"PRSQSTN":"PRZQSTN";if(!q.contains(cmd))q.add(cmd);}if(!q.isEmpty())receiver.send(q.toArray(new String[0]));}
 void sourceDialog(int idx){
  LinkedHashMap<String,String> entries=sources(idx);if(entries.isEmpty()){Toast.makeText(this,"Keine Quellen gemeldet",Toast.LENGTH_SHORT).show();return;}
  String[] keys=entries.keySet().toArray(new String[0]),labels=entries.values().toArray(new String[0]);
  new AlertDialog.Builder(this).setTitle("Quelle auswählen").setItems(labels,(d,n)->{
   if(!zoneOn(idx)||!sources(idx).containsKey(keys[n]))return;
   send((idx==0?"SLI":"SLZ")+keys[n]); // Choose source first. Detail-only controls follow actual reported source.
  }).show();
 }
 boolean sharedMedium(int idx,String selected){int other=1-idx;return zoneOn(other)&&(effectiveSource(other).equals(selected)||isRadio(selected)&&isRadio(effectiveSource(other)));}
 void mediaAction(int idx,String selected,String command){
  if(!zoneOn(idx)||!effectiveSource(idx).equals(selected))return;
  Runnable run=()->{if(zoneOn(idx)&&effectiveSource(idx).equals(selected))send(command);};
  if(sharedMedium(idx,selected))new AlertDialog.Builder(this).setMessage("Die Medienauswahl kann auch die andere Zone beeinflussen.").setNegativeButton("Abbrechen",null).setPositiveButton("Wechseln",(d,n)->run.run()).show();else run.run();
 }
 void netDialog(int idx){
  if(!zoneOn(idx)||!effectiveSource(idx).equals("2B"))return;
  int control=effectiveZone(idx);LinkedHashMap<String,String> entries=services(control);entries.remove("");
  if(entries.isEmpty()){Toast.makeText(this,"Keine NET-Dienste gemeldet",Toast.LENGTH_SHORT).show();return;}
  String[] ids=entries.keySet().toArray(new String[0]),labels=entries.values().toArray(new String[0]);
  new AlertDialog.Builder(this).setTitle("NET-Dienst auswählen").setItems(labels,(d,n)->{
   if(services(effectiveZone(idx)).containsKey(ids[n]))mediaAction(idx,"2B","NSV"+ids[n]+"0");
  }).show();
 }
 LinkedHashMap<Integer,String> radioEntries(int idx){String source=effectiveSource(idx);if(capabilities!=null)return capabilities.radioPresets(source);LinkedHashMap<Integer,String> list=new LinkedHashMap<>();if(isRadio(source))for(int n=1;n<=40;n++)list.put(n,String.format(Locale.ROOT,"P%02d",n));return list;}
 void radioDialog(int idx){
  String source=effectiveSource(idx);if(!zoneOn(idx)||!isRadio(source))return;LinkedHashMap<Integer,String> entries=radioEntries(idx);
  if(entries.isEmpty()){Toast.makeText(this,"Keine gespeicherten Sender gemeldet",Toast.LENGTH_SHORT).show();return;}
  Integer[] numbers=entries.keySet().toArray(new Integer[0]);String[] labels=entries.values().toArray(new String[0]);
  new AlertDialog.Builder(this).setTitle("Radiospeicherplatz auswählen").setItems(labels,(d,n)->{
   if(effectiveSource(idx).equals(source)&&radioEntries(idx).containsKey(numbers[n]))mediaAction(idx,source,(effectiveZone(idx)==0?"PRS":"PRZ")+Protocol.hex(numbers[n]));
  }).show();
 }
 String netName(String code){if(capabilities!=null){String name=capabilities.serviceName(code);if(name!=null)return name;}for(int i=1;i<netCodes.length;i++)if(netCodes[i].equals(code))return netNames[i];return "";}
 void queryProfile(){if(receiver!=null&&connected&&SystemClock.elapsedRealtime()>=nextProfileQuery){nextProfileQuery=SystemClock.elapsedRealtime()+(capabilities==null?15000:120000);receiver.send("NRIQSTN");}}
 void details(int idx){Diagnostics.event("UI","details zone="+(idx+1));
  detailIndex=idx; detailDragging=false; detailCommands.clear();audioLabels.clear();audioRows.clear();modeButton=null;profileNote=null;
  detailPage=col();detailPage.setBackgroundColor(BG);detailPage.setPadding(dp(12),dp(6),dp(12),dp(6));
  LinearLayout top=row();Button back=btn("‹");add(top,back,48,48);hgap(top,6);ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.dz_logo);logo.setContentDescription("Onkyo dahoam");add(top,logo,28,28);hgap(top,6);weighted(top,label(idx==0?"Wohnzimmer":"Esstisch",21,FG),48);back.setOnClickListener(v->closeDetails());add(detailPage,top,-1,48);
  detailStatus=label("",13,MUTED);add(detailPage,detailStatus,-1,32);
  ScrollView scroll=new ScrollView(this);LinearLayout content=col();scroll.addView(content);detailPage.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout controls=section(content,"Steuerung");
  LinearLayout powerRow=row();detailPower=btn("–");detailSource=btn("Quelle");add(powerRow,detailPower,86,48);hgap(powerRow,6);weighted(powerRow,detailSource,48);add(controls,powerRow,-1,48);
  detailPower.setOnClickListener(v->{Zone z=zones[idx];if(z.power!=null)send((idx==0?"PWR":"ZPW")+(z.power?"00":"01"));});
  detailSource.setOnClickListener(v->sourceDialog(idx));gap(controls,6);
  LinearLayout volumeRow=row();detailSlider=new SeekBar(this);detailSlider.setMax(CAP);weighted(volumeRow,detailSlider,48);detailVolume=label("--",22,FG);add(volumeRow,detailVolume,48,48);add(controls,volumeRow,-1,48);
  detailSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){detailDragging=true;zones[idx].lastDrag=0;}public void onProgressChanged(SeekBar s,int n,boolean user){if(user){detailVolume.setText(Protocol.volumeLabel(n,scale(idx)));dragVolume(idx,n,detailDragging);}}public void onStopTrackingTouch(SeekBar s){detailDragging=false;setVolume(idx,s.getProgress());render();}});
  detailMute=btn("Stumm");add(controls,detailMute,-1,48);detailMute.setOnClickListener(v->{Zone z=zones[idx];if(z.mute!=null)send((idx==0?"AMT":"ZMT")+(z.mute?"00":"01"));});
  if(idx==0){
   LinearLayout sound=section(content,"Klang");
   modeButton=btn("Listening Mode");add(sound,modeButton,-1,48);modeButton.setOnClickListener(v->modeDialog());
   gap(sound,4);audioRow(sound,"Bass","TFRB","TFRBDOWN","TFRBUP");audioRow(sound,"Höhen","TFRT","TFRTDOWN","TFRTUP");audioRow(sound,"Center","CTL","CTLDOWN","CTLUP");audioRow(sound,"Subwoofer","SWL","SWLDOWN","SWLUP");
  }
  radioPanel=section(content,"Radio");LinearLayout play=radioPanel;
  radioChoice=btn("Radiospeicherplatz auswählen");add(play,radioChoice,-1,48);radioChoice.setOnClickListener(v->radioDialog(idx));
  LinearLayout presets=row();Button prev=btn("‹ Sender"),next=btn("Sender ›");prev.setTag("radio");next.setTag("radio");detailCommands.add(prev);detailCommands.add(next);weighted(presets,prev,48);hgap(presets,6);weighted(presets,next,48);add(play,presets,-1,48);
  prev.setOnClickListener(v->radioPreset(idx,"DOWN"));next.setOnClickListener(v->radioPreset(idx,"UP"));
  netPanel=section(content,"NET-Dienste");netChoice=btn("NET-Dienst auswählen");add(netPanel,netChoice,-1,48);netChoice.setOnClickListener(v->netDialog(idx));
  profileNote=label("",12,MUTED);add(content,profileNote,-1,40);TextView limit=label("Maximalpegel gemäß Receiver-Profil; Esstisch LINE OUT ggf. nicht regelbar.",12,MUTED);add(content,limit,-1,40);
  setContentView(detailPage);queryDetails();renderDetails();
 }
 LinearLayout section(LinearLayout parent,String title){gap(parent,8);LinearLayout c=col();c.setBackground(shape(PANEL,14));c.setPadding(dp(12),dp(8),dp(12),dp(10));TextView t=label(title,17,FG);t.setTypeface(null,Typeface.BOLD);add(c,t,-1,30);add(parent,c,-1,-2);return c;}
 void audioRow(LinearLayout parent,String name,String key,String down,String up){
  LinearLayout r=row();weighted(r,label(name,15,FG),48);TextView value=label("–",14,MUTED);value.setTag(key);value.setGravity(Gravity.CENTER);audioLabels.add(value);add(r,value,54,48);Button minus=btn("−"),plus=btn("+");minus.setTag(key);plus.setTag(key);detailCommands.add(minus);detailCommands.add(plus);add(r,minus,48,48);hgap(r,5);add(r,plus,48,48);add(parent,r,-1,48);audioRows.put(key,r);gap(parent,4);
  minus.setOnClickListener(v->send(down));plus.setOnClickListener(v->send(up));
 }
 void radioPreset(int idx,String direction){String source=effectiveSource(idx);if(zoneOn(idx)&&isRadio(source))mediaAction(idx,source,(effectiveZone(idx)==0?"PRS":"PRZ")+direction);}
 void queryDetails(){if(receiver!=null&&connected&&detailIndex>=0){
  ArrayList<String> q=new ArrayList<>();if(detailIndex==0){q.add("LMDQSTN");if(allowedAudio("TFRB")||allowedAudio("TFRT"))q.add("TFRQSTN");if(allowedAudio("CTL"))q.add("CTLQSTN");if(allowedAudio("SWL"))q.add("SWLQSTN");}
  if(zoneOn(detailIndex)&&isRadio(effectiveSource(detailIndex)))q.add(effectiveZone(detailIndex)==0?"PRSQSTN":"PRZQSTN");
  if(!q.isEmpty())receiver.send(q.toArray(new String[0]));
 }}
 LinkedHashMap<String,String> listeningModes(){
  LinkedHashMap<String,String> modes=new LinkedHashMap<>();
  modes.put("00","Stereo");modes.put("01","Direct");modes.put("0C","All Channel Stereo");
  if(Boolean.FALSE.equals(zones[1].power))modes.put("11","Pure Audio");
  modes.put("80","Dolby Surround / Atmos");modes.put("82","DTS Neural:X / DTS:X");
  return modes;
 }
 void modeDialog(){if(!zoneOn(0)||!audio.containsKey("LMD"))return;
  LinkedHashMap<String,String> modes=listeningModes();String[] ids=modes.keySet().toArray(new String[0]),labels=modes.values().toArray(new String[0]);
  new AlertDialog.Builder(this).setTitle("Listening Mode").setItems(labels,(d,i)->{if(zoneOn(0)&&listeningModes().containsKey(ids[i])){send("LMD"+ids[i]);if(!demo&&receiver!=null)receiver.send("LMDQSTN");}}).show();
 }
 String modeName(String code){if(code==null)return "–";switch(code){case "00":return "Stereo";case "01":return "Direct";case "0C":return "All Channel Stereo";case "11":return "Pure Audio";case "80":return "Dolby Surround / Atmos";case "82":return "DTS Neural:X / DTS:X";default:return code==null?"–":code;}}
 boolean allowedAudio(String key){if(capabilities==null||!capabilities.hasControls)return true;String id;switch(key){case "TFRB":id="Bass";break;case "TFRT":id="Treble";break;case "CTL":id="Center Level";break;case "SWL":id="Subwoofer Level";break;default:return false;}return capabilities.control(id,0);}
 void renderDetails(){if(detailIndex<0)return;Zone z=zones[detailIndex];boolean on=connected&&Boolean.TRUE.equals(z.power);
  detailStatus.setText(status.getText());detailPower.setText(z.power==null?"–":on?"An":"Aus");detailPower.setEnabled(connected&&z.power!=null);detailSource.setText(sourceName(z.source)+(z.source.equals("2B")&&!service.isEmpty()?" / "+service:""));detailSource.setEnabled(on);
  String media=effectiveSource(detailIndex);int control=effectiveZone(detailIndex);boolean radio=on&&isRadio(media),net=on&&media.equals("2B");
  radioPanel.setVisibility(radio?View.VISIBLE:View.GONE);netPanel.setVisibility(net?View.VISIBLE:View.GONE);
  if(radio){int n=currentPreset[control];String caption=radioEntries(detailIndex).get(n);radioChoice.setText(n<1?"Radiospeicherplatz auswählen":caption==null?String.format(Locale.ROOT,"P%02d · Sender wählen",n):caption);radioChoice.setEnabled(!radioEntries(detailIndex).isEmpty());}
  if(net)netChoice.setText(service.isEmpty()?"NET-Dienst auswählen":service+" · Dienst wechseln");
  detailSlider.setEnabled(canVolume(z));if(!detailDragging){detailSlider.setMax(maxVolume(detailIndex));detailSlider.setProgress(Math.max(0,shownVolume(z)));detailVolume.setText(volumeText(z));}
  detailMute.setText(z.mute==null?"Mute · unbekannt":z.mute?"Stumm · aufheben":"Ton stummschalten");detailMute.setEnabled(on&&z.mute!=null);
  if(modeButton!=null){modeButton.setText("Listening Mode · "+modeName(audio.get("LMD")));modeButton.setEnabled(on&&audio.containsKey("LMD"));}
  if(profileNote!=null)profileNote.setText(demo?"Demo-Geräteprofil":capabilities==null?"Geräteprofil noch nicht bestätigt": "Geräteprofil · "+capabilities.model);for(Map.Entry<String,View> item:audioRows.entrySet())item.getValue().setVisibility(allowedAudio(item.getKey())?View.VISIBLE:View.GONE);
  for(TextView t:audioLabels){String v=audio.get((String)t.getTag());t.setText(v==null?"–":v.equals("00")?"0":v);}
  for(Button b:detailCommands){String k=(String)b.getTag();boolean radioControl=isRadio(effectiveSource(detailIndex));boolean toneAllowed=!(k.startsWith("TFR")&&("01".equals(audio.get("LMD"))||"11".equals(audio.get("LMD"))));b.setEnabled(on&&(k.equals("radio")?radioControl&&(capabilities==null||!capabilities.hasControls||capabilities.control("TUNER Control",detailIndex)):allowedAudio(k)&&toneAllowed&&audio.containsKey(k)));}
 }
 void closeDetails(){Diagnostics.event("UI","dashboard");detailIndex=-1;detailDragging=false;setContentView(dashboard);render();}
 @Override public void onBackPressed(){if(detailIndex>=0)closeDetails();else super.onBackPressed();}
 void openGoogleTv(){
  Diagnostics.event("UI","Google TV shortcut");
  try{
   Intent launch=getPackageManager().getLaunchIntentForPackage("com.google.android.videos");
   if(launch!=null){startActivity(launch);return;}
  }catch(ActivityNotFoundException|SecurityException e){Diagnostics.error("TV_LAUNCH_ERROR",e);}
  new AlertDialog.Builder(this).setTitle("Google TV nicht verfügbar")
   .setMessage("Installiere oder aktiviere Google TV auf deinem Smartphone. Alternativ kannst du die TV-Fernbedienung über die Android-Schnelleinstellungen öffnen.")
   .setNegativeButton("Schließen",null)
   .setPositiveButton("Play Store",(d,n)->{
    try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.google.android.videos")));}
    catch(ActivityNotFoundException e){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.videos")));}catch(ActivityNotFoundException ignored){Toast.makeText(this,"Play Store nicht verfügbar",Toast.LENGTH_LONG).show();}}
   }).show();
 }
 void widgetMenu(){
  android.appwidget.AppWidgetManager m=android.appwidget.AppWidgetManager.getInstance(this);
  new AlertDialog.Builder(this).setTitle("Homescreen-Widgets").setMessage("Erstelle beliebig viele Onkyo dahoam-Szenen. Ein Tippen führt die gewählten Einstellungen aus, auch ohne geöffnete App. FM4 beispielsweise: Esstisch einschalten, FM wählen, Speicherplatz einstellen, Lautstärke 40. Wohnzimmer unverändert. Nach dem Hinzufügen auf Einrichten oder das Zahnrad tippen. ↻ prüft nur die Verbindung; Titel und Aktionsbereich starten die Szene.")
   .setNegativeButton("Schließen",null).setPositiveButton("Widget hinzufügen",(d,n)->{if(m.isRequestPinAppWidgetSupported()){if(!m.requestPinAppWidget(new ComponentName(this,SceneWidget.class),null,null))Toast.makeText(this,"Bitte über Homescreen-Widget-Auswahl hinzufügen",Toast.LENGTH_LONG).show();}else Toast.makeText(this,"Homescreen lange drücken → Widgets → Onkyo dahoam",Toast.LENGTH_LONG).show();}).show();
 }
 void connectionMenu(){new AlertDialog.Builder(this).setTitle("Verbindung & Diagnose").setItems(new String[]{"Automatisch suchen","IP manuell eingeben","Demo","Diagnosebericht","Homescreen-Widgets"},(d,n)->{
  if(n==0){Diagnostics.event("UI","automatic discovery requested");demo=false;manual=false;deviceId="";prefs.edit().putBoolean("manual",false).remove("deviceId").apply();SceneWidget.refreshAll(this);disconnect();tick();}
  else if(n==1)connectionDialog();else if(n==2)startDemo();else if(n==3)diagnosticsDialog();else widgetMenu();
 }).show();}
 void diagnosticsDialog(){
  Diagnostics.event("UI","diagnostics opened");
  String info="Lokales Protokoll · max. 256 KiB · automatische Löschung nach spätestens 72 Stunden bei laufender App. Bei wenig Speicher wird die Aufzeichnung pausiert.\nEnthält Befehle, Status, Fehler, Zeitpunkte und Geräteprofil. Keine IP/MAC, WLAN-Namen, Zugangsdaten oder Mediennamen. Nur auf deinen Wunsch exportiert.";
  try{String s=Diagnostics.snapshot();info+="\n\nLetzte Einträge:\n"+s.substring(Math.max(0,s.length()-1300));}catch(java.io.IOException e){Diagnostics.error("LOG_READ_ERROR",e);}
  TextView t=label(info,13,FG);t.setPadding(dp(16),dp(8),dp(16),dp(8));t.setTextIsSelectable(true);ScrollView sc=new ScrollView(this);sc.addView(t);
  new AlertDialog.Builder(this).setTitle("Diagnosebericht").setView(sc)
   .setPositiveButton("TXT exportieren",(d,n)->{Diagnostics.event("UI","export requested");Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("text/plain");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"Onkyo dahoam-Diagnose-v0.7.0.txt");try{startActivityForResult(i,404);}catch(ActivityNotFoundException e){Diagnostics.error("EXPORT_ERROR",e);Toast.makeText(this,"Dateiauswahl nicht verfügbar",Toast.LENGTH_LONG).show();}})
   .setNeutralButton("Log löschen",(d,n)->new AlertDialog.Builder(this).setMessage("Alle bisherigen Diagnoseeinträge löschen?").setNegativeButton("Abbrechen",null).setPositiveButton("Löschen",(x,y)->{try{Diagnostics.clear();}catch(java.io.IOException e){Diagnostics.error("LOG_CLEAR_ERROR",e);}}).show()).setNegativeButton("Schließen",null).show();
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=404||result!=RESULT_OK||data==null||data.getData()==null)return;
  try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData(),"wt")){if(out==null)throw new java.io.IOException("No output");out.write(Diagnostics.snapshot().getBytes(java.nio.charset.StandardCharsets.UTF_8));Toast.makeText(this,"Diagnose gespeichert",Toast.LENGTH_LONG).show();}catch(Exception e){Diagnostics.error("EXPORT_ERROR",e);Toast.makeText(this,"Export fehlgeschlagen",Toast.LENGTH_LONG).show();}
 }
 void connectionDialog(){EditText input=new EditText(this);input.setSingleLine(true);input.setHint("192.168.1.123");input.setText(ip);new AlertDialog.Builder(this).setTitle("Receiver verbinden").setMessage("Automatische Suche im lokalen WLAN/LAN. Alternativ IP eingeben. Netzwerk-Standby am Onkyo aktivieren.").setView(input).setPositiveButton("IP verbinden",(d,n)->{String v=input.getText().toString().trim();if(!validIp(v)){Toast.makeText(this,"Private IPv4-Adresse erforderlich",Toast.LENGTH_LONG).show();return;}Diagnostics.event("UI","manual IP accepted");ip=v;manual=true;demo=false;prefs.edit().putString("ip",ip).putBoolean("manual",true).apply();SceneWidget.refreshAll(this);disconnect();tick();}).setNeutralButton("Automatisch suchen",(d,n)->{demo=false;manual=false;deviceId="";prefs.edit().putBoolean("manual",false).remove("deviceId").apply();SceneWidget.refreshAll(this);disconnect();tick();}).setNegativeButton("Demo",(d,n)->startDemo()).show();}
 boolean validIp(String s){try{String[] p=s.split("\\.",-1);if(p.length!=4)return false;int[] a=new int[4];for(int i=0;i<4;i++){if(!p[i].matches("[0-9]{1,3}"))return false;a[i]=Integer.parseInt(p[i]);if(a[i]>255)return false;}return a[0]==10||a[0]==192&&a[1]==168||a[0]==172&&a[1]>=16&&a[1]<=31;}catch(Exception e){return false;}}
 Network lan(){ConnectivityManager m=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);for(Network n:m.getAllNetworks()){NetworkCapabilities c=m.getNetworkCapabilities(n);if(c!=null&&(c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)))return n;}return null;}
 void clear(){Arrays.fill(currentPreset,-1);for(Zone z:zones){z.power=null;z.mute=null;z.vol=-1;z.pending=-1;z.unavailable=false;z.source="";}service="";serviceCode="";audio.clear();capabilities=null;nextProfileQuery=0;}
 void disconnect(){if(connected||connecting||receiver!=null)Diagnostics.event("CONNECTION","disconnect generation="+generation);generation++;if(receiver!=null){receiver.close();receiver=null;}connected=false;connecting=false;choosing=false;activeNetwork=null;clear();}
 void tick(){if(!foreground||demo)return;Network n=lan();if(n==null){if(!status.getText().toString().startsWith("Kein WLAN"))Diagnostics.event("NETWORK","no WiFi/Ethernet");disconnect();status.setText("Kein WLAN/LAN – bitte Heimnetz verbinden");render();return;}if(activeNetwork!=null&&!activeNetwork.equals(n)){Diagnostics.event("NETWORK","network changed");disconnect();}if(choosing)return;if(!connected&&!connecting){if(manual&&!ip.isEmpty())connect(n);else discover(n);}else if(connected&&receiver!=null){receiver.query();queryProfile();queryDashboardMedia();queryDetails();}}
 void discover(Network n){disconnect();Diagnostics.event("DISCOVERY","start");connecting=true;activeNetwork=n;int g=generation;status.setText("Suche TX-NR656 im Netzwerk …");render();
  ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);LinkProperties lp=cm.getLinkProperties(n);
  ArrayList<java.net.InetAddress> targets=new ArrayList<>();if(lp!=null)for(LinkAddress l:lp.getLinkAddresses()){if(l.getAddress() instanceof java.net.Inet4Address){try{targets.add(Discovery.broadcast(l.getAddress(),l.getPrefixLength()));}catch(Exception ignored){}}}
  new Thread(()->{try(java.net.DatagramSocket s=new java.net.DatagramSocket(null)){n.bindSocket(s);s.bind(new java.net.InetSocketAddress(0));List<Discovery.Device> found=Discovery.find(s,targets,1800);Diagnostics.event("DISCOVERY","received unique devices="+found.size());runOnUiThread(()->{if(g!=generation||!foreground)return;connecting=false;List<Discovery.Device> matches=new ArrayList<>();for(Discovery.Device d:found)if(d.model.equalsIgnoreCase("TX-NR656")&&d.port==60128)matches.add(d);
   Diagnostics.event("DISCOVERY","matching TX-NR656="+matches.size());if(matches.isEmpty()){status.setText("TX-NR656 nicht gefunden · Netzwerk-Standby prüfen");render();return;}
   if(!deviceId.isEmpty())for(Discovery.Device d:matches)if(deviceId.equals(d.id)){selectDevice(d,n);return;}
   if(matches.size()==1){selectDevice(matches.get(0),n);return;}
   choosing=true;String[] labels=new String[matches.size()];for(int i=0;i<matches.size();i++)labels[i]=matches.get(i).model+" · "+matches.get(i).ip;
   new AlertDialog.Builder(this).setTitle("Receiver auswählen").setItems(labels,(d,i)->{choosing=false;if(g==generation)selectDevice(matches.get(i),n);}).setNegativeButton("Abbrechen",(d,i)->{choosing=false;status.setText("Auswahl abgebrochen");}).setOnCancelListener(d->choosing=false).show();
  });}catch(Exception e){Diagnostics.error("DISCOVERY_ERROR",e);runOnUiThread(()->{if(g==generation){connecting=false;status.setText("Gerätesuche fehlgeschlagen · IP manuell möglich");render();}});}},"onkyo-discovery").start();
 }
 void selectDevice(Discovery.Device d,Network n){Diagnostics.event("DISCOVERY","TX-NR656 selected");ip=d.ip;deviceId=d.id;prefs.edit().putString("ip",ip).putString("deviceId",deviceId).apply();SceneWidget.refreshAll(this);connect(n);}
 final Runnable poll=new Runnable(){public void run(){tick();handler.postDelayed(this,4000);}};
 void connect(Network n){disconnect();Diagnostics.event("CONNECTION","TCP connect manual="+manual);activeNetwork=n;connecting=true;status.setText("Verbinde mit "+ip+" …");render();final int g=generation;receiver=new Receiver(ip,n.getSocketFactory(),new Receiver.Listener(){public void message(String s){runOnUiThread(()->{if(g==generation){receive(s);render();}});}public void status(String s,boolean online){runOnUiThread(()->{if(g==generation){Diagnostics.event("CONNECTION","online="+online+" generation="+g);connected=online;connecting=false;if(!online){clear();}status.setText(online?"Verbunden · "+ip:"Nicht erreichbar – neuer Versuch folgt");if(online){queryProfile();queryDetails();}render();}});}});}
 void startDemo(){disconnect();Diagnostics.event("UI","demo started");demo=true;connected=true;zones[0].power=true;zones[1].power=true;zones[0].vol=35;zones[1].vol=25;zones[0].source="2B";zones[1].source="80";zones[0].mute=false;zones[1].mute=false;serviceCode="0A";service="Spotify";capabilities=Capabilities.demo();audio.put("LMD","00");audio.put("TFRB","+2");audio.put("TFRT","00");audio.put("CTL","00");audio.put("SWL","-3");audio.put("ZTNB","00");audio.put("ZTNT","00");audio.put("ZBL","00");status.setText("DEMO · kein Receiver verbunden");render();}
 void send(String... commands){for(String c:commands)Diagnostics.command(demo?"DEMO_ACTION":"UI_ACTION",c);if(!connected){Diagnostics.event("UI","command blocked offline");return;}if(demo){for(String s:commands){if(s.startsWith("NSV")&&s.length()>=5){serviceCode=s.substring(3,5);service=netName(serviceCode);}else if(s.startsWith("PRS")||s.startsWith("PRZ")){int i=s.startsWith("PRS")?0:1;String value=s.substring(3);int n=currentPreset[i];if(value.equals("UP"))currentPreset[i]=n<1||n>=5?1:n+1;else if(value.equals("DOWN"))currentPreset[i]=n<=1?5:n-1;else receive(s);}else if(s.startsWith("TFR")||s.startsWith("ZTN")||s.startsWith("CTL")||s.startsWith("SWL")||s.startsWith("ZBL")){String key=s.startsWith("TFR")||s.startsWith("ZTN")?s.substring(0,4):s.substring(0,3);String old=audio.get(key);try{int number=Integer.parseInt(old);int delta=s.endsWith("DOWN")?-1:1;audio.put(key,String.format(Locale.ROOT,"%+d",Math.max(-10,Math.min(10,number+delta))));}catch(Exception ignored){}}else receive(s);}render();}else if(receiver!=null)receiver.send(commands);}
 void receive(String s){if(s.length()<5)return;String k=s.substring(0,3),v=s.substring(3);if(k.equals("NRI")){try{Capabilities c=Capabilities.parse(v);if(!c.model.equalsIgnoreCase("TX-NR656")){Diagnostics.event("PROFILE","unexpected model (omitted)");status.setText("Gerätemodell abweichend · Profil nicht übernommen");return;}capabilities=c;Diagnostics.event("PROFILE",c.summary());service=netName(serviceCode);nextProfileQuery=SystemClock.elapsedRealtime()+120000;status.setText("Verbunden · "+ip+" · Geräteprofil erkannt");}catch(Exception e){Diagnostics.error("PROFILE_PARSE_ERROR",e);}return;}if(v.equals("N/A")||v.equals("QSTN")){if(v.equals("N/A")&&(k.equals("MVL")||k.equals("ZVL"))){Zone unavailable=zones[k.equals("MVL")?0:1];unavailable.vol=-1;unavailable.pending=-1;unavailable.unavailable=true;Diagnostics.event("VOLUME","not available zone="+(k.equals("MVL")?1:2));}audio.remove(k);audio.remove(k+"B");audio.remove(k+"T");return;}
  Zone z=(k.equals("ZPW")||k.equals("ZVL")||k.equals("SLZ")||k.equals("ZMT"))?zones[1]:zones[0];
  switch(k){case "PRS":case "PRZ":if(v.matches("[0-9A-Fa-f]{2}")){int n=Integer.parseInt(v,16);if(n>=1&&n<=40)currentPreset[k.equals("PRS")?0:1]=n;}break;case "LMD":case "CTL":case "SWL":case "ZBL":audio.put(k,v);break;case "TFR":case "ZTN":if(v.length()>=6&&v.charAt(0)=='B'&&v.charAt(3)=='T'){audio.put(k+"B",v.substring(1,3));audio.put(k+"T",v.substring(4,6));}break;case "PWR":case "ZPW":if(v.equals("00")||v.equals("01")){z.power=v.equals("01");if(!z.power)z.pending=-1;}break;case "MVL":case "ZVL":receiveVolume(z,v);break;case "SLI":case "SLZ":if(v.matches("[0-9A-Fa-f]{2}")){String next=v.toUpperCase(Locale.ROOT);if(!next.equals(z.source)){z.source=next;currentPreset[k.equals("SLI")?0:1]=-1;if(!demo)queryDashboardMedia();}}break;case "AMT":case "ZMT":if(v.equals("00")||v.equals("01"))z.mute=v.equals("01");break;case "NMS":if(v.length()>=9){serviceCode=v.substring(7,9).toUpperCase(Locale.ROOT);service=netName(serviceCode);}break;default:break;}
 }
 @Override protected void onStart(){super.onStart();SceneWidget.observeNetwork(this);SceneWidget.refreshAll(this);Diagnostics.event("APP","foreground");foreground=true;handler.post(poll);}
 @Override protected void onStop(){Diagnostics.event("APP","background");foreground=false;handler.removeCallbacks(poll);if(!demo){disconnect();render();}super.onStop();}
 @Override protected void onDestroy(){handler.removeCallbacks(poll);disconnect();super.onDestroy();}
}
