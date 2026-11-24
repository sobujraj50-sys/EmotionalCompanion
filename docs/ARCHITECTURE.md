# Architecture

## Shape
Single Gradle module (`:app`), layered by package. MVVM with unidirectional data flow: screens observe a `StateFlow<UiState>` from a ViewModel. ViewModels call domain use cases and interfaces, and implementations live in `data/`, `ai/` and `core/`.

```
com.emotionalcompanion
├── core/
│   ├── designsystem/   colors, type, theme, shared components        [A ✅]
│   ├── audio/          PCM format, capture, playback, focus           [A: PcmAudioFormat ✅ · D]
│   ├── network/        WebSocket client wrapper                       [D]
│   ├── security/       Keystore-backed secret storage                 [C]
│   └── common/         AppContainer (DI), dispatchers, result types   [A ✅]
├── ai/
│   ├── provider/       Provider-neutral interfaces (LiveVoiceProvider)[D]
│   ├── gemini/         Gemini Live implementation                     [D]
│   ├── personality/    PersonalityEngine → system instruction         [E]
│   └── emotion/        Emotional preference model                     [E]
├── data/ · domain/     repositories, models, use cases                [C+]
├── features/           home, onboarding, conversation, voicestudio, personality, settings  [A: foundation screen · B+]
├── navigation/                                                        [B]
└── service/            foreground mic service (future phase)
```
Packages appear only when they get real code (CLAUDE.md rule).

## Replaceability
- `LiveVoiceProvider` (D) is the only thing the conversation feature knows about. Gemini Live is one implementation. Gemini TTS and Android `TextToSpeech` become fallback `SpeechOutput` implementations.
- Audio capture and playback sit behind small interfaces so half-duplex and full-duplex strategies can be swapped (spec §13).

## Dependency injection
Manual constructor injection through `AppContainer`, created in `EmotionalCompanionApp`. ViewModels get dependencies through a `ViewModelProvider.Factory`. Reason: Hilt/KSP must be version-locked to KGP 2.4.20, and that couldn't be verified when Milestone A was written. Migration to Hilt is mechanical if needed later.

## Dependency verification (2026-11-24)
| Item | Version | Source |
|---|---|---|
| Android Gradle Plugin | 9.5.0 | developer.android.com/build/releases/gradle-plugin (Oct 2026) |
| Gradle | 9.6.0 | AGP 9.5 compatibility table (minimum = default = 9.6.0) |
| Build-Tools | 36.0.0 | AGP 9.5 compatibility table |
| JDK | 17+ | AGP 9.5 compatibility table |
| Kotlin / Compose compiler plugin | 2.4.20 | kotlinlang.org "What's new in Kotlin 2.4.20" (Sept 2026) |
| Compose BOM | 2026.11.01 | developer.android.com Compose BOM mapping page (stability label not confirmed) |
| core-ktx | 1.19.1 | developer.android.com/jetpack/androidx/versions |
| activity-compose | 1.13.0 | same |
| lifecycle | 2.11.1 | same |
| junit / ext-junit / espresso | 4.13.2 / 1.3.0 / 3.7.0 | long-standing stable releases (not checked as newest) |

AGP 9 built-in Kotlin: AGP ships a runtime dependency on KGP 2.2.10. A newer KGP is chosen by adding `kotlin-gradle-plugin` to the root `buildscript` classpath (AGP 9.0 release notes). Only `org.jetbrains.kotlin.plugin.compose` is applied in `:app`.

## SDK levels
- `minSdk 26`: adaptive icons without PNG fallbacks. `AcousticEchoCanceler`/`NoiseSuppressor` and `AudioRecord` behavior are consistent from here.
- `compileSdk`/`targetSdk 36`: AGP 9.5 supports up to 37. 36 is chosen as the widely installed stable platform. Raise it once the SDK is installed and tested.
