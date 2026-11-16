---
layout: doc
title: How to Block YouTube on Android (App and Browser)
description: Block the YouTube app and youtube.com on Android, or allow them only at set times. No root, no Google account; what each method can and cannot stop.
lang: en
ref: block-youtube-android
date: 2026-11-16
---

# How to block YouTube on Android

You can block YouTube on Android by filtering `youtube.com` and its subdomains with a
DNS filter, by removing the app, or by allowing it only during set hours. The domain
route needs no root and no Google account, and it covers the app, Chrome and every other
browser on the phone at the same time.

YouTube is a single service on several hostnames. Blocking one is not enough:

| Hostname | What it carries |
|---|---|
| `youtube.com` | The site itself |
| `www.youtube.com`, `m.youtube.com` | Desktop and mobile web |
| `youtube.googleapis.com` | API calls the app makes |
| `googlevideo.com` | The video streams |
| `ytimg.com` | Thumbnails and player assets |

Blocking `youtube.com` and `*.youtube.com` stops the app's page loads and the site, but a
video already cached in the app can still play, and thumbnails may still load.

## Block it with a custom blocklist

Acutis Firewall has a custom blocklist that accepts plain domains and `*.` wildcards.

1. Open the app → **Blocked Sites → Custom Blocklist**.
2. Add these lines:

   ```
   youtube.com
   *.youtube.com
   googlevideo.com
   *.googlevideo.com
   ytimg.com
   *.ytimg.com
   ```

3. Turn the firewall on. Opening YouTube now fails with "site can't be reached" or
   "no connection", because the domain does not resolve.
4. Set a PIN in **Settings → Security** so the list cannot be edited or the firewall
   switched off.

The wildcard form `*.youtube.com` matches the domain and any subdomain; the bare
`youtube.com` line is redundant but harmless. Both are listed here so the list reads
the way it does.

## Allow it only at set times

Blocking YouTube outright is rarely what a parent wants. A time rule is usually better.

1. **Time Rules → +** and choose **Domain**.
2. Enter `youtube.com` (rules match the domain and its subdomains).
3. Choose **BLOCK**, then **Schedule** for a bedtime window such as `20:00`–`07:00`, or
   **Daily Limit** for 30 minutes a day.
4. Pick the days and save.

A schedule rule blocks the domain inside the window and lets it through outside; a daily
limit blocks it after the child has used it for that many minutes that day. Usage is
counted from DNS queries, so it measures time spent with the domain resolving, not
seconds of video watched.

## Remove the app entirely

If the phone has no Google account, uninstall the YouTube app
(**Settings → Apps → YouTube → Uninstall**). On many phones it is a system app that can
only be disabled, not removed — **Disable** in the same screen is enough; the icon
disappears and the app cannot run.

If the phone does have a Google account, Family Link can block the app:
**Family Link → your child → Controls → Apps → YouTube → Block**. This also stops
reinstalls from the Play Store. It needs a Google account on the child's phone, which
some parents specifically do not want.

## What still gets through

- **A different DNS resolver.** If the child sets **Settings → Network → Private DNS**
  to a hostname, or turns on Chrome's secure DNS (`chrome://settings/security`), those
  queries are encrypted and skip the filter. Turn both off, and note that they can be
  turned back on without the PIN on most phones.
- **A different VPN.** Android allows one VPN at a time. Another VPN app takes over from
  Acutis Firewall and filtering stops.
- **Mobile data with a proxy, or a hotspot from another phone.** DNS filtering is
  applied on this phone only.
- **YouTube content inside other apps** — an embedded player in a news app still loads
  from `youtube.com` and is filtered, but a video already buffered before the rule
  applied will finish playing.

## FAQ

**Does blocking youtube.com also block YouTube Music?** It blocks `music.youtube.com`,
because it is a subdomain. The separate YouTube Music app uses the same hosts and stops
working too.

**Can I block only YouTube and not the rest of Google?** Yes. The custom blocklist takes
exact domains; nothing else is affected.

**Does this need root?** No. It is DNS filtering through Android's `VpnService`.

**Will the child see a warning page?** No — the domain fails to resolve, so the browser
shows its own network error. There is no custom block page.

**Can I whitelist one channel?** No. Filtering works on domains, not paths or channels.

## Install it

- [Acutis Firewall on Google Play](https://play.google.com/store/apps/details?id=com.acutis.firewall)
- [Acutis Firewall on F-Droid](https://f-droid.org/packages/com.acutis.firewall/)
