package at.local.dualzone;
import java.util.*;
import java.io.*;
public class RadioControlTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  Capabilities c=Capabilities.demo();c.presets.clear();c.presets.add(new Capabilities.Preset(7,1,"Ö3",""));c.presets.add(new Capabilities.Preset(9,1,"FM4",""));
  check(RadioControl.nextPreset(c,7,1)==9&&RadioControl.nextPreset(c,9,1)==7&&RadioControl.nextPreset(c,7,-1)==9);
  List<String> writes=new ArrayList<>();SceneExecutor.Wire w=new SceneExecutor.Wire(){public String query(String prefix){throw new AssertionError();}public void set(String cmd,String p,String v){writes.add(cmd);}};
  RadioControl.State s=new RadioControl.State();s.scale[1]=2;s.max[1]=164;
  RadioControl.toggle(s,w,c,1);check(writes.equals(Arrays.asList("ZPW01","ZVL5A","SLZ24")));writes.clear();
  s.on[1]=true;s.source[1]="24";s.raw[1]=90;s.raw[0]=50;s.preset=7;
  RadioControl.step(s,w,c,1);check(writes.equals(Arrays.asList("PRZ09")));writes.clear();
  c.presets.clear();RadioControl.step(s,w,c,-1);check(writes.equals(Arrays.asList("TUZDOWN")));writes.clear();
  RadioControl.adjust(s,w,1);check(writes.equals(Arrays.asList("ZVL5C"))&&s.raw[1]==92);writes.clear();
  RadioControl.adjust(s,w,3);check(writes.equals(Arrays.asList("ZVL62"))&&s.raw[1]==98);writes.clear();
  s.on[0]=true;s.source[0]="12";RadioControl.adjust(s,w,-1);check(writes.equals(Arrays.asList("MVL31","ZVL60")));writes.clear();
  RadioControl.toggle(s,w,c,1);check(writes.equals(Arrays.asList("ZPW00")));writes.clear();
  s.on[1]=true;s.source[1]="12";s.raw[1]=60;RadioControl.startFM(s,w,c,1);check(writes.equals(Arrays.asList("ZVL5A","SLZ24")));writes.clear();
  s.source[1]="24";s.preset=7;c.presets.add(new Capabilities.Preset(9,1,"FM4",""));RadioControl.selectPreset(s,w,c,1,9);check(writes.equals(Arrays.asList("PRZ09")));writes.clear();
  s.on[0]=false;s.on[1]=false;RadioControl.adjust(s,w,1);check(writes.isEmpty());
  List<String> queries=new ArrayList<>();
  SceneExecutor.Wire standby=new SceneExecutor.Wire(){public String query(String prefix){queries.add(prefix);if(prefix.equals("PWR")||prefix.equals("ZPW"))return "00";if(prefix.equals("SLI")||prefix.equals("SLZ"))return "24";throw new AssertionError(prefix);}public void set(String a,String b,String c){throw new AssertionError(a);}};
  RadioControl.State off=RadioControl.read(standby,c);check(off.raw[0]==-1&&off.raw[1]==-1&&!queries.contains("MVL")&&!queries.contains("ZVL"));
  s.on[0]=true;s.source[0]="12";s.raw[0]=50;
  s.on[1]=true;s.source[1]="24";s.raw[1]=90;s.preset=7;
  RadioControl.adjustDining(s,w,1);check(writes.equals(Arrays.asList("ZVL5C"))&&s.raw[0]==50);writes.clear();
  RadioControl.stepDining(s,w,c);check(writes.equals(Arrays.asList("PRZ09")));writes.clear();
  s.on[1]=false;RadioControl.adjustDining(s,w,1);RadioControl.stepDining(s,w,c);check(writes.isEmpty());
  s.on[1]=true;s.source[1]="12";RadioControl.adjustDining(s,w,1);RadioControl.stepDining(s,w,c);check(writes.isEmpty());
  System.out.println("Radio preset wrap, tuning fallback, volume 45, active-only and dining-only volume passed");
 }
}
