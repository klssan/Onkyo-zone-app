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
  try{return prefs(c).edit().putString(key(id),new JSONObject().put("name",s.name).put("icon",s.icon).put("main",json(s.zones[0])).put("second",json(s.zones[1])).put("share",s.share).put("demo",s.demo).toString()).putString("status_"+id,"Bereit").commit();}catch(JSONException e){return false;}
 }
 static String status(Context c,int id){String s=prefs(c).getString("status_"+id,"Bereit");return s.equals("Wird ausgeführt …")?"Bereit":s;}
 static void status(Context c,int id,String value){prefs(c).edit().putString("status_"+id,value).apply();}
 static void delete(Context c,int id){prefs(c).edit().remove(key(id)).remove("status_"+id).apply();}
}
