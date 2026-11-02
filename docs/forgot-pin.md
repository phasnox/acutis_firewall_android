# Forgot your PIN?

The PIN cannot be recovered. It never leaves the phone, and the app keeps only a
scrambled form of it that cannot be turned back into the PIN, so nobody can look it up,
not even us. What you can do is remove it, and how depends on how the app was set up.

**Which setup do you have?** Open Android **Settings** and search for **device admin**.

- Acutis Firewall is not listed there, or is switched off: [PIN only](#pin-only).
- Acutis Firewall is switched on: [PIN and uninstall protection](#pin-and-uninstall-protection).
- You set the phone up with the `adb shell dpm set-device-owner` command: [Device Owner](#device-owner).

## PIN only

The PIN only guards the app's own settings, so Android lets you do either of these:

- **Uninstall the app** as you would any other, and install it again if you want to
  start over.
- **Clear its storage**: Settings → Apps → Acutis Firewall → Storage & cache → Clear
  storage. This removes the PIN together with everything else the app keeps, including
  your time rules and blocklists. Then open the app, set a new PIN, and turn the firewall
  back on.

## PIN and uninstall protection

Android refuses to uninstall the app, and greys out its Clear storage button, until
uninstall protection is removed. Without the PIN you cannot remove it from inside the
app, but Android always lets you remove it from its own Settings:

1. Open Android **Settings** and search for **device admin**. It is usually under
   Security → Device admin apps.
2. Tap **Acutis Firewall**, then **Deactivate this device admin app**, and confirm. If
   an "Ask a parent" PIN screen appears, it cannot stop this: tap Cancel, go back to
   Settings and confirm.
3. Acutis Firewall will say **Uninstall protection was removed**. That is expected: it
   is the alert a child would trigger, and it cannot tell the two of you apart.

Then either:

- **Uninstall the app** as usual. That also removes the alert.
- **Keep it, with a new PIN**: clear its storage (Settings → Apps → Acutis Firewall →
  Storage & cache → Clear storage), open the app, set a new PIN, and turn uninstall
  protection back on. The alert keeps appearing until protection is back on. Clearing
  storage cannot hide it, or a child could do the same.

## Device Owner

If you set the phone up with `adb shell dpm set-device-owner`, Android lets nothing but
the app itself remove it, and turning it off in the app needs the PIN. Without the PIN,
**the only way out is a factory reset from the phone's recovery mode, which erases
everything on the phone.** The app blocks the reset in Android Settings, which is why it
has to be done from recovery mode.

1. Back up what you can: photos, contacts and anything else that is not already synced.
2. Turn the phone off, then start it in recovery mode. The buttons depend on the phone,
   often Power and Volume Down held together; search for your phone model and
   "recovery mode".
3. Choose **Wipe data/factory reset**.

After the reset, Android may ask for the Google account that was on the phone before.
That is Android's Factory Reset Protection, not Acutis Firewall.

---

**Next time:** write the PIN down and keep it somewhere your child cannot get to.
