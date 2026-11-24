# PROJECT_STATE

_Last updated: 2026-11-24, Milestone A (foundation)_

## Current status
| Milestone | State |
|---|---|
| A: Environment & foundation | **Code complete. Waiting for first GitHub Actions build** |
| B: Design & navigation | Not started |
| C: Secure API configuration | Not started |
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

## Verification record
| Check | Where | Result |
|---|---|---|
| Environment inspection | Claude's cloud workspace | JDK 21, Gradle 8.14.3, no Android SDK, no dependency downloads |
| Wrapper generation | Cloud workspace (offline) | ✅ |
| XML / TOML well-formedness, catalog references, secret scan | Cloud workspace | ✅ |
| `testDebugUnitTest`, `lintDebug`, `assemble*` | GitHub Actions | ⏳ **Not yet run** |
| Install & launch on phone | Physical device | ⏳ **Not yet run** |

Milestone A is **not** done until the GitHub Actions run is green and the APK opens on the phone.

## Decisions
1. **Build on GitHub Actions**, the same way as the user's other apps. ZIP upload → unpack → test → lint → APK → Release.
2. **Toolchain copied from NexusNotes** (AGP 9.2.1, Gradle 9.4.1, Kotlin 2.3.20, BOM 2026.08.00, compileSdk 37), because it is proven on CI. Newer versions are deferred to a separate upgrade commit.
3. **Lint doesn't block the APK.** It runs and its report is uploaded, and the Release notes say whether it passed.
4. **Manual DI instead of Hilt (for now).** Fewer moving parts. Revisit if the dependency graph grows.
5. **Room is deferred** to the milestone that first stores data (no empty placeholder modules).
6. **Single `:app` module with package-level layering** ("start manageable", spec §5).

## Blockers / open questions
- The GitHub repository doesn't exist yet. The user creates it, and then the first build runs.
- Signing secrets aren't set. Until they are, each new APK requires uninstalling the previous one.

## Next milestone
**B: Design & navigation.** Onboarding, Home Screen layout, bottom navigation, voice-orb state machine and animation (driven by real state only), Settings foundation, reduced-motion option.
