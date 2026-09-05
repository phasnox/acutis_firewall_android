#!/usr/bin/env bash
# Shell smoke test for uninstall protection, run against a booted emulator.
#
# Asserts the two claims that actually matter and that no unit test can reach:
#   1. while the device admin is active, the package cannot be uninstalled
#   2. once it is deactivated, it can be  (the parent's escape hatch)
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

info "all shell assertions passed"
