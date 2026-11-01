# Uninstall protection

Stops a child removing Acutis Firewall without the parent PIN.

## What it actually does

Android gives an app **no uninstall callback** — there is no hook that lets an app
show a PIN prompt when someone taps Uninstall. What Acutis Firewall does instead is
register as a **device administrator**. While an admin is active the platform itself
refuses to uninstall the package, which reroutes anyone trying to remove it through
Settings → Security → Device admin apps → Deactivate — a screen the app *can* react
to, and where it asks for the PIN.

Turn it on in **Settings → Security → Uninstall protection**. You need a PIN set
first, since the PIN is what unlocks it again.

## What it stops, and what it does not

| Route | Result |
|---|---|
| Long-press icon → Uninstall | Blocked by the OS |
| Settings → Apps → Uninstall | Greyed out |
| Play Store → Uninstall | Blocked |
| `adb uninstall` | Fails with `DELETE_FAILED_DEVICE_POLICY_MANAGER` |
| Notification → "Disable Firewall" | Asks for the PIN |
| Deactivate the device admin | Warning shown; PIN prompt is best-effort and often does not appear. Tamper alert either way |
| Settings → Apps → Clear storage | **Not blocked.** Detected on next launch |
| Safe Mode → deactivate admin | **Not blocked** — the app does not run in Safe Mode |

Two things are worth being clear about:

- **The PIN prompt on the deactivate screen is best-effort, and on stock Android it
  probably never appears.** Two platform defences stop it, both deliberate: Settings
  holds an `OP_SYSTEM_ALERT_WINDOW` restriction while that screen is up ("don't let
  anyone overlay stuff on top of the screen"), and calls `stopAppSwitches()` before
  asking us for a warning ("Don't allow the admin to put a dialog up in front of us
  while we interact with the user"). Device logs confirm our app is only allowed to
  start an activity while it has a visible window, which it does not have here.

  So do not rely on the prompt. What you *can* rely on is that **uninstall is blocked**
  and that **you are told when protection is removed** - both verified on Android 10,
  13 and 15.
- **Clearing app storage wipes the PIN** along with every other setting. The admin
  registration survives that, so the app treats "admin active but no PIN" as proof
  the data was wiped, alerts you, and — deliberately — lets protection be switched
  off without a PIN in that state, so you are never locked out of your own device.

If you want something genuinely unremovable, use Device Owner mode below.

## Turning it off

Settings → Security → Uninstall protection → off → enter the PIN. The admin is
removed and the app can be uninstalled normally.

If the app is unavailable, Settings → Security → Device admin apps → Acutis Firewall
→ Deactivate always works for a plain device admin. This is guaranteed by Android:
a non-device-owner admin can always be deactivated by the user.

> `adb shell dpm remove-active-admin` is **not** a reliable escape hatch. It calls
> `forceRemoveActiveAdmin`, which throws `SecurityException: Attempt to remove
> non-test admin` on production builds. It only works for Android Studio deploys
> (which mark the app `testOnly`) and userdebug builds.

## Device Owner mode (advanced, genuinely unremovable)

This is the only configuration that cannot be undone from Settings. It blocks
uninstall outright, plus Safe Mode and factory reset.

**Read this first: undoing it may require a factory reset.** `dpm` cannot remove a
device owner. The only ways out are the in-app "Uninstall protection → off" path
(which calls `clearDeviceOwnerApp`) or wiping the device. Set this up only on a
phone you are willing to reset.

It requires a freshly factory-reset device with **no accounts added**:

```bash
adb shell dpm set-device-owner com.acutis.firewall/.admin.UninstallProtectionAdminReceiver
```

Once provisioned, the app applies on its own:

- `setUninstallBlocked` — the package cannot be uninstalled at all
- `DISALLOW_SAFE_BOOT` — closes the Safe Mode bypass
- `DISALLOW_FACTORY_RESET`

It deliberately does **not** apply `DISALLOW_UNINSTALL_APPS` (which would block
uninstalling every app on the device) or `DISALLOW_CONFIG_VPN` (which would lock you
out of VPN settings too).

## Store policy

Google Play's Device and Network Abuse policy bars apps that prevent uninstall
"unless authorized by a parent or guardian through a parental control app" — which
is this use case. The feature is opt-in, disclosed, and reversible with the PIN, and
the app deliberately uses **no AccessibilityService**, which is the usual cause of
parental-control app rejections.
