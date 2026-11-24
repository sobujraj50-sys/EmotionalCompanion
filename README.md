# Emotional Companion (temporary name)

A personal Android voice companion: warm Bengali–English conversation through Gemini Live native audio, shown as an animated voice orb. It is openly an AI and never presents itself as a real person.

**Status:** Milestone A (project foundation). The app builds into a single static screen. No voice, network or microphone features yet. See `PROJECT_STATE.md`.

## How it is built: GitHub Actions
No local build is needed. GitHub builds the APK on every push to `main`.

1. **Update the code:** upload the project ZIP to the repo root with **Add file → Upload files → Commit**. The workflow unpacks it, commits the source, runs unit tests and lint, builds the APK and creates a **Release**. Claude can also push changes directly.
2. **Get the APK:** open the repo → **Releases** → newest release → download `EmotionalCompanion-…apk` on the phone and install it. The first time, allow "Install unknown apps" for your browser.
3. **If a build fails:** **Actions** tab → the red run → open the failed step and copy the log. The `reports` artifact holds the test and lint reports.

### Signing (so new builds install as updates)
Without signing secrets, each build uses a temporary debug key. A new APK then **cannot update** the installed app, so you must uninstall it first. To fix that, add two repository secrets (**Settings → Secrets and variables → Actions**):
- `KEYSTORE_BASE64`: the keystore file, base64-encoded (key alias must be `ec`)
- `KEYSTORE_PASSWORD`: its password (used for both store and key)

## Optional: local build on Windows
Android Studio with JDK 17+ and the SDK for API 37. Open the folder, then:

```cmd
gradlew.bat assembleDebug
gradlew.bat testDebugUnitTest
gradlew.bat lintDebug
```

## Install on your phone

(Only for local builds.) 1. On the phone, open **Settings → About phone** and tap **Build number** 7 times. Then enable **Developer options → USB debugging**.
2. Connect by USB and accept the debugging prompt.
3. Either press **Run ▶** in Android Studio, or use `adb install -r <apk>`.

Installing straight from GitHub Releases needs no USB at all (see above).

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
