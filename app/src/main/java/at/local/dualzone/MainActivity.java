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
 TextView status;LinearLayout linked;Button groupMinus,groupPlus;Receiver receiver;SharedPreferences prefs;
 LinearLayout dashboard,mainControls,detailContainer;ScrollView detailScroll;
 static final class Detail {LinearLayout body;Button header,mute,mode;final Map<String,View> rows=new HashMap<>();final Map<String,TextView> values=new HashMap<>();final Map<String,Button[]> buttons=new HashMap<>();}
 final Detail[] detailsPanels={new Detail(),new Detail()};final boolean[] detailOpen={false,false};
 boolean manual,choosing;String deviceId="";Network activeNetwork;final Map<String,String> audio=new HashMap<>();
 boolean connected,demo,connecting,foreground;int generation;String ip="",service="";Capabilities capabilities;String serviceCode="";long nextProfileQuery;final int[] currentPreset={-1,-1};
 final Handler handler=new Handler(Looper.getMainLooper());
 static class Zone{Boolean power,mute;int vol=-1,pending=-1;long pendingUntil;boolean unavailable;String source="";TextView value;Button powerBtn,src,mediaPrev,mediaChoice,mediaPlay,mediaNext;LinearLayout mediaRow;SeekBar slider;boolean dragging;long lastDrag;}
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
 @Override public void onCreate(Bundle b){super.onCreate(b);Diagnostics.open(getFilesDir());Diagnostics.event("APP","v0.8.1 Android API="+Build.VERSION.SDK_INT+" screenDp="+getResources().getConfiguration().screenWidthDp+"x"+getResources().getConfiguration().screenHeightDp);prefs=getSharedPreferences("config",MODE_PRIVATE);
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  LinearLayout root=col();root.setBackgroundColor(BG);root.setPadding(dp(8),dp(4),dp(8),dp(4));dashboard=root;setContentView(root);
  LinearLayout head=row();ImageView brand=new ImageView(this);brand.setImageResource(R.drawable.dz_logo);brand.setContentDescription("Onkyo dahoam");add(head,brand,25,25);hgap(head,4);
  TextView title=label("Onkyo dahoam",15,FG);title.setContentDescription("Onkyo dahoam – TX-NR656");title.setTypeface(null,Typeface.BOLD);title.setMaxLines(2);weighted(head,title,44);
  linked=row();groupMinus=btn("−");groupMinus.setTextSize(21);groupMinus.setContentDescription("Alle aktiven Zonen leiser");add(linked,groupMinus,44,44);hgap(linked,3);groupPlus=btn("+");groupPlus.setTextSize(21);groupPlus.setContentDescription("Alle aktiven Zonen lauter");add(linked,groupPlus,44,44);add(head,linked,-2,44);hgap(head,4);
  groupMinus.setOnClickListener(v->stepGroup(-1));groupPlus.setOnClickListener(v->stepGroup(1));
  Button tv=btn("TV");tv.setTextSize(12);tv.setContentDescription("Google TV öffnen");add(head,tv,40,44);hgap(head,4);tv.setOnClickListener(v->openGoogleTv());
  Button config=btn("⚙");config.setTextSize(20);config.setContentDescription("Verbindung");add(head,config,44,44);config.setOnClickListener(v->connectionMenu());add(root,head,-1,44);
  status=label("",11,MUTED);status.setMaxLines(2);status.setOnClickListener(v->diagnosticsDialog());add(root,status,-1,30);
  mainControls=col();add(mainControls,zoneCard(0),-1,-2);gap(mainControls,5);add(mainControls,zoneCard(1),-1,-2);add(root,mainControls,-1,-2);
  gap(root,5);TextView caption=label("Erweiterte Einstellungen",12,MUTED);add(root,caption,-1,22);
  detailScroll=new ScrollView(this);detailScroll.setFillViewport(false);detailScroll.setClipToPadding(false);detailScroll.setVerticalScrollBarEnabled(false);
  detailContainer=col();detailScroll.addView(detailContainer,new ScrollView.LayoutParams(-1,-2));
  for(int i=0;i<2;i++){if(b!=null)detailOpen[i]=b.getBoolean("detail"+i,false);add(detailContainer,detailCard(i),-1,-2);gap(detailContainer,5);}
  root.addView(detailScroll,new LinearLayout.LayoutParams(-1,0,1));
  ip=prefs.getString("ip","");manual=prefs.getBoolean("manual",false);deviceId=prefs.getString("deviceId","");demo=false;status.setText("Receiver wird gesucht …");render();
 }
 LinearLayout zoneCard(int idx){Zone z=zones[idx];LinearLayout c=col();c.setPadding(dp(8),dp(5),dp(8),dp(5));c.setBackground(shape(PANEL,12));
  LinearLayout top=row();TextView title=label(idx==0?"Wohnzimmer":"Esstisch",16,FG);title.setTypeface(null,Typeface.BOLD);title.setSingleLine(true);weighted(top,title,44);hgap(top,4);
  z.src=btn("Quelle");z.src.setTextSize(13);z.src.setSingleLine(true);z.src.setEllipsize(android.text.TextUtils.TruncateAt.END);z.src.setContentDescription(idx==0?"Quelle Wohnzimmer":"Quelle Esstisch");weighted(top,z.src,44);hgap(top,4);
  z.powerBtn=btn("--");z.powerBtn.setTextSize(13);z.powerBtn.setContentDescription(idx==0?"Power Wohnzimmer":"Power Esstisch");add(top,z.powerBtn,52,44);add(c,top,-1,44);
  LinearLayout vol=row();z.slider=new SeekBar(this);z.slider.setMax(CAP);z.slider.setPadding(dp(8),0,dp(8),0);z.slider.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));z.slider.setThumbTintList(android.content.res.ColorStateList.valueOf(ACCENT));weighted(vol,z.slider,44);z.value=label("--",20,FG);z.value.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);z.value.setTypeface(null,Typeface.BOLD);add(vol,z.value,48,44);add(c,vol,-1,44);
  z.mediaRow=row();z.mediaPrev=btn("‹");z.mediaChoice=btn("Quelle");z.mediaPlay=btn("⏯");z.mediaNext=btn("›");z.mediaChoice.setTextSize(12);z.mediaChoice.setSingleLine(true);z.mediaChoice.setEllipsize(android.text.TextUtils.TruncateAt.END);
  add(z.mediaRow,z.mediaPrev,44,44);weighted(z.mediaRow,z.mediaChoice,44);add(z.mediaRow,z.mediaPlay,44,44);add(z.mediaRow,z.mediaNext,44,44);
  c.addView(z.mediaRow,new LinearLayout.LayoutParams(-1,dp(44)));
  z.mediaPrev.setOnClickListener(v->mediaStep(idx,-1));z.mediaNext.setOnClickListener(v->mediaStep(idx,1));z.mediaPlay.setOnClickListener(v->transport(idx,"P/P"));z.mediaChoice.setOnClickListener(v->{String media=effectiveSource(idx);if(isRadio(media))radioDialog(idx);else if(media.equals("2B"))netDialog(idx);else if(media.equals("12"))openGoogleTv();});
  z.powerBtn.setOnClickListener(v->{if(z.power!=null)send((idx==0?"PWR":"ZPW")+(z.power?"00":"01"));});z.src.setOnClickListener(v->sourceDialog(idx));
  z.slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){z.dragging=true;z.lastDrag=0;}public void onProgressChanged(SeekBar s,int n,boolean user){if(user){z.value.setText(Protocol.volumeLabel(n,scale(idx)));dragVolume(idx,n,z.dragging);}}public void onStopTrackingTouch(SeekBar s){z.dragging=false;setVolume(idx,s.getProgress());render();}});
  return c;
 }
 int zoneIndex(Zone z){return z==zones[0]?0:1;}
 int scale(int idx){if(capabilities!=null&&capabilities.volumeReported[idx])return capabilities.volumeScale[idx];return zones[idx].vol>100?2:1;}
 int maxVolume(int idx){return Protocol.maxVolume(capabilities==null?0:capabilities.volumeMax[idx],scale(idx),zones[idx].vol);}
 int shownVolume(Zone z){return z.pending>=0?z.pending:z.vol;}
 boolean canVolume(Zone z){return connected&&(demo||!RemoteVpn.selected(this)||Objects.equals(activeNetwork,RemoteVpn.network(this)))&&Boolean.TRUE.equals(z.power)&&!z.unavailable&&z.vol>=0&&z.vol<=100*scale(zoneIndex(z));}
 void dragVolume(int idx,int target,boolean dragging){Zone z=zones[idx];long now=SystemClock.elapsedRealtime();if(!dragging||now-z.lastDrag>=10){z.lastDrag=now;setVolume(idx,target);}}
 void stepZone(int idx,int direction){Zone z=zones[idx];if(canVolume(z)){setVolume(idx,Math.max(0,Math.min(maxVolume(idx),shownVolume(z)+direction*scale(idx))));render();}}
 void stepGroup(int direction){if(!canGroup(direction))return;if(canLink()){int[] t=Protocol.linkedScaled(shownVolume(zones[0]),shownVolume(zones[1]),scale(0),scale(1),maxVolume(0),maxVolume(1),direction*2);setVolumes(t[0],t[1]);}else for(int i=0;i<2;i++)if(canVolume(zones[i]))stepZone(i,direction);render();}
 boolean canGroup(int direction){
  if(!connected)return false;boolean any=false;
  for(int i=0;i<2;i++){Zone z=zones[i];if(z.power==null)return false;if(!Boolean.TRUE.equals(z.power))continue;
   if(!canVolume(z))return false;any=true;int target=shownVolume(z)+direction*scale(i);if(target<0||target>maxVolume(i))return false;
  }return any;
 }
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
 boolean canLink(){return canVolume(zones[0])&&canVolume(zones[1]);}
 void render(){expireVolumes();for(Zone z:zones){boolean on=Boolean.TRUE.equals(z.power);z.powerBtn.setEnabled(connected&&z.power!=null);z.powerBtn.setText(z.power==null?"--":on?"An":"Aus");z.powerBtn.setTextColor(on?ACCENT:FG);z.src.setEnabled(connected&&on);z.src.setText(z.source.isEmpty()?"Quelle":z.source.equals("2B")&&!service.isEmpty()?service:z.source.equals("80")?"SOURCE":sourceName(z.source));z.slider.setEnabled(canVolume(z));renderMedia(zoneIndex(z));if(!z.dragging){z.slider.setMax(maxVolume(zoneIndex(z)));z.slider.setProgress(Math.max(0,shownVolume(z)));z.value.setText(volumeText(z));z.slider.setContentDescription("Lautstärke "+(zoneIndex(z)==0?"Wohnzimmer":"Esstisch")+(z.unavailable?" · nicht verfügbar":" · Maximum "+Protocol.volumeLabel(maxVolume(zoneIndex(z)),scale(zoneIndex(z)))));}}
  groupMinus.setEnabled(canGroup(-1));groupPlus.setEnabled(canGroup(1));renderDetails();publishWidgetState();
 }
 String sourceName(String c){if(c.equals("80"))return "SOURCE";if(capabilities!=null){String name=capabilities.sourceName(c);if(name!=null)return name;}for(int i=0;i<codes.length;i++)if(codes[i].equals(c))return names[i];return c.isEmpty()?"Quelle unbekannt":"Quelle "+c;}
 LinkedHashMap<String,String> sources(int idx){if(capabilities!=null&&capabilities.hasSelectors)return capabilities.sources(idx);LinkedHashMap<String,String> out=new LinkedHashMap<>();if(idx==1)out.put("80","SOURCE");for(int j=0;j<codes.length;j++)if(idx==0||!codes[j].equals("05"))out.put(codes[j],names[j]);return out;}
 LinkedHashMap<String,String> services(int idx){LinkedHashMap<String,String> out=new LinkedHashMap<>();out.put("","NET allgemein");if(capabilities!=null&&capabilities.hasServices)out.putAll(capabilities.services(idx));else for(int j=1;j<netCodes.length;j++)out.put(netCodes[j],netNames[j]);return out;}
 long lastWidgetPublish;
 void publishWidgetState(){
  if(demo||!RadioWidget.exists(this)||!connected||capabilities==null||activeNetwork==null||zones[0].power==null||zones[1].power==null)return;
  if(RemoteVpn.selected(this)&&!activeNetwork.equals(RemoteVpn.network(this)))return;
  long now=SystemClock.elapsedRealtime();if(now-lastWidgetPublish<4000)return;lastWidgetPublish=now;
  RadioControl.State state=new RadioControl.State();
  for(int i=0;i<2;i++){state.on[i]=Boolean.TRUE.equals(zones[i].power);state.source[i]=zones[i].source;state.raw[i]=zones[i].vol;state.scale[i]=scale(i);state.max[i]=maxVolume(i);}
  int tuner=state.tuner();state.preset=tuner<0?-1:currentPreset[tuner];RadioWidget.store(this,state,capabilities,activeNetwork);
 }
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
  // Visible controls have even gaps; transport-only rows fill the width.
  boolean first=true,choiceVisible=z.mediaChoice.getVisibility()==View.VISIBLE;
  for(Button button:new Button[]{z.mediaPrev,z.mediaChoice,z.mediaPlay,z.mediaNext}){
   LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)button.getLayoutParams();
   if(button.getVisibility()!=View.VISIBLE)continue;
   int width=button==z.mediaChoice||!choiceVisible?0:dp(44);float weight=button==z.mediaChoice||!choiceVisible?1:0;
   int margin=first?0:dp(6);first=false;
   if(lp.width!=width||lp.weight!=weight||lp.leftMargin!=margin){lp.width=width;lp.weight=weight;lp.leftMargin=margin;button.setLayoutParams(lp);}
  }
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
 LinearLayout detailCard(int idx){
  Detail d=detailsPanels[idx];LinearLayout card=col();card.setPadding(dp(8),dp(4),dp(8),dp(4));card.setBackground(shape(PANEL,12));
  d.header=btn("");d.header.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);d.header.setTextSize(15);d.header.setContentDescription(idx==0?"Details Wohnzimmer":"Details Esstisch");add(card,d.header,-1,44);
  d.body=col();d.body.setPadding(dp(3),dp(4),dp(3),dp(2));add(card,d.body,-1,-2);
  d.mute=btn("Ton stummschalten");add(d.body,d.mute,-1,44);d.mute.setOnClickListener(v->{Zone z=zones[idx];if(zoneOn(idx)&&z.mute!=null)send((idx==0?"AMT":"ZMT")+(z.mute?"00":"01"));});
  if(idx==0){gap(d.body,4);d.mode=btn("Hörmodus");add(d.body,d.mode,-1,44);d.mode.setOnClickListener(v->modeDialog());
   audioRow(d,"Bass","TFRB","TFRBDOWN","TFRBUP",idx);audioRow(d,"Höhen","TFRT","TFRTDOWN","TFRTUP",idx);audioRow(d,"Center","CTL","CTLDOWN","CTLUP",idx);audioRow(d,"Subwoofer","SWL","SWLDOWN","SWLUP",idx);
  }else{
   audioRow(d,"Bass","ZTNB","ZTNBDOWN","ZTNBUP",idx);audioRow(d,"Höhen","ZTNT","ZTNTDOWN","ZTNTUP",idx);
  }
  d.header.setOnClickListener(v->{detailOpen[idx]=!detailOpen[idx];renderDetails();if(detailOpen[idx])queryDetails();});
  return card;
 }
 void audioRow(Detail d,String name,String key,String down,String up,int idx){
  LinearLayout r=row();weighted(r,label(name,14,FG),44);Button minus=btn("−"),plus=btn("+");add(r,minus,44,44);
  TextView value=label("–",14,MUTED);value.setGravity(Gravity.CENTER);add(r,value,42,44);add(r,plus,44,44);gap(d.body,3);add(d.body,r,-1,44);
  d.rows.put(key,r);d.values.put(key,value);d.buttons.put(key,new Button[]{minus,plus});
  minus.setOnClickListener(v->{if(zoneOn(idx)&&allowedAudio(key)&&audio.containsKey(key))send(down);});plus.setOnClickListener(v->{if(zoneOn(idx)&&allowedAudio(key)&&audio.containsKey(key))send(up);});
 }
 void radioPreset(int idx,String direction){String source=effectiveSource(idx);if(zoneOn(idx)&&isRadio(source))mediaAction(idx,source,(effectiveZone(idx)==0?"PRS":"PRZ")+direction);}
 void queryDetails(){if(receiver!=null&&connected){
  ArrayList<String> q=new ArrayList<>();if(detailOpen[0]&&zoneOn(0)){q.add("LMDQSTN");if(allowedAudio("TFRB")||allowedAudio("TFRT"))q.add("TFRQSTN");if(allowedAudio("CTL"))q.add("CTLQSTN");if(allowedAudio("SWL"))q.add("SWLQSTN");}
  if(detailOpen[1]&&zoneOn(1)&&(allowedAudio("ZTNB")||allowedAudio("ZTNT")))q.add("ZTNQSTN");
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
 boolean allowedAudio(String key){
  int idx=key.startsWith("ZTN")?1:0;String id;switch(key){case "TFRB":case "ZTNB":id="Bass";break;case "TFRT":case "ZTNT":id="Treble";break;case "CTL":id="Center Level";break;case "SWL":id="Subwoofer Level";break;default:return false;}
  if(idx==1)return capabilities!=null&&capabilities.hasControls&&capabilities.control(id,1);
  return capabilities==null||!capabilities.hasControls||capabilities.control(id,0);
 }
 void renderDetails(){
  for(int i=0;i<2;i++){Detail d=detailsPanels[i];if(d.header==null)continue;Zone z=zones[i];boolean on=zoneOn(i);
   d.header.setText((i==0?"Wohnzimmer":"Esstisch")+"   "+(detailOpen[i]?"▴":"▾"));d.body.setVisibility(detailOpen[i]?View.VISIBLE:View.GONE);
   d.mute.setText(z.mute==null?"Stumm · unbekannt":z.mute?"Stumm · aufheben":"Ton stummschalten");d.mute.setEnabled(on&&z.mute!=null);
   if(d.mode!=null){d.mode.setText("Hörmodus · "+modeName(audio.get("LMD")));d.mode.setEnabled(on&&audio.containsKey("LMD"));}
   for(String key:d.rows.keySet()){boolean supported=allowedAudio(key);d.rows.get(key).setVisibility(supported?View.VISIBLE:View.GONE);
    String v=audio.get(key);d.values.get(key).setText(v==null?"–":v.equals("00")?"0":v);
    boolean toneAllowed=!(key.startsWith("TFR")&&("01".equals(audio.get("LMD"))||"11".equals(audio.get("LMD"))));
    for(Button button:d.buttons.get(key))button.setEnabled(on&&supported&&toneAllowed&&v!=null);
   }
  }
 }
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);for(int i=0;i<2;i++)out.putBoolean("detail"+i,detailOpen[i]);}
 @Override public void onBackPressed(){if(detailOpen[0]||detailOpen[1]){detailOpen[0]=detailOpen[1]=false;renderDetails();}else super.onBackPressed();}
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
  new AlertDialog.Builder(this).setTitle("Homescreen-Widgets")
   .setItems(new String[]{"Szene hinzufügen","Lautstärke gemeinsam (2×1)","Radio (3×2)"},(d,n)->{
    Class<?> provider=n==0?SceneWidget.class:n==1?RadioWidget.Universal.class:RadioWidget.class;
    android.appwidget.AppWidgetManager m=android.appwidget.AppWidgetManager.getInstance(this);
    if(!m.isRequestPinAppWidgetSupported()||!m.requestPinAppWidget(new ComponentName(this,provider),null,null))
      Toast.makeText(this,"Homescreen lange drücken → Widgets → Onkyo dahoam",Toast.LENGTH_LONG).show();
   }).setNegativeButton("Schließen",null).show();
 }
 void connectionMenu(){new AlertDialog.Builder(this).setTitle("Verbindung & Diagnose").setItems(new String[]{"Automatisch suchen","IP manuell eingeben","Demo","Diagnosebericht","Homescreen-Widgets","Fernzugriff einrichten"},(d,n)->{
  if(n==0){Diagnostics.event("UI","automatic discovery requested");demo=false;manual=false;deviceId="";prefs.edit().putBoolean("manual",false).remove("deviceId").apply();SceneWidget.refreshAll(this);disconnect();tick();}
  else if(n==1)connectionDialog();else if(n==2)startDemo();else if(n==3)diagnosticsDialog();else if(n==4)widgetMenu();else startActivity(new Intent(this,RemoteSetupActivity.class));
 }).show();}
 void diagnosticsDialog(){
  Diagnostics.event("UI","diagnostics opened");
  String info="Lokales Protokoll · max. 256 KiB · automatische Löschung nach spätestens 72 Stunden bei laufender App. Bei wenig Speicher wird die Aufzeichnung pausiert.\nEnthält Befehle, Status, Fehler, Zeitpunkte und Geräteprofil. Keine IP/MAC, WLAN-Namen, Zugangsdaten oder Mediennamen. Nur auf deinen Wunsch exportiert.";
  try{String s=Diagnostics.snapshot();info+="\n\nLetzte Einträge:\n"+s.substring(Math.max(0,s.length()-1300));}catch(java.io.IOException e){Diagnostics.error("LOG_READ_ERROR",e);}
  TextView t=label(info,13,FG);t.setPadding(dp(16),dp(8),dp(16),dp(8));t.setTextIsSelectable(true);ScrollView sc=new ScrollView(this);sc.addView(t);
  new AlertDialog.Builder(this).setTitle("Diagnosebericht").setView(sc)
   .setPositiveButton("TXT exportieren",(d,n)->{Diagnostics.event("UI","export requested");Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("text/plain");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"Onkyo dahoam-Diagnose-v0.8.1.txt");try{startActivityForResult(i,404);}catch(ActivityNotFoundException e){Diagnostics.error("EXPORT_ERROR",e);Toast.makeText(this,"Dateiauswahl nicht verfügbar",Toast.LENGTH_LONG).show();}})
   .setNeutralButton("Log löschen",(d,n)->new AlertDialog.Builder(this).setMessage("Alle bisherigen Diagnoseeinträge löschen?").setNegativeButton("Abbrechen",null).setPositiveButton("Löschen",(x,y)->{try{Diagnostics.clear();}catch(java.io.IOException e){Diagnostics.error("LOG_CLEAR_ERROR",e);}}).show()).setNegativeButton("Schließen",null).show();
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=404||result!=RESULT_OK||data==null||data.getData()==null)return;
  try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData(),"wt")){if(out==null)throw new java.io.IOException("No output");out.write(Diagnostics.snapshot().getBytes(java.nio.charset.StandardCharsets.UTF_8));Toast.makeText(this,"Diagnose gespeichert",Toast.LENGTH_LONG).show();}catch(Exception e){Diagnostics.error("EXPORT_ERROR",e);Toast.makeText(this,"Export fehlgeschlagen",Toast.LENGTH_LONG).show();}
 }
 void connectionDialog(){EditText input=new EditText(this);input.setSingleLine(true);input.setHint("192.168.1.123");input.setText(ip);new AlertDialog.Builder(this).setTitle("Receiver verbinden").setMessage("Automatische Suche im lokalen WLAN/LAN. Alternativ IP eingeben. Netzwerk-Standby am Onkyo aktivieren.").setView(input).setPositiveButton("IP verbinden",(d,n)->{String v=input.getText().toString().trim();if(!validIp(v)){Toast.makeText(this,"Private IPv4-Adresse erforderlich",Toast.LENGTH_LONG).show();return;}Diagnostics.event("UI","manual IP accepted");ip=v;manual=true;demo=false;prefs.edit().putString("ip",ip).putBoolean("manual",true).apply();SceneWidget.refreshAll(this);disconnect();tick();}).setNeutralButton("Automatisch suchen",(d,n)->{demo=false;manual=false;deviceId="";prefs.edit().putBoolean("manual",false).remove("deviceId").apply();SceneWidget.refreshAll(this);disconnect();tick();}).setNegativeButton("Demo",(d,n)->startDemo()).show();}
 boolean validIp(String s){try{String[] p=s.split("\\.",-1);if(p.length!=4)return false;int[] a=new int[4];for(int i=0;i<4;i++){if(!p[i].matches("[0-9]{1,3}"))return false;a[i]=Integer.parseInt(p[i]);if(a[i]>255)return false;}return a[0]==10||a[0]==192&&a[1]==168||a[0]==172&&a[1]>=16&&a[1]<=31;}catch(Exception e){return false;}}
 Network lan(){return SceneWidget.lan(this);}
 void clear(){Arrays.fill(currentPreset,-1);for(Zone z:zones){z.power=null;z.mute=null;z.vol=-1;z.pending=-1;z.unavailable=false;z.source="";}service="";serviceCode="";audio.clear();capabilities=null;nextProfileQuery=0;}
 void disconnect(){if(connected||connecting||receiver!=null)Diagnostics.event("CONNECTION","disconnect generation="+generation);generation++;if(receiver!=null){receiver.close();receiver=null;}connected=false;connecting=false;choosing=false;activeNetwork=null;clear();}
 void tick(){if(!foreground||demo)return;Network n=lan();if(n==null){if(connected||connecting||receiver!=null)disconnect();if(RemoteVpn.selected(this))RemoteVpn.request(this);status.setText(RemoteVpn.selected(this)?RemoteVpn.status(this):"Offline · kein WLAN/LAN");render();return;}if(activeNetwork!=null&&!activeNetwork.equals(n)){Diagnostics.event("NETWORK","network changed");disconnect();}if(choosing)return;if(!connected&&!connecting){if(RemoteVpn.selected(this))connect(n,RemoteVpn.target(this));else if(manual&&!ip.isEmpty())connect(n);else discover(n);}else if(connected&&receiver!=null){receiver.query();queryProfile();queryDashboardMedia();queryDetails();}}
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
 void connect(Network n){connect(n,ip);}
 void connect(Network n,String host){disconnect();Diagnostics.event("CONNECTION","TCP connect manual="+manual);activeNetwork=n;connecting=true;status.setText(RemoteVpn.selected(this)?"Onkyo verbindet …":"Verbinde mit "+host+" …");render();final int g=generation;receiver=new Receiver(host,n.getSocketFactory(),new Receiver.Listener(){public void message(String s){runOnUiThread(()->{if(g==generation){receive(s);render();}});}public void status(String s,boolean online){runOnUiThread(()->{if(g==generation){Diagnostics.event("CONNECTION","online="+online+" generation="+g);connected=online;connecting=false;if(!online){clear();if(RemoteVpn.selected(MainActivity.this))new Thread(()->RemoteVpn.invalidate(MainActivity.this,n),"remote-invalidate").start();}status.setText(online?(RemoteVpn.selected(MainActivity.this)?RemoteVpn.status(MainActivity.this):"Verbunden · "+host):"Onkyo nicht erreichbar – neuer Versuch folgt");if(online){queryProfile();queryDetails();}render();}});}});}
 void startDemo(){disconnect();Diagnostics.event("UI","demo started");demo=true;connected=true;zones[0].power=true;zones[1].power=true;zones[0].vol=35;zones[1].vol=25;zones[0].source="2B";zones[1].source="80";zones[0].mute=false;zones[1].mute=false;serviceCode="0A";service="Spotify";capabilities=Capabilities.demo();audio.put("LMD","00");audio.put("TFRB","+2");audio.put("TFRT","00");audio.put("CTL","00");audio.put("SWL","-3");audio.put("ZTNB","00");audio.put("ZTNT","00");audio.put("ZBL","00");status.setText("DEMO · kein Receiver verbunden");render();}
 void send(String... commands){for(String c:commands)Diagnostics.command(demo?"DEMO_ACTION":"UI_ACTION",c);if(!connected||!demo&&RemoteVpn.selected(this)&&!Objects.equals(activeNetwork,RemoteVpn.network(this))){Diagnostics.event("UI","command blocked offline");return;}if(demo){for(String s:commands){if(s.startsWith("NSV")&&s.length()>=5){serviceCode=s.substring(3,5);service=netName(serviceCode);}else if(s.startsWith("PRS")||s.startsWith("PRZ")){int i=s.startsWith("PRS")?0:1;String value=s.substring(3);int n=currentPreset[i];if(value.equals("UP"))currentPreset[i]=n<1||n>=5?1:n+1;else if(value.equals("DOWN"))currentPreset[i]=n<=1?5:n-1;else receive(s);}else if(s.startsWith("TFR")||s.startsWith("ZTN")||s.startsWith("CTL")||s.startsWith("SWL")||s.startsWith("ZBL")){String key=s.startsWith("TFR")||s.startsWith("ZTN")?s.substring(0,4):s.substring(0,3);String old=audio.get(key);try{int number=Integer.parseInt(old);int delta=s.endsWith("DOWN")?-1:1;audio.put(key,String.format(Locale.ROOT,"%+d",Math.max(-10,Math.min(10,number+delta))));}catch(Exception ignored){}}else receive(s);}render();}else if(receiver!=null)receiver.send(commands);}
 void receive(String s){if(s.length()<5)return;String k=s.substring(0,3),v=s.substring(3);if(k.equals("NRI")){try{Capabilities c=Capabilities.parse(v);if(!c.model.equalsIgnoreCase("TX-NR656")){Diagnostics.event("PROFILE","unexpected model (omitted)");status.setText("Gerätemodell abweichend · Profil nicht übernommen");return;}capabilities=c;if(!demo)SceneProfile.save(this,v);Diagnostics.event("PROFILE",c.summary());service=netName(serviceCode);nextProfileQuery=SystemClock.elapsedRealtime()+120000;status.setText(RemoteVpn.selected(this)?RemoteVpn.status(this):"Verbunden · "+ip+" · Geräteprofil erkannt");}catch(Exception e){Diagnostics.error("PROFILE_PARSE_ERROR",e);}return;}if(v.equals("N/A")||v.equals("QSTN")){if(v.equals("N/A")&&(k.equals("MVL")||k.equals("ZVL"))){Zone unavailable=zones[k.equals("MVL")?0:1];unavailable.vol=-1;unavailable.pending=-1;unavailable.unavailable=true;Diagnostics.event("VOLUME","not available zone="+(k.equals("MVL")?1:2));}audio.remove(k);audio.remove(k+"B");audio.remove(k+"T");return;}
  Zone z=(k.equals("ZPW")||k.equals("ZVL")||k.equals("SLZ")||k.equals("ZMT"))?zones[1]:zones[0];
  switch(k){case "PRS":case "PRZ":if(v.matches("[0-9A-Fa-f]{2}")){int n=Integer.parseInt(v,16);if(n>=1&&n<=40)currentPreset[k.equals("PRS")?0:1]=n;}break;case "LMD":case "CTL":case "SWL":case "ZBL":audio.put(k,v);break;case "TFR":case "ZTN":if(v.length()>=6&&v.charAt(0)=='B'&&v.charAt(3)=='T'){audio.put(k+"B",v.substring(1,3));audio.put(k+"T",v.substring(4,6));}break;case "PWR":case "ZPW":if(v.equals("00")||v.equals("01")){z.power=v.equals("01");if(!z.power)z.pending=-1;}break;case "MVL":case "ZVL":receiveVolume(z,v);break;case "SLI":case "SLZ":if(v.matches("[0-9A-Fa-f]{2}")){String next=v.toUpperCase(Locale.ROOT);if(!next.equals(z.source)){z.source=next;currentPreset[k.equals("SLI")?0:1]=-1;if(!demo)queryDashboardMedia();}}break;case "AMT":case "ZMT":if(v.equals("00")||v.equals("01"))z.mute=v.equals("01");break;case "NMS":if(v.length()>=9){serviceCode=v.substring(7,9).toUpperCase(Locale.ROOT);service=netName(serviceCode);}break;default:break;}
 }
 @Override protected void onStart(){super.onStart();SceneWidget.observeNetwork(this);SceneWidget.refreshAll(this);Diagnostics.event("APP","foreground");foreground=true;handler.post(poll);}
 @Override protected void onStop(){Diagnostics.event("APP","background");foreground=false;handler.removeCallbacks(poll);if(!demo){disconnect();render();}super.onStop();}
 @Override protected void onDestroy(){handler.removeCallbacks(poll);disconnect();super.onDestroy();}
}
