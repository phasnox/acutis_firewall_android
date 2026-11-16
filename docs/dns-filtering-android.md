---
layout: doc
title: "DNS Filtering on Android: Parental Control Without Root"
description: How DNS filtering blocks adult and dangerous sites in every app, how Private DNS providers differ from a local filter, and which one to use on a child's phone.
lang: en
ref: dns-filtering-android
date: 2026-11-16
---

# DNS filtering for parental control on Android

DNS filtering blocks a site by refusing to translate its name into an IP address, which
means it works in every app on the phone, not only in the browser. It can be done on the
device itself with Android's `VpnService` API — no root, no account, no upstream provider
seeing the queries. The alternative is to point Android's Private DNS at a resolver that
filters for you, which is less work to set up and hands your query log to that operator.

## What DNS filtering is

Every connection starts with a DNS query: "what is the IP address of example.com?" The
answer is what the app then connects to. If the resolver answers "no such domain"
(NXDOMAIN) for a site on a blocklist, the app has nowhere to connect and the request
fails before any content is fetched.

Because DNS is asked before every connection — by the browser, by an app's embedded
webview, by an ad SDK — filtering at that layer covers all of them at once. It is also
cheap: one query per domain, not a per-packet inspection.

## Why it covers every app

A browser extension filters one browser. An accessibility service can read what is on
screen but needs the Accessibility permission, which Play policy restricts and parents
should distrust in any app. A VPN-based DNS filter sits where the operating system asks
for name resolution, so every app on the phone inherits it.

That is why this approach is the standard non-root option on Android. The cost is that
filtering is domain-level: it cannot tell one page of a site from another, and it cannot
inspect video already streaming from a domain that was not yet blocked.

## Local filter vs. public filtering resolver

| | Acutis Firewall (on-device) | Cloudflare 1.1.1.1 for Families | AdGuard DNS Family | NextDNS |
|---|---|---|---|---|
| Setup | Install app, toggle on | Set Private DNS hostname | Set Private DNS hostname | Create profile, set Private DNS hostname |
| Blocklist lives | On the phone | At the resolver | At the resolver | At the resolver |
| Who sees each query | Nobody (answered locally) | Cloudflare | AdGuard | NextDNS |
| Custom domains and wildcards | Yes | No | No | Yes, in the dashboard |
| Daily time limits and schedules | Yes | No | No | No |
| Uninstall protection | Yes, with the PIN | n/a | n/a | n/a |
| Price | Free (MIT) | Free | Free tier | Free tier with a query cap |

Cloudflare's family resolver is `1.1.1.2` and `1.0.0.2` for malware, and `1.1.1.3`,
`1.0.0.3` for malware plus adult content; its Private DNS hostnames are
`security.cloudflare-dns.com` and `family.cloudflare-dns.com`. AdGuard's family endpoint
is `family.adguard-dns.com` (addresses `94.140.14.15` and `94.140.15.16`). Both operators
publish what they log; neither filters per app, and neither can be edited from the phone.

The practical difference is who holds the query log. A network resolver sees every domain
the phone looks up, including ones that are not blocked. A local filter answers on the
device and forwards only what it did not block, to the resolver your network already
uses — so the router or carrier sees no more than before the app was installed.

## Limits you should know before trusting it

- **Encrypted DNS bypasses the filter.** Chrome's "Use secure DNS" and Android's Private
  DNS both send queries over HTTPS or TLS, outside port 53, where a local filter cannot
  see them. Turn both off on a child's phone. On a network resolver this is not an issue
  — the resolver is the one being asked.
- **One VPN at a time.** A local DNS filter is a VPN service. Any other VPN app replaces
  it, and Android shows only one active VPN.
- **Blocking is by name.** A newly registered site, or adult content hosted on a shared
  domain that also serves legitimate pages, may not be blocked until a list catches up.
- **The app sees a network error, not a block page.** Blocked domains answer NXDOMAIN,
  so the browser shows its own "site can't be reached" screen. There is no custom page and
  no per-site message.
- **Traffic that never asks DNS.** Apps that hardcode an IP address, or use their own
  encrypted resolver, are outside DNS filtering entirely.

## How to check that it works

1. With the filter on, open a domain from the blocklist. It should fail to load.
2. Turn the filter off and reload. It should load — proving the filter, not the network,
   was doing the blocking.
3. In Acutis Firewall, the blocked counter on the home screen climbs as sites are hit;
   if it stays still while you browse to blocked sites, the VPN is not intercepting
   (another VPN is active, or Private DNS is on).

## FAQ

**Does DNS filtering need root?** No. It uses Android's `VpnService`, the same API a VPN
app uses, and Android 10 or later.

**Does it slow down the phone?** A local filter only handles DNS, which is a small
fraction of traffic. Acutis Firewall routes only the DNS addresses through the tunnel, so
downloads and video do not pass through the app at all.

**Is DNS filtering the same as a firewall?** It is one kind of it. A packet filter can
block ports and addresses too; a DNS filter only controls name resolution, which is what
covers content filtering and time limits.

**Can it block ads?** Yes, if the ad domains are on a list. It will not remove ads served
from the same domain as the content.

**Which is better for a child's phone?** A local filter if you want per-domain rules,
time limits and no query log outside the phone. A filtering resolver if you want setup in
one field and are comfortable with the provider seeing the queries.

## Install it

- [Acutis Firewall on Google Play](https://play.google.com/store/apps/details?id=com.acutis.firewall)
- [Acutis Firewall on F-Droid](https://f-droid.org/packages/com.acutis.firewall/)
