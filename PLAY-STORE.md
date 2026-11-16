# Publishing to Google Play

Everything in this file is what still has to happen outside the repository. The code
side is done: the app targets API 36, declares the `specialUse` foreground-service
subtype Play requires, and the release build signs with the upload key when its
credentials are provided.

## 1. Version and artifacts

| Item | Value |
|------|-------|
| Package | `com.acutis.firewall` |
| Version name | `1.0.12` |
| Version code | `10012` |
| Min SDK | 29 (Android 10) |
| Target SDK | 36 (Android 16) — required for new apps since 31 Aug 2026 |
| Artifact | `app/build/outputs/bundle/release/app-release.aab`; CI publishes the signed `v1.0.12` assets on the GitHub release and uploads the AAB to the internal testing track. Copy the AAB to `play-store/acutis-firewall-1.0.12-signed.aab` if keeping a local copy. |

Play accepts only the AAB, never an APK. Never reuse a version code: build `10013`
for the next upload even if only the listing text changes.

Live tracks (checked 16 Nov 2026): production serves `10009 (1.0.9)`; `10011 (1.0.11)`
went to internal testing on 16 Nov and is superseded by `1.0.12`, which fixes a
regression found on a clean install — see section 7.

## 2. Release automation

Pushing a `v*` tag runs `.github/workflows/release.yml`:

1. `test` — `./gradlew testDebugUnitTest`.
2. `build` — rewrites `versionCode`/`versionName` from the tag, signs with the
   `KEYSTORE_*` secrets, and uploads the APK/AAB as workflow artifacts.
3. `release` — creates the GitHub release with both files attached.
4. `play` — uploads the AAB to the **internal testing** track with release notes
   from `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`, using the
   `PLAY_CONFIG_JSON` secret (a Play Console service account with *Release to
   testing tracks* on this app). The step is skipped when the secret is absent.

Promotion to production stays manual: use *Promote release* on the internal track
in the Console, or the Publisher API. Nothing in CI can publish to production.

The service account is `play-publisher@black-seer-492520-s6.iam.gserviceaccount.com`.
It already had access to the ChampSheep app; the Acutis app was added on 16 Nov 2026
with *Release apps to testing tracks*, *Manage testing tracks*, *Manage store
presence* and *Manage policy declarations*. To rotate the key: create a new JSON key
in the Google Cloud project, `gh secret set PLAY_CONFIG_JSON -R
phasnox/acutis_firewall_android < key.json`, then delete the old key.

## 3. Signing

Release signing is read from `keystore.properties` in the repository root, or from
`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` in the
environment (that is what CI uses). `keystore.properties` is gitignored.

```properties
storeFile=/absolute/path/to/upload-key.jks
storePassword=…
keyAlias=upload
keyPassword=…
```

If the app does not exist in Play Console yet, create the upload key, enrol in Play
App Signing when creating the app, and keep the key backed up. The repository also
contains `acutis-firewall.jks`; if that is the key F-Droid releases are signed with,
a separate upload key is preferable so losing it does not break F-Droid's
`AllowedAPKSigningKeys`.

Build and verify locally:

```bash
./gradlew clean bundleRelease
jarsigner -verify app/build/outputs/bundle/release/app-release.aab
bundletool validate --bundle app/build/outputs/bundle/release/app-release.aab
```

`apksigner` only checks APKs; AABs are JAR-signed, so use `jarsigner` (or
`keytool -printcert -jarfile`) and check the certificate fingerprint matches your
upload key.

The `v1.0.11` tag was built and signed by CI (same certificate as `v1.0.10`).
Repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`.

## 3. Store listing assets

All files live in `fastlane/metadata/android/en-US/`, which is also the path
`fastlane supply` reads.

| Asset | File | Requirement |
|-------|------|-------------|
| Title | `title.txt` | ≤ 30 characters |
| Short description | `short_description.txt` | ≤ 80 characters |
| Full description | `full_description.txt` | ≤ 4000 characters, HTML subset |
| App icon | `images/icon.png` | 512 × 512, 32-bit PNG, ≤ 1 MB |
| Feature graphic | `images/featureGraphic.png` | 1024 × 500, JPEG or 24-bit PNG, no alpha |
| Phone screenshots | `images/phoneScreenshots/1..4.jpg` | 1080 × 1920 (9:16), ≥ 2 required |
| Changelog | `changelogs/10011.txt` | Shown as "What's new" |

The previous screenshots were 686 px wide and 2.22:1 tall, which the Console
rejects (maximum dimension may not exceed twice the minimum). The replacements were
captured on a Pixel 7 at 1080 × 1920 with full status-bar indicators and no
notifications.

Alt text for the Console:

- Icon: “Acutis Firewall shield crest on a white background”
- Feature graphic: “Acutis Firewall crest beside the text: Blocks adult and
  dangerous content entirely on your device”
- Screenshot 1: “Home screen with the shield active: Firewall is enabled, 131,184
  sites blocked”
- Screenshot 2: “Default blocklists with adult content, malware, gambling and
  social media categories enabled”
- Screenshot 3: “Time rules: a 30 minute daily limit for social media and a 60
  minute block for youtube.com”
- Screenshot 4: “Settings showing version 1.0.9 and the Bitcoin donation
  section”

## 4. App content declarations

Play will not let the app go to production until every section under **App content**
is complete.

- **Privacy policy**: `https://acutis.castillo.sh/privacy-policy.html`
- **App access**: no login, no account, no restricted areas. All features are
  available after install; Android's VPN consent and the optional device-admin
  prompt are system dialogs, not app logins.
- **Ads**: no ads.
- **Content rating questionnaire**: category Tools or Parenting. Answer "no" to
  every content question — the app blocks adult content but does not contain any.
  Do not select "Designed for Families".
- **Target audience**: 18+ (parents and guardians). The app is a parental-control
  tool, not child-directed; selecting children would pull it into the Families
  policy.
- **Data safety**: no data collected, no data shared. There is no analytics, no ad
  SDK, no account and no developer server. Blocklists are downloaded from GitHub;
  DNS queries are answered on-device and forwarded to the device's own resolver.
  All preferences, PIN, rules and the blocklist database stay in app-private
  storage and are deleted on uninstall.
- **Foreground service**: declare `specialUse` with the text below. Play requires a
  short video showing the user starting the service.
- **Device admin**: optional uninstall protection; explain it as below if the
  Console asks.

### Foreground service declaration (`specialUse`)

Use case / subtype (must match the manifest):

> Local DNS filtering firewall that keeps running while parental-control protection is enabled

Description of app functionality:

> The app is a parental-control DNS firewall. The parent taps the shield on the home
> screen; Android shows its VPN consent dialog; the app then runs a foreground
> service that establishes a local VPN, answers DNS queries on-device against the
> enabled blocklists, and forwards allowed queries to the device's resolver. The
> service has to keep running while the parent uses other apps, otherwise filtering
> silently stops.

User impact if the task is deferred or interrupted:

> If the system defers the start, no domains are blocked and any app can reach
> adult, gambling or malware sites until the service starts. If the system stops
> the service, filtering stops while the UI still says protection is on, which is
> exactly the state a child could exploit.

Demo video: `docs/fgs-demo.mp4` is published at
<https://acutis.castillo.sh/fgs-demo.mp4> and shows the shield tap, the VPN consent
dialog, the transition to "Firewall is enabled" and the ongoing notification. Use
that URL in the declaration, or upload the same file unlisted to YouTube if you
prefer a YouTube link.

### Device admin explanation

> Uninstall protection is off by default. When a parent enables it in Settings,
> Android shows its device-admin consent screen; the app requests no device
> policies (`<uses-policies />` is empty) and only uses the active-admin state to
> prevent its own uninstall, so a child cannot remove the filter. It is reversible:
> the parent can turn it off in the app with the PIN, or deactivate it in system
> Settings, which raises an in-app tamper alert. The app never uses
> AccessibilityService.

### VPN explanation

> The VPN is a local DNS filter, not a proxy. No traffic is routed to a server
> operated by the developer. Android's VpnService is the only way to filter DNS for
> all apps on an unrooted device.

## 5. Upload and rollout

1. Complete every **App content** section and the store listing.
2. Create an **Internal testing** release, upload the AAB, add your account as a
   tester and install it from Play. Test the paths below on that build; the debug
   build is not a substitute.
3. Promote to **Closed testing** if you want a wider check, then to **Production**
   with a staged rollout (10% → 50% → 100%).
4. Watch **Android vitals** and the pre-launch report for crashes and ANRs.

Test on the Play build before promoting (run 16 Nov 2026 on a clean Android 15
emulator, release build signed with a throwaway key):

- [x] First run: blocklist download (120,828 sites), shield tap, VPN consent, "Firewall is enabled"
- [x] DNS filtering blocks a blocklisted domain (`example.org` added to a custom list → NXDOMAIN) and allows a normal domain (`wikipedia.org` resolves) — only after the 1.0.12 fix, see section 7
- [ ] Time rule applies (daily limit and schedule) — screen loads with a rule present; the Social Media category ships empty, so nothing was actually blocked
- [x] PIN gates disabling the firewall: tapping the shield raises "PIN Required"
- [ ] Optional device admin: enable, try uninstall, disable with PIN
- [x] Reboot with auto-start on: protection returns, custom DNS still used
- [x] Update the app with protection on: `MY_PACKAGE_REPLACED` restarts the service and protection returns (verified with an in-place 10012 → 10013 install)
- [x] Custom DNS server setting persists and is used (`Upstream DNS candidates: [/9.9.9.9]`)
- [ ] Notification "Disable Firewall" prompts for the PIN — with no PIN set the action stops the service (verified); with a PIN the action reuses the shield's PIN gate, but emulator shade taps could not reach the action button to confirm end to end
- [x] Dark theme: text and status-bar icons stay legible

Two defects were found this way and are fixed in 1.0.12: filtered DNS resolved
nothing on networks whose first resolver is unreachable (section 7), and the
ongoing notification was invisible on Android 13+ because `POST_NOTIFICATIONS` was
declared but never requested at runtime.

## 6. Notes and risks

- Play requires target API 36 for new apps since 31 Aug 2026. The previous build
  targeted 35 and would have been rejected.
- The `specialUse` FGS declaration and its demo video are reviewed manually. If the
  reviewer does not accept the use case, the fallback is to keep the VPN alive as a
  `specialUse` service but make the notification even more explicit about what is
  running.
- Uninstall protection is the most likely review friction: it must remain opt-in,
  PIN-reversible, and honestly described in the listing. Do not claim the app
  "cannot be uninstalled"; the guide at `docs/uninstall-protection.md` already words
  this correctly.
- F-Droid builds from source and links `acutis-firewall-%v-signed.apk`; the CI
  workflow still produces that name. Keep `metadata/com.acutis.firewall.yml`
  updated separately when a new tag is built.

## 7. Why 1.0.11 never reached production

Installed on a clean Android 15 emulator, 1.0.11 came up as "Firewall is enabled"
with the VPN active and then resolved nothing: `ping google.com` returned
`unknown host google.com` while the same command worked with the firewall off.

The cause was upstream selection. `startVpn()` took the first address of the
device's DNS list; this emulator advertises `fec0::3` (site-local IPv6 with no
route) before `10.0.2.3`, so every forwarded query sat in
`java.net.SocketTimeoutException: Poll timed out` and the app returned no answer.
Any dual-stack network that lists an unroutable IPv6 resolver first behaved the
same way — filtering silently became a black hole.

1.0.12 fixes it: `UpstreamDns.candidates()` keeps every usable resolver, orders
IPv4 before IPv6, drops the intercepted `8.8.8.8`/`8.8.4.4` and loopback, and
`forwardToUpstream()` tries each candidate with a 1.5 s timeout, promoting the one
that answers. A unit test covers the ordering rule.

Lesson for the checklist: "Firewall is enabled" in the UI only proves the tunnel
exists. Always resolve a domain with the firewall on.
