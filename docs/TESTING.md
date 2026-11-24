# Testing

## Automated
| Suite | Command (Windows) | Contents |
|---|---|---|
| Unit | `gradlew.bat testDebugUnitTest` | `PcmAudioFormatTest` (5), `TimeOfDayTest` (3), `ThemeModeTest` (3), `GeminiModelCatalogTest` (4), `ConnectionTestResultTest` (2), `ConversationTransitionsTest` (5), `LiveRulesTest` (7) |
| Lint | `gradlew.bat lintDebug` | Report: `app\build\reports\lint-results-debug.html` |
| Instrumented | `gradlew.bat connectedDebugAndroidTest` | None yet (added with Milestone B UI) |

## Results log
| Date | Where | Command | Result |
|---|---|---|---|
| 2026-11-24 | Cloud workspace | Gradle wrapper generation | ✅ |
| 2026-11-24 | Cloud workspace | XML well-formedness of manifest and resources | ✅ |
| 2026-11-24 | GitHub Actions (build 1) | `testDebugUnitTest` | ✅ |
| 2026-11-24 | GitHub Actions (build 1) | `lintDebug` | ✅ |
| 2026-11-24 | GitHub Actions (build 1) | `assembleDebug` | ✅ APK in Release v0.1.0-mA-build1 |
| 2026-11-24 | User's phone | Milestone A launch | ✅ (opened in light theme, following the phone) |
| 2026-11-24 | GitHub Actions (build 2) | unit tests · lint · `assembleDebug` | ✅ Release v0.2.0-mB-build2 |
| 2026-11-24 | User's phone | Milestone B screens | ✅ (status-bar icons hard to read → fixed in build 3) |
| 2026-11-24 | GitHub Actions (build 3) | lint | ❌ ContextCastToActivity → fixed |
| 2026-11-24 | GitHub Actions (build 4) | unit tests · lint · `assembleDebug` | ✅ Release v0.3.0-mC-build4 |
| 2026-11-24 | GitHub Actions (build 5) | unit tests · lint · `assembleDebug` | ✅ Release v0.4.0-mD-build5 |

## Physical-device checklist: Milestone A
- [ ] APK from GitHub Releases downloads and installs
- [ ] Launcher shows "Emotional Companion" with the orb icon
- [ ] App opens on the dark foundation screen and doesn't crash
- [ ] With phone language set to বাংলা, the Bengali text renders with no broken conjuncts or boxes
- [ ] Content doesn't sit under the status or navigation bars
- [ ] Settings → Apps → Emotional Companion → Permissions shows **no permissions**

## Physical-device checklist: Milestone B
Uninstall the Milestone A app first (debug signing key changed).
- [ ] First launch shows 3 onboarding pages. "Skip" jumps to the last page, and "Get started" opens Home
- [ ] After closing and reopening the app, onboarding does **not** appear again
- [ ] App is **dark** by default, even if the phone is in light mode
- [ ] Home: greeting matches the time of day, the chip says "Gemini not set up", the orb breathes slowly, and the waveform is flat
- [ ] Mic and speaker buttons are disabled with the explanation underneath. The voice button opens the Voice tab
- [ ] Bottom navigation switches Home / Voice / Settings, and Back from Voice or Settings returns to Home
- [ ] Settings → Theme: Light and Follow phone both apply instantly and survive an app restart
- [ ] Settings → Reduce motion: the orb stops moving on Home
- [ ] Settings → Orb animation preview: all 9 states look different. Listening/Speaking size follows the slider
- [ ] Phone language বাংলা: all screens show Bengali text with no broken conjuncts
- [ ] No permissions are listed for the app

## Physical-device checklist: Milestone C
Uninstall the previous build first (debug signing key changes per build).
- [ ] Status bar time and battery are clearly readable in the dark theme
- [ ] Home chip says "Gemini not set up". Tapping it opens the Gemini API screen
- [ ] Taking a screenshot on the Gemini API screen is blocked
- [ ] Paste a wrong key (e.g. delete a few characters) → Save and test → "Google rejected the key" (record the exact message shown)
- [ ] Paste the real key → Save and test → "Connected…". The field clears, and the key is not shown again
- [ ] Wi-Fi and data off → Test connection → "No internet connection"
- [ ] Free-only on: only 2 models listed. Off: 4 models. Turning it back on moves a 3.8 selection back to the default
- [ ] Close and reopen the app: key still saved, Home chip says "Gemini key saved"
- [ ] Remove key → confirm → chip returns to "Gemini not set up"
- [ ] Bengali language: the API screen text is all Bengali

## Physical-device checklist: Milestone D (first real conversation)
Uninstall the previous build, install build 5, then add the API key again (Settings → API key → Save and test).
- [ ] Tap the mic: the disclosure dialog appears once, then Android asks for microphone permission
- [ ] Denying permission shows the microphone-permission error card
- [ ] After allowing: orb goes Connecting → Listening, chip says "Connected", timer runs, waveform moves with **your** voice
- [ ] Say "আসসালামু আলাইকুম, কেমন আছো?": orb goes Thinking → Speaking, and she answers **out loud in Bengali**. Captions appear under "This conversation"
- [ ] Bengali pronunciation and Bengali–English mixing sound natural (write a short note)
- [ ] Talk over her while she speaks: she stops (Interrupted), then listens again
- [ ] Tap the orb while she speaks: she goes silent at once
- [ ] Speaker button switches between loudspeaker and earpiece. With Bluetooth earphones, test that audio goes to them when the speaker is off
- [ ] No echo loop (she doesn't answer her own voice). If she does, turn off Settings → Conversation → "Interrupt by speaking" and retest
- [ ] Tap ✕: mic stops immediately, orb returns to Resting, and the status-bar mic indicator turns off
- [ ] Press Home during a conversation: it stops. Coming back shows "stopped when you left the app"
- [ ] Wi-Fi/data off mid-conversation: Reconnecting, then a clear error. No crash
- [ ] Stay silent for 60 s: the session ends with the "quiet moment" message
- [ ] Usage line on Home increases after a conversation and survives an app restart
- [ ] If any error card appears, copy its **Details** line and send it
