# Testing

## Automated
| Suite | Command (Windows) | Contents |
|---|---|---|
| Unit | `gradlew.bat testDebugUnitTest` | `PcmAudioFormatTest` (5 tests) |
| Lint | `gradlew.bat lintDebug` | Report: `app\build\reports\lint-results-debug.html` |
| Instrumented | `gradlew.bat connectedDebugAndroidTest` | None yet (added with Milestone B UI) |

## Results log
| Date | Where | Command | Result |
|---|---|---|---|
| 2026-11-24 | Cloud workspace | Gradle wrapper generation | ✅ |
| 2026-11-24 | Cloud workspace | XML well-formedness of manifest and resources | ✅ |
| — | GitHub Actions | `testDebugUnitTest` | ⏳ pending first run |
| — | GitHub Actions | `lintDebug` | ⏳ pending first run |
| — | GitHub Actions | `assembleDebug` / `assembleRelease` | ⏳ pending first run |

## Physical-device checklist: Milestone A
- [ ] APK from GitHub Releases downloads and installs
- [ ] Launcher shows "Emotional Companion" with the orb icon
- [ ] App opens on the dark foundation screen and doesn't crash
- [ ] With phone language set to বাংলা, the Bengali text renders with no broken conjuncts or boxes
- [ ] Content doesn't sit under the status or navigation bars
- [ ] Settings → Apps → Emotional Companion → Permissions shows **no permissions**
