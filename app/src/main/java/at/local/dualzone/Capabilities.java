package at.local.dualzone;
import java.io.StringReader;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
final class Capabilities {
 static final class Entry{
  final String id,name;final int zones;
  Entry(String id,String name,int zones){this.id=id;this.name=name;this.zones=zones;}
 }
 final String model;final int[] volumeMax={0,0},volumeScale={1,1};final boolean[] volumeReported={false,false};String firmware="unknown";boolean hasSelectors,hasServices,hasControls;
 static final class Preset{
  final int number,band;final String name,frequency;
  Preset(int number,int band,String name,String frequency){this.number=number;this.band=band;this.name=name;this.frequency=frequency;}
  String label(){return String.format(Locale.ROOT,"P%02d",number)+(name.isEmpty()?(frequency.isEmpty()?"":" · "+frequency):" · "+name+(frequency.isEmpty()?"":" · "+frequency));}
 }
 final List<Preset> presets=new ArrayList<>();boolean hasPresets;
 final List<Entry> selectors=new ArrayList<>(),net=new ArrayList<>(),controls=new ArrayList<>();
 Capabilities(String model){this.model=model;}
 static int mask(String value,int fallback){try{return value.isEmpty()?fallback:Integer.parseInt(value,16);}catch(NumberFormatException e){return 0;}}
 static Element child(Element e,String tag){NodeList list=e.getChildNodes();for(int i=0;i<list.getLength();i++){Node n=list.item(i);if(n instanceof Element&&((Element)n).getTagName().equals(tag))return (Element)n;}return null;}
 static Capabilities parse(String payload)throws Exception{
  int start=payload.indexOf('<');if(start<0||payload.length()>500000)throw new IllegalArgumentException("NRI XML");String xml=payload.substring(start);
  String upper=xml.toUpperCase(Locale.ROOT);if(upper.contains("<!DOCTYPE")||upper.contains("<!ENTITY"))throw new IllegalArgumentException("DTD not allowed");
  DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
  javax.xml.parsers.DocumentBuilder builder=factory.newDocumentBuilder();
  builder.setEntityResolver((publicId,systemId)->{throw new org.xml.sax.SAXException("External entity blocked");});
  Document doc=builder.parse(new InputSource(new StringReader(xml)));Element root=doc.getDocumentElement();
  if(!root.getTagName().equals("response")||!root.getAttribute("status").equals("ok"))throw new IllegalArgumentException("NRI status");
  Element device=child(root,"device");if(device==null)throw new IllegalArgumentException("NRI device");
  Element modelElement=child(device,"model");String model=modelElement==null?device.getAttribute("id"):modelElement.getTextContent().trim();if(model.isEmpty())throw new IllegalArgumentException("NRI model");
  Capabilities c=new Capabilities(model);Element fw=child(device,"firmwareversion");if(fw!=null&&fw.getTextContent().trim().matches("[0-9-]{5,40}"))c.firmware=fw.getTextContent().trim();
  Element zones=child(device,"zonelist");if(zones!=null){
   NodeList entries=zones.getChildNodes();for(int i=0;i<entries.getLength();i++){Node node=entries.item(i);if(!(node instanceof Element))continue;Element z=(Element)node;if(!z.getTagName().equals("zone")||!z.getAttribute("value").equals("1"))continue;
    String id=z.getAttribute("id");if(!id.equals("1")&&!id.equals("2"))continue;int index=Integer.parseInt(id)-1;
    String step=z.getAttribute("volstep");if(step.equals("0")||step.equals("1")){c.volumeReported[index]=true;c.volumeScale[index]=step.equals("0")?2:1;}
    try{int maximum=Integer.parseInt(z.getAttribute("volmax"));if(maximum>0&&maximum<=100*c.volumeScale[index])c.volumeMax[index]=maximum;}catch(NumberFormatException ignored){}
   }
  }
  Element presetList=child(device,"presetlist");if(presetList!=null){
   c.hasPresets=true;NodeList entries=presetList.getChildNodes();
   for(int i=0;i<entries.getLength();i++){Node node=entries.item(i);if(!(node instanceof Element))continue;Element e=(Element)node;if(!e.getTagName().equals("preset"))continue;
    try{int number=Integer.parseInt(e.getAttribute("id"),16),band=Integer.parseInt(e.getAttribute("band"));if(number<1||number>40||band<1||band>2)continue;
     String name=e.getAttribute("name").trim(),frequency=e.getAttribute("freq");if(name.length()>80)name=name.substring(0,80);if(!frequency.matches("[0-9]{1,5}(\\.[0-9]{1,3})?"))frequency="";
     c.presets.add(new Preset(number,band,name,frequency.isEmpty()?"":frequency+(band==1?" MHz":" kHz")));
    }catch(NumberFormatException ignored){}
   }
  }
  c.hasSelectors=load(child(device,"selectorlist"),"selector",c.selectors,3,false);
  c.hasServices=load(child(device,"netservicelist"),"netservice",c.net,3,true);
  c.hasControls=load(child(device,"controllist"),"control",c.controls,3,false);
  if(!c.hasSelectors&&!c.hasServices&&!c.hasControls)throw new IllegalArgumentException("Empty profile");
  return c;
 }
 static boolean load(Element list,String tag,List<Entry> result,int defaultZone,boolean service){
  if(list==null)return false;NodeList ns=list.getChildNodes();boolean recognized=false;
  for(int i=0;i<ns.getLength();i++){Node n=ns.item(i);if(!(n instanceof Element))continue;Element e=(Element)n;if(!e.getTagName().equals(tag))continue;String id=e.getAttribute("id"),value=e.getAttribute("value");if(id.isEmpty()||!value.matches("[01]"))continue;recognized=true;if(!value.equals("1"))continue;
   boolean control=tag.equals("control");if(!control){if(!id.matches("[0-9a-fA-F]{2}"))continue;id=id.toUpperCase(Locale.ROOT);}
   String name=control?id:e.getAttribute("name").trim();if(name.isEmpty())name=id;
   int zones=mask(e.getAttribute("zone"),defaultZone);if(service)zones&=mask(e.getAttribute("enable"),zones);
   result.add(new Entry(id,name,zones));
  }return recognized||list.getAttribute("count").equals("0");
 }
 LinkedHashMap<String,String> sources(int zone){return options(selectors,zone);}
 LinkedHashMap<String,String> services(int zone){return options(net,zone);}
 static LinkedHashMap<String,String> options(List<Entry> entries,int zone){LinkedHashMap<String,String> out=new LinkedHashMap<>();for(Entry e:entries)if((e.zones&(1<<zone))!=0)out.put(e.id,e.name);return out;}
 String sourceName(String code){return name(selectors,code);}
 String serviceName(String code){return name(net,code);}
 static String name(List<Entry> entries,String code){for(Entry e:entries)if(e.id.equals(code))return e.name;return null;}
 boolean control(String id,int zone){for(Entry e:controls)if(e.id.equals(id)&&(e.zones&(1<<zone))!=0)return true;return false;}
 String summary(){StringBuilder s=new StringBuilder("TX-NR656 firmware="+firmware+" selectors="+hasSelectors+" services="+hasServices+" controls="+hasControls);
  for(int z=0;z<2;z++){s.append(" zone").append(z+1).append(" volmaxRaw=").append(volumeMax[z]).append(" scale=").append(volumeScale[z]).append(" sources=").append(sources(z).keySet()).append(" net=").append(services(z).keySet());for(String k:new String[]{"Bass","Treble","Center Level","Subwoofer Level","TUNER Control","Music Optimizer","LMD Stereo","LMD Direct","LMD Pure Audio"})s.append(" ").append(k).append("=").append(control(k,z));}
  return s.toString();}
 LinkedHashMap<Integer,String> radioPresets(String source){
  LinkedHashMap<Integer,String> list=new LinkedHashMap<>();int band=source.equals("24")?1:source.equals("25")?2:0;if(band==0)return list;
  if(hasPresets){for(Preset p:presets)if(p.band==band)list.put(p.number,p.label());}
  else for(int i=1;i<=40;i++)list.put(i,String.format(Locale.ROOT,"P%02d",i));
  return list;
 }
 static Capabilities demo(){
  Capabilities c=new Capabilities("TX-NR656");c.volumeMax[0]=82;c.volumeMax[1]=82;c.volumeReported[0]=c.volumeReported[1]=true;c.hasPresets=true;for(int i=1;i<=5;i++)c.presets.add(new Preset(i,1,"Demo FM "+i,""));c.hasSelectors=c.hasServices=c.hasControls=true;
  String[] ids={"10","01","02","11","05","03","25","24","23","12","22","2B","2E","80"};
  String[] names={"BD/DVD","CBL/SAT","GAME","STRM BOX","PC","AUX","AM","FM","CD","TV","PHONO","NET","Bluetooth","SOURCE"};
  for(int i=0;i<ids.length;i++)c.selectors.add(new Entry(ids[i],names[i],ids[i].equals("05")?1:ids[i].equals("80")?2:3));
  String[] services={"0E","0A","12","00","F0","1B","40","18"};String[] labels={"TuneIn Radio","Spotify","Deezer","Music Server","USB","TIDAL","Chromecast built-in","AirPlay"};
  for(int i=0;i<services.length;i++)c.net.add(new Entry(services[i],labels[i],services[i].equals("40")?1:3));
  for(String key:new String[]{"Bass","Treble","Center Level","Subwoofer Level"})c.controls.add(new Entry(key,key,1));
  c.controls.add(new Entry("TUNER Control","TUNER Control",3));return c;
 }
}
