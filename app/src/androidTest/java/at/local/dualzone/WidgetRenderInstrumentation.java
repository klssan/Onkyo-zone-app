package at.local.dualzone;
import android.app.*;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
public final class WidgetRenderInstrumentation extends Instrumentation {
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static String value(View root,int id){return ((TextView)root.findViewById(id)).getText().toString();}
 @Override public void onStart(){
  Bundle result=new Bundle();final Throwable[] problem={null};
  runOnMainSync(()->{try{
   Context c=getTargetContext();long now=System.currentTimeMillis();
   Scene scene=new Scene("FM4 Küche","Radio",new Scene.Zone(Scene.KEEP,"",-1,-1,""),new Scene.Zone(Scene.ON,"24",40,4,""),false,false);
   WidgetStatus.Line last=WidgetStatus.execution("Bestätigt",now,false,0,false);
   RemoteViews views=SceneWidget.render(c,scene,WidgetStatus.connection(false,false,WidgetStatus.REACHABLE,now,true,now,null),last);
   View v=views.apply(c,new FrameLayout(c));
   require(value(v,R.id.scene_name).equals("FM4 Küche"),"name");
   require(value(v,R.id.scene_main).equals("Haupt: Unverändert"),"main unchanged");
   require(value(v,R.id.scene_second).equals("Zone 2: An · FM P04 · Vol. 40"),"configured action");
   require(value(v,R.id.scene_connection).equals("Kein WLAN/LAN"),"offline");
   require(value(v,R.id.scene_status).contains("Bestätigt"),"previous run retained");
   require(v.findViewById(R.id.scene_check).getVisibility()==View.VISIBLE,"read-only check visible");
   require(v.findViewById(R.id.scene_shared).getVisibility()==View.GONE,"shared flag");
   float d=c.getResources().getDisplayMetrics().density;int width=Math.round(300*d),height=Math.round(200*d);
   v.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));v.layout(0,0,width,height);
   for(int id:new int[]{R.id.scene_name,R.id.scene_main,R.id.scene_second,R.id.scene_connection,R.id.scene_status}){View item=v.findViewById(id);require(item.getHeight()>0&&item.getWidth()>0,"clipped widget row "+id);}
   RemoteViews reachable=SceneWidget.render(c,scene,WidgetStatus.connection(false,true,WidgetStatus.REACHABLE,now,true,now,null),WidgetStatus.execution("Teilweise (2)",now,false,0,false));
   View other=reachable.apply(c,new FrameLayout(c));require(value(other,R.id.scene_connection).contains("erreichbar")&&value(other,R.id.scene_status).contains("Teilweise"),"separate result");
  }catch(Throwable e){problem[0]=e;}});
  if(problem[0]==null){result.putString("widget_render_ok","true");finish(Activity.RESULT_OK,result);}
  else{result.putString("widget_render_error",problem[0].toString());finish(Activity.RESULT_CANCELED,result);}
 }
}
