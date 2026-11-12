# StudIN V2 — test report

Command: `./gradlew :core:domain:test :app:testDebugUnitTest` (JDK 21, Robolectric 4.16, SDK 35).
Result: **96 tests, 0 failures** (12 Nov 2026).

| Suite | Tests | Result | Covers |
|---|---|---|---|
| `com.studin.domain.*` (7 classes) | 31 | ✅ | state machine (incl. property test), policy precedence, classifier, SM-2, CSV fuzz, quiz, planner, streaks, Lite AI |
| `DesignSystemTest` | 5 | ✅ | spacing on 4 dp grid, radii, motion ranges, chart axis steps, WCAG contrast of palette |
| `ThemePreferenceTest` (Robolectric) | 1 | ✅ | theme default SYSTEM + persistence of all modes |
| `OnboardingTest` | 2 | ✅ | goal snapping, intro slide content & image descriptions |
| `HomeUiTest` | 8 | ✅ | greeting, initials, continue-block selection, plan remainder, formatting |
| `FocusUiTest` | 8 | ✅ | presets, slider snapping, state labels, timer text, ring progress |
| `StudyUiModelsTest` | 11 | ✅ | subject aggregation, next topic, priority cycle, history, review summary, due labels, quiz verdicts |
| `AiPresentationTest` | 8 | ✅ | engine-status mapping, message formatting (bullets/numbers/code) |
| `InsightsMathTest` | 12 | ✅ | 7D/30D/12W bucketing, goal scaling, consistency, observations, heatmap mapping |
| `SettingsPresentationTest` | 6 | ✅ | settings labels/formatting |
| `A11yRulesControlTest` (Robolectric) | 2 | ✅ | audit rules detect bad controls / accept good ones |
| `AccessibilityAuditTest` (Robolectric, real app) | 1 | ✅ | 39 renders, labels / targets / headings |
| `ScreenshotTour` (Robolectric + Roborazzi, real app) | 1 | ✅ | 60 captures; drives onboarding, empty states, focus start/pause/resume/finish, AI quick action, Insights filters, review reveal, gate |

Compose UI interactions exercised end-to-end through the real UI: onboarding Next/Skip/exam
selection/Continue/Start studying, navigation by route, focus lifecycle, "Show answer",
"Plan my revision", Insights "30D"/"12W", gate launch.

Build gates: `assembleDebug` ✅, `assembleRelease` (R8) ✅, `lintDebug` ✅ 0 errors.

## Not executed
Instrumented tests on a device/emulator, TalkBack, soft-keyboard, Macrobenchmark (no KVM).
Robolectric's monotonic clock does not advance, so screenshots of the active timer show 25:00 and
the completed card shows 0 min — timing itself is covered by `FocusStateMachineTest`.

Local note: this environment reaches Maven Central only through a mirror, so tests were run with
`-Probolectric.repo=https://maven-central.storage-download.googleapis.com/maven2/`; CI does not need it.
