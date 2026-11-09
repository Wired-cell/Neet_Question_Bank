# Release Instructions - Question Bank App

Since I cannot create GitHub releases directly in this environment, here's how you can create a release and upload the APK:

---

## 📦 Step 1: Build the APK

Follow the instructions in **BUILD_INSTRUCTIONS.md** to build the APK using Android Studio or command line.

You'll get:
- `app-debug.apk` (for testing)
- `app-release.apk` (for distribution - needs signing)

---

## 🚀 Step 2: Create a GitHub Release

### Method A: Using GitHub Website (Easiest)

1. Go to your repository: https://github.com/Wired-cell/Neet_Question_Bank
2. Click on **Releases** (on the right sidebar, under "About")
3. Click **Draft a new release**
4. Fill in the details:
   - **Tag version**: `v1.0.0` (already created)
   - **Release title**: `Version 1.0.0 - Initial Release`
   - **Description**:
     ```
     First release of Neet Question Bank App
     
     Features:
     - Vault system with Physics, Chemistry, Biology
     - Offline OCR with ML Kit
     - Enhanced option detection (always picks last 4)
     - Image cropping for diagrams
     - Test mode with 180-minute timer
     - 45 questions from each sub-vault
     - Google AI Studio integration (API key in settings)
     ```
5. **Attach APK**: Drag and drop your `app-release.apk` file into the release
6. Click **Publish release**

### Method B: Using GitHub CLI

If you have the GitHub CLI installed locally:

```bash
# Navigate to your local repo
git clone https://github.com/Wired-cell/Neet_Question_Bank.git
cd Neet_Question_Bank

# Create release and upload APK
gh release create v1.0.0 \
  --title "Version 1.0.0 - Initial Release" \
  --notes "First release with all features: OCR, Test Mode with 180-minute timer, Image Cropping, Enhanced option detection" \
  app/build/outputs/apk/release/app-release.apk
```

---

## 📝 Step 3: Verify the Release

1. Go to: https://github.com/Wired-cell/Neet_Question_Bank/releases
2. You should see **v1.0.0** release
3. The APK file should be available for download

---

## 🎯 Step 4: Share the Release

Once the release is created:
- Share the release URL: `https://github.com/Wired-cell/Neet_Question_Bank/releases/tag/v1.0.0`
- Users can download the APK directly from GitHub
- Or install via the release page

---

## 📌 Notes

### About GitHub Releases
- GitHub releases are **public** (since your repo is public)
- APK files can be large (20-50MB for debug, 10-20MB for release)
- GitHub has a **2GB file size limit** for releases
- You can attach multiple files (APK, source code, etc.)

### About APK Types
| Type | File | Use Case |
|------|------|----------|
| Debug | `app-debug.apk` | Testing only, not for distribution |
| Release (unsigned) | `app-release.apk` | Needs signing for distribution |
| Release (signed) | `app-release.apk` | Ready for distribution |
| Bundle | `app-release.aab` | For Google Play Store |

### Recommendation
For your first release, create a **signed release APK** and upload it to GitHub releases. This allows users to:
1. Download directly from GitHub
2. Install on their Android devices (API 24+)
3. Test all features

---

## 🔗 Quick Links

- **Repository**: https://github.com/Wired-cell/Neet_Question_Bank
- **Releases Page**: https://github.com/Wired-cell/Neet_Question_Bank/releases
- **Tag v1.0.0**: https://github.com/Wired-cell/Neet_Question_Bank/tree/v1.0.0

---

## 💡 Pro Tip

If you want to automate releases in the future, you can:
1. Set up **GitHub Actions** workflow
2. Automatically build and release on push to main
3. Use a signing key stored in GitHub Secrets

Example workflow file (`.github/workflows/release.yml`):
```yaml
name: Build and Release

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    - name: Build APK
      run: ./gradlew assembleRelease
    - name: Upload Release
      uses: softprops/action-gh-release@v1
      with:
        files: app/build/outputs/apk/release/app-release.apk
        tag_name: ${{ github.ref_name }}
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

---

## 🎉 All Set!

You now have:
1. ✅ Complete source code in your GitHub repo
2. ✅ Tag `v1.0.0` created
3. ✅ Build instructions
4. ✅ Release instructions

Just build the APK and create the release using the steps above!
