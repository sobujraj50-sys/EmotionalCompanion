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

## Models API: verified 2026-11-24
Source: https://ai.google.dev/api/models
| Topic | Fact | Status |
|---|---|---|
| Get model | `GET https://generativelanguage.googleapis.com/v1beta/models/{model}` | Verified, used by the connection test |
| Auth | `?key=API_KEY` query parameter (the page's REST examples). No header option is documented there | Verified |
| Model fields | `name`, `displayName`, `supportedGenerationMethods`, `inputTokenLimit`, … | Verified (not parsed yet) |
| HTTP error mapping (400/401/403/404/429/5xx) | General HTTP semantics. Google's exact error bodies are not documented on that page | Assumption, to confirm with a real invalid key on the phone |

## Free tier: verified 2026-11-24
Source: https://ai.google.dev/gemini-api/docs/pricing
- Free of charge on the free tier: Gemini 3.1 Flash Live Preview, Gemini 2.5 Flash Native Audio (Live), Gemini 3.1 Flash TTS Preview, Gemini 2.5 Flash Preview TTS.
- Not free: Gemini 2.5 Pro Preview TTS.
- `gemini-3.8-live` / `-extended-thinking` / `3.8-flash-tts`: **not on the pricing page**, so treated as possibly paid and hidden in Free-only mode.
- Free-tier content **is used to improve Google's products**. Paid-tier content is not.

## Pending: must be confirmed in Milestone D
- Exact WebSocket endpoint URL and API version path. The Live overview page didn't list it; check the Live API reference / WebSockets guide.
- Setup message schema: model field, `generationConfig`, response modalities, `speechConfig` / voice-name field names.
- Prebuilt voice catalog per model, including whether Sulafat, Achernar, Aoede, Leda, Vindemiatrix and Kore are available on `gemini-3.8-live`.
- Bengali (`bn-BD`) support for native-audio output, and how language is set.
- Mono channel count (assumed in `PcmAudioFormat`).
- Voice activity detection config, interruption signal, turn-complete signal.
- Session duration limits, context-window compression, session resumption and GoAway handling.
- Error and close codes for quota exhaustion and invalid key.
- Free-tier **rate limits** (requests/minute, concurrent sessions) for the Live models (rate-limits page). Google AI Pro is a consumer subscription and is **not** assumed to include API quota.
