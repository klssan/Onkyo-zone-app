import subprocess
subprocess.check_call(["adb","install","-r","app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"])
out=subprocess.check_output(["adb","shell","am","instrument","-w","-e","radioOnly","true","at.local.dualzone.test/at.local.dualzone.WidgetRenderInstrumentation"],text=True,timeout=90)
print(out)
assert "widget_render_ok=true" in out and "INSTRUMENTATION_CODE: -1" in out, "Radio widget layout checks failed"
