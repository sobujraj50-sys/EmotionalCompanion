# PROJECT_STATE

_Last updated: 2026-11-24, Milestone C (secure API configuration)_

## Current status
| Milestone | State |
|---|---|
| A: Environment & foundation | ✅ Done (CI green, opened on the user's phone 2026-11-24) |
| B: Design & navigation | ✅ Done (phone screenshots 2026-11-24). Status-bar contrast fixed afterwards |
| C: Secure API configuration | **CI build green (build 4). Waiting for phone test with a real key** |
| D: Live audio prototype | Not started |
| E: Emotional Voice Studio | Not started |
| F: Testing & APK | Not started |

## Milestone A: what exists
- Gradle Kotlin DSL project with version catalog (`gradle/libs.versions.toml`)
- Gradle wrapper 9.4.1
- GitHub Actions workflow (`.github/workflows/build.yml`): unpacks an uploaded ZIP, runs unit tests and lint, builds the APK, publishes a Release
- Single `:app` module, `minSdk 26`, `compileSdk 37`, `targetSdk 36`, Java 17
- Optional release signing from repository secrets. Without secrets, a debug APK is built
- Design-system colors, typography and dark/light themes (`core/designsystem`)
- Static foundation screen in English and Bengali, with an AI-disclosure line
- `PcmAudioFormat` (verified Gemini Live PCM rates) with 5 unit tests
- Manual DI container (`AppContainer`)
- Backup and device transfer disabled for all app data. No permissions requested
- Docs: README, CLAUDE.md, this file, `docs/*`

## Milestone B: what exists
- Settings in DataStore: theme (**Dark default**, Light, Follow phone), reduced motion, onboarding done
- 3-page onboarding (welcome, AI honesty, privacy/microphone control), shown once
- Bottom navigation: Home · Voice · Settings
- Home: app name, time-of-day greeting, connection chip, voice orb, state label, waveform, speaker/mic/voice controls, recent-conversation card, AI notice
- `VoiceOrb`: 9 states (Idle, Connecting, Listening, Thinking, Speaking, Interrupted, Reconnecting, Offline, Error). Size follows real amplitude only. Reduce-motion stops all looping animation
- `AmplitudeWaveform`: draws real levels only. Flat baseline while no audio exists
- Honest gaps: mic and speaker buttons are disabled with an explanation; the Voice tab says the Studio isn't built; the API key row says it comes in Milestone C
- Settings → Testing → Orb animation preview: shows each state with a hand-set level, clearly labeled as a test tool
- Unit tests: `TimeOfDayTest` (3), `ThemeModeTest` (3), plus `PcmAudioFormatTest` (5)

## Milestone C: what exists
- `SecretStore` + `KeystoreSecretStore`: AES-256-GCM, key in Android Keystore, ciphertext + IV in app-private prefs (excluded from backup). If the Keystore key is lost, the stored key is cleared
- Settings → Gemini API screen: paste key, show/hide, **Save and test**, Test connection, Remove (with confirmation), replace by pasting a new key. **FLAG_SECURE** blocks screenshots on this screen
- Connection test = `models.get` over HTTPS (no tokens used). HTTP codes are mapped to clear EN/BN messages (400 key rejected, 401/403 permission, 404 model unavailable, 429 quota, 5xx server, plus no internet and timeout)
- `GeminiModelCatalog`: 4 Live model IDs from the models page, with free-tier status from the pricing page. **Free-only mode on by default** hides `gemini-3.8-live*` (not on the pricing page). Default model: `gemini-3.1-flash-live-preview`
- Home chip: "Gemini key saved" once a key exists. It opens the API screen
- `INTERNET` permission added. No other permissions
- Unit tests: `GeminiModelCatalogTest` (4), `ConnectionTestResultTest` (2)

## Verification record
| Check | Where | Result |
|---|---|---|
| Environment inspection | Claude's cloud workspace | JDK 21, Gradle 8.14.3, no Android SDK, no dependency downloads |
| Wrapper generation | Cloud workspace (offline) | ✅ |
| XML / TOML well-formedness, catalog references, secret scan | Cloud workspace | ✅ |
| `testDebugUnitTest` | GitHub Actions run 45433695447 (2026-11-24) | ✅ BUILD SUCCESSFUL |
| `lintDebug` | same run | ✅ BUILD SUCCESSFUL |
| `assembleDebug` (no signing secrets) | same run | ✅ BUILD SUCCESSFUL, `EmotionalCompanion-0.1.0-mA-build1.apk` (11.7 MB) in Release `v0.1.0-mA-build1` |
| Milestone A install & launch | User's phone (screenshot) | ✅ |
| Milestone B: `testDebugUnitTest`, `lintDebug`, `assembleDebug` | GitHub Actions run 45442019853 | ✅ BUILD SUCCESSFUL ×3, Release `v0.2.0-mB-build2` |
| Milestone B on phone | User's screenshots (Home, Voice, Settings) | ✅ Dark theme, nav, orb, honest disabled mic |
| Milestone C build 3 | GitHub Actions run 45444638514 | ⚠️ APK built, **lint error** `ContextCastToActivity` (ApiConfigScreen) |
| Milestone C build 4 (fix: `LocalActivity`) | GitHub Actions run 45445065154 | ✅ tests · lint · APK, Release `v0.3.0-mC-build4` |
| Milestone C on phone with real key | Physical device | ⏳ **Not yet run** |

Milestone C is done once the phone checklist in docs/TESTING.md passes with a real key.

## Decisions
1. **Build on GitHub Actions**, the same way as the user's other apps. ZIP upload → unpack → test → lint → APK → Release.
2. **Toolchain copied from NexusNotes** (AGP 9.2.1, Gradle 9.4.1, Kotlin 2.3.20, BOM 2026.08.00, compileSdk 37), because it is proven on CI. Newer versions are deferred to a separate upgrade commit.
3. **Lint doesn't block the APK.** It runs and its report is uploaded, and the Release notes say whether it passed.
4. **Manual DI instead of Hilt (for now).** Fewer moving parts. Revisit if the dependency graph grows.
5. **Room is deferred** to the milestone that first stores data (no empty placeholder modules).
6. **Single `:app` module with package-level layering** ("start manageable", spec §5).

## Blockers / open questions
- Repo: https://github.com/sobujraj50-sys/EmotionalCompanion
- Signing secrets aren't set. Until they are, each new APK requires uninstalling the previous one (and re-entering the API key).
- `gemini-3.1-flash-live-preview` is labeled **legacy** on the models page. It is the only free-confirmed Live model besides the access-limited 2.5 native-audio preview. Re-check the pricing page before Milestone D, in case a 3.8 Live model gets a free tier.

## Next milestone
**D: Live audio prototype.** First verify the Live WebSocket endpoint, setup message, voice config, VAD and interruption fields, session limits and resumption (docs/API_CONTRACT.md "Pending"). Then add RECORD_AUDIO, AudioRecord 16 kHz capture with AEC/NS, AudioTrack 24 kHz playback queue, a WebSocket client, conversation states driving the orb, real amplitude for the waveform, mic stop, and error/quota handling.
