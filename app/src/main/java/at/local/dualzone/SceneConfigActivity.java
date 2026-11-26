package at.local.dualzone;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;
public final class SceneConfigActivity extends Activity {
 int id;EditText name;Spinner icon;CheckBox shared,demo;ZoneForm[] forms=new ZoneForm[2];LinearLayout root;
 final String[] icons={"Radio","Musik","TV","Haus","Power"};
 int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
 void text(LinearLayout parent,String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setPadding(0,dp(10),0,dp(3));parent.addView(t);}
 Spinner spin(LinearLayout parent,String[] labels,int selected){Spinner s=new Spinner(this);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(a);s.setSelection(Math.max(0,selected));parent.addView(s,new LinearLayout.LayoutParams(-1,dp(48)));return s;}
 EditText input(LinearLayout parent,String hint,String value,boolean number){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setInputType(number?InputType.TYPE_CLASS_NUMBER:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);e.setText(value);parent.addView(e,new LinearLayout.LayoutParams(-1,dp(48)));return e;}
 class ZoneForm{
  Spinner power,source,service;EditText volume,preset;String[] codes;String[] services=Scene.NET_CODES;
  ZoneForm(int index,Scene.Zone old){
   text(root,index==0?"Hauptzone":"Zone 2",22);
   text(root,"Power",14);power=spin(root,new String[]{"Unverändert","Einschalten","Ausschalten"},old.power);
   ArrayList<String> cs=new ArrayList<>(),ns=new ArrayList<>();for(int i=0;i<Scene.SOURCE_CODES.length;i++){String k=Scene.SOURCE_CODES[i];if(index==0&&k.equals("80")||index==1&&k.equals("05"))continue;cs.add(k);ns.add(Scene.SOURCE_NAMES[i]);}codes=cs.toArray(new String[0]);
   text(root,"Quelle",14);source=spin(root,ns.toArray(new String[0]),cs.indexOf(old.source));
   text(root,"Lautstärke (leer = unverändert, max. 50)",14);volume=input(root,"0–50",old.volume<0?"":""+old.volume,true);
   text(root,"FM/AM-Speicherplatz (optional, 1–40)",14);preset=input(root,"z. B. 4",old.preset<0?"":""+old.preset,true);
   text(root,"NET-Dienst (optional)",14);service=spin(root,Scene.NET_NAMES,Arrays.asList(services).indexOf(old.service));
   AdapterView.OnItemSelectedListener change=new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long row){update();}public void onNothingSelected(AdapterView<?> p){}};
   power.setOnItemSelectedListener(change);source.setOnItemSelectedListener(change);update();
  }
  void update(){boolean enabled=power.getSelectedItemPosition()!=Scene.OFF;int pos=source.getSelectedItemPosition();if(pos<0)return;String code=codes[pos];source.setEnabled(enabled);volume.setEnabled(enabled);preset.setEnabled(enabled&&(code.equals("24")||code.equals("25")));service.setEnabled(enabled&&code.equals("2B"));}
  int number(EditText e){String x=e.getText().toString().trim();return x.isEmpty()?-1:Integer.parseInt(x);}
  Scene.Zone value(){int p=power.getSelectedItemPosition();if(p==Scene.OFF)return new Scene.Zone(p,"",-1,-1,"");String code=codes[source.getSelectedItemPosition()];return new Scene.Zone(p,code,number(volume),preset.isEnabled()?number(preset):-1,service.isEnabled()?services[service.getSelectedItemPosition()]:"");}
 }
 @Override public void onCreate(Bundle b){super.onCreate(b);id=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);Intent result=new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);setResult(RESULT_CANCELED,result);if(!SceneWidget.owns(this,id)){finish();return;}
  Scene old=SceneStore.get(this,id);ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(12),dp(18),dp(16));scroll.addView(root);setContentView(scroll);
  text(root,"Onkyo-Szene",26);text(root,"Ein Tippen startet die gewählten Aktionen. Unveränderte Einstellungen werden nicht gesetzt.",15);
  text(root,"Name",14);name=input(root,"z. B. FM4 Küche",old==null?"":old.name,false);name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(40)});
  text(root,"Symbol",14);icon=spin(root,icons,old==null?0:Arrays.asList(icons).indexOf(old.icon));
  Scene.Zone empty=new Scene.Zone(Scene.KEEP,"",-1,-1,"");
  forms[0]=new ZoneForm(0,old==null?empty:old.zones[0]);forms[1]=new ZoneForm(1,old==null?empty:old.zones[1]);
  shared=new CheckBox(this);shared.setText("Gemeinsame Quellen dürfen auch die andere Zone beeinflussen (SOURCE, Radio, NET)");shared.setChecked(old!=null&&old.share);root.addView(shared);
  demo=new CheckBox(this);demo.setText("Nur Demo (keine Receiver-Befehle)");demo.setChecked(old!=null&&old.demo);root.addView(demo);
  text(root,"Radio: FM4 muss am Receiver auf dem gewählten Speicherplatz gespeichert sein. NET-Dienste sind abhängig von Firmware und Verfügbarkeit. Netzwerk-Standby aktivieren.",13);
  Button save=new Button(this);save.setText("Widget speichern");root.addView(save,new LinearLayout.LayoutParams(-1,dp(52)));save.setOnClickListener(v->{try{
   Scene scene=new Scene(name.getText().toString().trim(),icons[icon.getSelectedItemPosition()],forms[0].value(),forms[1].value(),shared.isChecked(),demo.isChecked());
   if(!SceneStore.save(this,id,scene))throw new IllegalStateException("Speichern fehlgeschlagen");SceneWidget.refresh(this,id,"Bereit");setResult(RESULT_OK,result);finish();
  }catch(IllegalArgumentException|IllegalStateException e){new AlertDialog.Builder(this).setTitle("Szene prüfen").setMessage(e instanceof NumberFormatException?"Bitte gültige Zahlen eingeben.":e.getMessage()).setPositiveButton("OK",null).show();}});
 }
}
