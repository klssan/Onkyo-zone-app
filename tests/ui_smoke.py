#!/usr/bin/env python3
import subprocess,time,xml.etree.ElementTree as ET
def adb(*args):
    return subprocess.check_output(["adb",*args],text=True)
def nodes():
    adb("shell","uiautomator","dump","/sdcard/onkyo-ui.xml")
    return list(ET.fromstring(adb("shell","cat","/sdcard/onkyo-ui.xml")).iter("node"))
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
adb("shell","am","start","-n","at.local.dualzone/.MainActivity")
wait_for("Hauptzone")
wait_for("Nebenzone")
wait_for("Google TV öffnen")
if "package:" not in subprocess.run(["adb","shell","pm","path","com.google.android.videos"],capture_output=True,text=True).stdout:
    tap("Google TV öffnen")
    wait_for("Google TV nicht verfügbar")
    tap("Schließen")
tap("Verbindung")
tap("Demo")
wait_for("DEMO · kein Receiver verbunden")
tap("Details Nebenzone")
wait_for("Ton stummschalten")
assert not any(n.get("text") in ("Bass","Höhen","Balance") for n in nodes())
adb("shell","input","keyevent","4")
tap("Details Hauptzone")
wait_for("Bass")
adb("shell","input","keyevent","4")
tap("Verbindung")
tap("Diagnosebericht")
wait_for("TXT exportieren")
tap("Schließen")
tap("Verbindung")
tap("Homescreen-Widgets")
wait_for("Widget hinzufügen")
print("Android UI smoke test passed; widget menu reachable")
