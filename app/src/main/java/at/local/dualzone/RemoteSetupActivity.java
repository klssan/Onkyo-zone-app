package at.local.dualzone;
import android.app.*;
import android.os.*;
import android.content.*;
import android.net.VpnService;
import android.view.*;
import android.widget.*;
import java.io.*;

public final class RemoteSetupActivity extends Activity {
 static final int IMPORT=601,PERMISSION=602;
 LinearLayout root;TextView state;EditText ip;Button importButton,connectButton,stopButton,removeButton;
 boolean busy;
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 void text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setPadding(0,dp(5),0,dp(8));root.addView(t);}
 Button button(String title,Runnable action){Button b=new Button(this);b.setText(title);root.addView(b,new LinearLayout.LayoutParams(-1,dp(52)));b.setOnClickListener(v->action.run());return b;}
 @Override public void onCreate(Bundle b){
  super.onCreate(b);ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(20),dp(20),dp(20));scroll.addView(root);setContentView(scroll);
  text("Fernzugriff einrichten",26);
  text("1. Zu Hause die FRITZ!Box öffnen: Heimnetz → Netzwerk → Onkyo → immer gleiche IPv4-Adresse zuweisen.\n2. Internet → Freigaben → VPN (WireGuard) → Einzelgerät verbinden. Einstellungen (.conf) herunterladen.",15);
  button("FRITZ!Box öffnen",()->{try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("http://fritz.box")));}catch(Exception ignored){Toast.makeText(this,"fritz.box im Browser öffnen",Toast.LENGTH_LONG).show();}});
  text("3. Onkyo-IP prüfen und FRITZ!Box-Datei auswählen.",16);
  ip=new EditText(this);ip.setSingleLine(true);ip.setText(getSharedPreferences("config",MODE_PRIVATE).getString("ip",""));ip.setHint("z. B. 192.168.178.42");ip.setInputType(android.text.InputType.TYPE_CLASS_TEXT);root.addView(ip);
  importButton=button("WireGuard-Datei importieren",()->{
   if(!SceneService.privateIp(ip.getText().toString().trim())){message("Zuerst zu Hause mit dem Onkyo verbinden oder seine private IP eingeben.");return;}
   Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);
   try{startActivityForResult(i,IMPORT);}catch(Exception e){message("Dateiauswahl nicht verfügbar.");}
  });
  text("4. WLAN ausschalten, mobile Daten aktivieren und Fernzugriff testen. Android fragt einmalig nach der VPN-Berechtigung.",15);
  connectButton=button("Fernzugriff verbinden & testen",()->{
   if(!RemoteVault.exists(this)){message("Zuerst die .conf-Datei importieren.");return;}
   Intent permission=VpnService.prepare(this);if(permission!=null)startActivityForResult(permission,PERMISSION);else connect();
  });
  state=new TextView(this);state.setTextSize(16);state.setPadding(0,dp(8),0,dp(8));root.addView(state);
  stopButton=button("Fernzugriff trennen",()->work(()->RemoteVpn.stop(this),"Fernzugriff getrennt.","Trennen fehlgeschlagen."));
  removeButton=button("Konfiguration löschen",()->new AlertDialog.Builder(this).setMessage("Gespeicherte VPN-Schlüssel auf diesem Handy löschen? Die Verbindung in der FRITZ!Box bei Bedarf ebenfalls entfernen.").setNegativeButton("Abbrechen",null).setPositiveButton("Löschen",(d,n)->work(()->{RemoteVpn.stop(this);RemoteVault.remove(this);},"Konfiguration gelöscht.","Löschen fehlgeschlagen.")).show());
  text("Nur Onkyo dahoam und die Onkyo-IP nutzen den Tunnel. Andere Apps bleiben unverändert. Keine Portfreigabe. Die Datei enthält private Schlüssel; nach Import aus Downloads löschen.",13);
  text("Nach erfolgreicher VPN-Freigabe wird bei Bedarf automatisch verbunden. Im bekannten Heimnetz bevorzugt die App die lokale Verbindung. Bei fehlendem Internet oder einer Störung werden Steuerbefehle nicht nachträglich wiederholt. Bei einem anderen aktiven Android-VPN dieses zuerst trennen.",13);
  refresh();
 }
 void message(String s){new AlertDialog.Builder(this).setMessage(s).setPositiveButton("OK",null).show();}
 void refresh(){state.setText(RemoteVpn.status(this));boolean selected=RemoteVpn.selected(this);importButton.setEnabled(!busy&&!selected);ip.setEnabled(!busy&&!selected);connectButton.setEnabled(!busy&&RemoteVault.exists(this));stopButton.setEnabled(!busy&&selected);removeButton.setEnabled(!busy&&RemoteVault.exists(this));}
 interface Work{void run()throws Exception;}
 void work(Work action,String success,String failure){
  busy=true;refresh();state.setText("Bitte warten …");
  new Thread(()->{boolean ok=false;try{action.run();ok=true;}catch(Exception e){Diagnostics.error("REMOTE_SETUP_ERROR",e);}final boolean done=ok;
   runOnUiThread(()->{busy=false;SceneWidget.refreshAll(this);if(!isFinishing()&&!isDestroyed()){refresh();message(done?success:failure);}});
  },"remote-setup").start();
 }
 void connect(){work(()->RemoteVpn.start(this),"Onkyo über Fernzugriff bestätigt. Zurück zur Steuerung.","Keine bestätigte Onkyo-Verbindung. Mobile Daten, MyFRITZ!-Erreichbarkeit, feste Onkyo-IP und Netzwerk-Standby prüfen. Automatische Wiederverbindung ist aktiv.");}
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(result!=RESULT_OK){if(request==PERMISSION)message("VPN-Freigabe nicht erteilt. Lokale Steuerung bleibt möglich.");return;}
  if(request==PERMISSION){connect();return;}
  if(request!=IMPORT||data==null||data.getData()==null)return;
  final android.net.Uri uri=data.getData();final String target=ip.getText().toString().trim();
  work(()->{try(InputStream in=getContentResolver().openInputStream(uri)){
   if(in==null)throw new IOException("Datei fehlt");
   ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;
   while((n=in.read(buf))!=-1){if(out.size()+n>32768)throw new IOException("Datei zu groß");out.write(buf,0,n);}
   RemoteConfig cfg=RemoteConfig.restrict(new ByteArrayInputStream(out.toByteArray()),target,getPackageName());RemoteVault.save(this,cfg);RemoteVpn.settings(this).edit().putString("target",cfg.ip).apply();
  }},"Konfiguration sicher importiert. Jetzt Fernzugriff testen.","Import fehlgeschlagen. Einzelgerät-.conf und private Onkyo-IP prüfen.");
 }
}
