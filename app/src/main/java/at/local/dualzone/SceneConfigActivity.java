package at.local.dualzone;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;
public final class SceneConfigActivity extends Activity {
 static final int BG=MainActivity.BG,PANEL=MainActivity.PANEL,FG=MainActivity.FG,MUTED=MainActivity.MUTED,ACCENT=MainActivity.ACCENT;
 int id;EditText name;String savedIcon="Radio";CheckBox shared,demo;ZoneForm[] forms=new ZoneForm[2];LinearLayout root;Capabilities profile;
 int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
 LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 void add(LinearLayout parent,View v,int h){parent.addView(v,new LinearLayout.LayoutParams(-1,h<0?h:dp(h)));}
 void weight(LinearLayout r,View v,int h,float w){r.addView(v,new LinearLayout.LayoutParams(0,h<0?h:dp(h),w));}
 void gap(LinearLayout p,int h){add(p,new View(this),h);}
 GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(10));return d;}
 TextView label(String t,int size,int color){TextView v=new TextView(this);v.setText(t);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
 Button button(String text){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(FG);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(10),0,dp(10),0);b.setBackground(shape(0xff344247));return b;}
 LinearLayout card(LinearLayout parent){LinearLayout c=col();c.setPadding(dp(10),dp(8),dp(10),dp(8));c.setBackground(shape(PANEL));add(parent,c,-2);return c;}
 final class Choice {
  final Button button;final String title;LinkedHashMap<String,String> entries=new LinkedHashMap<>();String key="";Runnable change=()->{};
  Choice(LinearLayout parent,String title,LinkedHashMap<String,String> options,String selected){
   this.title=title;button=button("");button.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);button.setSingleLine(true);button.setEllipsize(android.text.TextUtils.TruncateAt.END);
   LinearLayout line=row();weight(line,label(title,13,MUTED),44,1);weight(line,button,44,2);add(parent,line,44);
   set(options,selected);button.setOnClickListener(v->{String[] keys=entries.keySet().toArray(new String[0]);String[] labels=entries.values().toArray(new String[0]);new AlertDialog.Builder(SceneConfigActivity.this).setTitle(title).setSingleChoiceItems(labels,new ArrayList<>(entries.keySet()).indexOf(key),(dialog,n)->{key=keys[n];render();dialog.dismiss();change.run();}).setNegativeButton("Abbrechen",null).show();});
  }
  void set(LinkedHashMap<String,String> options,String selected){entries=options;key=entries.containsKey(selected)?selected:entries.keySet().iterator().next();render();}
  void render(){button.setText(entries.get(key)+"  ▾");button.setContentDescription(title+": "+entries.get(key));}
 }
 static LinkedHashMap<String,String> options(String[] ids,String[] names){LinkedHashMap<String,String> m=new LinkedHashMap<>();for(int i=0;i<ids.length;i++)m.put(ids[i],names[i]);return m;}
 static LinkedHashMap<String,String> supported(String[] ids,String[] names,Map<String,String> available,String old,int index){
  LinkedHashMap<String,String> m=new LinkedHashMap<>();for(int i=0;i<ids.length;i++){String key=ids[i];if(index==0&&key.equals("80")||index==1&&key.equals("05"))continue;
   if(key.isEmpty()||available==null||available.containsKey(key)||key.equals(old))m.put(key,available!=null&&available.containsKey(key)?available.get(key):names[i]);}return m;
 }
 static LinkedHashMap<String,String> presets(Capabilities c,String source,int previous){
  LinkedHashMap<String,String> m=new LinkedHashMap<>();m.put("-1","Unverändert");
  if(c==null||!c.hasPresets){for(int i=1;i<=40;i++)m.put(""+i,String.format(Locale.ROOT,"P%02d",i));}
  else for(Map.Entry<Integer,String> e:c.radioPresets(source).entrySet())m.put(""+e.getKey(),e.getValue());
  if(previous>0&&!m.containsKey(""+previous))m.put(""+previous,String.format(Locale.ROOT,"P%02d · bisher",previous));return m;
 }
 class ZoneForm{
  final int index;final Choice power,source,preset,service;final LinearLayout body,presetRow,serviceRow,volumeRow;
  final CheckBox volumeEnabled;final SeekBar volume;final TextView volumeValue,hint;String presetBand;int previousPreset;
  ZoneForm(int index,Scene.Zone old){
   this.index=index;previousPreset=old.preset;presetBand=old.source;
   LinearLayout box=card(root);TextView title=label(index==0?"Wohnzimmer":"Esstisch",17,FG);title.setTypeface(null,Typeface.BOLD);add(box,title,28);
   power=new Choice(box,"Power",options(new String[]{"0","1","2"},new String[]{"Unverändert","Einschalten","Ausschalten"}),""+old.power);
   body=col();add(box,body,-2);
   source=new Choice(body,"Quelle",supported(Scene.SOURCE_CODES,Scene.SOURCE_NAMES,profile!=null&&profile.hasSelectors?profile.sources(index):null,old.source,index),old.source);
   presetRow=col();add(body,presetRow,-2);preset=new Choice(presetRow,"Sender",presets(profile,old.source,old.preset),""+old.preset);
   serviceRow=col();add(body,serviceRow,-2);service=new Choice(serviceRow,"NET-Dienst",supported(Scene.NET_CODES,Scene.NET_NAMES,profile!=null&&profile.hasServices?profile.services(index):null,old.service,-1),old.service);
   volumeEnabled=new CheckBox(SceneConfigActivity.this);volumeEnabled.setText("Lautstärke festlegen");volumeEnabled.setTextColor(FG);volumeEnabled.setTextSize(13);volumeEnabled.setChecked(old.volume>=0);volumeEnabled.setButtonTintList(ColorStateList.valueOf(ACCENT));add(body,volumeEnabled,40);
   volumeRow=row();volume=new SeekBar(SceneConfigActivity.this);int max=profile==null?100:Math.min(100,Protocol.maxVolume(profile.volumeMax[index],profile.volumeScale[index],0)/profile.volumeScale[index]);
   volume.setMax(Math.max(max,old.volume));volume.setProgress(old.volume<0?Math.min(45,max):old.volume);volume.setProgressTintList(ColorStateList.valueOf(ACCENT));volume.setThumbTintList(ColorStateList.valueOf(ACCENT));weight(volumeRow,volume,44,1);volumeValue=label(""+volume.getProgress(),19,ACCENT);volumeValue.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);volumeRow.addView(volumeValue,new LinearLayout.LayoutParams(dp(42),dp(44)));add(body,volumeRow,44);
   hint=label("",11,0xffffbd85);add(body,hint,-2);
   volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int n,boolean user){volumeValue.setText(""+n);update();}});
   power.change=()->update();source.change=()->{if(!source.key.equals(presetBand)){presetBand=source.key;previousPreset=-1;preset.set(presets(profile,presetBand,-1),"-1");}update();};preset.change=()->update();service.change=()->update();volumeEnabled.setOnCheckedChangeListener((b,on)->update());update();
  }
  void update(){
   boolean enabled=!power.key.equals("2");body.setVisibility(enabled?View.VISIBLE:View.GONE);
   presetRow.setVisibility(source.key.equals("24")||source.key.equals("25")?View.VISIBLE:View.GONE);serviceRow.setVisibility(source.key.equals("2B")?View.VISIBLE:View.GONE);
   volumeRow.setVisibility(volumeEnabled.isChecked()?View.VISIBLE:View.GONE);
   boolean keep=power.key.equals("0")&&(!source.key.isEmpty()||volumeEnabled.isChecked());
   hint.setText("Nur wirksam, wenn die Zone bereits eingeschaltet ist.");hint.setVisibility(keep?View.VISIBLE:View.GONE);
  }
  Scene.Zone value(){int p=Integer.parseInt(power.key);if(p==Scene.OFF)return new Scene.Zone(p,"",-1,-1,"");
   return new Scene.Zone(p,source.key,volumeEnabled.isChecked()?volume.getProgress():-1,presetRow.getVisibility()==View.VISIBLE?Integer.parseInt(preset.key):-1,serviceRow.getVisibility()==View.VISIBLE?service.key:"");
  }
 }
 @Override public void onCreate(Bundle b){super.onCreate(b);id=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);Intent result=new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);setResult(RESULT_CANCELED,result);if(!SceneWidget.owns(this,id)){finish();return;}
  profile=SceneProfile.load(this);Scene old=SceneStore.get(this,id);if(old!=null)savedIcon=old.icon;
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  LinearLayout page=col();page.setBackgroundColor(BG);page.setPadding(dp(10),dp(6),dp(10),dp(6));setContentView(page);
  LinearLayout top=row();ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.dz_logo);top.addView(logo,new LinearLayout.LayoutParams(dp(28),dp(28)));TextView title=label("  Widget konfigurieren",18,FG);title.setTypeface(null,Typeface.BOLD);weight(top,title,44,1);Button close=button("×");top.addView(close,new LinearLayout.LayoutParams(dp(44),dp(44)));close.setOnClickListener(v->finish());add(page,top,44);
  ScrollView scroll=new ScrollView(this);root=col();root.setPadding(0,0,0,dp(6));scroll.addView(root);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  name=new EditText(this);name.setSingleLine(true);name.setTextSize(17);name.setTextColor(FG);name.setHintTextColor(MUTED);name.setHint("Widget-Name");name.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);name.setText(old==null?"":old.name);name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(40)});add(root,name,48);
  TextView info=label(profile==null?"Geräteprofil fehlt · App einmal mit Onkyo verbinden":"Sender und Quellen aus zuletzt bestätigtem Geräteprofil",11,MUTED);add(root,info,-2);gap(root,8);
  Scene.Zone empty=new Scene.Zone(Scene.KEEP,"",-1,-1,"");forms[0]=new ZoneForm(0,old==null?empty:old.zones[0]);gap(root,6);forms[1]=new ZoneForm(1,old==null?empty:old.zones[1]);gap(root,6);
  LinearLayout advanced=card(root);Button toggle=button("Erweitert  ▾");add(advanced,toggle,44);LinearLayout extras=col();add(advanced,extras,-2);extras.setVisibility(View.GONE);
  shared=new CheckBox(this);shared.setText("Gemeinsame Quellen dürfen andere Zone beeinflussen");shared.setTextColor(FG);shared.setChecked(old!=null&&old.share);add(extras,shared,-2);
  demo=new CheckBox(this);demo.setText("Nur Demo");demo.setTextColor(FG);demo.setChecked(old!=null&&old.demo);add(extras,demo,44);
  add(extras,label("Radio-Szene für eine Zone: erneut tippen schaltet sie aus, wenn Sender und Quelle übereinstimmen.",12,MUTED),-2);
  toggle.setOnClickListener(v->{boolean open=extras.getVisibility()!=View.VISIBLE;extras.setVisibility(open?View.VISIBLE:View.GONE);toggle.setText(open?"Erweitert  ▴":"Erweitert  ▾");});
  Button save=button("Widget speichern");save.setBackground(shape(0xff28483f));save.setTextColor(ACCENT);add(page,save,48);save.setOnClickListener(v->{try{
   if(SceneWidget.busy(id))throw new IllegalStateException("Bitte laufende Aktion abschließen lassen");
   Scene scene=new Scene(name.getText().toString().trim(),savedIcon,forms[0].value(),forms[1].value(),shared.isChecked(),demo.isChecked());
   if(!SceneStore.save(this,id,scene))throw new IllegalStateException("Speichern fehlgeschlagen");SceneWidget.observeNetwork(this);SceneWidget.refresh(this,id);setResult(RESULT_OK,result);finish();
  }catch(IllegalArgumentException|IllegalStateException e){new AlertDialog.Builder(this).setTitle("Szene prüfen").setMessage(e.getMessage()).setPositiveButton("OK",null).show();}});
  page.setFocusableInTouchMode(true);page.requestFocus();getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
 }
}
