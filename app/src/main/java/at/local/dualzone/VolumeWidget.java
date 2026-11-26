package at.local.dualzone;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** A launcher shortcut to a real, zone-specific volume slider. RemoteViews cannot host SeekBar. */
public abstract class VolumeWidget extends AppWidgetProvider {
 abstract int zone();
 static String name(int zone){return zone==0?"Wohnzimmer":"Esstisch";}
 static Intent intent(Context c,int zone){
  return new Intent(c,VolumeActivity.class).putExtra("zone",zone)
   .setAction("at.local.dualzone.VOLUME_"+zone)
   .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
 }
 static RemoteViews render(Context c,int zone){
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.volume_widget);
  v.setTextViewText(R.id.volume_widget_name,name(zone));
  v.setContentDescription(R.id.volume_widget_root,"Lautstärkeregler "+name(zone)+" öffnen");
  v.setOnClickPendingIntent(R.id.volume_widget_root,PendingIntent.getActivity(c,400+zone,intent(c,zone),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  return v;
 }
 @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){
  for(int id:ids)manager.updateAppWidget(id,render(c,zone()));
 }
 public static final class LivingRoom extends VolumeWidget { @Override int zone(){return 0;} }
 public static final class DiningRoom extends VolumeWidget { @Override int zone(){return 1;} }
}
