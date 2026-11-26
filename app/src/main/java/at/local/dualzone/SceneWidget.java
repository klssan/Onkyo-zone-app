package at.local.dualzone;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.view.View;
import android.widget.RemoteViews;
import java.util.concurrent.ConcurrentHashMap;
public final class SceneWidget extends AppWidgetProvider {
 static final String RUN="at.local.dualzone.RUN_SCENE",CHECK="at.local.dualzone.CHECK_RECEIVER";
 private static final ConcurrentHashMap<Integer,String> phases=new ConcurrentHashMap<>();
 private static ConnectivityManager.NetworkCallback monitor;
 static boolean owns(Context c,int id){if(id==AppWidgetManager.INVALID_APPWIDGET_ID)return false;AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(id);return info!=null&&new ComponentName(c,SceneWidget.class).equals(info.provider);}
 static Network lan(Context c){
  ConnectivityManager m=c.getSystemService(ConnectivityManager.class);
  if(m==null)return null;Network active=m.getActiveNetwork();if(isLan(m,active))return active;
  for(Network n:m.getAllNetworks())if(isLan(m,n))return n;return null;
 }
 static boolean isLan(ConnectivityManager m,Network n){if(n==null)return false;NetworkCapabilities cap=m.getNetworkCapabilities(n);return cap!=null&&(cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));}
 static int[] ids(Context c){return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,SceneWidget.class));}
 static boolean busy(int id){return phases.containsKey(id);}
 static void phase(Context c,int id,String phase){if(phase==null)phases.remove(id);else phases.put(id,phase);refresh(c,id);}
 static void notice(Context c,int id,String text){phase(c,id,text);new Handler(Looper.getMainLooper()).postDelayed(()->{if(text.equals(phases.get(id)))phase(c,id,null);},3000);}
 static void refreshAll(Context c){for(int id:ids(c))refresh(c,id);}
 static synchronized void observeNetwork(Context c){
  if(monitor!=null||ids(c).length==0)return;
  Context app=c.getApplicationContext();ConnectivityManager m=app.getSystemService(ConnectivityManager.class);if(m==null)return;
  Handler handler=new Handler(Looper.getMainLooper());Runnable refresh=()->refreshAll(app);
  monitor=new ConnectivityManager.NetworkCallback(){
   void update(){handler.removeCallbacks(refresh);handler.postDelayed(refresh,300);}
   @Override public void onAvailable(Network n){update();}
   @Override public void onLost(Network n){SceneStore.invalidateNetwork(app,n.getNetworkHandle());update();}
   @Override public void onCapabilitiesChanged(Network n,NetworkCapabilities caps){update();}
  };
  try{m.registerNetworkCallback(new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET).build(),monitor);}catch(RuntimeException e){monitor=null;Diagnostics.error("WIDGET_MONITOR_ERROR",e);}
 }
 static synchronized void stopMonitor(Context c){if(monitor!=null){try{c.getSystemService(ConnectivityManager.class).unregisterNetworkCallback(monitor);}catch(RuntimeException ignored){}monitor=null;}}
 static int color(int tone){switch(tone){case WidgetStatus.GOOD:return 0xff7bd8bc;case WidgetStatus.WARN:return 0xffffbd85;case WidgetStatus.ERROR:return 0xffffa59f;default:return 0xffc2d0d1;}}
 static RemoteViews render(Context c,Scene scene,WidgetStatus.Line connection,WidgetStatus.Line execution){
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.scene_widget);
  v.setTextViewText(R.id.scene_name,scene==null?"DualZone Szene":scene.name);
  v.setTextViewText(R.id.scene_main,scene==null?"Hauptzone: —":"Haupt: "+scene.zoneSummary(0));
  v.setTextViewText(R.id.scene_second,scene==null?"Zone 2: —":"Zone 2: "+scene.zoneSummary(1));
  v.setTextViewText(R.id.scene_connection,scene==null?"Tippen zum Einrichten":connection.text);
  v.setTextColor(R.id.scene_connection,color(scene==null?WidgetStatus.NEUTRAL:connection.tone));
  v.setTextViewText(R.id.scene_status,scene==null?"":execution.text);
  v.setTextColor(R.id.scene_status,color(execution.tone));
  v.setTextViewText(R.id.scene_shared,scene!=null&&scene.share?"Gemeinsame Quellen erlaubt":"");
  v.setViewVisibility(R.id.scene_shared,scene!=null&&scene.share?View.VISIBLE:View.GONE);
  v.setViewVisibility(R.id.scene_check,scene==null?View.GONE:View.VISIBLE);
  String symbol="▶";if(scene!=null){switch(scene.icon){case "Radio":symbol="♫";break;case "Musik":symbol="♪";break;case "TV":symbol="TV";break;case "Haus":symbol="⌂";break;case "Power":symbol="⏻";break;}}
  v.setTextViewText(R.id.scene_icon,symbol);
  return v;
 }
 static RemoteViews compact(Context c,Scene scene,WidgetStatus.Line connection,WidgetStatus.Line execution){
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.scene_widget_small);
  v.setTextViewText(R.id.scene_name,scene==null?"Einrichten":scene.name);
  v.setTextViewText(R.id.scene_actions_text,scene==null?"":scene.compactSummary());
  WidgetStatus.Line state=WidgetStatus.compact(connection,execution);
  v.setTextViewText(R.id.scene_status,scene==null?"":state.text);
  v.setTextColor(R.id.scene_status,color(state.tone));
  v.setContentDescription(R.id.scene_run,scene==null?"DualZone Szene einrichten":scene.name+". "+scene.summary()+". "+connection.text+". "+execution.text);
  v.setViewVisibility(R.id.scene_check,scene==null?View.GONE:View.VISIBLE);
  return v;
 }
 static void refresh(Context c,int id){
  if(!owns(c,id))return;Scene scene=SceneStore.get(c,id);Network n=lan(c);String phase=phases.get(id);
  WidgetStatus.Line connection=SceneStore.connection(c,scene!=null&&scene.demo,n==null?0:n.getNetworkHandle(),phase),execution=SceneStore.execution(c,id,phase!=null);
  Bundle options=AppWidgetManager.getInstance(c).getAppWidgetOptions(id);boolean small=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,60)<240||options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,60)<180;
  RemoteViews v=small?compact(c,scene,connection,execution):render(c,scene,connection,execution);
  Intent edit=new Intent(c,SceneConfigActivity.class).setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).setData(Uri.parse("onkyo://edit/"+id)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  PendingIntent editPI=PendingIntent.getActivity(c,id,edit,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  v.setOnClickPendingIntent(R.id.scene_edit,editPI);
  if(scene==null){v.setOnClickPendingIntent(R.id.scene_run,editPI);if(!small)v.setOnClickPendingIntent(R.id.scene_actions,editPI);}
  else{
   Intent run=new Intent(c,SceneService.class).setAction(RUN).setData(Uri.parse("onkyo://run/"+id)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
   Intent check=new Intent(c,SceneService.class).setAction(CHECK).setData(Uri.parse("onkyo://check/"+id)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
   v.setOnClickPendingIntent(R.id.scene_run,PendingIntent.getForegroundService(c,id,run,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
   if(!small)v.setOnClickPendingIntent(R.id.scene_actions,PendingIntent.getForegroundService(c,id,run,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
   v.setOnClickPendingIntent(R.id.scene_check,PendingIntent.getForegroundService(c,id,check,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
   if(small)v.setOnClickPendingIntent(R.id.scene_status,PendingIntent.getForegroundService(c,id,run,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  }
  AppWidgetManager.getInstance(c).updateAppWidget(id,v);
 }
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){observeNetwork(c);for(int id:ids)refresh(c,id);}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){refresh(c,id);}
 @Override public void onDeleted(Context c,int[] ids){for(int id:ids){phases.remove(id);SceneStore.delete(c,id);}}
 @Override public void onDisabled(Context c){stopMonitor(c);}
}
