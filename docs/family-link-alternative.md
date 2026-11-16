---
layout: doc
title: "Family Link Alternative for Android (Free, No Account)"
description: Family Link needs a Google account on your child's phone and cannot filter inside every app. What a free on-device alternative does better, and what it cannot do.
lang: en
ref: family-link-alternative
date: 2026-11-16
---

# A free Google Family Link alternative for Android

If you want parental control on an Android phone without a Google account on it, Family
Link is not an option: it manages a Google account, so the child needs one and the parent
needs another. A free, on-device DNS filter is the alternative — it does content
filtering, time limits and uninstall protection from the phone itself, with no account on
either side.

## What Family Link does well

Family Link is free, made by Google, and covers ground an on-device filter cannot:

- **App management.** Approve or block installs from the Play Store, and block apps
  already installed — including system apps such as YouTube.
- **Screen time.** Daily limits and a bedtime that locks the whole device, not one domain.
- **Location.** The child's device location in the parent app.
- **Account control.** Filtering follows the Google account, so a signed-in Chrome on any
  device behaves the same way.
- **Remote administration.** Everything is changed from the parent's phone.

Google documents all of it at [families.google/familylink](https://families.google/familylink/).

## Where Family Link does not fit

- **The phone has no Google account.** A child's first phone, a hand-me-down, a
  de-Googled ROM, or a device where you deliberately kept the account off. Family Link
  cannot manage what is not there.
- **You do not want a Google account for a child.** Creating one for a minor means
  Google holds the identity, the location history and the app list.
- **You want domain-level rules.** Family Link blocks apps and Google search results. It
  has no per-domain blocklist, no wildcards, no "allow this site only between 18:00 and
  20:00".
- **You want the filter to keep working offline.** A local filter answers on the phone;
  Family Link's restrictions are checked against Google's services.

## What an on-device filter does instead

Acutis Firewall runs a local VPN that filters DNS, and it is free and MIT-licensed:

- **Blocklists** for adult content, malware and phishing, gambling, and social media,
  plus a custom list that takes exact domains and `*.` wildcards.
- **Time rules** per domain: a schedule (block YouTube from 20:00 to 07:00) or a daily
  limit (30 minutes of YouTube).
- **A PIN** that guards settings, and optional uninstall protection that blocks uninstall,
  force stop and clearing storage. Android still allows a device admin to be deactivated
  in Settings, and the app alerts you when that happens.
- **No account, no server, no analytics.** Queries are answered on the phone; blocklists
  are downloaded from public open-source lists.

## Side by side

| | Family Link | Acutis Firewall |
|---|---|---|
| Google account on the child's phone | Required | Not needed |
| Blocks adult sites in every app | Partly, via Google services | Yes, at the DNS layer |
| Blocks a specific app | Yes | Only by blocking its domains |
| Whole-device screen time | Yes | No — per-domain only |
| Per-domain rules and wildcards | No | Yes |
| Location tracking | Yes | No |
| Remote management | Yes | No — configured on the phone |
| Who sees the queries | Google | Nobody |
| Price | Free | Free (MIT) |

The two are complementary: Family Link for app installs and whole-device screen time, a
DNS filter for per-domain control and for the case where there is no account at all.

## Can I run both?

Yes, and they do not conflict — but only one VPN can be active at a time. Acutis Firewall
is a VPN service, so if the child installs another VPN app, filtering stops until it is
removed. Family Link does not use a VPN, so it keeps working alongside.

Two practical notes: add Acutis Firewall to Family Link's approved apps so it cannot be
blocked by accident, and set a PIN before handing the phone over.

## FAQ

**Is this really free?** Yes. Acutis Firewall is MIT-licensed, with no ads, no
subscription and no in-app purchases. Donations are optional.

**Do I need to root the phone?** No. Filtering uses Android's `VpnService` API.

**Can it replace Family Link completely?** Not for whole-device screen time or app
approval, and it has no location features. It replaces the content filtering and adds
per-domain rules.

**Does it work on a phone with no Google account?** Yes. That is the case it was built
for: nothing in the app needs an account or a Google service.

**What does the child see?** A network error for a blocked domain, and a notification
while the firewall is running. The app has no visible block page.

## Install it

- [Acutis Firewall on Google Play](https://play.google.com/store/apps/details?id=com.acutis.firewall)
- [Acutis Firewall on F-Droid](https://f-droid.org/packages/com.acutis.firewall/)
