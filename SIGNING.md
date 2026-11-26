# Permanent APK signing
The first release with ONKYO_KEYSTORE_B64 configured is the permanent-signing baseline. The previous hosted-runner debug keys were ephemeral and cannot be recovered from APK certificates. Existing installs may require a final uninstall before installing that baseline. Back up or note scene settings first; uninstall removes app data and widgets.

## One-time setup (Windows PowerShell, JDK and GitHub CLI installed)
Run on a trusted computer; keep the keystore backed up privately. Never commit it or paste it into chat. Use a new dedicated key for this app only.

```powershell
keytool -genkeypair -v -keystore onkyo-signing.jks -storetype JKS -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=Onkyo dahoam"
gh auth login
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path .\onkyo-signing.jks).Path)) | gh secret set ONKYO_KEYSTORE_B64 --repo klssan/Onkyo-zone-app
gh workflow run build.yml --repo klssan/Onkyo-zone-app
```

The Android debug signing configuration expects the alias and passwords above; secrecy is provided by the dedicated keystore bytes in GitHub's encrypted secret storage, not by the standard debug password. Restrict repository write access, keep the file offline/private and do not reuse it for other apps. Never regenerate the key once the first permanent release is published. Alternatively add the base64 content directly via Settings > Secrets and variables > Actions > New repository secret, without exposing it publicly.

Before the permanent baseline, CI may publish explicitly marked temporary debug builds. Once a permanent release exists, CI refuses to publish without this secret. After the baseline, CI compares the new APK's certificate and versionCode against the previous permanent release, then installs the old APK and updates it in the emulator without uninstalling, verifying an app-data marker survives. Keep package ID at.local.dualzone unchanged and increase versionCode and versionName for each release.
