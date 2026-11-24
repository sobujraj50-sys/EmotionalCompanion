# Architecture

## Shape
Single Gradle module (`:app`), layered by package. MVVM with unidirectional data flow: screens observe a `StateFlow<UiState>` from a ViewModel. ViewModels call domain use cases and interfaces, and implementations live in `data/`, `ai/` and `core/`.

```
com.emotionalcompanion
├── core/
│   ├── designsystem/   colors, type, theme, icons, VoiceOrb, waveform [A ✅ · B ✅]
│   ├── audio/          PCM format, capture, playback, focus           [A: PcmAudioFormat ✅ · D]
│   ├── network/        WebSocket client wrapper                       [D]
│   ├── security/       SecretStore, KeystoreSecretStore, ApiKeyValidator [C ✅]
│   └── common/         AppContainer (DI), dispatchers, result types   [A ✅]
├── ai/
│   ├── provider/       Provider-neutral interfaces (LiveVoiceProvider)[D]
│   ├── gemini/         model catalog, connection tester [C ✅] · Live client [D]
│   ├── personality/    PersonalityEngine → system instruction         [E]
│   └── emotion/        Emotional preference model                     [E]
├── data/settings      DataStore UI preferences                       [B ✅]
├── domain/conversation ConversationState, ConnectionStatus           [B ✅]
├── features/           onboarding, home, voicestudio (stub notice), settings [B ✅] · conversation, personality [D/E]
├── navigation/         bottom-bar NavHost                             [B ✅]
└── service/            foreground mic service (future phase)
```
Packages appear only when they get real code (CLAUDE.md rule).

## Replaceability
- `LiveVoiceProvider` (D) is the only thing the conversation feature knows about. Gemini Live is one implementation. Gemini TTS and Android `TextToSpeech` become fallback `SpeechOutput` implementations.
- Audio capture and playback sit behind small interfaces so half-duplex and full-duplex strategies can be swapped (spec §13).

## Dependency injection
Manual constructor injection through `AppContainer`, created in `EmotionalCompanionApp`. ViewModels get dependencies through a `ViewModelProvider.Factory`. Reason: Hilt/KSP must be version-locked to KGP 2.4.20, and that couldn't be verified when Milestone A was written. Migration to Hilt is mechanical if needed later.

## Dependency choices (2026-11-24)
The project builds on **GitHub Actions**, not on a local PC. So it uses the exact toolchain of the user's NexusNotes app, which already builds there and runs on his phone. That is proven, which matters more here than being the newest.

| Item | Version | Note |
|---|---|---|
| Android Gradle Plugin | 9.2.1 | AGP 9.2 needs Gradle ≥ 9.4.1 and supports up to API 37 (AGP 9.2 release notes) |
| Gradle | 9.4.1 | wrapper |
| Kotlin / Compose compiler plugin | 2.3.20 | AGP 9 built-in Kotlin. Only `org.jetbrains.kotlin.plugin.compose` is applied |
| Compose BOM | 2026.08.00 | Compose 1.12 requires compileSdk 37 |
| core-ktx / activity / lifecycle | 1.19.1 / 1.13.0 / 2.11.1 | androidx versions page |
| junit / ext-junit / espresso | 4.13.2 / 1.3.0 / 3.7.0 | |

Newer releases exist (AGP 9.5.0 with Gradle 9.6.0, Kotlin 2.4.20, BOM 2026.11.01). Upgrade in a dedicated commit once a milestone is green, one step at a time.

## SDK levels
- `minSdk 26`: adaptive icons without PNG fallbacks. `AcousticEchoCanceler`/`NoiseSuppressor` and `AudioRecord` behavior are consistent from here.
- `compileSdk 37` (required by Compose 1.12), `targetSdk 36`. The CI runner downloads platform 37 automatically after the SDK licenses are accepted.
