#!/usr/bin/env bash
# Shell smoke test for uninstall protection, run against a booted emulator.
#
# Asserts the claims that actually matter and that no unit test can reach:
#   1. while the device admin is active, the package cannot be uninstalled
#   2. once it is deactivated, it can be  (the parent's escape hatch)
#   3. the tamper record outlives Clear storage, so deactivating protection and then
#      clearing storage cannot wipe the tamper alert
set -uo pipefail

PKG=com.acutis.firewall
ADMIN="$PKG/com.acutis.firewall.admin.UninstallProtectionAdminReceiver"
APK=app/build/outputs/apk/debug/app-debug.apk

fail() { echo "FAIL: $*" >&2; exit 1; }
info() { echo "--- $*"; }

info "installing $APK"
# -t because the emulator build is marked testOnly so that `dpm` can force-remove
# the admin during cleanup.
adb install -r -t "$APK" || fail "install failed"

info "activating device admin"
adb shell dpm set-active-admin --user 0 "$ADMIN" || fail "could not activate admin"
adb shell dumpsys device_policy | grep -q "com.acutis.firewall" \
  || fail "admin not registered in device_policy"

info "uninstall MUST be refused while the admin is active"
# adb prints "Failure [...]" and may still exit 0, so assert on the output.
OUT=$(adb uninstall "$PKG" 2>&1)
echo "$OUT"
case "$OUT" in
  *DELETE_FAILED_DEVICE_POLICY_MANAGER*)
    info "PASS: blocked with DELETE_FAILED_DEVICE_POLICY_MANAGER" ;;
  *Success*)
    fail "package was uninstalled while the device admin was active" ;;
  *)
    fail "unexpected uninstall result: $OUT" ;;
esac

adb shell pm list packages | grep -q "package:$PKG" \
  || fail "package disappeared despite the block"

info "escape hatch: deactivate, then uninstall MUST succeed"
adb shell dpm remove-active-admin --user 0 "$ADMIN" || fail "could not deactivate admin"

OUT=$(adb uninstall "$PKG" 2>&1)
echo "$OUT"
case "$OUT" in
  *Success*) info "PASS: uninstall works once protection is off" ;;
  *)         fail "parent escape hatch is broken: $OUT" ;;
esac

info "tamper record MUST survive Clear storage"
# Removing the admin outside the app sets this component's enabled state (the
# instrumented tests cover that). It is the copy of the tamper flag that lives outside
# app data; if Clear storage reset it, a child could deactivate and then wipe the alert.
RECORD_CLASS=com.acutis.firewall.admin.TamperRecord
# Captured before matching: grep -q on a pipe can SIGPIPE adb, which pipefail reports.
record_set() {
  local dump
  dump=$(adb shell dumpsys package "$PKG") || return 2
  [[ "$dump" == *"$RECORD_CLASS"* ]]
}
component_state() { adb shell dumpsys package "$PKG" | grep -i -A3 'components:' || true; }

adb install -r -t "$APK" || fail "reinstall failed"
record_set && { component_state; fail "tamper record listed on a fresh install, so this check cannot tell"; }
adb shell pm enable "$PKG/$RECORD_CLASS" || fail "could not set the tamper record"
record_set || { component_state; fail "dumpsys does not list the tamper record once set"; }
OUT=$(adb shell pm clear "$PKG" 2>&1)
echo "$OUT"
case "$OUT" in
  *Success*) ;;
  *) fail "clear storage failed: $OUT" ;;
esac
record_set || { component_state; fail "Clear storage reset the tamper record; the tamper alert can be wiped"; }
info "PASS: tamper record survives Clear storage"
adb uninstall "$PKG" >/dev/null 2>&1 || fail "cleanup uninstall failed"

info "all shell assertions passed"
