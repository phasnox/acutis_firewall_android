#!/usr/bin/env bash
# TEMPORARY (#6) - records how force stop and clear storage behave over adb, with a
# plain device admin and as Device Owner. Prints RESULT lines and never fails the job.
# Remove before merging.
set -u

PKG=com.acutis.firewall
ADMIN="$PKG/com.acutis.firewall.admin.UninstallProtectionAdminReceiver"
APK=app/build/outputs/apk/debug/app-debug.apk

result() { echo "RESULT[$1]: $2"; }
oneline() { tr '\r\n' '  '; }
alive() { if adb shell pidof "$PKG" >/dev/null 2>&1; then echo yes; else echo no; fi; }
launch() { adb shell am start -W -n "$PKG/.MainActivity" >/dev/null 2>&1; sleep 3; }
admin_lines() { adb shell dumpsys device_policy | grep -c "$PKG"; }

adb install -r -t "$APK" >/dev/null 2>&1

# Plain device admin: what uninstall protection is.
adb shell dpm set-active-admin --user 0 "$ADMIN" >/dev/null 2>&1
result admin-dumpsys-lines "$(admin_lines)"
launch
result admin-running-before-force-stop "$(alive)"
adb shell am force-stop "$PKG"
sleep 2
result admin-running-after-force-stop "$(alive)"
launch
result admin-pm-clear "$(adb shell pm clear "$PKG" 2>&1 | oneline)"
result admin-dumpsys-lines-after-clear "$(admin_lines)"
adb shell dpm remove-active-admin --user 0 "$ADMIN" >/dev/null 2>&1
result admin-dumpsys-lines-after-remove "$(admin_lines)"

# Device Owner.
result do-set "$(adb shell dpm set-device-owner "$ADMIN" 2>&1 | oneline)"
launch
result do-running-before-force-stop "$(alive)"
adb shell am force-stop "$PKG"
sleep 2
result do-running-after-force-stop "$(alive)"
result do-force-stop-log "$(adb logcat -d | grep -m1 'force stop protected package' | oneline)"
result do-pm-clear "$(adb shell pm clear "$PKG" 2>&1 | oneline)"
adb shell dpm remove-active-admin --user 0 "$ADMIN" >/dev/null 2>&1

exit 0
