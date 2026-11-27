package at.local.dualzone;
import android.content.*;
import org.json.*;
final class SceneStore {
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("widgets",Context.MODE_PRIVATE);}
 static String key(int id){return "scene_"+id;}
 static Scene get(Context c,int id){
  try{String raw=prefs(c).getString(key(id),null);if(raw==null)return null;JSONObject j=new JSONObject(raw);
   return new Scene(j.getString("name"),j.getString("icon"),zone(j.getJSONObject("main")),zone(j.getJSONObject("second")),j.getBoolean("share"),j.getBoolean("demo"));
  }catch(Exception ignored){return null;}
 }
 static Scene.Zone zone(JSONObject j)throws JSONException{return new Scene.Zone(j.getInt("power"),j.getString("source"),j.getInt("volume"),j.getInt("preset"),j.getString("service"));}
 static JSONObject json(Scene.Zone z)throws JSONException{return new JSONObject().put("power",z.power).put("source",z.source).put("volume",z.volume).put("preset",z.preset).put("service",z.service);}
 static boolean save(Context c,int id,Scene s){
  try{return prefs(c).edit().putString(key(id),new JSONObject().put("name",s.name).put("icon",s.icon).put("main",json(s.zones[0])).put("second",json(s.zones[1])).put("share",s.share).put("demo",s.demo).toString()).remove("status_"+id).remove("run_"+id).remove("runAt_"+id).remove("pending_"+id).commit();}catch(JSONException e){return false;}
 }
 static boolean begin(Context c,int id){return prefs(c).edit().putLong("pending_"+id,System.currentTimeMillis()).commit();}
 static void complete(Context c,int id,String value){prefs(c).edit().putString("run_"+id,value).putLong("runAt_"+id,System.currentTimeMillis()).remove("pending_"+id).commit();}
 static WidgetStatus.Line execution(Context c,int id,boolean running){
  SharedPreferences p=prefs(c);long pending=p.getLong("pending_"+id,0);
  return WidgetStatus.execution(p.getString("run_"+id,""),p.getLong("runAt_"+id,0),pending>0,pending,running);
 }
 static String target(Context c){if(RemoteVpn.selected())return "vpn:"+RemoteVpn.target;SharedPreferences p=c.getSharedPreferences("config",Context.MODE_PRIVATE);return p.getBoolean("manual",false)?"manual:"+p.getString("ip",""):"auto:"+p.getString("deviceId","");}
 static void probe(Context c,String code,long networkHandle,String target){prefs(c).edit().putString("probeCode",code).putLong("probeAt",System.currentTimeMillis()).putLong("probeNetwork",networkHandle).putString("probeTarget",target).apply();}
 static void invalidateNetwork(Context c,long networkHandle){SharedPreferences p=prefs(c);if(p.getLong("probeNetwork",0)==networkHandle)p.edit().remove("probeCode").remove("probeAt").remove("probeNetwork").remove("probeTarget").apply();}
 static WidgetStatus.Line connection(Context c,boolean demo,long handle,String phase){
  SharedPreferences p=prefs(c);boolean match=handle!=0&&handle==p.getLong("probeNetwork",0)&&target(c).equals(p.getString("probeTarget",""));
  return WidgetStatus.connection(demo,handle!=0,p.getString("probeCode",WidgetStatus.UNKNOWN),p.getLong("probeAt",0),match,System.currentTimeMillis(),phase);
 }
 static void delete(Context c,int id){prefs(c).edit().remove(key(id)).remove("status_"+id).remove("run_"+id).remove("runAt_"+id).remove("pending_"+id).apply();}
}
