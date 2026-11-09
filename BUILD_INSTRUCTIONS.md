# Build Instructions - Question Bank App

## 📱 How to Build the APK

Since I cannot build APKs directly in this environment (no Java/Gradle/Android SDK available), here are the steps to build the APK yourself:

---

## 🔧 Method 1: Using Android Studio (Recommended)

### Step 1: Clone the Repository
```bash
git clone https://github.com/Wired-cell/Neet_Question_Bank.git
cd Neet_Question_Bank
```

### Step 2: Open in Android Studio
1. Open **Android Studio**
2. Click **File → Open**
3. Navigate to the cloned `Neet_Question_Bank` folder
4. Click **OK**

### Step 3: Sync Gradle
- Android Studio will automatically sync Gradle dependencies
- Wait for the sync to complete (you'll see "Build: idle" at the bottom)

### Step 4: Build Debug APK
1. Click **Build → Build Bundle(s) / APK(s) → Build APK**
2. Wait for the build to complete
3. Android Studio will show a notification: "APK generated"
4. Click the notification or go to **Build → Analyze APK**

### Step 5: Find the APK
- The debug APK will be generated at:
  ```
  Neet_Question_Bank/app/build/outputs/apk/debug/app-debug.apk
  ```
- You can install this on your Android device

---

## 🔧 Method 2: Using Command Line (Gradle)

### Prerequisites
- Java JDK 17+ installed
- Android SDK installed
- Gradle 8.3+ installed

### Step 1: Clone the Repository
```bash
git clone https://github.com/Wired-cell/Neet_Question_Bank.git
cd Neet_Question_Bank
```

### Step 2: Build Debug APK
```bash
# On Linux/Mac
./gradlew assembleDebug

# On Windows
gradlew.bat assembleDebug
```

### Step 3: Find the APK
```
# The APK will be at:
ls app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔧 Method 3: Build Release APK (For Distribution)

### Using Android Studio
1. Click **Build → Build Bundle(s) / APK(s) → Build APK**
2. Select **release** instead of debug
3. The APK will be at:
   ```
   Neet_Question_Bank/app/build/outputs/apk/release/app-release.apk
   ```

### Using Command Line
```bash
# Build release APK
./gradlew assembleRelease

# The APK will be at:
ls app/build/outputs/apk/release/app-release.apk
```

**Note**: For release builds, you need to set up signing. See the signing section below.

---

## 🔐 Signing the Release APK

### Step 1: Generate a Keystore (if you don't have one)
```bash
keytool -genkey -v -keystore my-release-key.jks \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias my-alias
```

### Step 2: Configure Signing in Android Studio
1. Click **File → Project Structure**
2. Select **Modules → app → Signing Configs**
3. Click **+** → Add new signing config
4. Enter details:
   - Store file: path to your `.jks` file
   - Store password: your password
   - Key alias: your alias
   - Key password: your password
5. Click **OK**

### Step 3: Select Signing Config
1. In **Project Structure → Modules → app → Build Types**
2. Select **release**
3. Set **Signing Config** to your new config
4. Click **OK**

### Step 4: Build Signed APK
```bash
./gradlew assembleRelease
```

---

## 📋 Requirements

### Minimum Requirements
- **Android Studio**: Latest version (2023.2+)
- **Android SDK**: API 34 (compile), API 24+ (minSdk)
- **Java**: JDK 17+
- **Gradle**: 8.3+
- **Device**: Android 7.0+ (API 24)

### Dependencies (Automatically Downloaded)
- AndroidX libraries
- Jetpack Compose
- Room Database
- CameraX
- ML Kit (for OCR)
- Cropper library
- Coil (image loading)
- Retrofit (API calls)

---

## 🐞 Troubleshooting

### Error: "Gradle sync failed"
1. Click **File → Invalidate Caches / Restart**
2. Try again

### Error: "Could not find com.android.tools.build:gradle:8.3.0"
1. Click **File → Project Structure → Project**
2. Change Gradle version to match your installed version
3. Click **OK** and sync

### Error: "SDK not found"
1. Click **Tools → SDK Manager**
2. Install Android SDK 34 and 24
3. Try again

### Error: "No toolchains found"
1. Install Java JDK 17 from [Oracle](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) or [OpenJDK](https://adoptium.net/)
2. Set JAVA_HOME environment variable
3. Restart Android Studio

---

## 📦 APK Output Locations

| Build Type | APK Location |
|------------|--------------|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Release (unsigned) | `app/build/outputs/apk/release/app-release.apk` |
| Release (signed) | `app/build/outputs/apk/release/app-release.apk` |
| Bundle | `app/build/outputs/bundle/release/app-release.aab` |

---

## 🚀 Quick Start (Android Studio)

1. **Clone**: `git clone https://github.com/Wired-cell/Neet_Question_Bank.git`
2. **Open**: File → Open → Select folder
3. **Sync**: Wait for Gradle sync
4. **Build**: Build → Build APK
5. **Install**: Transfer `app-debug.apk` to device and install

---

## 📝 Notes

- **Debug APK**: Can be installed directly, but has debug signature
- **Release APK**: Needs signing for distribution
- **Bundle**: For Google Play Store upload

---

## 🎯 Next Steps

Once you have the APK:
1. **Install** on your Android device (API 24+)
2. **Test** all features:
   - Vault system
   - Add questions with OCR
   - Image cropping
   - Test mode with timer
3. **Report** any issues

---

## 💬 Need Help?

If you encounter any issues building the APK:
1. Check the **Build** tab in Android Studio for errors
2. Try **File → Invalidate Caches / Restart**
3. Make sure you have the latest Android Studio and SDK
4. Ensure Java JDK 17+ is installed

You can also ask me for help troubleshooting specific errors!
