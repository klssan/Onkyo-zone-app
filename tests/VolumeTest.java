package at.local.dualzone;
import java.util.*;
public class VolumeTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  String xml="<response status='ok'><device id='TX-NR656'><zonelist><zone id='1' value='1' volmax='82' volstep='1'/><zone id='2' value='1' volmax='0' volstep='1'/></zonelist><selectorlist count='0'/></device></response>";
  Capabilities c=Capabilities.parse(xml);
  check(c.volumeMax[0]==82&&c.volumeMax[1]==0&&c.volumeScale[0]==1&&c.volumeScale[1]==1);
  check(Protocol.maxVolume(82,1,55)==82&&Protocol.maxVolume(0,1,55)==100&&Protocol.maxVolume(50,1,55)==55);
  check(Protocol.volume("37")==55&&Protocol.volume("037")==55&&Protocol.volumeMatches("37","037"));
  check(Protocol.volume("0A5")==165&&Protocol.volumeLabel(165,2).equals("82,5")&&Protocol.volumeLabel(55,1).equals("55"));
  check(Protocol.volume("N/A")==-1&&Protocol.volume("FFF")==-1);
  check(Arrays.equals(Protocol.linkedScaled(55,45,1,1,82,70,-20),new int[]{45,35}));
  check(Arrays.equals(Protocol.linkedScaled(55,45,1,1,82,70,100),new int[]{80,70}));
  check(Arrays.equals(Protocol.linkedScaled(55,91,1,2,82,140,8),new int[]{59,99}));
  check(Arrays.equals(Protocol.linkedScaled(55,91,1,2,82,140,100),new int[]{79,139}));
  Scene scene=new Scene("Lautstärke","Musik",new Scene.Zone(Scene.KEEP,"",55,-1,""),new Scene.Zone(Scene.KEEP,"",-1,-1,""),false,false);
  List<String> commands=new ArrayList<>();SceneExecutor.Wire wire=new SceneExecutor.Wire(){public String query(String p){return p.equals("PWR")?"01":p.equals("ZPW")?"00":"12";}public void set(String cmd,String p,String v){commands.add(cmd);}};
  check(SceneExecutor.execute(scene,wire,c)==1&&commands.equals(Arrays.asList("MVL37")));
  c.volumeMax[0]=50;commands.clear();try{SceneExecutor.execute(scene,wire,c);throw new AssertionError();}catch(SceneExecutor.SceneException ok){check(commands.isEmpty());}
  c.volumeScale[0]=2;c.volumeMax[0]=164;check(SceneExecutor.execute(scene,wire,c)==1&&commands.equals(Arrays.asList("MVL6E")));
  System.out.println("Volume above 50, NRI maxima, half steps, padded replies and scene limits passed");
 }
}
