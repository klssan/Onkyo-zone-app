# Onkyo Dual Zone
Unofficial local Android controller for the Onkyo TX-NR656.

## v0.3
- Query NRI receiver information on connection, retry if unavailable, refresh periodically. Filter source and NET service menus by receiver-reported zone masks and enabled flags.
- Main-zone tone and level rows follow reported controls and actual command replies; Zone 2 has no tone section. Direct / Pure Audio disables bass and treble. Listening modes remain a conservative static subset.
- Confirm changes to shared NET service. AM added. Demo includes a representative TX-NR656 capability profile, not live device data.
- NRI fallback: conservative static menus and confirmed state. Volume limit 50 unchanged. NRI does not guarantee a streaming service still works.
- Existing automatic eISCP UDP discovery and reconnect, manual IP fallback and zone details.
- Real hardware validation, persistent APK signing, scenes, widgets and richer media browsing still pending.

Reference: TX-NR656 sample receiver XML at https://github.com/mkulesh/onpc/blob/master/doc/ISCP/ReceiverInformation/TX-NR656.xml (schema reference; implementation independent).

APK from GitHub Actions artifacts. Debug signing may require uninstalling previous builds.
