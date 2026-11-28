#!/usr/bin/env python3
import subprocess,time,xml.etree.ElementTree as ET, pathlib, sys
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
def wait_until(predicate, description):
    # Retry a fresh observation, not the action or the entire test.
    last=[]
    for _ in range(8):
        last=nodes()
        result=predicate(last)
        if result:
            return result
        time.sleep(1)
    visible=[(n.get("text"),n.get("content-desc"),n.get("class")) for n in last]
    raise AssertionError(f"{description}; visible UI: {visible}")

def wait_for(name):
    return wait_until(lambda ns: next((n for n in ns if
        (n.get("text") or "").casefold()==name.casefold() or
        (n.get("content-desc") or "").casefold()==name.casefold()),None), "missing "+name)

def wait_for_seekbars(count):
    wait_until(lambda ns: sum(n.get("class")=="android.widget.SeekBar" for n in ns)==count,
               f"expected {count} visible zone sliders")
def tap(name):
    import re
    n=wait_for(name)
    assert n.get("enabled")=="true", f"disabled control: {name}"
    x,y,X,Y=map(int,re.findall(r"\d+",n.get("bounds")))
    assert X>x and Y>y, f"invalid bounds: {name}"
    adb("shell","input","tap",str((x+X)//2),str((y+Y)//2))
    time.sleep(0.5)
def main():
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
    wait_for_seekbars(2)
    tap("Details Esstisch")
    wait_for("Ton stummschalten")
    wait_for_seekbars(2)
    tap("Details Esstisch")
    tap("Details Wohnzimmer")
    wait_for("Bass")
    wait_for_seekbars(2)
    tap("Details Wohnzimmer")
    tap("Power Esstisch")
    wait_for_seekbars(1)
    tap("Verbindung")
    tap("Diagnosebericht")
    wait_for("TXT exportieren")
    tap("Schließen")
    tap("Verbindung")
    tap("Homescreen-Widgets")
    wait_for("Szene hinzufügen")
    wait_for("Lautstärke gemeinsam (2×1)")
    wait_for("Radio (2×2)")
    print("Android UI smoke test passed; widget menu reachable")

def capture_failure():
    out=pathlib.Path("ci-diagnostics")
    out.mkdir(exist_ok=True)
    for name,args in [
        ("screen.png",["exec-out","screencap","-p"]),
        ("logcat.txt",["logcat","-d","-t","2000"]),
        ("window.txt",["shell","dumpsys","window"]),
        ("ui.xml",["shell","cat","/sdcard/onkyo-ui.xml"]),
    ]:
        try:
            with (out/name).open("wb") as target:
                subprocess.run(["adb",*args],stdout=target,stderr=subprocess.PIPE,timeout=30,check=True)
        except Exception as error:
            print(f"Diagnostic {name} unavailable: {error}",file=sys.stderr)

if __name__=="__main__":
    try:
        main()
    except Exception:
        capture_failure()
        raise
