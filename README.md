# Onkyo Dual Zone
Unofficial local Android controller for TX-NR656.

## v0.5
Native home-screen scene widgets, one independent configuration per widget. Set power on/off/unchanged, source, volume (0–50), optional FM/AM preset (1–40) or NET service for each zone. Explicit opt-in for shared source effects. User-initiated short foreground service performs fresh LAN discovery, verifies replies, shows success/partial failure/error on widget, never repeats a write automatically. Demo option sends no network commands. Add from Connection > Homescreen-Widgets or Android launcher; configure by tapping unconfigured widget or gear. Existing FM4 preset number must be supplied; no hardcoded frequency. Live device validation still pending. Manual IP scenes require an NRI model match. NET service selection experimental. No native TV power control. Logs remain local and capped.
## v0.4.2
A compact TV button opens the installed Google TV app via its public launcher, with a missing-app dialog. No separate TV volume control or new pairing; direct launch of Google's internal remote screen is not a supported API. The remote is also available via Android Quick Settings.

## v0.4.1
Local rotating diagnostic logs: Connection > Diagnosebericht (or tap the status). Export TXT and attach to the development chat. No automatic upload. Raw XML, IP/MAC, SSID, credentials and media names omitted. Response correlation is observational, not a transaction acknowledgement. Clear from diagnostics menu. Two files of max 128 KiB each; automatic deletion when the oldest entry is 72 hours old, checked on startup, export and logging (once per minute). Logging stops and stored logs are removed below 16 MiB available space. No background task while app is closed; expiry is checked at next launch. Exported copies are not managed by the app.

Automated JVM protocol, TCP, discovery, capability and logging tests plus Android emulator smoke test. Hardware verification pending. Test volume cap 50. Persistent signing, scenes and widgets pending.

APK from GitHub Actions artifacts. Debug signing may require uninstalling earlier versions.
