# Emotional Companion (temporary name)

A personal Android voice companion: warm Bengali–English conversation through Gemini Live native audio, shown as an animated voice orb. It is openly an AI and never presents itself as a real person.

**Status:** Phase 01 code-complete (v1.0.0). Builds, lint, 36 unit tests and 8 emulator tests are green on CI. The live Gemini voice features still need testing on the phone with a real key. See `PROJECT_STATE.md`.

## Quick start on your phone
1. **Get a Gemini API key** at Google AI Studio (aistudio.google.com). It's free. Don't turn on billing if you want to stay free.
2. **Install the APK** from this repo's **Releases** (newest first). Allow "Install unknown apps" for your browser once. Uninstall the old version first, unless signing secrets are set (see below).
3. **Open the app** → finish the 3 intro pages → tap the **"Gemini not set up"** chip → paste the key → **Save and test**. It should say "Connected".
4. **Voice tab** → listen to the voices with the Bangla test line → pick one. Optionally open **Edit personality** (nickname, language, warmth…).
5. **Home** → tap the **microphone** → accept the notice and the microphone permission → talk. Tap **✕** to stop. Tap the orb to silence her mid-sentence.
6. Something wrong? Copy the **Details** line from the error card and send it.

## How it is built: GitHub Actions
No local build is needed. GitHub builds the APK on every push to `main`.

1. **Update the code:** upload the project ZIP to the repo root with **Add file → Upload files → Commit**. The workflow unpacks it, commits the source, runs unit tests and lint, builds the APK and creates a **Release**. Claude can also push changes directly.
2. **Get the APK:** open the repo → **Releases** → newest release → download `EmotionalCompanion-…apk` on the phone and install it. The first time, allow "Install unknown apps" for your browser.
3. **Automatic tests:** every push runs unit tests and lint, then the `emulator-tests` job opens the app on an Android 14 emulator (navigation, secure key storage, protocol parsing, microphone and playback). Each test's result is printed in that job's log.
4. **If a build fails:** **Actions** tab → the red run → open the failed step and copy the log. The `reports` artifact holds the test and lint reports.

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
