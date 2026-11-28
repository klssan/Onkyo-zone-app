package at.local.dualzone;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.*;
import android.os.Bundle;
import android.widget.*;
public class RadioWidget extends AppWidgetProvider {
 static final String UP="RADIO_UP",DOWN="RADIO_DOWN",LIVING="RADIO_LIVING",DINING="RADIO_DINING",PLUS="RADIO_PLUS",MINUS="RADIO_MINUS",CHECK="RADIO_CHECK",TOGGLE="RADIO_TOGGLE",SELECT="RADIO_SELECT";
 static final int ACTIVE=0xff7bd8bc,OFF=0xff8a9b9e,FG=0xfff1f5f4,WARN=0xffffbd85;
 static volatile String phase="";
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("radio_widget",Context.MODE_PRIVATE);}
 static boolean exists(Context c){return ids(c,RadioWidget.class).length+ids(c,Universal.class).length>0;}
 static int[] ids(Context c,Class<?> type){return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,type));}
 static boolean owns(Context c,int id){for(Class<?> type:new Class<?>[]{RadioWidget.class,Universal.class})for(int x:ids(c,type))if(x==id)return true;return false;}
 static boolean isRadio(Context c,int id){for(int x:ids(c,RadioWidget.class))if(x==id)return true;return false;}
 static int lastPreset(Context c){SharedPreferences p=prefs(c);return SceneStore.target(c).equals(p.getString("lastTarget",""))?p.getInt("lastPreset",-1):-1;}
 static boolean action(String a){return java.util.Arrays.asList(UP,PLUS,MINUS,CHECK,TOGGLE).contains(a);}
 static void refreshAll(Context c){
  if(!exists(c))return;AppWidgetManager m=AppWidgetManager.getInstance(c);
  for(int id:ids(c,RadioWidget.class))m.updateAppWidget(id,render(c,id,false));
  for(int id:ids(c,Universal.class))m.updateAppWidget(id,render(c,id,true));
 }
 static void phase(Context c,String value){phase=value;refreshAll(c);}
 static void store(Context c,RadioControl.State s,Capabilities profile,Network n){
  SharedPreferences.Editor e=prefs(c).edit().putLong("at",System.currentTimeMillis()).putLong("network",n.getNetworkHandle()).putString("target",SceneStore.target(c)).putString("station",s.station(profile)).remove("error");
  for(int i=0;i<2;i++){e.putBoolean("on"+i,s.on[i]).putBoolean("fm"+i,s.fm(i));if(s.on[i]&&s.raw[i]>=0)e.putString("volume"+i,Protocol.volumeLabel(s.raw[i],s.scale[i]));else e.remove("volume"+i);}
  if(s.fm(1)&&s.preset>0)e.putInt("lastPreset",s.preset).putString("lastTarget",SceneStore.target(c));
  e.apply();refreshAll(c);
 }
 static void storeVolumes(Context c,RadioControl.State s,Network n){
  SharedPreferences p=prefs(c);
  boolean same=n.getNetworkHandle()==p.getLong("network",0)&&SceneStore.target(c).equals(p.getString("target",""));
  boolean samePower=same&&s.on[0]==p.getBoolean("on0",false)&&s.on[1]==p.getBoolean("on1",false);
  SharedPreferences.Editor e=p.edit().putLong("at",System.currentTimeMillis()).putLong("network",n.getNetworkHandle()).putString("target",SceneStore.target(c)).remove("error");
  if(!samePower)e.putString("station",s.on[0]||s.on[1]?"FM · Status unbekannt":"Radio aus");
  for(int i=0;i<2;i++){e.putBoolean("on"+i,s.on[i]);if(!samePower)e.putBoolean("fm"+i,false);if(s.on[i]&&s.raw[i]>=0)e.putString("volume"+i,Protocol.volumeLabel(s.raw[i],s.scale[i]));else e.remove("volume"+i);}
  e.apply();refreshAll(c);
 }
 static void failure(Context c,String error){prefs(c).edit().putString("error",error).putLong("at",0).apply();refreshAll(c);}
 static RemoteViews render(Context c,int id,boolean universal){
  SharedPreferences p=prefs(c);Network n=SceneWidget.lan(c);
  boolean fresh=n!=null&&p.getLong("network",0)==n.getNetworkHandle()&&SceneStore.target(c).equals(p.getString("target",""))&&System.currentTimeMillis()-p.getLong("at",0)<120000&&p.getLong("at",0)>0;
  RemoteViews v=new RemoteViews(c.getPackageName(),universal?R.layout.universal_volume_widget:R.layout.radio_widget);
  boolean a=p.getBoolean("on0",false),b=p.getBoolean("on1",false);
  String va=p.getString("volume0","–"),vb=p.getString("volume1","–");
  if(!universal){
   boolean on=fresh&&p.getBoolean("fm1",false);
   String vol=vb;
   String error=p.getString("error","");
   String state=!phase.isEmpty()?"…":n==null?"Offline":!error.isEmpty()?"Fehler":!fresh?"?":on?"An · "+vol:"Aus";
   set(v,R.id.radio_status,"Radio\n"+state,on&&phase.isEmpty()?ACTIVE:!fresh||!error.isEmpty()||!phase.isEmpty()?WARN:OFF);
   v.setContentDescription(R.id.radio_status,"Radio "+state+(error.isEmpty()?"":" · "+error)+(phase.isEmpty()?"":" · "+phase));
   click(c,v,id,R.id.radio_status,TOGGLE);
   String station=on?p.getString("station","FM"):"FM";
   set(v,R.id.radio_station,station,on?ACTIVE:OFF);
   v.setContentDescription(R.id.radio_station,"Nächster FM-Sender · "+station);
   click(c,v,id,R.id.radio_station,UP);
  }else{
   set(v,R.id.volume_living,"Wohnzimmer"+(fresh?(a?"  "+va:"  Aus"):"  –"),fresh&&a?ACTIVE:OFF);
   set(v,R.id.volume_dining,"Esstisch"+(fresh?(b?"  "+vb:"  Aus"):"  –"),fresh&&b?ACTIVE:OFF);
  }
  int volumeColor=fresh&&(universal?(a||b):p.getBoolean("fm1",false))?ACTIVE:OFF;
  set(v,R.id.radio_plus,"+",volumeColor);set(v,R.id.radio_minus,"−",volumeColor);
  click(c,v,id,R.id.radio_plus,PLUS);click(c,v,id,R.id.radio_minus,MINUS);
  String error=p.getString("error","");String status=!phase.isEmpty()?phase:n==null?(RemoteVpn.selected(c)?RemoteVpn.status(c):"Offline · tippen zum Prüfen"):!error.isEmpty()?error:fresh?(a||b?"Verbunden":"Beide Zonen aus"):"Status prüfen";
  if(universal){set(v,R.id.radio_status,status,fresh&&error.isEmpty()&&phase.isEmpty()?ACTIVE:WARN);click(c,v,id,R.id.radio_status,CHECK);}
  return v;
 }
 static void set(RemoteViews v,int id,String text,int color){v.setTextViewText(id,text);v.setTextColor(id,color);}
 static void click(Context c,RemoteViews v,int id,int view,String action){
  Intent i=new Intent(c,SceneService.class).setAction(action).setData(Uri.parse("onkyo://radio/"+id+"/"+action)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
  v.setOnClickPendingIntent(view,PendingIntent.getForegroundService(c,id,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));
 }
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){SceneWidget.observeNetwork(c);refreshAll(c);}
 @Override public void onDeleted(Context c,int[] ids){SharedPreferences.Editor e=prefs(c).edit();for(int id:ids)e.remove("zone_"+id).remove("preset_"+id);e.apply();}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){refreshAll(c);}
 public static final class Universal extends RadioWidget {}
}
