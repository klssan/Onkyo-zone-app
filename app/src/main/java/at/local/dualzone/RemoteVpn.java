package at.local.dualzone;
import android.content.Context;
import android.net.Network;
/** LAN-only isolation build: no VPN provider, credentials, service or third-party dependency. */
final class RemoteVpn {
 static boolean selected(Context c){return false;}
 static Network network(Context c){return null;}
 static Network ensure(Context c,boolean explicit,boolean forceVpn){return null;}
 static String target(Context c){return "";}
 static String status(Context c){return "Nur Heimnetz";}
 static void request(Context c){}
 static void invalidate(Context c,Network n){}
}
