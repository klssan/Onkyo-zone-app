# Permanent signing and release channels

Create one dedicated keystore on a trusted Windows PC with a JDK. Keep a private offline backup; never regenerate or share it after the first permanent release.

```powershell
keytool -genkeypair -v -keystore onkyo-signing.jks -storetype JKS -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=Onkyo dahoam"
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path .\onkyo-signing.jks).Path)) | Set-Clipboard
```

Add the clipboard content as repository Actions secret ONKYO_KEYSTORE_B64 at https://github.com/klssan/Onkyo-zone-app/settings/secrets/actions. Clear the clipboard afterward. The conventional debug alias/passwords are required by the Android debug build; the key bytes are the secret. Never put the key into a commit or chat.

On the active feature/remote-wireguard branch, .github/workflows/remote-preview.yml now requires this secret before publishing. Push = beta; dispatch with channel rc = full emulator tests; dispatch with channel stable and exact RC tag = promote identical verified APK after manual hardware acceptance. Beta verifies JVM, compilation, package ID and signature. RC additionally tests UI, widgets and upgrade from the previous permanent release (if any). Stable copies exactly the RC bytes; no rebuild. All new beta/RC versions have monotonically increasing versionCode (1000 + workflow run number); Android package at.local.dualzone remains fixed. No permanent release is allowed with a changed signing certificate. A first installation over a temporary debug build may require uninstall once, which deletes app data, imported VPN settings and widgets. Re-import the WireGuard file afterward. Existing stable main branch is legacy; do not publish from it while this channel is active. When merging, unify the workflows rather than resetting versionCode.

With GitHub CLI installed and authenticated, use:
```powershell
gh workflow run remote-preview.yml --repo klssan/Onkyo-zone-app --ref feature/remote-wireguard -f channel=beta
gh workflow run remote-preview.yml --repo klssan/Onkyo-zone-app --ref feature/remote-wireguard -f channel=rc
```
