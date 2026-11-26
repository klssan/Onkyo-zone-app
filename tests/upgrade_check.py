"""Exercise a real in-place Android package update and verify data retention."""
import subprocess

PKG = "at.local.dualzone"
def adb(*args):
    return subprocess.check_output(["adb", *args], text=True, timeout=90).strip()

assert "Success" in adb("install", "previous/previous.apk")
adb("shell", "run-as", PKG, "sh", "-c", "'mkdir -p files && echo retained > files/upgrade_check'")
assert "retained" == adb("shell", "run-as", PKG, "cat", "files/upgrade_check")
assert "Success" in adb("install", "-r", "app/build/outputs/apk/debug/app-debug.apk")
assert "retained" == adb("shell", "run-as", PKG, "cat", "files/upgrade_check")
adb("shell", "run-as", PKG, "rm", "files/upgrade_check")
print("In-place upgrade and app-data retention passed")
