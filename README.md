# Onkyo Dual Zone
Unofficial local Android controller for TX-NR656.

## v0.4.1
Local rotating diagnostic logs: Connection > Diagnosebericht (or tap the status). Export TXT and attach to the development chat. No automatic upload. Raw XML, IP/MAC, SSID, credentials and media names omitted. Response correlation is observational, not a transaction acknowledgement. Clear from diagnostics menu. Two files of max 128 KiB each; automatic deletion when the oldest entry is 72 hours old, checked on startup, export and logging (once per minute). Logging stops and stored logs are removed below 16 MiB available space. No background task while app is closed; expiry is checked at next launch. Exported copies are not managed by the app.

Automated JVM protocol, TCP, discovery, capability and logging tests plus Android emulator smoke test. Hardware verification pending. Test volume cap 50. Persistent signing, scenes and widgets pending.

APK from GitHub Actions artifacts. Debug signing may require uninstalling earlier versions.
