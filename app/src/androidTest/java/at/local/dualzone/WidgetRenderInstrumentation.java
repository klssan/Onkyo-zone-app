package at.local.dualzone;
import android.app.*;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
public final class WidgetRenderInstrumentation extends Instrumentation {
 boolean radioOnly;
 @Override public void onCreate(Bundle args){super.onCreate(args);radioOnly=args!=null&&"true".equals(args.getString("radioOnly"));start();}
 static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static String value(View root,int id){return ((TextView)root.findViewById(id)).getText().toString();}
 static void checkRadio(Context c){
  float d=c.getResources().getDisplayMetrics().density;
  for(int size:new int[]{110,128,160}){
   RemoteViews face=RadioWidget.render(c,123,false);
   RadioWidget.radioFace(face,"An · 35",RadioWidget.ACTIVE,"FM4 · 104.1",true);
   View radio=face.apply(c,new FrameLayout(c));
   int px=Math.round(size*d);
   radio.measure(View.MeasureSpec.makeMeasureSpec(px,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(px,View.MeasureSpec.EXACTLY));radio.layout(0,0,px,px);
   require(value(radio,R.id.radio_status).equals("An · 35")&&value(radio,R.id.radio_station_label).equals("FM4"),"active labels");
   require(value(radio,R.id.radio_plus).equals("+"),"plus");
   for(int[] ids:new int[][]{{R.id.radio_power_tile,R.id.radio_power_icon,R.id.radio_status},{R.id.radio_station_tile,R.id.radio_station,R.id.radio_station_label}}){
    View tile=radio.findViewById(ids[0]),icon=radio.findViewById(ids[1]),label=radio.findViewById(ids[2]);
    require(((ImageView)icon).getDrawable()!=null,"icon present");
    require(Math.abs(2*icon.getLeft()+icon.getWidth()-tile.getWidth())<=2&&Math.abs(2*icon.getTop()+icon.getHeight()-tile.getHeight())<=2,"icon centered");
    require(label.getTop()>=icon.getBottom()-1&&label.getBottom()<=tile.getHeight(),"caption below icon");
    int left=icon.getLeft(),top=icon.getTop();
    ((TextView)label).setText("");
    radio.measure(View.MeasureSpec.makeMeasureSpec(px,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(px,View.MeasureSpec.EXACTLY));radio.layout(0,0,px,px);
    require(icon.getLeft()==left&&icon.getTop()==top,"icon position unchanged without text");
   }
  }
  require(RadioWidget.stationLabel("FM · P04").equals("P04")&&RadioWidget.stationLabel("101,20 MHz").equals("101,20"),"station fallbacks");
 }
 @Override public void onStart(){
  Bundle result=new Bundle();final Throwable[] problem={null};
  if(radioOnly){runOnMainSync(()->{try{checkRadio(getTargetContext());}catch(Throwable e){problem[0]=e;}});if(problem[0]==null){result.putString("widget_render_ok","true");finish(Activity.RESULT_OK,result);}else{result.putString("widget_render_error",problem[0].toString());finish(Activity.RESULT_CANCELED,result);}return;}
  runOnMainSync(()->{try{
   Context c=getTargetContext();long now=System.currentTimeMillis();
   require(c.getApplicationInfo().loadLabel(c.getPackageManager()).toString().equals("Onkyo dahoam"),"app label");
   require(c.getApplicationInfo().icon==R.drawable.dz_logo,"launcher icon");
   require(c.getDrawable(R.drawable.dz_logo)!=null,"logo drawable");
   Scene scene=new Scene("FM4 Küche","Radio",new Scene.Zone(Scene.KEEP,"",-1,-1,""),new Scene.Zone(Scene.ON,"24",40,4,""),false,false);
   WidgetStatus.Line last=WidgetStatus.execution("Bestätigt",now,false,0,false);
   RemoteViews views=SceneWidget.render(c,scene,WidgetStatus.connection(false,false,WidgetStatus.REACHABLE,now,true,now,null),last);
   View v=views.apply(c,new FrameLayout(c));
   require(value(v,R.id.scene_name).equals("FM4 Küche"),"name");
   require(value(v,R.id.scene_status).equals("Offline"),"offline");
   require(v.findViewById(R.id.scene_check).getVisibility()==View.VISIBLE,"read-only check visible");
   float d=c.getResources().getDisplayMetrics().density;int width=Math.round(300*d),height=Math.round(200*d);
   v.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));v.layout(0,0,width,height);
   for(int id:new int[]{R.id.scene_name,R.id.scene_status}){View item=v.findViewById(id);require(item.getHeight()>0&&item.getWidth()>0,"clipped widget row "+id);}
   View small=SceneWidget.compact(c,scene,WidgetStatus.connection(false,false,WidgetStatus.REACHABLE,now,true,now,null),last).apply(c,new FrameLayout(c));
   require(value(small,R.id.scene_name).equals("FM4 Küche"),"title only");
   require(value(small,R.id.scene_status).equals("Offline"),"compact offline");
   require(small.findViewById(R.id.scene_brand)!=null&&v.findViewById(R.id.scene_brand)!=null,"branding on both widget sizes");
   require(small.findViewById(R.id.scene_brand).getAlpha()<0.3f,"subtle background logo");
   small.measure(View.MeasureSpec.makeMeasureSpec(Math.round(64*d),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Math.round(64*d),View.MeasureSpec.EXACTLY));small.layout(0,0,Math.round(64*d),Math.round(64*d));
   require(small.findViewById(R.id.scene_name).getHeight()>0&&small.findViewById(R.id.scene_status).getHeight()>0,"compact title and status");
   require(((TextView)small.findViewById(R.id.scene_name)).getTextSize()>((TextView)small.findViewById(R.id.scene_status)).getTextSize(),"title more prominent");
   checkRadio(c);
   View universal=RadioWidget.render(c,124,true).apply(c,new FrameLayout(c));require(value(universal,R.id.volume_living).contains("Wohnzimmer")&&value(universal,R.id.volume_dining).contains("Esstisch"),"two aligned volume labels");
   for(int zone=0;zone<2;zone++){
    View volume=VolumeWidget.render(c,zone).apply(c,new FrameLayout(c));
    require(value(volume,R.id.volume_widget_name).equals(VolumeWidget.name(zone)),"volume widget zone");
    require(volume.findViewById(R.id.volume_widget_root).getContentDescription().toString().contains(VolumeWidget.name(zone)),"volume widget description");
    require(VolumeWidget.intent(c,zone).getIntExtra("zone",-1)==zone,"widget targets correct zone");
    volume.measure(View.MeasureSpec.makeMeasureSpec(Math.round(64*d),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Math.round(64*d),View.MeasureSpec.EXACTLY));volume.layout(0,0,Math.round(64*d),Math.round(64*d));
    require(volume.findViewById(R.id.volume_widget_name).getHeight()>0,"volume widget label");
   }
   RemoteViews reachable=SceneWidget.render(c,scene,WidgetStatus.connection(false,true,WidgetStatus.REACHABLE,now,true,now,null),WidgetStatus.execution("Teilweise (2)",now,false,0,false));
   View other=reachable.apply(c,new FrameLayout(c));require(value(other,R.id.scene_status).contains("Teilweise"),"partial result");require(other.findViewById(R.id.scene_run).getContentDescription().toString().contains("erreichbar"),"accessible full status");
  }catch(Throwable e){problem[0]=e;}});
  if(problem[0]==null){
   MainActivity activity=null;
   try{
    activity=(MainActivity)startActivitySync(new android.content.Intent(getTargetContext(),MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
    final MainActivity a=activity;
    runOnMainSync(()->{try{
     a.startDemo();
     for(MainActivity.Zone z:a.zones){
      LinearLayout volumeRow=(LinearLayout)z.slider.getParent();
      require(volumeRow.getChildCount()==2&&volumeRow.getChildAt(0)==z.slider&&volumeRow.getChildAt(1)==z.value,"one slider and value per zone");
     }
     require(a.linked.getParent()==a.dashboard.getChildAt(0),"group in header");
     require(a.zones[0].src.getParent()==a.zones[0].powerBtn.getParent(),"source and power on same row");
     require(a.detailsPanels[0].body.getVisibility()==View.GONE&&a.detailsPanels[1].body.getVisibility()==View.GONE,"details collapsed by default");
     a.detailsPanels[0].header.performClick();a.detailsPanels[1].header.performClick();
     require(a.detailsPanels[0].body.getVisibility()==View.VISIBLE&&a.detailsPanels[1].body.getVisibility()==View.VISIBLE,"independent details");
     require(a.mainControls.getParent()==a.dashboard&&a.detailScroll.getParent()==a.dashboard,"main controls outside details scroll");
     require(a.zones[0].slider.isEnabled()&&a.zones[1].slider.isEnabled(),"main controls stay usable");
     a.detailsPanels[0].header.performClick();require(a.detailsPanels[0].body.getVisibility()==View.GONE&&a.detailsPanels[1].body.getVisibility()==View.VISIBLE,"independent close");
     a.detailsPanels[1].header.performClick();
     a.receive("MVL037");a.receive("ZVL32");a.render();
     require(a.zones[0].slider.isEnabled(),"volume above 50 enabled");
     require(a.zones[0].slider.getMax()==82,"profile maximum");
     require(a.zones[0].vol==55,"padded receiver volume");
     a.setVolume(0,60);a.render();require(a.zones[0].vol==60,"raise volume");
     a.setVolume(0,45);a.render();require(a.zones[0].vol==45,"lower volume");
     a.setVolume(1,55);a.render();require(a.zones[1].vol==55,"zone 2 volume above 50");
     a.receive("ZVLN/A");a.render();require(!a.zones[1].slider.isEnabled(),"unavailable volume");
     a.receive("ZVL37");a.render();require(a.zones[1].slider.isEnabled(),"volume recovered");
     require(a.groupPlus.isEnabled(),"linked SOURCE available");int before=a.zones[0].vol,second=a.zones[1].vol;a.groupPlus.performClick();require(a.zones[0].vol==before+1&&a.zones[1].vol==second+1,"relative group step");
     a.groupMinus.performClick();require(a.zones[0].vol==before&&a.zones[1].vol==second,"relative group minus");
     a.receive("SLZ24");a.render();require(a.linked.getVisibility()==View.VISIBLE&&a.groupPlus.isEnabled(),"global volume independent of source");require(a.zones[1].mediaRow.getVisibility()==View.VISIBLE&&a.zones[1].mediaPrev.getVisibility()==View.VISIBLE,"FM controls");a.receive("PRZ04");a.render();require(a.zones[1].mediaChoice.getText().toString().contains("Demo FM 4"),"preset label");
     a.receive("SLZ12");a.render();require(a.zones[1].mediaChoice.getText().toString().equals("Google TV öffnen"),"TV shortcut");
     a.receive("SLZ03");a.render();require(a.zones[1].mediaRow.getVisibility()==View.GONE,"AUX hides media controls");
     a.receive("SLZ80");a.render();require(a.zones[1].mediaChoice.getText().toString().contains("folgt Wohnzimmer"),"SOURCE follows");
     a.dragVolume(0,52,false);require(a.zones[0].vol==52,"live drag before release");
     require(!a.listeningModes().containsKey("11"),"Pure Audio hidden while Esstisch on");
     a.receive("ZPW00");a.render();int livingBefore=a.zones[0].vol,diningBefore=a.zones[1].vol;
     require(a.groupPlus.isEnabled(),"single active zone group enabled");a.groupPlus.performClick();
     require(a.zones[0].vol==livingBefore+1&&a.zones[1].vol==diningBefore,"inactive zone not modified");
     require(a.listeningModes().containsKey("11"),"Pure Audio when Esstisch off");
     require(a.listeningModes().containsKey("80")&&a.listeningModes().containsKey("82"),"surround choices");
     a.receive("LMD80");require(a.modeName(a.audio.get("LMD")).contains("Dolby Surround"),"confirmed mode");
    }catch(Throwable e){problem[0]=e;}});
   }catch(Throwable e){problem[0]=e;}
   finally{if(activity!=null){final MainActivity a=activity;runOnMainSync(a::finish);}}
  }
  for(int zone=0;zone<2&&problem[0]==null;zone++){
   VolumeActivity popup=null;
   try{
    final int selected=zone;
    popup=(VolumeActivity)startActivitySync(VolumeWidget.intent(getTargetContext(),zone));
    final VolumeActivity a=popup;
    runOnMainSync(()->{try{
     require(a.popupZone==selected,"popup zone");
     require(((LinearLayout)a.zones[selected].slider.getParent()).getChildCount()==2,"popup slider only");
     a.disconnect();a.render();require(!a.zones[selected].slider.isEnabled(),"offline popup disabled");
     a.startDemo();require(a.zones[selected].slider.isEnabled(),"popup slider enabled");
     int other=a.zones[1-selected].vol;
     a.dragVolume(selected,42,false);require(a.zones[selected].vol==42&&a.zones[1-selected].vol==other,"popup controls only selected zone");
    }catch(Throwable e){problem[0]=e;}});
   }catch(Throwable e){problem[0]=e;}
   finally{if(popup!=null){final VolumeActivity a=popup;runOnMainSync(a::finish);}}
  }
  if(problem[0]==null){result.putString("widget_render_ok","true");finish(Activity.RESULT_OK,result);}
  else{result.putString("widget_render_error",problem[0].toString());finish(Activity.RESULT_CANCELED,result);}
 }
}
