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
 static final int BG=0xff101619,PANEL=0xff20292d,FG=0xffeef3f1,MUTED=0xffa8b9bb,ACCENT=0xff7bd8bc,CAP=50;
 final String[] names={"TV","CBL/SAT","GAME","BD/DVD","STRM BOX","PC","AUX","CD","PHONO","FM","NET","Bluetooth"};
 final String[] codes={"12","01","02","10","11","05","03","23","22","24","2B","2E"};
 final String[] netNames={"NET allgemein","Spotify","TuneIn Radio","Musikserver","Favoriten","Deezer"};
 final String[] netCodes={"","0A","0E","00","01","12"};
 final Zone[] zones={new Zone(),new Zone()};
 TextView status,groupText;SeekBar group;Receiver receiver;SharedPreferences prefs;
 LinearLayout dashboard,detailPage; int detailIndex=-1; TextView detailStatus,detailVolume; Button detailPower,detailSource,detailMute,modeButton; SeekBar detailSlider; boolean detailDragging,manual,choosing; String deviceId=""; Network activeNetwork; final Map<String,String> audio=new HashMap<>(); final ArrayList<Button> detailCommands=new ArrayList<>(); final ArrayList<TextView> audioLabels=new ArrayList<>();
 boolean connected,demo,connecting,foreground,dragGroup;int generation,groupA,groupB;String ip="",service="";
 final Handler handler=new Handler(Looper.getMainLooper());
 static class Zone{Boolean power,mute;int vol=-1;String source="";TextView value;Button powerBtn,src;SeekBar slider;boolean dragging;}
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
 @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("config",MODE_PRIVATE);
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  LinearLayout root=col();root.setBackgroundColor(BG);root.setPadding(dp(12),dp(6),dp(12),dp(6));dashboard=root;setContentView(root);
  LinearLayout head=row();TextView title=label("TX-NR656",22,FG);title.setTypeface(null,Typeface.BOLD);weighted(head,title,44);
  Button config=btn("Verbindung");add(head,config,112,44);config.setOnClickListener(v->connectionDialog());add(root,head,-1,44);
  status=label("",13,MUTED);status.setMaxLines(2);add(root,status,-1,34);gap(root,4);
  root.addView(zoneCard(0),new LinearLayout.LayoutParams(-1,0,1));gap(root,6);
  root.addView(zoneCard(1),new LinearLayout.LayoutParams(-1,0,1));gap(root,6);
  LinearLayout linked=col();linked.setPadding(dp(12),dp(4),dp(12),dp(4));linked.setBackground(shape(PANEL,14));
  groupText=label("Beide Zonen",16,FG);add(linked,groupText,-1,28);group=new SeekBar(this);group.setMax(2*CAP);add(linked,group,-1,44);add(root,linked,-1,80);
  group.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onStartTrackingTouch(SeekBar s){dragGroup=true;groupA=zones[0].vol;groupB=zones[1].vol;}
   public void onProgressChanged(SeekBar s,int n,boolean user){if(user&&dragGroup){try{int[] a=Protocol.linked(groupA,groupB,(int)Math.round((n-groupA-groupB)/2.0),CAP);groupText.setText("Beide Zonen   "+a[0]+" / "+a[1]);}catch(Exception ignored){}}}
   public void onStopTrackingTouch(SeekBar s){dragGroup=false;if(canLink()&&groupA==zones[0].vol&&groupB==zones[1].vol){int[] a=Protocol.linked(groupA,groupB,(int)Math.round((s.getProgress()-groupA-groupB)/2.0),CAP);send("MVL"+Protocol.hex(a[0]),"ZVL"+Protocol.hex(a[1]));}render();}
  });
  ip=prefs.getString("ip","");manual=prefs.getBoolean("manual",false);deviceId=prefs.getString("deviceId","");demo=false;status.setText("Receiver wird gesucht …");render();
 }
 LinearLayout zoneCard(int idx){Zone z=zones[idx];LinearLayout c=col();c.setPadding(dp(12),dp(5),dp(12),dp(5));c.setBackground(shape(PANEL,14));c.setGravity(Gravity.CENTER_VERTICAL);
  LinearLayout top=row();TextView title=label(idx==0?"Hauptzone":"Nebenzone",20,FG);title.setTypeface(null,Typeface.BOLD);weighted(top,title,44);z.powerBtn=btn("--");add(top,z.powerBtn,74,44);hgap(top,5);Button details=btn("›");add(top,details,44,44);add(c,top,-1,44);gap(c,4);
  z.src=btn("Quelle");add(c,z.src,-1,44);gap(c,2);
  LinearLayout vol=row();z.slider=new SeekBar(this);z.slider.setMax(CAP);weighted(vol,z.slider,44);z.value=label("--",23,FG);z.value.setGravity(Gravity.CENTER);add(vol,z.value,48,44);add(c,vol,-1,44);
  z.powerBtn.setOnClickListener(v->{if(z.power!=null)send((idx==0?"PWR":"ZPW")+(z.power?"00":"01"));});z.src.setOnClickListener(v->sourceDialog(idx));details.setOnClickListener(v->details(idx));
  z.slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){z.dragging=true;}public void onProgressChanged(SeekBar s,int n,boolean user){if(user)z.value.setText(""+n);}public void onStopTrackingTouch(SeekBar s){z.dragging=false;if(canVolume(z))send((idx==0?"MVL":"ZVL")+Protocol.hex(s.getProgress()));render();}});
  return c;
 }
 boolean canVolume(Zone z){return connected&&Boolean.TRUE.equals(z.power)&&z.vol>=0&&z.vol<=CAP;}
 boolean canLink(){return canVolume(zones[0])&&canVolume(zones[1])&&zones[1].source.equals("80");}
 void render(){for(Zone z:zones){boolean on=Boolean.TRUE.equals(z.power);z.powerBtn.setEnabled(connected&&z.power!=null);z.powerBtn.setText(z.power==null?"--":on?"An":"Aus");z.powerBtn.setTextColor(on?ACCENT:FG);z.src.setEnabled(connected&&on);z.src.setText(sourceName(z.source)+(z.source.equals("2B")&&!service.isEmpty()?" / "+service:"")+(z.source.equals("80")?" · "+sourceName(zones[0].source):""));z.slider.setEnabled(canVolume(z));if(!z.dragging){z.slider.setProgress(Math.max(0,Math.min(CAP,z.vol)));z.value.setText(z.vol<0?"--":""+z.vol);}}
  group.setEnabled(canLink());if(!dragGroup){group.setProgress(Math.max(0,zones[0].vol+zones[1].vol));groupText.setText(canLink()?"Beide Zonen   "+zones[0].vol+" / "+zones[1].vol:"Beide Zonen · nur SOURCE");} renderDetails();
 }
 String sourceName(String c){if(c.equals("80"))return "SOURCE";for(int i=0;i<codes.length;i++)if(codes[i].equals(c))return names[i];return c.isEmpty()?"Quelle unbekannt":"Quelle "+c;}
 void sourceDialog(int idx){ArrayList<String> labels=new ArrayList<>(Arrays.asList(names));if(idx==1)labels.add(0,"SOURCE");new AlertDialog.Builder(this).setTitle("Quelle auswählen").setItems(labels.toArray(new String[0]),(d,n)->{String name=labels.get(n),prefix=idx==0?"SLI":"SLZ";if(name.equals("SOURCE")){send(prefix+"80");return;}int j=Arrays.asList(names).indexOf(name);if(name.equals("NET")){new AlertDialog.Builder(this).setTitle("NET-Dienst (experimentell)").setItems(netNames,(dd,k)->{if(k==0)send(prefix+"2B");else if(idx==1&&Boolean.TRUE.equals(zones[0].power)&&zones[0].source.equals("2B"))new AlertDialog.Builder(this).setMessage("NET-Dienst kann auch die Hauptzone ändern.").setNegativeButton("Abbrechen",null).setPositiveButton("Wechseln",(x,y)->send(prefix+"2B","NSV"+netCodes[k]+"0")).show();else send(prefix+"2B","NSV"+netCodes[k]+"0");}).show();}else send(prefix+codes[j]);}).show();}
 void details(int idx){
  detailIndex=idx; detailDragging=false; detailCommands.clear();audioLabels.clear();modeButton=null;
  detailPage=col();detailPage.setBackgroundColor(BG);detailPage.setPadding(dp(12),dp(6),dp(12),dp(6));
  LinearLayout top=row();Button back=btn("‹");add(top,back,48,48);hgap(top,10);weighted(top,label(idx==0?"Hauptzone":"Nebenzone",23,FG),48);back.setOnClickListener(v->closeDetails());add(detailPage,top,-1,48);
  detailStatus=label("",13,MUTED);add(detailPage,detailStatus,-1,32);
  ScrollView scroll=new ScrollView(this);LinearLayout content=col();scroll.addView(content);detailPage.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout controls=section(content,"Steuerung");
  LinearLayout powerRow=row();detailPower=btn("–");detailSource=btn("Quelle");add(powerRow,detailPower,86,48);hgap(powerRow,6);weighted(powerRow,detailSource,48);add(controls,powerRow,-1,48);
  detailPower.setOnClickListener(v->{Zone z=zones[idx];if(z.power!=null)send((idx==0?"PWR":"ZPW")+(z.power?"00":"01"));});
  detailSource.setOnClickListener(v->sourceDialog(idx));gap(controls,6);
  LinearLayout volumeRow=row();detailSlider=new SeekBar(this);detailSlider.setMax(CAP);weighted(volumeRow,detailSlider,48);detailVolume=label("--",22,FG);add(volumeRow,detailVolume,48,48);add(controls,volumeRow,-1,48);
  detailSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){detailDragging=true;}public void onProgressChanged(SeekBar s,int n,boolean user){if(user)detailVolume.setText(""+n);}public void onStopTrackingTouch(SeekBar s){detailDragging=false;if(canVolume(zones[idx]))send((idx==0?"MVL":"ZVL")+Protocol.hex(s.getProgress()));renderDetails();}});
  detailMute=btn("Stumm");add(controls,detailMute,-1,48);detailMute.setOnClickListener(v->{Zone z=zones[idx];if(z.mute!=null)send((idx==0?"AMT":"ZMT")+(z.mute?"00":"01"));});
  if(idx==0){
   LinearLayout sound=section(content,"Klang");
   modeButton=btn("Listening Mode");add(sound,modeButton,-1,48);modeButton.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Listening Mode").setItems(new String[]{"Stereo","Direct","All Channel Stereo","Pure Audio"},(d,i)->send("LMD"+new String[]{"00","01","0C","11"}[i])).show());
   gap(sound,4);audioRow(sound,"Bass","TFRB","TFRBDOWN","TFRBUP");audioRow(sound,"Höhen","TFRT","TFRTDOWN","TFRTUP");audioRow(sound,"Center","CTL","CTLDOWN","CTLUP");audioRow(sound,"Subwoofer","SWL","SWLDOWN","SWLUP");
  }else{
   LinearLayout sound=section(content,"Klang");audioRow(sound,"Bass","ZTNB","ZTNBDOWN","ZTNBUP");audioRow(sound,"Höhen","ZTNT","ZTNTDOWN","ZTNTUP");audioRow(sound,"Balance","ZBL","ZBLDOWN","ZBLUP");
   TextView note=label("Nur vom Receiver bestätigte Funktionen sind aktiv.",12,MUTED);note.setPadding(0,dp(4),0,dp(4));add(sound,note,-1,-2);
  }
  LinearLayout play=section(content,"Radio");
  TextView freq=label("FM-Speicherplätze",14,MUTED);add(play,freq,-1,28);
  LinearLayout presets=row();Button prev=btn("‹ Sender"),next=btn("Sender ›");prev.setTag("radio");next.setTag("radio");detailCommands.add(prev);detailCommands.add(next);weighted(presets,prev,48);hgap(presets,6);weighted(presets,next,48);add(play,presets,-1,48);
  prev.setOnClickListener(v->radioPreset(idx,"DOWN"));next.setOnClickListener(v->radioPreset(idx,"UP"));
  TextView limit=label("Testversion · Lautstärkelimit "+CAP,12,MUTED);add(content,limit,-1,32);
  setContentView(detailPage);queryDetails();renderDetails();
 }
 LinearLayout section(LinearLayout parent,String title){gap(parent,8);LinearLayout c=col();c.setBackground(shape(PANEL,14));c.setPadding(dp(12),dp(8),dp(12),dp(10));TextView t=label(title,17,FG);t.setTypeface(null,Typeface.BOLD);add(c,t,-1,30);add(parent,c,-1,-2);return c;}
 void audioRow(LinearLayout parent,String name,String key,String down,String up){
  LinearLayout r=row();weighted(r,label(name,15,FG),48);TextView value=label("–",14,MUTED);value.setTag(key);value.setGravity(Gravity.CENTER);audioLabels.add(value);add(r,value,54,48);Button minus=btn("−"),plus=btn("+");minus.setTag(key);plus.setTag(key);detailCommands.add(minus);detailCommands.add(plus);add(r,minus,48,48);hgap(r,5);add(r,plus,48,48);add(parent,r,-1,48);gap(parent,4);
  minus.setOnClickListener(v->send(down));plus.setOnClickListener(v->send(up));
 }
 void radioPreset(int idx,String direction){Zone z=zones[idx];if(!connected||!Boolean.TRUE.equals(z.power))return;String prefix=idx==0?"PRS":"PRZ";if(idx==1&&z.source.equals("80"))prefix="PRS";send(prefix+direction);}
 void queryDetails(){if(receiver!=null&&connected&&detailIndex>=0){if(detailIndex==0)receiver.send("LMDQSTN","TFRQSTN","CTLQSTN","SWLQSTN");else receiver.send("ZTNQSTN","ZBLQSTN");}}
 String modeName(String code){if(code==null)return "–";switch(code){case "00":return "Stereo";case "01":return "Direct";case "0C":return "All Channel Stereo";case "11":return "Pure Audio";default:return code==null?"–":code;}}
 void renderDetails(){if(detailIndex<0)return;Zone z=zones[detailIndex];boolean on=connected&&Boolean.TRUE.equals(z.power);
  detailStatus.setText(status.getText());detailPower.setText(z.power==null?"–":on?"An":"Aus");detailPower.setEnabled(connected&&z.power!=null);detailSource.setText(sourceName(z.source)+(z.source.equals("2B")&&!service.isEmpty()?" / "+service:""));detailSource.setEnabled(on);
  detailSlider.setEnabled(canVolume(z));if(!detailDragging){detailSlider.setProgress(Math.max(0,Math.min(CAP,z.vol)));detailVolume.setText(z.vol<0?"–":""+z.vol);}
  detailMute.setText(z.mute==null?"Mute · unbekannt":z.mute?"Stumm · aufheben":"Ton stummschalten");detailMute.setEnabled(on&&z.mute!=null);
  if(modeButton!=null){modeButton.setText("Listening Mode · "+modeName(audio.get("LMD")));modeButton.setEnabled(on&&audio.containsKey("LMD"));}
  for(TextView t:audioLabels){String v=audio.get((String)t.getTag());t.setText(v==null?"–":v.equals("00")?"0":v);}
  for(Button b:detailCommands){String k=(String)b.getTag();boolean radio=z.source.equals("24")||z.source.equals("80")&&zones[0].source.equals("24");b.setEnabled(on&&(k.equals("radio")?radio:audio.containsKey(k)));}
 }
 void closeDetails(){detailIndex=-1;detailDragging=false;setContentView(dashboard);render();}
 @Override public void onBackPressed(){if(detailIndex>=0)closeDetails();else super.onBackPressed();}
 void connectionDialog(){EditText input=new EditText(this);input.setSingleLine(true);input.setHint("192.168.1.123");input.setText(ip);new AlertDialog.Builder(this).setTitle("Receiver verbinden").setMessage("Automatische Suche im lokalen WLAN/LAN. Alternativ IP eingeben. Netzwerk-Standby am Onkyo aktivieren.").setView(input).setPositiveButton("IP verbinden",(d,n)->{String v=input.getText().toString().trim();if(!validIp(v)){Toast.makeText(this,"Private IPv4-Adresse erforderlich",Toast.LENGTH_LONG).show();return;}ip=v;manual=true;demo=false;prefs.edit().putString("ip",ip).putBoolean("manual",true).apply();disconnect();tick();}).setNeutralButton("Automatisch suchen",(d,n)->{demo=false;manual=false;deviceId="";prefs.edit().putBoolean("manual",false).remove("deviceId").apply();disconnect();tick();}).setNegativeButton("Demo",(d,n)->startDemo()).show();}
 boolean validIp(String s){try{String[] p=s.split("\\.",-1);if(p.length!=4)return false;int[] a=new int[4];for(int i=0;i<4;i++){if(!p[i].matches("[0-9]{1,3}"))return false;a[i]=Integer.parseInt(p[i]);if(a[i]>255)return false;}return a[0]==10||a[0]==192&&a[1]==168||a[0]==172&&a[1]>=16&&a[1]<=31;}catch(Exception e){return false;}}
 Network lan(){ConnectivityManager m=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);for(Network n:m.getAllNetworks()){NetworkCapabilities c=m.getNetworkCapabilities(n);if(c!=null&&(c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)))return n;}return null;}
 void clear(){for(Zone z:zones){z.power=null;z.mute=null;z.vol=-1;z.source="";}service="";audio.clear();}
 void disconnect(){generation++;if(receiver!=null){receiver.close();receiver=null;}connected=false;connecting=false;choosing=false;activeNetwork=null;clear();}
 void tick(){if(!foreground||demo)return;Network n=lan();if(n==null){disconnect();status.setText("Kein WLAN/LAN – bitte Heimnetz verbinden");render();return;}if(activeNetwork!=null&&!activeNetwork.equals(n)){disconnect();}if(choosing)return;if(!connected&&!connecting){if(manual&&!ip.isEmpty())connect(n);else discover(n);}else if(connected&&receiver!=null){receiver.query();queryDetails();}}
 void discover(Network n){disconnect();connecting=true;activeNetwork=n;int g=generation;status.setText("Suche TX-NR656 im Netzwerk …");render();
  ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);LinkProperties lp=cm.getLinkProperties(n);
  ArrayList<java.net.InetAddress> targets=new ArrayList<>();if(lp!=null)for(LinkAddress l:lp.getLinkAddresses()){if(l.getAddress() instanceof java.net.Inet4Address){try{targets.add(Discovery.broadcast(l.getAddress(),l.getPrefixLength()));}catch(Exception ignored){}}}
  new Thread(()->{try(java.net.DatagramSocket s=new java.net.DatagramSocket(null)){n.bindSocket(s);s.bind(new java.net.InetSocketAddress(0));List<Discovery.Device> found=Discovery.find(s,targets,1800);runOnUiThread(()->{if(g!=generation||!foreground)return;connecting=false;List<Discovery.Device> matches=new ArrayList<>();for(Discovery.Device d:found)if(d.model.equalsIgnoreCase("TX-NR656")&&d.port==60128)matches.add(d);
   if(matches.isEmpty()){status.setText("TX-NR656 nicht gefunden · Netzwerk-Standby prüfen");render();return;}
   if(!deviceId.isEmpty())for(Discovery.Device d:matches)if(deviceId.equals(d.id)){selectDevice(d,n);return;}
   if(matches.size()==1){selectDevice(matches.get(0),n);return;}
   choosing=true;String[] labels=new String[matches.size()];for(int i=0;i<matches.size();i++)labels[i]=matches.get(i).model+" · "+matches.get(i).ip;
   new AlertDialog.Builder(this).setTitle("Receiver auswählen").setItems(labels,(d,i)->{choosing=false;if(g==generation)selectDevice(matches.get(i),n);}).setNegativeButton("Abbrechen",(d,i)->{choosing=false;status.setText("Auswahl abgebrochen");}).setOnCancelListener(d->choosing=false).show();
  });}catch(Exception e){runOnUiThread(()->{if(g==generation){connecting=false;status.setText("Gerätesuche fehlgeschlagen · IP manuell möglich");render();}});}},"onkyo-discovery").start();
 }
 void selectDevice(Discovery.Device d,Network n){ip=d.ip;deviceId=d.id;prefs.edit().putString("ip",ip).putString("deviceId",deviceId).apply();connect(n);}
 final Runnable poll=new Runnable(){public void run(){tick();handler.postDelayed(this,4000);}};
 void connect(Network n){disconnect();activeNetwork=n;connecting=true;status.setText("Verbinde mit "+ip+" …");render();final int g=generation;receiver=new Receiver(ip,n.getSocketFactory(),new Receiver.Listener(){public void message(String s){runOnUiThread(()->{if(g==generation){receive(s);render();}});}public void status(String s,boolean online){runOnUiThread(()->{if(g==generation){connected=online;connecting=false;if(!online){clear();}status.setText(online?"Verbunden · "+ip:"Nicht erreichbar – neuer Versuch folgt");if(online)queryDetails();render();}});}});}
 void startDemo(){disconnect();demo=true;connected=true;zones[0].power=true;zones[1].power=true;zones[0].vol=35;zones[1].vol=25;zones[0].source="2B";zones[1].source="80";zones[0].mute=false;zones[1].mute=false;service="Spotify";audio.put("LMD","00");audio.put("TFRB","+2");audio.put("TFRT","00");audio.put("CTL","00");audio.put("SWL","-3");audio.put("ZTNB","00");audio.put("ZTNT","00");audio.put("ZBL","00");status.setText("DEMO · kein Receiver verbunden");render();}
 void send(String... commands){if(!connected)return;if(demo){for(String s:commands){if(s.startsWith("NSV")&&s.length()>=5){for(int i=1;i<netCodes.length;i++)if(netCodes[i].equals(s.substring(3,5)))service=netNames[i];}else if(s.startsWith("TFR")||s.startsWith("ZTN")||s.startsWith("CTL")||s.startsWith("SWL")||s.startsWith("ZBL")){String key=s.startsWith("TFR")||s.startsWith("ZTN")?s.substring(0,4):s.substring(0,3);String old=audio.get(key);try{int number=Integer.parseInt(old);int delta=s.endsWith("DOWN")?-1:1;audio.put(key,String.format(Locale.ROOT,"%+d",Math.max(-10,Math.min(10,number+delta))));}catch(Exception ignored){}}else receive(s);}render();}else if(receiver!=null)receiver.send(commands);}
 void receive(String s){if(s.length()<5)return;String k=s.substring(0,3),v=s.substring(3);if(v.equals("N/A")||v.equals("QSTN")){audio.remove(k);audio.remove(k+"B");audio.remove(k+"T");return;}
  Zone z=(k.equals("ZPW")||k.equals("ZVL")||k.equals("SLZ")||k.equals("ZMT"))?zones[1]:zones[0];
  switch(k){case "LMD":case "CTL":case "SWL":case "ZBL":audio.put(k,v);break;case "TFR":case "ZTN":if(v.length()>=6&&v.charAt(0)=='B'&&v.charAt(3)=='T'){audio.put(k+"B",v.substring(1,3));audio.put(k+"T",v.substring(4,6));}break;case "PWR":case "ZPW":if(v.equals("00")||v.equals("01"))z.power=v.equals("01");break;case "MVL":case "ZVL":z.vol=Protocol.volume(v);break;case "SLI":case "SLZ":if(v.matches("[0-9A-Fa-f]{2}"))z.source=v.toUpperCase(Locale.ROOT);break;case "AMT":case "ZMT":if(v.equals("00")||v.equals("01"))z.mute=v.equals("01");break;case "NMS":if(v.length()>=9){String code=v.substring(7,9);service="";for(int i=1;i<netCodes.length;i++)if(netCodes[i].equals(code))service=netNames[i];}break;default:break;}
 }
 @Override protected void onStart(){super.onStart();foreground=true;handler.post(poll);}
 @Override protected void onStop(){foreground=false;handler.removeCallbacks(poll);if(!demo){disconnect();render();}super.onStop();}
 @Override protected void onDestroy(){handler.removeCallbacks(poll);disconnect();super.onDestroy();}
}
