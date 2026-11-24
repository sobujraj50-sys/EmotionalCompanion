# Emotional Companion (temporary name)

A personal Android voice companion: warm Bengali–English conversation through Gemini Live native audio, shown as an animated voice orb. It is openly an AI and never presents itself as a real person.

**Status:** Milestone A (project foundation). The app builds into a single static screen. No voice, network or microphone features yet. See `PROJECT_STATE.md`.

## Requirements (Windows)

| Tool | Version |
|---|---|
| Android Studio | A recent stable release that supports **Android Gradle Plugin 9.5** |
| JDK | 17 or newer (Android Studio's bundled JBR is fine) |
| Android SDK | Platform **API 36**, Build-Tools **36.0.0** (install via SDK Manager) |
| Gradle | Not needed. The wrapper downloads **9.6.0** on first build |

## First build

1. Unzip the project to a short path without spaces, e.g. `C:\dev\EmotionalCompanion`.
2. Android Studio → **File → Open** → choose that folder. Let Gradle sync finish.
   - If sync says *"The project is using an incompatible version of the Android Gradle plugin"*, update Android Studio (**Help → Check for Updates**). Only if updating isn't possible, lower `agp` in `gradle/libs.versions.toml` to the newest version your Studio supports. Then match the Gradle version from the AGP release notes.
3. Studio creates `local.properties` with your SDK path. It is git-ignored and must not be committed.
4. Build from the Studio terminal (**View → Tool Windows → Terminal**):

```cmd
gradlew.bat assembleDebug
gradlew.bat testDebugUnitTest
gradlew.bat lintDebug
```

The APK appears at `app\build\outputs\apk\debug\app-debug.apk`.

## Install on your phone

1. On the phone, open **Settings → About phone** and tap **Build number** 7 times. Then enable **Developer options → USB debugging**.
2. Connect by USB and accept the debugging prompt.
3. Either press **Run ▶** in Android Studio, or:

```cmd
%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

The debug build installs as `com.emotionalcompanion.debug`, so it can sit beside a later release build.

## Configuration

The Gemini API key is **never** put in source code, Gradle files or `local.properties`. From Milestone C it is entered inside the app and stored encrypted with Android Keystore. See `docs/PRIVACY.md`.

## Documentation

- `PROJECT_STATE.md`: current status, decisions, blockers, next milestone
- `CLAUDE.md`: standing rules for AI-assisted development
- `docs/ARCHITECTURE.md`: structure and dependency choices
- `docs/API_CONTRACT.md`: Gemini Live protocol facts, verified vs. pending
- `docs/PRIVACY.md`: audio and data handling
- `docs/TESTING.md`: automated and on-device test plan
- `docs/ROADMAP.md`: phases after Phase 01
