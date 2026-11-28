package at.local.dualzone;
import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import java.util.*;
public final class RadioConfigActivity extends Activity {
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 void label(LinearLayout root,String text){TextView v=new TextView(this);v.setText(text);v.setTextSize(15);v.setPadding(0,dp(8),0,0);root.addView(v);}
 Spinner spinner(LinearLayout root,List<String> labels,int selected){Spinner s=new Spinner(this);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(a);s.setSelection(Math.max(0,selected));root.addView(s,new LinearLayout.LayoutParams(-1,dp(48)));return s;}
 @Override public void onCreate(Bundle b){super.onCreate(b);
  int id=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
  if(!RadioWidget.isRadio(this,id)){finish();return;}
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(12),dp(18),dp(12));setContentView(root);
  label(root,"Radio · Einstellungen");label(root,"Ausgabezone");
  int oldZone=RadioWidget.zone(this,id);
  Spinner zone=spinner(root,Arrays.asList("Wohnzimmer","Esstisch"),oldZone);
  label(root,"Sender (FM)");
  ArrayList<Integer> values=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();values.add(-1);labels.add("Letzter Sender");
  String ip=getSharedPreferences("config",MODE_PRIVATE).getString("ip","");
  Capabilities c=VolumeProfile.get(this,SceneWidget.lan(this),ip);
  if(c!=null&&c.hasPresets){for(Capabilities.Preset p:c.presets)if(p.band==1){values.add(p.number);labels.add("P"+String.format(Locale.ROOT,"%02d",p.number)+(p.name.isEmpty()?"":" · "+p.name));}}
  else for(int i=1;i<=40;i++){values.add(i);labels.add(String.format(Locale.ROOT,"P%02d",i));}
  Spinner sender=spinner(root,labels,values.indexOf(RadioWidget.pendingPreset(this,id)));
  label(root,"Startlautstärke 45 (Geräte-Maximum gilt). Die Ausgabezone gilt für den nächsten Start. FM ist ein gemeinsamer Tuner.");
  Button save=new Button(this);save.setText("Speichern");root.addView(save);save.setOnClickListener(v->{
   if(!RadioWidget.isRadio(this,id)){finish();return;}
   int selected=zone.getSelectedItemPosition(),preset=values.get(sender.getSelectedItemPosition());
   RadioWidget.prefs(this).edit().putInt("zone_"+id,selected).putInt("preset_"+id,preset).apply();
   RadioWidget.refreshAll(this);
   if(preset>0&&selected==oldZone&&RadioWidget.prefs(this).getBoolean("fm"+selected,false)&&SceneWidget.lan(this)!=null){
    Intent i=new Intent(this,SceneService.class).setAction(RadioWidget.SELECT).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);startForegroundService(i);
   }
   finish();
  });
  Button cancel=new Button(this);cancel.setText("Abbrechen");root.addView(cancel);cancel.setOnClickListener(v->finish());
 }
}