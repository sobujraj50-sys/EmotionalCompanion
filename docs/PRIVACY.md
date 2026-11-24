# Privacy and data handling

## Principles
- The microphone is on only when the user starts it, and its state is always visible. One tap stops it immediately.
- No hidden recording and no continuous ambient upload. Audio goes to Gemini only during an active session the user started.
- Before the first session, the app clearly states that audio is sent to Google's Gemini API.
- Conversation retention is opt-in. Retained data can be deleted from Settings.
- Private relationship details (nicknames, personal notes) go into the system instruction only through settings the user controls and can see.
- Logs never contain the API key, raw audio, or transcript text.

## Current state (Milestone A)
- No permissions requested. No network or microphone code.
- `allowBackup="false"` plus data-extraction rules exclude every domain from cloud backup and device transfer.

## API key (from Milestone C)
- Entered in-app, encrypted with an AES key held in Android Keystore, and stored in app-private storage.
- **Limitation:** with a direct client-side key, anyone with root or debug access to the phone could recover it in use. That is acceptable for a personal APK on the user's own device, but not for distribution. A distributed build must use a backend or Gemini **ephemeral tokens** (recommended by Google's Live API docs).

## Provider data handling
Google's data-use terms differ between free and paid Gemini API tiers. Free-tier content may be used to improve Google products. The exact current wording will be quoted with date in Milestone C from https://ai.google.dev/gemini-api/terms, before any audio is sent.
