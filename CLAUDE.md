# CLAUDE.md: standing development rules

Read `PROJECT_STATE.md` first in every session. Update it at the end of every milestone.

## Product guardrails (non-negotiable)
- The companion is transparently an AI. It never claims or implies it is the user's real wife or any real person.
- It never fabricates shared memories, expresses jealousy, or manipulates attachment. It supports the user's real relationship.
- It does not claim certainty about the user's emotions.

## Engineering rules
- Never invent model IDs, API fields, SDK methods, dependency versions, or build results. Verify against official docs and record the source and date in `docs/API_CONTRACT.md` or `docs/ARCHITECTURE.md`.
- Never report a build or test as passing without real command output. Record results in `PROJECT_STATE.md`.
- Never hardcode, log, commit or document an API key. Nothing secret goes in `BuildConfig`, `local.properties` or the docs.
- No fake functionality: no placeholder screens that pretend to work, and no simulated audio waveforms. Visuals must reflect real state or amplitude.
- Free-only by default: no automatic paid-model switching, no billing assumptions.
- Keep the AI provider and audio engines behind interfaces so they stay replaceable.
- Complete and validate one milestone before starting the next.
- When a build fails: read the full error, find the root cause, apply the smallest fix, rebuild, record. Don't churn unrelated dependencies.

## Conventions
- Package root: `com.emotionalcompanion`. Layers: `core/`, `ai/`, `data/`, `domain/`, `features/<name>/`, `navigation/`, `service/`.
- Create a package only when it gets real code. No empty placeholder classes.
- Kotlin official code style. Compose UI uses `MaterialTheme` tokens from `core/designsystem`, never raw hex in screens.
- User-facing strings go in `res/values/strings.xml`, with Bengali in `res/values-bn/`.
- Windows commands: `gradlew.bat assembleDebug`, `gradlew.bat testDebugUnitTest`, `gradlew.bat lintDebug`.
- Commits: small, one concern each, imperative subject line.
