package at.local.dualzone;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.*;
import android.widget.*;
public class RadioWidget extends AppWidgetProvider {
 static final String UP="RADIO_UP",DOWN="RADIO_DOWN",LIVING="RADIO_LIVING",DINING="RADIO_DINING",PLUS="RADIO_PLUS",MINUS="RADIO_MINUS",CHECK="RADIO_CHECK";
 static final int ACTIVE=0xff7bd8bc,OFF=0xff8a9b9e,FG=0xfff1f5f4,WARN=0xffffbd85;
 static volatile String phase="";
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("radio_widget",Context.MODE_PRIVATE);}
 static boolean exists(Context c){return ids(c,RadioWidget.class).length+ids(c,Universal.class).length>0;}
 static int[] ids(Context c,Class<?> type){return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,type));}
 static boolean owns(Context c,int id){for(Class<?> type:new Class<?>[]{RadioWidget.class,Universal.class})for(int x:ids(c,type))if(x==id)return true;return false;}
 static boolean action(String a){return java.util.Arrays.asList(UP,DOWN,LIVING,DINING,PLUS,MINUS,CHECK).contains(a);}
 static void refreshAll(Context c){
  if(!exists(c))return;AppWidgetManager m=AppWidgetManager.getInstance(c);
  for(int id:ids(c,RadioWidget.class))m.updateAppWidget(id,render(c,id,false));
  for(int id:ids(c,Universal.class))m.updateAppWidget(id,render(c,id,true));
 }
 static void phase(Context c,String value){phase=value;refreshAll(c);}
 static void store(Context c,RadioControl.State s,Capabilities profile,Network n){
  SharedPreferences.Editor e=prefs(c).edit().putLong("at",System.currentTimeMillis()).putLong("network",n.getNetworkHandle()).putString("target",SceneStore.target(c)).putString("station",s.station(profile)).remove("error");
  for(int i=0;i<2;i++){e.putBoolean("on"+i,s.on[i]).putBoolean("fm"+i,s.fm(i));if(s.raw[i]>=0)e.putString("volume"+i,Protocol.volumeLabel(s.raw[i],s.scale[i]));}
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
   set(v,R.id.radio_up,"▲",FG);set(v,R.id.radio_down,"▼",FG);
   set(v,R.id.radio_living,"Wohnzimmer\n"+(fresh?(a?(p.getBoolean("fm0",false)?"FM · ":"An · ")+va:"Aus"):"–"),fresh&&a?ACTIVE:OFF);
   set(v,R.id.radio_dining,"Esstisch\n"+(fresh?(b?(p.getBoolean("fm1",false)?"FM · ":"An · ")+vb:"Aus"):"–"),fresh&&b?ACTIVE:OFF);
   click(c,v,id,R.id.radio_up,UP);click(c,v,id,R.id.radio_down,DOWN);click(c,v,id,R.id.radio_living,LIVING);click(c,v,id,R.id.radio_dining,DINING);
   set(v,R.id.radio_station,fresh?p.getString("station","FM"):"FM · Status unbekannt",fresh?ACTIVE:OFF);
  }else{
   set(v,R.id.volume_living,"Wohnzimmer  "+va,fresh&&a?ACTIVE:OFF);
   set(v,R.id.volume_dining,"Esstisch  "+vb,fresh&&b?ACTIVE:OFF);
  }
  int volumeColor=fresh&&(a||b)?ACTIVE:OFF;
  set(v,R.id.radio_plus,"+",volumeColor);set(v,R.id.radio_minus,"−",volumeColor);
  click(c,v,id,R.id.radio_plus,PLUS);click(c,v,id,R.id.radio_minus,MINUS);
  String error=p.getString("error","");String status=!phase.isEmpty()?phase:n==null?(RemoteVpn.selected(c)?RemoteVpn.status(c):"Offline · tippen zum Prüfen"):!error.isEmpty()?error:fresh?(a||b?"Verbunden":"Beide Zonen aus"):"Status prüfen";
  set(v,R.id.radio_status,status,fresh&&error.isEmpty()&&phase.isEmpty()?ACTIVE:WARN);
  click(c,v,id,R.id.radio_status,CHECK);
  return v;
 }
 static void set(RemoteViews v,int id,String text,int color){v.setTextViewText(id,text);v.setTextColor(id,color);}
 static void click(Context c,RemoteViews v,int id,int view,String action){
  Intent i=new Intent(c,SceneService.class).setAction(action).setData(Uri.parse("onkyo://radio/"+id+"/"+action)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
  v.setOnClickPendingIntent(view,PendingIntent.getForegroundService(c,id,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));
 }
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){SceneWidget.observeNetwork(c);refreshAll(c);}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){refreshAll(c);}
 public static final class Universal extends RadioWidget {}
}
