package at.local.dualzone;
import android.content.*;
import android.net.Network;
/** Short-lived profile cache bound to the verified receiver selection and LAN. */
final class VolumeProfile {
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("volume_profile",Context.MODE_PRIVATE);}
 static void save(Context c,Network n,String ip,String raw){
  if(n==null||!SceneService.privateIp(ip)||raw==null||raw.length()>500000)return;
  prefs(c).edit().putString("target",SceneStore.target(c)).putString("ip",ip).putLong("network",n.getNetworkHandle()).putLong("at",System.currentTimeMillis()).putString("nri",raw).apply();
 }
 static Capabilities get(Context c,Network n,String ip){
  try{SharedPreferences p=prefs(c);long age=System.currentTimeMillis()-p.getLong("at",0);
   if(n==null||age<0||age>600000||!ip.equals(p.getString("ip",""))||n.getNetworkHandle()!=p.getLong("network",0)||!SceneStore.target(c).equals(p.getString("target","")))return null;
   Capabilities profile=Capabilities.parse(p.getString("nri",""));return profile.model.equalsIgnoreCase("TX-NR656")?profile:null;
  }catch(Exception e){return null;}
 }
 static void clear(Context c){prefs(c).edit().clear().apply();}
}
