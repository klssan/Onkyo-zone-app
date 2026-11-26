import subprocess,sys
results=[]
for test in ("tests/ui_smoke.py","tests/widget_render.py"):
    results.append(subprocess.run([sys.executable,test]).returncode)
if any(results):
    subprocess.run(["adb","logcat","-d","-t","400","-s","AndroidRuntime","ActivityTaskManager"])
    sys.exit(1)
print("All Android checks passed")
