# StudIN V2 – Initial repository audit

_Date: 12 Nov 2026 · Baseline commit `cb7d12f` (branch `ccr-179656fe-z0b0rf`, identical to `main`)._

## 1. Baseline (recorded before any V2 change)

| Command | Result |
|---|---|
| `./gradlew :core:domain:test` | ✅ 31 tests, 0 failures |
| `./gradlew :app:testDebugUnitTest` (Robolectric `ScreenshotTour`) | ✅ 1 test, renders 13 screens |
| `./gradlew :app:assembleDebug` | ✅ BUILD SUCCESSFUL (2 m 40 s cold-ish) |
| `./gradlew :app:assembleRelease` (previous session) | ✅ 71 MB minified APK |

Environment limitation: no KVM → no Android emulator. Visual validation is done with
Robolectric native graphics + Roborazzi (renders the real Compose tree on the JVM).

## 2. Architecture

* **Modules**: `:core:domain` (pure Kotlin + 1 Java file, JVM) and `:app` (Android).
* **Languages**: 74 Kotlin files, 2 Java files (`TextNormalizer.java`, `UsageStatsBridge.java`).
* **UI**: 100 % Jetpack Compose + Material 3; no XML layouts. Only XML: manifest, themes,
  drawables, accessibility config.
* **Navigation**: Navigation-Compose string routes (`ui/navigation/Routes.kt`), single `NavHost`
  in `StudinNavHost.kt`. The bottom bar is a **custom floating overlay** drawn on top of the
  `NavHost`; screens compensate with a magic `BottomBarPadding = 110.dp`.
* **State**: `@HiltViewModel` + `StateFlow` + `collectAsStateWithLifecycle` everywhere.
* **DI**: Hilt (`AppModule`, `AppEntryPoint`), `@HiltWorker` for reminders.
* **Persistence**: Room v1 (8 entities, schema exported), DataStore preferences (settings +
  focus snapshot).
* **Platform**: `FocusController` (state machine owner), `FocusService` (FGS specialUse),
  `StudinAccessibilityService` (Focus Guard), `GateActivity`, `BootReceiver`, `ReminderWorker`.
* **AI**: `StudyAssistant` → `OnDeviceLlm` (MediaPipe, optional) / `LiteAi` (always);
  `OcrEngine` (ML Kit); `ClassifierProvider` (tier 0/1).

## 3. Feature inventory (all must be preserved)
Onboarding (exam, goal, privacy, notifications) · Home dashboard (countdown, today ring, streak,
due cards, plan, quick actions) · Focus timer (presets, custom, topic picker, pause/resume/finish/
stop, confetti, today list) · App rules (Allow/Smart/Block, suggested setup, keyword lists) ·
Focus Guard + usage-stats fallback · Break gate (MCQ, breathing, emergency) · Study (decks,
syllabus, priority, mastery, focus-on-topic) · Deck detail (CRUD, rename, export CSV) · Review
(SM-2, flip) · Quiz (MCQ) · CSV import · AI card generation (notes, OCR) · AI Tutor (chat,
content check, model import/load/delete) · Insights (week chart, 12-week heatmap, accuracy,
backlog, subjects) · Settings (profile, focus, appearance, motivation, export/delete) ·
Permission health · Privacy policy · Reminders.

## 4. Screen hierarchy
```
Onboarding ─► Home ─┬─ Settings ─┬─ Permission health
                    │            └─ Privacy
  Bottom tabs:      ├─ Focus ── Rules
  Home/Focus/Study/ ├─ Study ─┬─ Deck ─ Review / Quiz / Generate
  AI Tutor/Insights │         ├─ Review · Quiz · Import · Generate
                    ├─ AI Tutor (Tutor | Content check | Model)
                    └─ Insights
GateActivity (separate task, launched by Focus Guard / service)
```

## 5. Dependencies (unchanged by V2)
AGP 8.13.2, Kotlin 2.2.21, KSP, Compose BOM 2025.11.01, Material 3, Navigation 2.9.6,
Lifecycle 2.9.4, Room 2.8.4, DataStore 1.1.7, WorkManager 2.10.5, Hilt 2.57.2,
MediaPipe tasks-genai 0.10.27, ML Kit text-recognition 16.0.1; tests: JUnit 4, Robolectric 4.16,
Roborazzi 1.79.0, Compose UI test.

## 6. UI inconsistencies found
| # | Defect | Evidence |
|---|---|---|
| 1 | Floating bottom bar overlays scrollable content; magic 110 dp bottom padding | `StudinNavHost.kt`, 5 screens use `BottomBarPadding` |
| 2 | Dark-only neon/glass look (glow orbs, blur, gradient buttons, glass cards) | `GradientBackground`, `GlassCard`, `GradientButton`, 15 `blur`/infinite animations |
| 3 | Raw colours inside screens (160 usages of `StudinColors.*` / `Color(0x…)` in 17 feature files) | grep |
| 4 | 14 different corner radii (3–32 dp) | grep `RoundedCornerShape` |
| 5 | Oversized hero/ring cards, giant 220 dp timer ring, large empty AI Tutor canvas | screenshots `02_home`, `03_focus`, `08_ai` |
| 6 | Duration presets wrap the 90-min chip onto its own row | `03_focus` |
| 7 | No navigation rail / width adaptation for tablets & foldables | `StudinNavHost.kt` |
| 8 | Light theme exists but is not the default and was never validated | `Theme.kt` |
| 9 | Particle confetti + infinite glow animations; reduced-motion not respected | `Confetti.kt`, `Components.kt` |
| 10 | Logo is a raster PNG (old navy/mint art) – blurry at some densities, no themed icon art | `drawable-nodpi/studin_app_icon.png` |
| 11 | Onboarding is text-heavy; no imagery explaining what the app does | `OnboardingScreen.kt` |

## 7. Technical debt
* Components are loosely named (`GlassCard`, `Pill`, …) with arbitrary sizes; no spacing tokens.
* `hiltViewModel` import from the deprecated package (warning only).
* Insights only supports a fixed 7-day/12-week view (no period filter).
* AI Tutor has no copy / retry affordances.

## 8. Critical risks
* `FocusController`, `FocusService`, `StudinAccessibilityService`, `GateActivity` logic must
  not change behaviour – only presentation.
* Room schema must stay at v1 (no data migration needed for a UI redesign).
* Hilt graph and DataStore keys must remain compatible with installed data.

## 9. Test coverage
* Domain: 31 unit/property/fuzz tests (state machine, policy, classifier, SM-2, CSV, quiz,
  planner, streaks, Lite AI).
* App: 1 Robolectric screenshot smoke test. No Compose interaction tests, no UI-state unit tests.

## 10. Migration strategy
1. Replace design tokens (colour, type, spacing, shape, motion) in `core/designsystem`.
2. Build `StudIN*` components; keep the public screen signatures/route contracts.
3. Replace the overlay bar with a Material 3 `Scaffold` + `NavigationBar` / `NavigationRail`
   (window-width based) so insets are applied once by the scaffold.
4. Rebuild each screen's presentation layer on the new components, keeping ViewModels and
   repositories (add UI-only state like the Insights period filter).
5. New vector logo + onboarding illustrations.
6. Tests: UI-state unit tests, Compose interaction tests, screenshot matrix.

## 11. Files expected to change
`core/designsystem/*` (rewrite), `ui/navigation/StudinNavHost.kt`, `MainActivity.kt`,
all `feature/**/*Screen.kt` (+ small ViewModel additions), `platform/gate/GateActivity.kt`
(presentation), `res/values*/themes.xml`, `res/drawable*/*`, `res/mipmap*`, docs, tests.
Logic files (`core/data`, `ai`, `platform/focus`, `platform/accessibility`, `core/domain`) are
out of scope except for additive, presentation-driven helpers.
