import subprocess
subprocess.check_call(["adb","install","-r","app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"])
out=subprocess.check_output(["adb","shell","am","instrument","-w","at.local.dualzone.test/at.local.dualzone.WidgetRenderInstrumentation"],text=True)
print(out)
if "widget_render_ok=true" not in out or "INSTRUMENTATION_CODE: -1" not in out:
    raise AssertionError("Widget RemoteViews rendering not confirmed")
