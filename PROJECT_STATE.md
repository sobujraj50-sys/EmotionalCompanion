# PROJECT_STATE

_Last updated: 2026-11-24, Milestone A (foundation)_

## Current status
| Milestone | State |
|---|---|
| A: Environment & foundation | **Code complete. Build not yet run** (see Blockers) |
| B: Design & navigation | Not started |
| C: Secure API configuration | Not started |
| D: Live audio prototype | Not started |
| E: Emotional Voice Studio | Not started |
| F: Testing & APK | Not started |

## Milestone A: what exists
- Gradle Kotlin DSL project with version catalog (`gradle/libs.versions.toml`)
- Gradle wrapper 9.6.0 (`gradlew`, `gradlew.bat`, `gradle/wrapper/`)
- Single `:app` module, `minSdk 26`, `compileSdk`/`targetSdk 36`, Java 17
- Design-system colors, typography and dark/light themes (`core/designsystem`)
- Static foundation screen in English and Bengali, with an AI-disclosure line
- `PcmAudioFormat` (verified Gemini Live PCM rates) with 5 unit tests
- Manual DI container (`AppContainer`)
- Backup and device transfer disabled for all app data
- No permissions requested
- `.gitignore` covering `local.properties`, keystores, secrets and build outputs
- Docs: README, CLAUDE.md, this file, `docs/*`

## Verification record
| Check | Where | Result |
|---|---|---|
| Environment inspection | Claude's cloud workspace | JDK 21, Gradle 8.14.3, **no Android SDK, no network for dependency downloads** |
| Wrapper generation | Cloud workspace (`gradle wrapper --gradle-version 9.6.0 --offline`) | ✅ Succeeded |
| XML well-formedness | Cloud workspace | ✅ See commit log / TESTING.md |
| `gradlew.bat assembleDebug` | User's Windows PC | ⏳ **Not yet run** |
| `gradlew.bat testDebugUnitTest` | User's Windows PC | ⏳ **Not yet run** |
| `gradlew.bat lintDebug` | User's Windows PC | ⏳ **Not yet run** |
| Install & launch on phone | Physical device | ⏳ **Not yet run** |

Milestone A is **not** done until the three Gradle commands above pass on the user's machine and the output is recorded here.

## Decisions
1. **AGP 9.5.0 + Gradle 9.6.0 + Kotlin 2.4.20.** These are the newest stable versions on the official release pages as of 2026-11-24. AGP 9's built-in Kotlin is used, so `org.jetbrains.kotlin.android` is not applied. KGP 2.4.20 is pinned through the root buildscript classpath, as the AGP 9 notes describe.
2. **Manual DI instead of Hilt (for now).** Hilt needs KSP version-locked to KGP. That could not be verified offline, and a small single-module app doesn't need it yet. Revisit if the dependency graph grows.
3. **Room deferred to the milestone that first stores data.** It also needs KSP, and the spec forbids empty placeholder modules.
4. **Single `:app` module with package-level layering.** "Start manageable" (spec §5). Split into Gradle modules later only if build times or boundaries require it.
5. **Debug `applicationIdSuffix = ".debug"`**, so debug and release builds can coexist on the phone.

## Blockers / open questions
- **Build not run.** Claude's workspace has no Android SDK and can't download dependencies. The first real build happens on the user's PC. Paste the output back so failures can be fixed at the root.
- **Android Studio version unknown.** AGP 9.5 needs a recent Studio. If sync rejects it, update Studio first (README step 2).
- **Compose BOM 2026.11.01.** This is the newest entry on the official BOM mapping page, but the stable/beta label could not be confirmed from the fetched text. If Gradle can't resolve it, use the previous BOM listed on that page.
- **Test library versions** (junit 4.13.2, androidx.test.ext:junit 1.3.0, espresso 3.7.0) are long-standing stable releases, not confirmed as the newest.

## Next milestone
**B: Design & navigation.** Onboarding, Home Screen layout, bottom navigation, voice-orb state machine and animation (driven by real state only), Settings foundation, reduced-motion option.
