package at.local.dualzone;
import android.content.Context;
/** Only a previously confirmed profile of this receiver; no credentials or network access. */
final class SceneProfile {
 static void save(Context c,String raw){
  if(raw==null||raw.length()>500000)return;
  c.getSharedPreferences("scene_profile",Context.MODE_PRIVATE).edit().putString("target",SceneStore.target(c)).putString("nri",raw).apply();
 }
 static Capabilities load(Context c){
  try{android.content.SharedPreferences p=c.getSharedPreferences("scene_profile",Context.MODE_PRIVATE);
   if(!SceneStore.target(c).equals(p.getString("target","")))return null;
   Capabilities profile=Capabilities.parse(p.getString("nri",""));return profile.model.equalsIgnoreCase("TX-NR656")?profile:null;
  }catch(Exception e){return null;}
 }
}
