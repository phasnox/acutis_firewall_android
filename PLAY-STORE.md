# Publishing to Google Play

Everything in this file is what still has to happen outside the repository. The code
side is done: the app targets API 36, declares the `specialUse` foreground-service
subtype Play requires, and the release build signs with the upload key when its
credentials are provided.

## 1. Version and artifacts

| Item | Value |
|------|-------|
| Package | `com.acutis.firewall` |
| Version name | `1.0.9` |
| Version code | `10009` |
| Min SDK | 29 (Android 10) |
| Target SDK | 36 (Android 16) — required for new apps since 31 Aug 2026 |
| Artifact | `app/build/outputs/bundle/release/app-release.aab`; the signed `v1.0.9` assets are on the GitHub release and in `play-store/acutis-firewall-1.0.9-signed.aab` |

Play accepts only the AAB, never an APK. Never reuse a version code: build `10010`
for the next upload even if only the listing text changes.

## 2. Signing

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

The `v1.0.9` tag was built and signed by CI (same certificate as `v1.0.8`).
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
| Changelog | `changelogs/10009.txt` | Shown as "What's new" |

The previous screenshots were 686 px wide and 2.22:1 tall, which the Console
rejects (maximum dimension may not exceed twice the minimum). The replacements were
captured on a Pixel 7 at 1080 × 1920 with full status-bar indicators and no
notifications.

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

Test on the Play build before promoting:

- [ ] First run: blocklist download, shield tap, VPN consent, notification shown
- [ ] DNS filtering blocks an adult domain and allows a normal domain
- [ ] Time rule applies (daily limit and schedule)
- [ ] PIN gates disabling the firewall and changing settings
- [ ] Optional device admin: enable, try uninstall, disable with PIN
- [ ] Reboot with auto-start on: protection returns
- [ ] Update the app from Play with protection on: protection returns (fix in 1.0.9)
- [ ] Custom DNS server setting persists and is used
- [ ] Notification "Disable Firewall" prompts for the PIN
- [ ] Dark theme: status-bar icons stay legible

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
