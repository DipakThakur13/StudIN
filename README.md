<p align="center"><img src="docs/brand_wordmark.png" width="380" alt="StudIN — Focus · Learn · Grow"/></p>
<p align="center"><b>Focus deeper. Remember longer.</b><br/>
Syllabus-aware focus, active-recall breaks and on-device AI for Indian competitive-exam and college students.</p>

<p align="center">
Kotlin · Jetpack Compose · Material 3 · Hilt · Room · DataStore · WorkManager · MediaPipe LLM · ML Kit OCR · <b>100% offline</b>
</p>

---

## ⬇️ Download & test

**[Download the latest test APK (v2.1.0)](output/)** — `StudIN-v2.1.0-arm64.apk` (28.8 MB, recommended for phones) or `StudIN-v2.1.0-universal.apk` (70.7 MB, any device/emulator). Install steps and a test checklist are in [`output/README.md`](output/README.md).

---

## Highlights

| | |
|---|---|
| 🎯 **Focus sessions** | Animated timer with a persisted state machine (`IDLE → PREPARING → ACTIVE ⇄ PAUSED → COMPLETED/ABORTED`) that survives process death and reboots. Foreground timer notification with Pause/Stop actions. |
| 🛡️ **Focus Guard** | Per-app rules: **Allow / Smart / Block**. *Smart* mode reads visible titles (accessibility, opt-in) and runs a syllabus-aware classifier on-device, so a *Kinematics one-shot* stays open while *BGMI funny moments* is gated. |
| 🧠 **Active-recall break gate** | Opening a blocked app shows a gate: answer 0–5 flashcard MCQs for a short break, take a mindful pause, go back to studying, or use the always-available emergency exit. No penalties, no lockouts. |
| 📚 **Syllabus packs** | JEE, NEET, UPSC, GATE CS, CAT, College CS and Boards with weightage, Hindi/Hinglish aliases and starter flashcards. |
| 🔁 **Spaced repetition** | SM-2 scheduler with Again/Hard/Good/Easy, 3D flip cards, MCQ quizzes, CSV import/export with strict validation. |
| ✨ **On-device AI** | *Lite AI* (works on every phone): notes → flashcards, summaries, keyword extraction, retrieval over your own cards, daily plan. *Full AI Tutor*: one tap downloads Google's Gemma 3 1B, which then runs fully offline. *ML Kit OCR*: scan handwritten/printed notes into cards. |
| 📈 **Insights** | 7D / 30D / 12W focus charts, 12-week consistency heatmap, quiz accuracy, review backlog, subject mastery, blocked attempts – with honest notes on what the numbers mean. |
| 🔒 **Privacy** | Nothing you study is ever uploaded. The only network use is the optional, user-started AI Tutor model download. No accounts, ads or analytics (ML Kit telemetry is disabled). Export/delete everything from Settings. |

## StudIN V2 — Warm Academic

<sub>Rendered from the real app (Hilt, Room, DataStore) by the Robolectric screenshot test `ScreenshotTour`. Full matrix (light, dark, compact, large, tablet, 1.3× font) in [`docs/screenshots/v2`](docs/screenshots/v2).</sub>

**Onboarding story** — illustrated slides explain the app before setup:

<p align="center"><img src="docs/screenshots/onboarding_story.jpg" alt="Onboarding: Welcome, Focus, Remember, AI Tutor, Progress, then exam, goal and privacy setup"/></p>

<p align="center"><img src="docs/screenshots/v2/light/home.jpg" width="19%" alt="light/home"/> <img src="docs/screenshots/v2/light/focus.jpg" width="19%" alt="light/focus"/> <img src="docs/screenshots/v2/light/study.jpg" width="19%" alt="light/study"/> <img src="docs/screenshots/v2/light/ai_conversation.jpg" width="19%" alt="light/ai_conversation"/> <img src="docs/screenshots/v2/light/insights.jpg" width="19%" alt="light/insights"/></p>
<p align="center"><img src="docs/screenshots/v2/light/review_revealed.jpg" width="19%" alt="light/review_revealed"/> <img src="docs/screenshots/v2/dark/home.jpg" width="19%" alt="dark/home"/> <img src="docs/screenshots/v2/dark/focus.jpg" width="19%" alt="dark/focus"/> <img src="docs/screenshots/v2/tablet/study.jpg" width="19%" alt="tablet/study"/> <img src="docs/screenshots/v2/light/gate.jpg" width="19%" alt="light/gate"/></p>

**Brand** — "Rising Sun Book" logo and illustrations (vector, sources in [`design/`](design)):

<p align="center"><img src="docs/logo.png" width="96" alt="StudIN app icon"/> &nbsp; <img src="docs/brand_illustrations.png" width="520" alt="StudIN illustrations"/></p>

V1 → V2 comparison: [`docs/screenshots/v1_vs_v2.jpg`](docs/screenshots/v1_vs_v2.jpg).

## On-device AI tiers

| Tier | What | Where |
|---|---|---|
| 0 | Your allow/block keyword lists | `ContentClassifier` |
| 1 | Normalised n-gram lexicon (English + Hinglish + Devanagari) + weights generated from **your** syllabus, calibrated with a logistic link. BLOCK only at high confidence with ≥ 2 signals; uncertain content is allowed. | `core/domain/.../classifier` |
| Lite | Extractive flashcards, summarisation, BM25 retrieval, study planner | `core/domain/.../ai/LiteAi.kt` |
| 2 (optional) | MediaPipe LLM Inference with **Gemma 3 1B IT int4**, downloaded in-app from Hugging Face (pinned + SHA-256 verified) on 64-bit phones with ≥ 4 GB RAM; power-aware | `app/.../ai/OnDeviceLlm.kt`, `ModelDownloader.kt` |

### Getting the full AI Tutor (Gemma 3 1B)
Open **AI Tutor** → **Download AI Tutor** → confirm. StudIN downloads Google's
`gemma3-1b-it-int4.task` (555 MB) from [`litert-community/Gemma3-1B-IT`](https://huggingface.co/litert-community/Gemma3-1B-IT)
in the background (progress notification, resumes if interrupted), pins it to repository revision
`a9bd856`, verifies its SHA-256 (`e3d981c0…`), installs it and switches the tutor to Gemma — after
that it runs fully offline. Requires a 64-bit phone with ≥ 4 GB RAM.

**Download key.** The Gemma repository is gated, so builds can bundle a Hugging Face *read* token
(from an account that accepted the Gemma licence on that repository) for one-tap downloads:

```properties
# local.properties  (gitignored — never commit tokens: Hugging Face auto-revokes leaked ones)
hf.token=hf_xxxxxxxxxxxxxxxxxxxxxxxx
```
or `-Pstudin.hfToken=…` / env `STUDIN_HF_TOKEN` in CI. Without one — or if the bundled token is
revoked — the app guides students to use their own free Hugging Face account (accept licence →
create Read token → paste). The token is only ever sent to `huggingface.co`, never to the CDN.
Note: a token inside an APK can be extracted by anyone with the APK; use a fine-grained,
read-only token you can rotate. Already have the file? **⋮ → Import model file**.

### How StudIN keeps the on-device AI under control
A 1B-parameter model drifts and invents facts if left alone, so the app decides what it may do
(`core/domain/.../ai/TutorGuard.kt`, unit-tested):

* **Intent routing** — identity, greetings, thanks, revision planning (real planner data), quiz,
  wellbeing, off-topic (syllabus classifier) and self-harm messages (Tele-MANAS 14416) are answered
  by the app; only study questions and note summaries reach Gemma.
* **One clean instruction turn** — strict rules first (never truncated), at most 3 relevant
  flashcards, previous exchange only for real follow-ups (no answers bleeding into new questions).
* **Calm sampling** — temperature 0.3 (0.2 for summaries/cards), top-K 20, top-P 0.9.
* **Output guard** — strips role echoes/special tokens, cuts repetition loops, stops native
  generation at ~120 words on a sentence boundary, closes open code blocks.
* **Honest UI** — inline markdown rendered properly and a "check important facts" note on model answers.
* **Resources** — single model load (mutex), prewarm on opening AI Tutor, unload after 3 min idle
  or on memory pressure, UI updates throttled while streaming.

## Architecture

```
:core:domain   Pure Kotlin + Java (JVM). Focus state machine, rule precedence, classifier,
               SM-2, CSV parser, quiz generator, planner, streaks, Lite AI, exam catalog.
               Fully unit-tested (incl. property/fuzz tests).
:app           Android. Compose UI + design system, Room/DataStore, Hilt, platform adapters:
  core/designsystem   Warm Academic tokens (light/dark), StudIN* components, illustrations
  core/data           Room entities/DAOs, repositories, DataStore preferences
  ai/                 OnDeviceLlm (MediaPipe), OcrEngine (ML Kit), StudyAssistant facade
  platform/focus      FocusController, FocusService, UsageStatsBridge (Java), BootReceiver
  platform/accessibility  Focus Guard (bounded, throttled inspection)
  platform/gate       Break gate activity
  feature/*           onboarding, home, focus, study, ai, insights, settings
```

Rule precedence: **emergency allowlist > user allow > session pass > app block > Smart feed rule > advisory**.

## Build

Requirements: JDK 17+ and the Android SDK (platform 36).

```bash
./gradlew :core:domain:test        # domain unit tests
./gradlew :app:assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease     # minified, signed with the debug key for testing
./gradlew :app:testDebugUnitTest   # unit + Robolectric tests: screenshot matrix → app/build/screenshots, a11y audit → app/build/reports/a11y
```

minSdk 26 · targetSdk 36.

## Docs
* **V2 redesign**: [final report](docs/STUDIN_V2_FINAL_REPORT.md) · [audit](docs/STUDIN_V2_INITIAL_AUDIT.md) · [design system](docs/STUDIN_V2_DESIGN_SYSTEM.md) · [architecture](docs/STUDIN_V2_ARCHITECTURE.md) · [screens](docs/STUDIN_V2_SCREEN_REDESIGN.md) · [accessibility](docs/STUDIN_V2_ACCESSIBILITY_REPORT.md) · [performance](docs/STUDIN_V2_PERFORMANCE_REPORT.md) · [tests](docs/STUDIN_V2_TEST_REPORT.md) · [screenshots](docs/STUDIN_V2_SCREENSHOT_REVIEW.md)
* [Privacy policy draft](docs/PRIVACY.md)
* [Threat model](docs/THREAT_MODEL.md)
* [ADR 0001 – on-device AI tiers](docs/adr/0001-on-device-ai-tiers.md)
* [ADR 0002 – non-coercive focus](docs/adr/0002-non-coercive-focus.md)

## Honest limitations
* Accessibility can only read what apps expose; some feed apps hide titles. Smart mode then falls back to app-level rules.
* Usage-stats data is approximate on some OEM ROMs; aggressive battery savers may delay the timer service (grant the battery exemption in *Permission health*).
* Play's Accessibility API policy must be reviewed before store submission.
