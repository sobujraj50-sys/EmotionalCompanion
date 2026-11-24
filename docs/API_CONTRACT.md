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

## Live WebSocket: verified 2026-11-24 (used in Milestone D)
Sources: https://ai.google.dev/api/live · https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket · https://ai.google.dev/gemini-api/docs/live-guide · https://ai.google.dev/gemini-api/docs/live-session

| Topic | Fact |
|---|---|
| Endpoint | `wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=API_KEY` |
| First message | `{"setup": {...}}`. Wait for `setupComplete` before sending anything else |
| Setup fields used | `model: "models/{id}"`, `generationConfig.responseModalities: ["AUDIO"]`, `generationConfig.speechConfig.voiceConfig.prebuiltVoiceConfig.voiceName`, `systemInstruction.parts[].text`, `inputAudioTranscription: {}`, `outputAudioTranscription: {}`, `contextWindowCompression.slidingWindow: {}`, `sessionResumption.handle` |
| Language | No language-code field. Native audio picks the language automatically. Steer it with the system instruction. Bengali is listed as `bn` |
| Audio in | `{"realtimeInput":{"audio":{"data":"<base64>","mimeType":"audio/pcm;rate=16000"}}}` (16-bit LE PCM). `audioStreamEnd` only with automatic VAD |
| Audio out | `serverContent.modelTurn.parts[].inlineData.data`, raw 16-bit LE PCM at 24 kHz |
| Turn signals | `serverContent.interrupted` (stop and clear playback), `generationComplete`, `turnComplete` |
| Captions | `serverContent.inputTranscription.text`, `serverContent.outputTranscription.text` |
| Limits | Audio-only session 15 min without compression (unlimited with it). Connection ≈10 min. Resumption handle valid 2 h |
| Lifecycle | `goAway.timeLeft` before close. `sessionResumptionUpdate.newHandle` + `resumable` |
| Voices | Prebuilt list (30) on the speech-generation page, including Sulafat (Warm), Achernar (Soft), Vindemiatrix (Gentle), Aoede (Breezy), Leda (Youthful), Kore (Firm) |
| Not on 3.1 Flash Live | Affective dialog, proactive audio |

| Text input | `{"realtimeInput":{"text":"..."}}` (WebSockets guide). Used for voice auditions |

### Open points (confirm on device)
- Placement of `responseModalities`/`speechConfig`: the reference has them under `generationConfig`, while the guide's sample has `responseModalities` at the top of `setup`. The reference is used.
- Close codes/reasons for invalid key, quota and invalid setup are not documented. They're classified loosely and the raw reason is shown.
- Whether a Live model reliably reads an audition line verbatim (the audition instruction asks it to).
- Free-tier rate limits for Live (concurrent sessions, requests/min) are not checked yet.
