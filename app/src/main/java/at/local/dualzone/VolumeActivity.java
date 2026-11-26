package at.local.dualzone;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;

/** Uses the same receiver session, volume bounds and live drag logic as the main screen. */
public final class VolumeActivity extends MainActivity {
 int popupZone=-1;
 TextView volumeHint;
 Button closeButton;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  popupZone=getIntent().getIntExtra("zone",-1);
  if(popupZone<0||popupZone>1){finish();return;}
  setFinishOnTouchOutside(false);
  LinearLayout root=col();root.setBackground(shape(PANEL,16));root.setPadding(dp(16),dp(12),dp(16),dp(12));
  LinearLayout top=row();TextView name=label(VolumeWidget.name(popupZone),21,FG);name.setTypeface(null,Typeface.BOLD);weighted(top,name,44);
  closeButton=btn("Fertig");add(top,closeButton,76,44);closeButton.setOnClickListener(v->closePopup());add(root,top,-1,44);
  ((ViewGroup)status.getParent()).removeView(status);status.setOnClickListener(null);add(root,status,-1,40);
  Zone z=zones[popupZone];View volumeRow=(View)z.slider.getParent();((ViewGroup)volumeRow.getParent()).removeView(volumeRow);add(root,volumeRow,-1,56);
  z.slider.setContentDescription("Lautstärke "+VolumeWidget.name(popupZone));
  volumeHint=label("",12,MUTED);add(root,volumeHint,-1,36);
  setContentView(root);
  int width=Math.min(dp(380),getResources().getDisplayMetrics().widthPixels-dp(32));
  getWindow().setLayout(width,WindowManager.LayoutParams.WRAP_CONTENT);
  getWindow().setGravity(Gravity.CENTER);
  render();
 }
 @Override void render(){
  super.render();
  if(popupZone<0||popupZone>1||volumeHint==null)return;
  Zone z=zones[popupZone];
  boolean verified=demo||capabilities!=null;
  boolean usable=verified&&canVolume(z);
  z.slider.setEnabled(usable);z.minus.setEnabled(usable&&shownVolume(z)>0);z.plus.setEnabled(usable&&shownVolume(z)<maxVolume(popupZone));
  String hint=!connected?"Keine bestätigte Verbindung":Boolean.FALSE.equals(z.power)?"Zone ausgeschaltet":!verified?"Geräteprofil wird geprüft":z.unavailable?"Lautstärke nicht regelbar":z.pending>=0?"Warte auf Bestätigung …":usable?"Maximum "+Protocol.volumeLabel(maxVolume(popupZone),scale(popupZone)):"Lautstärke wird abgefragt";
  volumeHint.setText(hint);
  closeButton.setEnabled(z.pending<0);
 }
 void closePopup(){
  if(popupZone>=0&&popupZone<2&&zones[popupZone].pending>=0){Toast.makeText(this,"Lautstärke noch nicht bestätigt",Toast.LENGTH_SHORT).show();return;}
  finish();
 }
 @Override public void onBackPressed(){closePopup();}
}
