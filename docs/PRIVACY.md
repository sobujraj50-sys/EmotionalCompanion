# Privacy and data handling

## Principles
- The microphone is on only when the user starts it, and its state is always visible. One tap stops it immediately.
- No hidden recording and no continuous ambient upload. Audio goes to Gemini only during an active session the user started.
- Before the first session, the app clearly states that audio is sent to Google's Gemini API.
- Conversation retention is opt-in. Retained data can be deleted from Settings.
- Private relationship details (nicknames, personal notes) go into the system instruction only through settings the user controls and can see.
- Logs never contain the API key, raw audio, or transcript text.

## Current state (Milestone D)
- Permissions: `INTERNET`, `RECORD_AUDIO` (asked at first conversation), `MODIFY_AUDIO_SETTINGS` (audio routing).
- Before the first conversation, a dialog explains that voice is streamed to Google while a conversation runs, and that free-tier data may be used to improve Google's products.
- The microphone runs **only** between the user's tap on the mic and the stop tap. The session also ends on leaving the app, losing audio focus, the session limit, the daily budget, or silence. No background or ambient capture, and no foreground service.
- Audio chunks go only to the Gemini Live WebSocket. Nothing is written to disk.
- Captions exist only in memory for the current app run (last 6 lines) and are never stored.
- Only local usage minutes (one number per day) are stored.
- `allowBackup="false"` plus data-extraction rules exclude every domain from cloud backup and device transfer.

## API key
- Entered in-app, encrypted (AES-256-GCM) with a key held in Android Keystore, and stored in app-private storage that backups exclude.
- The key is never shown again after saving, never logged, and the API screen sets `FLAG_SECURE` (no screenshots or screen recording).
- The key travels as a `?key=` URL parameter over HTTPS (Google's documented method). The URL and exception messages are never logged.
- **Limitation:** with a direct client-side key, anyone with root or debug access to the phone could recover it in use. That is acceptable for a personal APK on the user's own device, but not for distribution. A distributed build must use a backend or Gemini **ephemeral tokens** (recommended by Google's Live API docs).

## Provider data handling
Per Google's pricing page (checked 2026-11-24), content sent on the **free tier is used to improve Google's products**, and paid-tier content is not. The app says this on the API screen. Before Milestone D sends any audio, the onboarding/conversation disclosure must repeat it.
