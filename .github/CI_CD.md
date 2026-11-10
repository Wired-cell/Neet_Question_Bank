# CI/CD Setup - Question Bank App

## ✅ GitHub Actions Workflows

This repository now includes **automated CI/CD workflows** that will build your Android APK automatically!

---

## 📋 Available Workflows

### 1. **build-apk.yml** - Build Debug APK
- **Trigger**: Push to `main`, PR to `main`, or manual trigger
- **Output**: Debug APK (`app-debug.apk`)
- **Artifacts**: Uploaded to GitHub Actions artifacts (30 days retention)
- **Release**: Automatically creates a release on push to `main`

### 2. **release-apk.yml** - Build & Release APK
- **Trigger**: Push a new tag (e.g., `v1.0.0`) or manual trigger with version input
- **Output**: Both Debug and Release APKs
- **Artifacts**: Uploaded to GitHub Actions artifacts
- **Release**: Creates a GitHub release with both APKs

---

## 🚀 How to Use

### Option 1: Automatic Build on Push (Easiest)

Simply **push to `main`** branch:
```bash
git push origin main
```

GitHub Actions will:
1. ✅ Build the debug APK
2. ✅ Upload as artifact
3. ✅ Create a release with the APK

**Access the APK:**
- Go to: **Actions** tab → Click on the workflow run → Download artifacts
- Or: **Releases** tab → Download from latest release

---

### Option 2: Manual Trigger

1. Go to: **Actions** tab
2. Select **"Build Android APK"** workflow
3. Click **"Run workflow"** button
4. Wait for completion (~5-10 minutes)
5. Download the APK from artifacts

---

### Option 3: Create a Release (Recommended)

To create a **versioned release** (e.g., v1.0.0):

```bash
# Create and push a tag
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions will:
1. ✅ Build both Debug and Release APKs
2. ✅ Upload as artifacts
3. ✅ Create a GitHub release with both APKs

**Access the release:**
- Go to: **Releases** tab → Click on v1.0.0 → Download APKs

---

## 📁 Workflow Files

| File | Purpose | Trigger |
|------|---------|---------|
| `.github/workflows/build-apk.yml` | Build debug APK | Push/PR to main, manual |
| `.github/workflows/release-apk.yml` | Build & release APKs | Tag push, manual |

---

## ⚙️ Customization

### Change Java Version
Edit `build-apk.yml`:
```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v3
  with:
    java-version: '17'  # Change to '21' if needed
```

### Change Android SDK Version
Edit `build-apk.yml`:
```yaml
- name: Set up Android SDK
  uses: android-actions/setup-android@v2
  with:
    sdk-version: 34  # Change to match your compileSdk
    build-tools-version: 34.0.0
```

### Build Different Variants
To build a **signed release APK**, you need to:
1. Set up signing in your project
2. Modify the workflow to use `assembleRelease`

Example for signed APK:
```yaml
- name: Build Signed Release APK
  run: ./gradlew assembleRelease --no-daemon
  env:
    SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
    SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
```

---

## 🎯 Workflow Details

### build-apk.yml
```yaml
name: Build Android APK

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v3 (Java 17)
      - uses: android-actions/setup-android@v2 (SDK 34)
      - run: ./gradlew assembleDebug
      - Upload APK as artifact
      - Create release (on push to main)
```

### release-apk.yml
```yaml
name: Build and Release APK

on:
  push:
    tags: [ 'v*' ]
  workflow_dispatch:
    inputs:
      version: '1.0.0'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v3 (Java 17)
      - uses: android-actions/setup-android@v2 (SDK 34)
      - run: ./gradlew assembleDebug
      - run: ./gradlew assembleRelease
      - Upload both APKs as artifacts
      - Create GitHub release with both APKs
```

---

## 💰 Cost & Limits

- **Free for public repos**: ✅ Yes
- **Free for private repos**: 2000 minutes/month
- **Build time**: ~5-10 minutes per run
- **Storage**: Artifacts retained for 30 days (configurable)

---

## 🔧 Troubleshooting

### Error: "Build failed"
1. Check the **Actions** tab
2. Click on the failed workflow
3. View the logs to see what went wrong
4. Common issues:
   - Missing dependencies
   - License acceptance issues
   - SDK version mismatch

### Error: "Plugin not found"
Make sure:
- You have internet access (GitHub Actions has this)
- The plugin versions match your project
- The SDK is properly set up

### Error: "License not accepted"
The workflow includes `accept-licenses: true` to auto-accept Android SDK licenses.

---

## 📊 Benefits

| Benefit | Description |
|---------|-------------|
| ✅ **Automatic builds** | No manual building needed |
| ✅ **Fast feedback** | Know if build succeeds/fails immediately |
| ✅ **Artifact storage** | APKs available for 30 days |
| ✅ **Release automation** | Automatic releases on tag push |
| ✅ **No local setup** | No need to install Android SDK locally |
| ✅ **Free** | Uses GitHub's free tier |

---

## 🚀 Next Steps

1. **Push to main** to trigger the first build
2. **Check Actions tab** to see the workflow running
3. **Download APK** from artifacts or releases
4. **Test on device**

---

## 💬 Need Help?

- **Workflow issues**: Check Actions tab → Logs
- **Build issues**: Check if your project builds locally first
- **Customization**: Edit the YAML files in `.github/workflows/`

---

## 🎉 All Set!

Your repository now has **automated APK building**! Just push to `main` or create a tag, and GitHub Actions will build your APK automatically.

**Try it now:**
```bash
git push origin main
```

Then check the **Actions** tab in your GitHub repo! 🚀
