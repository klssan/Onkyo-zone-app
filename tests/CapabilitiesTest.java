package at.local.dualzone;
public class CapabilitiesTest{
 public static void main(String[] args)throws Exception{
  String xml="<response status='ok'><device id='TX-NR656'><model>TX-NR656</model><presetlist count='3'><preset id='04' band='1' freq='99.30' name='Demo Radio'/><preset id='0A' band='2' freq='720' name=''/><preset id='05' band='0' freq='0' name=''/></presetlist><selectorlist count='4'><selector id='05' value='1' name='PC' zone='01'/><selector id='2b' value='1' name='NET' zone='03'/><selector id='80' value='1' name='Source' zone='02'/><selector id='10' value='0' name='Disabled' zone='03'/></selectorlist><netservicelist count='3'><netservice id='0a' value='1' name='Spotify' zone='07' enable='07'/><netservice id='40' value='1' name='Chromecast built-in' zone='07' enable='01'/><netservice id='43' value='1' name='FlareConnect' zone='07' enable='0e'/></netservicelist><controllist count='3'><control id='Bass' value='1' zone='1'/><control id='Treble' value='0' zone='1'/><control id='TUNER Control' value='1'/></controllist></device></response>";
  Capabilities c=Capabilities.parse("X0000"+xml);
  if(!c.model.equals("TX-NR656")||!c.sources(0).containsKey("05")||c.sources(1).containsKey("05")||!c.sources(1).containsKey("80")||c.sources(0).containsKey("80")||c.sources(1).containsKey("10"))throw new AssertionError("selectors");
  if(!c.services(1).containsKey("0A")||c.services(1).containsKey("40")||c.services(0).containsKey("43")||!c.services(1).containsKey("43"))throw new AssertionError("services");
  if(!c.control("Bass",0)||c.control("Bass",1)||c.control("Treble",0)||!c.control("TUNER Control",1))throw new AssertionError("controls");
  if(!c.radioPresets("24").get(4).equals("P04 · Demo Radio · 99.30 MHz")||!c.radioPresets("25").containsKey(10)||!c.radioPresets("12").isEmpty()||c.radioPresets("24").containsKey(5))throw new AssertionError("presets");
  try{Capabilities.parse("<!DOCTYPE x [<!ENTITY ext SYSTEM 'file:///etc/passwd'>]>"+xml);throw new AssertionError("DTD accepted");}catch(IllegalArgumentException expected){}
  try{Capabilities.parse("<response status='error'/>");throw new AssertionError("error status accepted");}catch(IllegalArgumentException expected){}
  System.out.println("Capability tests passed");
 }
}
