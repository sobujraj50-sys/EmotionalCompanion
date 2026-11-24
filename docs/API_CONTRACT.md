# Gemini API contract

Every fact here carries a status. **Verified** = read on official docs, with date. **Pending** = must be confirmed before code depends on it. Implementation (Milestone D) may only rely on Verified rows.

## Live API: verified 2026-11-24
Source: https://ai.google.dev/gemini-api/docs/live and https://ai.google.dev/gemini-api/docs/models

| Topic | Fact | Status |
|---|---|---|
| Transport | Stateful WebSocket (WSS) | Verified |
| Input audio | Raw 16-bit PCM, 16 kHz, little-endian | Verified |
| Output audio | Raw 16-bit PCM, 24 kHz, little-endian | Verified |
| Other input | Text; JPEG images up to 1 FPS | Verified |
| Auth | API key supported. Docs recommend **ephemeral tokens** for client apps in production | Verified |
| Stable Live models | `gemini-3.8-live`, `gemini-3.8-live-extended-thinking` | Verified (models page) |
| Preview / legacy Live models | `gemini-3.5-live-translate-preview`, `gemini-3.1-flash-live-preview` (legacy), `gemini-2.5-flash-native-audio-preview-12-2025` (2.5 access limited to prior users) | Verified (models page) |
| TTS models | `gemini-3.8-flash-tts`, `gemini-3.8-flash-lite-tts` (stable). `gemini-3.1-flash-tts-preview` (legacy) | Verified (models page) |

## Pending: must be confirmed in Milestone D
- Exact WebSocket endpoint URL and API version path. The Live overview page didn't list it; check the Live API reference / WebSockets guide.
- Setup message schema: model field, `generationConfig`, response modalities, `speechConfig` / voice-name field names.
- Prebuilt voice catalog per model, including whether Sulafat, Achernar, Aoede, Leda, Vindemiatrix and Kore are available on `gemini-3.8-live`.
- Bengali (`bn-BD`) support for native-audio output, and how language is set.
- Mono channel count (assumed in `PcmAudioFormat`).
- Voice activity detection config, interruption signal, turn-complete signal.
- Session duration limits, context-window compression, session resumption and GoAway handling.
- Error and close codes for quota exhaustion and invalid key.
- **Free tier:** which of the above models are free-tier eligible, and their rate limits (pricing page). Google AI Pro is a consumer subscription and is **not** assumed to include API quota.
