# ✅ Automated APK Building - Now Active!

## 🎉 Great News!

I've set up **GitHub Actions** in your repository, which means **you don't need to manually build APKs anymore**! GitHub's servers will build your APK automatically with full internet access.

---

## 🚀 What's Been Set Up

### 1. **Two Workflows Created**

| Workflow | File | Trigger | Output |
|----------|------|---------|--------|
| Build Debug APK | `.github/workflows/build-apk.yml` | Push to `main`, PR, manual | Debug APK |
| Build & Release APK | `.github/workflows/release-apk.yml` | Tag push (v*), manual | Debug + Release APKs |

### 2. **Documentation Added**
- `.github/CI_CD.md` - Complete guide to using the workflows
- `.github/AUTOMATED_BUILD.md` - This file

---

## 🎯 How to Build Your APK (3 Simple Options)

### Option 1: Push to Main (Easiest) ⭐
```bash
# Simply push your code
git add .
git commit -m "Your commit message"
git push origin main
```

**What happens:**
- GitHub Actions automatically builds your debug APK
- APK is uploaded as an **artifact** (available for 30 days)
- No release is created by this workflow; tagged releases are handled separately by `release-apk.yml`

**Where to find it:**
1. Go to your repo: https://github.com/Wired-cell/Neet_Question_Bank
2. Click **Actions** tab
3. Click on the latest workflow run
4. Download the APK from **Artifacts**

OR
1. Click **Releases** tab
2. Download from the latest release

---

### Option 2: Manual Trigger

1. Go to: **Actions** tab
2. Select **"Build Android APK"** workflow
3. Click **"Run workflow"** dropdown
4. Click **"Run workflow"** button
5. Wait ~5-10 minutes
6. Download APK from artifacts

---

### Option 3: Create a Versioned Release (Best for Distribution) ⭐⭐

```bash
# Create a version tag
git tag v1.0.0
git push origin v1.0.0
```

**What happens:**
- GitHub Actions builds **both Debug and Release APKs**
- Both APKs are uploaded as artifacts
- A **GitHub release is created** with both APKs

**Where to find it:**
1. Go to **Releases** tab
2. Click on **v1.0.0**
3. Download both APKs

---

## ⚡ Why This is Better

| Old Way | New Way (GitHub Actions) |
|---------|--------------------------|
| ❌ You need Java, Android SDK, Gradle | ✅ Uses GitHub's pre-configured servers |
| ❌ You need internet for dependencies | ✅ GitHub has full internet access |
| ❌ Manual building | ✅ Automatic on push |
| ❌ Takes time on your machine | ✅ Runs in the cloud |
| ❌ Need to upload manually | ✅ Auto-uploads to releases |

---

## 📋 What's in Your Repo Now

```
Neet_Question_Bank/
├── .github/
│   ├── workflows/
│   │   ├── build-apk.yml          # Build debug APK
│   │   └── release-apk.yml        # Build & release APKs
│   ├── CI_CD.md                   # CI/CD documentation
│   └── AUTOMATED_BUILD.md          # This file
├── app/
├── build.gradle.kts
└── ... (your project files)
```

---

## 🔧 Customization

### Change the Version
Edit `release-apk.yml` and change:
```yaml
with:
  name: Release v1.0.0  # Change to your version
  tag_name: v1.0.0      # Change to your version
```

### Build Different Flavors
The workflows currently build:
- `build-apk.yml`: Debug APK only
- `release-apk.yml`: Both Debug and Release APKs

To build **only release APK**, modify:
```yaml
- name: Build Release APK
  run: ./gradlew assembleRelease --no-daemon
```

### Add Signing (For Release APKs)
To sign your release APK:
1. Set up signing in your project
2. Add secrets to GitHub: `SIGNING_KEY`, `SIGNING_PASSWORD`, etc.
3. Modify the workflow to use signing

---

## 💰 Cost

- **Free for public repos**: ✅ Yes, unlimited
- **Free for private repos**: 2000 minutes/month
- **Your usage**: ~5-10 minutes per build
- **Cost**: $0 (within free tier)

---

## 🎯 Try It Now!

**The fastest way:**
```bash
git push origin main
```

Then watch the magic happen in the **Actions** tab! 🎩✨

---

## 📊 Workflow Features

### build-apk.yml
- ✅ Triggers on push to `main`
- ✅ Triggers on pull requests to `main`
- ✅ Can be triggered manually
- ✅ Builds debug APK
- ✅ Uploads APK as artifact
- ✅ Uploads a debug APK artifact

### release-apk.yml
- ✅ Triggers on tag push (e.g., `v1.0.0`)
- ✅ Can be triggered manually with version input
- ✅ Builds both Debug and Release APKs
- ✅ Uploads both APKs as artifacts
- ✅ Creates GitHub release with both APKs

---

## 🔗 Quick Links

- **Repository**: https://github.com/Wired-cell/Neet_Question_Bank
- **Actions Tab**: https://github.com/Wired-cell/Neet_Question_Bank/actions
- **Releases Tab**: https://github.com/Wired-cell/Neet_Question_Bank/releases
- **CI/CD Docs**: https://github.com/Wired-cell/Neet_Question_Bank/blob/main/.github/CI_CD.md

---

## 💬 FAQ

### Q: Do I need to set up anything else?
**A**: No! Just push to `main` or create a tag. Everything is already configured.

### Q: How long does it take?
**A**: ~5-10 minutes per build (depends on GitHub's queue)

### Q: Can I build multiple APK types?
**A**: Yes! The `release-apk.yml` builds both Debug and Release. You can modify it to build any variant.

### Q: What if the build fails?
**A**: Check the **Actions** tab → Click on the failed workflow → View logs to see the error.

### Q: Can I customize the workflows?
**A**: Yes! Edit the YAML files in `.github/workflows/`. They're just text files.

---

## ✨ Summary

✅ **GitHub Actions workflows created**
✅ **Automatic APK building on push**
✅ **Versioned releases on tag push**
✅ **Full documentation provided**

**You're all set!** Just push to `main` or create a tag, and GitHub will build your APK automatically. 🎉

---

## 🚀 Ready to Go!

**Try it now:**
```bash
cd Neet_Question_Bank
git add .
git commit -m "Ready for CI/CD"
git push origin main
```

Then check: https://github.com/Wired-cell/Neet_Question_Bank/actions

Your first APK will be ready in ~5-10 minutes! 🎊
