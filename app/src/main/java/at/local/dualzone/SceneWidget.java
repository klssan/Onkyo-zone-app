package at.local.dualzone;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.widget.RemoteViews;
final public class SceneWidget extends AppWidgetProvider {
 static boolean owns(Context c,int id){if(id==AppWidgetManager.INVALID_APPWIDGET_ID)return false;AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(id);return info!=null&&new ComponentName(c,SceneWidget.class).equals(info.provider);}
 static void refresh(Context c,int id,String status){
  if(!owns(c,id))return;Scene scene=SceneStore.get(c,id);RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.scene_widget);
  v.setTextViewText(R.id.scene_name,scene==null?"Onkyo Szene":scene.name);
  v.setTextViewText(R.id.scene_summary,scene==null?"Einrichten":(scene.demo?"DEMO · ":"")+scene.summary());
  v.setTextViewText(R.id.scene_status,status==null?SceneStore.status(c,id):status);
  String symbol="▶";if(scene!=null){switch(scene.icon){case "Radio":symbol="♫";break;case "Musik":symbol="♪";break;case "TV":symbol="TV";break;case "Haus":symbol="⌂";break;case "Power":symbol="⏻";break;}}
  v.setTextViewText(R.id.scene_icon,symbol);
  Intent edit=new Intent(c,SceneConfigActivity.class).setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).setData(Uri.parse("onkyo://edit/"+id)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  PendingIntent editPI=PendingIntent.getActivity(c,id,edit,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  v.setOnClickPendingIntent(R.id.scene_edit,editPI);
  if(scene==null)v.setOnClickPendingIntent(R.id.scene_run,editPI);
  else{Intent run=new Intent(c,SceneService.class).setAction("at.local.dualzone.RUN_SCENE").setData(Uri.parse("onkyo://run/"+id)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);v.setOnClickPendingIntent(R.id.scene_run,PendingIntent.getForegroundService(c,id,run,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));}
  AppWidgetManager.getInstance(c).updateAppWidget(id,v);
 }
 static void result(Context c,int id,String value){if(!owns(c,id))return;SceneStore.status(c,id,value);refresh(c,id,value);}
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)refresh(c,id,null);}
 @Override public void onDeleted(Context c,int[] ids){for(int id:ids)SceneStore.delete(c,id);}
}
