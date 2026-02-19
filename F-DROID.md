# Publishing to F-Droid

## Option 1: Submit to Official F-Droid Repository (Recommended)

This gets your app in the main F-Droid app that millions use.

### Steps:

1. **Ensure your repo is public** on GitHub

2. **Fork the F-Droid Data repository**:
   ```bash
   git clone https://gitlab.com/fdroid/fdroiddata.git
   cd fdroiddata
   ```

3. **Copy the metadata file**:
   ```bash
   cp /path/to/acutis_firewall/metadata/com.acutis.firewall.yml metadata/
   ```

4. **Test the build locally** (optional but recommended):
   ```bash
   fdroid build -v -l com.acutis.firewall
   ```

5. **Submit a Merge Request** on GitLab:
   - Go to https://gitlab.com/fdroid/fdroiddata
   - Create a merge request with your metadata file
   - Title: "New app: Acutis Firewall"

6. **Wait for review** - F-Droid team will review and build from source

### Requirements for F-Droid:
- [x] Open source (MIT license)
- [x] No proprietary dependencies
- [x] Builds from source with Gradle
- [x] No tracking/analytics
- [x] No non-free network services

---

## Option 2: Create Your Own F-Droid Repository

Faster updates, you control everything.

### Steps:

1. **Install fdroidserver**:
   ```bash
   pip install fdroidserver
   ```

2. **Create repo directory**:
   ```bash
   mkdir -p fdroid/repo
   cd fdroid
   fdroid init
   ```

3. **Configure repo** - edit `config.yml`:
   ```yaml
   repo_url: https://yourdomain.com/fdroid/repo
   repo_name: Acutis Apps
   repo_description: Apps by Castillo
   ```

4. **Add your APK**:
   ```bash
   cp /path/to/acutis-firewall-1.0.0-signed.apk repo/
   fdroid update
   ```

5. **Host the `repo/` folder** on any web server or GitHub Pages

6. **Users add your repo** in F-Droid app:
   - F-Droid → Settings → Repositories → Add repository
   - Enter your repo URL

---

## Files Created

```
fastlane/metadata/android/en-US/
├── title.txt
├── short_description.txt
├── full_description.txt
├── changelogs/
│   └── 10000.txt          # Changelog for versionCode 10000
└── images/
    └── phoneScreenshots/
        ├── 1.jpg
        ├── 2.jpg
        ├── 3.jpg
        └── 4.jpg

metadata/
└── com.acutis.firewall.yml  # F-Droid metadata file
```

## Updating the App

When releasing a new version:

1. **Update changelog**: Create `fastlane/metadata/android/en-US/changelogs/{versionCode}.txt`

2. **Update metadata** (if submitting to official repo): Update `CurrentVersion` and `CurrentVersionCode` in the yml file

3. **Tag the release**: `git tag v1.0.1 && git push origin v1.0.1`
