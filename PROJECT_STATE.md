# PROJECT_STATE

_Last updated: 2026-11-25, Milestone F (testing & APK). Phase 01 code complete_

## Current status
| Milestone | State |
|---|---|
| A: Environment & foundation | ✅ Done (CI green, opened on the user's phone 2026-11-24) |
| B: Design & navigation | ✅ Done (phone screenshots 2026-11-24). Status-bar contrast fixed afterwards |
| C: Secure API configuration | CI green (build 4). Phone test with a real key **not reported yet** (user went straight to D) |
| D: Live audio prototype | CI green (build 5). Real conversation **not reported yet** (user went straight to E) |
| E: Emotional Voice Studio | CI green (build 6). Phone test **not reported yet** |
| F: Testing & APK | ✅ CI green incl. **8/8 emulator tests passed** (run 45454323263). Release `v1.0.0-phase01` |

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

## Milestone D: what exists
- `GeminiLiveClient` (OkHttp 5.3.1 WebSocket, no logging) + `LiveProtocol`: setup with model, AUDIO modality, prebuilt voice, system instruction, input/output transcription, `contextWindowCompression.slidingWindow`, `sessionResumption`. Audio sent as `realtimeInput.audio` (`audio/pcm;rate=16000`). Parses `setupComplete`, `serverContent` (audio parts, transcriptions, `interrupted`, `turnComplete`), `goAway`, `sessionResumptionUpdate`
- `AudioCaptureManager`: AudioRecord 16 kHz mono, VOICE_COMMUNICATION source, AcousticEchoCanceler + NoiseSuppressor when available, 100 ms chunks, real RMS level
- `AudioPlaybackManager`: AudioTrack 24 kHz, USAGE_VOICE_COMMUNICATION, queue with instant `clear()` on interruption, real playback level
- `AudioFocusManager`: transient focus, MODE_IN_COMMUNICATION (platform echo cancellation), loudspeaker ↔ earpiece/headset routing, stops the session on focus loss (e.g. phone call)
- `LiveSessionManager`: full lifecycle, reconnection with the resumption handle (max 3, backoff 1–4 s, also on `goAway`), **half-duplex fallback** (mic muted while the AI speaks) when "Interrupt by speaking" is off or the device has no AEC, tap-orb-to-silence, in-memory captions (6 lines, never stored)
- `ConversationStateManager` + pure `ConversationTransitions`: the orb shows only states caused by real events. The waveform shows real mic/playback levels
- `UsageController` + `UsageRules`: session length (default 10 min), daily budget (30 min), idle timeout (60 s). Counted locally and shown on Home as an estimate
- `ConnectionErrorHandler`: close codes/reasons and handshake HTTP codes → EN/BN messages. Close reason shown as "Details" (any `key=` is masked)
- First-use disclosure dialog (audio goes to Google, free-tier data use), then the RECORD_AUDIO request. Leaving the app stops the session (no background recording)
- Default personality (spec §12) with honesty rules as the system instruction. Voice `Sulafat` (Warm)
- Unit tests: `ConversationTransitionsTest` (5), `LiveRulesTest` (7)

## Milestone E: what exists
- `VoiceConfiguration`: all 30 prebuilt voices with Google's descriptors. The 6 spec candidates (Sulafat, Achernar, Vindemiatrix, Aoede, Leda, Kore) are shown first. Selection is saved and used by the live session
- `TestDialogue`: fixed Bangla (spec default line), English and mixed lines. Every voice reads the same line
- `VoiceAuditionPlayer`: plays a sample through **the same Gemini Live model** (short session, `realtimeInput.text`, no mic, USAGE_MEDIA playback, 30 s timeout). It counts toward the daily budget and is disabled during a conversation
- `PersonalitySettings` + `PersonalityEngine`: nickname (cleaned, max 30 chars), language preference (Bangla-first / mixed / English-first), 7 character levels (affection, care, humor, romance, expressiveness, talkativeness, initiative), 4 delivery levels (warmth, softness, energy, pace). All are turned into system-instruction text. **The honesty rules are a fixed constant appended last**, and no setting can remove them
- Voice tab = Voice Studio (test line, voices with Listen, delivery sliders, offline voice). Personality screen with the exact-instruction preview
- `OfflineSpeech` (Android TextToSpeech, bn-BD): offline phone-voice preview, plus a spoken notice when a conversation fails with no internet. It only reads text and is not presented as the AI
- `DefaultPersonality` replaced by `PersonalityEngine`
- Unit tests: `PersonalityEngineTest` (7)

## Milestone F: what exists
- Instrumented tests (`app/src/androidTest`), run on a real Android 14 (API 34, x86_64, google_apis) emulator in GitHub Actions on every push:
  - `AppNavigationTest`: launch → onboarding → Home (honest "not set up" state, AI notice) → Voice Studio (30 voices, Personality screen) → Settings → Gemini API screen
  - `SecureStorageAndProtocolTest`: Keystore round trip (ciphertext ≠ key, survives a new instance, clear), setup JSON shape, server-message parsing with the platform org.json (Bangla captions included)
  - `AudioPipelineTest`: real AudioRecord 16 kHz capture delivers chunks and stops; AudioTrack 24 kHz queue drains and clears instantly (interruption)
- Workflow: new `emulator-tests` job (after `build`, doesn't block the release) that prints every test result into the log
- Version 1.0.0-phase01

## Phase 01 Definition of Done (spec §20)
✅ verified · 🟡 verified in code/CI/emulator, needs the real phone + real Gemini key · ⏳ not verified

| Requirement | Status | Evidence |
|---|---|---|
| Android project compiles | ✅ | Every CI build since build 1 |
| Debug APK is generated | ✅ | GitHub Releases, latest `v1.0.0-phase01` |
| Premium Home Screen works | ✅ | User screenshots (B) + emulator `AppNavigationTest` |
| Navigation works | ✅ | User screenshots + emulator |
| API key can be entered securely | ✅/🟡 | Keystore test on emulator ✅. Entering a real key on the phone not reported |
| Gemini connection can be tested | 🟡 | `models.get` tester + unit tests. Not run against Google yet |
| Microphone capture works | 🟡 | Emulator AudioRecord test ✅. Real phone not reported |
| AI audio response playback works | 🟡 | Emulator AudioTrack test ✅. Gemini audio on the phone not reported |
| Voice selection works | 🟡 | UI + persistence ✅ (emulator). Audition against Gemini not reported |
| Bengali-English conversation is supported | 🟡 | Instruction + language preference ✅. Not heard yet |
| Emotional personality instructions are applied | 🟡 | `PersonalityEngineTest` ✅, sent in setup ✅. Audible effect not judged yet |
| Microphone stop control works | 🟡 | Stop path + emulator capture-stop test ✅. Phone not reported |
| API and network errors are handled | 🟡 | Unit tests for codes/reasons ✅. Real Google close reasons unseen |
| Free-only safeguards documented | ✅ | API_CONTRACT, PRIVACY, in-app texts |
| Actual test results are recorded | ✅ | This file + docs/TESTING.md |

**Phase 01 is code-complete and automatically tested, but not finished.** It's finished when the phone checklists for C, D and E (docs/TESTING.md) pass with a real Gemini key.

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
| Milestone C on phone with real key | Physical device | ⏳ **Not reported** |
| Milestone D build 5 | GitHub Actions run 45449536699 | ✅ tests · lint · APK, Release `v0.4.0-mD-build5` (13.8 MB) |
| Milestone D real conversation | Physical device | ⏳ **Not yet run** |
| Milestone E build 6 | GitHub Actions run 45451345002 | ✅ tests · lint · APK, Release `v0.5.0-mE-build6` |
| Milestone E on phone | Physical device | ⏳ **Not yet run** |
| Milestone F build 7 | GitHub Actions run 45452824132 | ✅ build + `emulator-tests` (8 tests, 0 failed) |
| Milestone F build 8 (per-test log) | GitHub Actions run 45454323263 | ✅ **8/8 PASSED, 0 skipped**: 3 navigation, 2 audio pipeline, 3 storage/protocol |

**Nothing that talks to Google (connection test, live conversation, voice audition) has run yet**: CI has no API key, by design.

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

## Known risks to check on the device
- `generationConfig.responseModalities/speechConfig` follow the API reference. The WebSocket guide's sample puts `responseModalities` directly in `setup`. If Gemini rejects the setup (error "Gemini rejected the session settings" + Details), move those fields and retest.
- Voice `Sulafat` comes from the TTS voice list, which the Live guide points to. If it's rejected for the Live model, the Details line will say so.
- Google's close-reason wording isn't documented, so error classification is best-effort. Details always shows the raw reason.
- `gemini-3.1-flash-live-preview` is labeled legacy. Affective dialog and proactive audio are **not supported on it** (Live guide), so they aren't enabled.
- Echo/feedback depends on the phone's AEC quality. If she hears herself, turn off "Interrupt by speaking" (half duplex).

- Audition relies on the model reading the text exactly. A Live model may answer the line instead of reading it. If that happens, switch auditions to Gemini TTS (`gemini-3.1-flash-tts-preview`, free tier confirmed) in F.
- Delivery/personality levels are prompt text, not API controls. Their audible effect must be judged by ear.

## Decisions in F
- **R8/minification stays off.** For a personal APK the size (≈14 MB) is fine, and minification can only be checked for crashes on a device. Turn it on in a later phase together with keep rules and a device test.
- **Stable signing needs the user's go-ahead.** It requires creating a signing key and storing it as two repository secrets. Until then, each CI build uses a new debug key, so a new APK can't update the installed app.

## Next steps
1. User runs the C → D → E phone checklists with a real key and reports the results and any error "Details" line.
2. Fix what the phone reveals (most likely: setup field placement, voice availability, echo tuning, audition reading the line verbatim).
3. Stable signing secrets (after approval).
4. Phase 02 (docs/ROADMAP.md): user-controlled memory, conversation retention via Room, mood journal, time-zone and reunion countdown.
