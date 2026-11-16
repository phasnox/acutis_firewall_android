---
layout: doc
title: How to Block Adult Content on Android (Free, No Root)
description: Three ways to block porn and adult sites on an Android phone, what each one misses, and how to set up a free on-device filter in about five minutes.
lang: en
ref: block-adult-content-android
date: 2026-11-16
---

# How to block adult content on Android

You can block adult sites on Android without root, in about five minutes, using either
Google Family Link, a private DNS resolver, or an on-device DNS filter such as Acutis
Firewall. Family Link is the least work if your child already has a Google account; an
on-device filter is the only one that works on a phone with no Google account and no
upstream provider seeing the queries.

| Method | Account needed | Covers every app | Who sees the DNS queries |
|---|---|---|---|
| Google Family Link | Yes, on both phones | Yes | Google |
| Private DNS resolver | No | Yes, until the browser switches to its own DoH | The resolver operator |
| Acutis Firewall | No | Yes, except traffic that bypasses system DNS | Nobody — answered on the phone |

## Method 1: Google Family Link

Family Link filters by controlling the child's Google account, not the phone's traffic.

1. Install **Google Family Link for parents** on your phone and **Family Link** on the
   child's, signed in as the child's Google account.
2. Follow the pairing flow until both apps show the same family.
3. In the parent app, open the child, then **Controls → Content restrictions → Google
   Search** and turn on **Filter explicit results**.
4. Still in **Controls**, open **Apps** and restrict installs to your approval. This
   stops a filtered browser being replaced with an unfiltered one.

What it misses: Family Link restricts Google properties and app installs. It does not
inspect DNS, so a website opened in a browser that Family Link does not manage — or an
app already installed from outside the Play Store — is not filtered.

## Method 2: Private DNS

Android 9 and later can send all DNS to an encrypted resolver you name. Several
resolvers offer a family endpoint that drops adult and malware domains before they
resolve:

- **Cloudflare 1.1.1.1 for Families**: Private DNS hostname `family.cloudflare-dns.com`
  (or the addresses `1.1.1.3` and `1.0.0.3`).
- **AdGuard Family**: Private DNS hostname `family.adguard-dns.com`.

To set it up: **Settings → Network & internet → Private DNS → Private DNS provider
hostname**, and enter the hostname. Android keeps it on for every network the phone
joins, including mobile data.

What it misses: the queries go to that provider, which can log them, and you cannot add
your own domains or set per-app rules. A browser configured with its own DNS over HTTPS
server (`chrome://settings/security → Use secure DNS`) can bypass Private DNS entirely.

## Method 3: An on-device DNS filter

Acutis Firewall starts a local VPN that answers DNS on the phone. Queries for a domain
on its blocklists never leave the device; everything else is forwarded to the resolver
your network already uses.

1. Install Acutis Firewall from
   [Google Play](https://play.google.com/store/apps/details?id=com.acutis.firewall) or
   [F-Droid](https://f-droid.org/packages/com.acutis.firewall/).
2. Open it and turn the firewall on. Android asks for permission to set up a VPN
   connection — that permission is what lets the app see DNS queries. Accept it.
3. Tap **Blocked Sites** and check that **Adult Content** is on. It is enabled by
   default; the counter under the switch shows how many domains the list holds.
4. Tap **Update Blocklists** so the list is current. It is fetched from public
   open-source lists, not from a server run by the developer.
5. Set a PIN in **Settings → Security → Enable PIN protection**, so the firewall cannot
   be switched off from the phone.

What it misses: the filter only sees DNS on port 53. Chrome's secure DNS and Android's
Private DNS both encrypt queries so they skip the tunnel — turn both off. Any other
active VPN also replaces Acutis Firewall, because Android allows one VPN at a time. And
a DNS filter blocks by domain name: a site that is new, or that serves adult content
from a shared domain, may resolve before the list catches up.

## Which one to pick

- **Child has a Google account and you want the least setup**: Family Link, plus
  Private DNS for the gaps.
- **No Google account on the phone**: Private DNS if you accept that the resolver
  operator sees the queries; Acutis Firewall if you do not.
- **You want per-domain rules, time limits and a PIN**: Acutis Firewall, which is free
  and open source, and does not need an account.

Running Family Link and Acutis Firewall together is fine — they do different jobs — but
do not run two VPN-based filters at once.

## FAQ

**Does blocking adult content need root?** No. DNS filtering uses Android's standard
`VpnService` API, which any app can request without root.

**Will this block adult content inside apps, not just browsers?** Yes, for any app that
resolves domains through Android's DNS. Apps with their own hardcoded DNS servers or
their own encrypted DNS are the exception.

**Can my child turn it off?** With Acutis Firewall, only with the PIN: uninstall
protection blocks uninstall, force stop and clearing storage. A determined user can
still deactivate the device admin in Settings, and the app alerts you when that happens.
See [uninstall protection](uninstall-protection.html).

**Is it free?** Acutis Firewall is MIT-licensed and free, with no account, ads or
subscription. Family Link is free but needs a Google account. Cloudflare and AdGuard
family resolvers are free.

## Install it

- [Acutis Firewall on Google Play](https://play.google.com/store/apps/details?id=com.acutis.firewall)
- [Acutis Firewall on F-Droid](https://f-droid.org/packages/com.acutis.firewall/)
