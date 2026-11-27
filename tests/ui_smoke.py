#!/usr/bin/env python3
import subprocess,time,xml.etree.ElementTree as ET
def adb(*args):
    return subprocess.check_output(["adb",*args],text=True,timeout=45)
def nodes():
    # A freshly booted emulator may briefly have no accessibility root.
    # Never reuse an old dump or treat a missing tree as an empty successful result.
    path="/sdcard/onkyo-ui.xml"
    last=None
    for attempt in range(6):
        try:
            adb("shell","rm","-f",path)
            output=adb("shell","uiautomator","dump",path)
            if "ERROR" in output:
                raise RuntimeError(output.strip())
            root=ET.fromstring(adb("shell","cat",path))
            result=list(root.iter("node"))
            if not result:
                raise RuntimeError("UI dump contains no nodes")
            return result
        except (subprocess.CalledProcessError,subprocess.TimeoutExpired,ET.ParseError,RuntimeError) as error:
            last=error
            print("UI dump retry",attempt+1,str(error),flush=True)
            time.sleep(2)
    raise AssertionError("Unable to obtain fresh UI tree") from last
def wait_for(name):
    for _ in range(8):
        for n in nodes():
            if (n.get("text") or "").casefold()==name.casefold() or (n.get("content-desc") or "").casefold()==name.casefold():
                return n
        time.sleep(1)
    raise AssertionError("missing "+name)
def tap(name):
    import re
    n=wait_for(name)
    x,y,X,Y=map(int,re.findall(r"\d+",n.get("bounds")))
    adb("shell","input","tap",str((x+X)//2),str((y+Y)//2))
    time.sleep(0.5)
adb("install","-r","app/build/outputs/apk/debug/app-debug.apk")
adb("shell","input","keyevent","KEYCODE_WAKEUP")
adb("shell","wm","dismiss-keyguard")
print(adb("shell","am","start","-W","-n","at.local.dualzone/.MainActivity"),flush=True)
time.sleep(2)
wait_for("Onkyo dahoam – TX-NR656")
wait_for("Wohnzimmer")
wait_for("Esstisch")
wait_for("Google TV öffnen")
if "package:" not in subprocess.run(["adb","shell","pm","path","com.google.android.videos"],capture_output=True,text=True).stdout:
    tap("Google TV öffnen")
    wait_for("Google TV nicht verfügbar")
    tap("Schließen")
tap("Verbindung")
tap("Demo")
wait_for("DEMO · kein Receiver verbunden")
wait_for("Alle aktiven Zonen leiser")
wait_for("Alle aktiven Zonen lauter")
assert len([n for n in nodes() if n.get("class")=="android.widget.SeekBar"])==2
tap("Details Esstisch")
wait_for("Ton stummschalten")
assert len([n for n in nodes() if n.get("class")=="android.widget.SeekBar"])==2
tap("Details Esstisch")
tap("Details Wohnzimmer")
wait_for("Bass")
assert len([n for n in nodes() if n.get("class")=="android.widget.SeekBar"])==2
tap("Details Wohnzimmer")
tap("Power Esstisch")
assert len([n for n in nodes() if n.get("class")=="android.widget.SeekBar"])==1
assert not any(n.get("content-desc") in ("Quelle Esstisch","Details Esstisch","Alle aktiven Zonen lauter") for n in nodes())
wait_for("Leiser Wohnzimmer")
wait_for("Lauter Wohnzimmer")
tap("Verbindung")
tap("Diagnosebericht")
wait_for("TXT exportieren")
tap("Schließen")
tap("Verbindung")
tap("Homescreen-Widgets")
wait_for("Szene hinzufügen")
wait_for("Lautstärke gemeinsam (2×1)")
wait_for("Radio (3×2)")
print("Android UI smoke test passed; widget menu reachable")
