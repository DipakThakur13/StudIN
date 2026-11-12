# StudIN V2 — screen redesign

Before/after (V1 dark neon → V2 Warm Academic): `docs/screenshots/v1_vs_v2.jpg`.
Full V2 collection: `docs/screenshots/v2/<variant>/` (light, dark, compact, large, tablet, font130).

| Screen | V1 problems | V2 result | Logic changes |
|---|---|---|---|
| **Onboarding** | Text-only pages, glowing logo | 5 illustrated, skippable intro slides (Welcome, Focus, Remember, AI Tutor, Progress) + 3-step setup (exam grid, goal, privacy & ready) with step progress; landscape puts art beside text | None (same `OnboardingViewModel.finish`) |
| **Home** | Oversized ring card, duplicate quick-action grid, overlay bar | Compact header (wordmark, reminders, profile), forest countdown hero, **Continue studying** as the most prominent action (resumes a running session), 2×2 progress grid, plan list with "View all", illustrated empty state | UI helpers in `HomeUi.kt` (greeting, continue-block selection) |
| **Focus** | 220 dp glowing ring, presets wrapping "90" onto its own row, confetti | Width-adaptive ring (200–300 dp), single 5-option duration row (15/25/50/90/Custom sheet), topic sheet, orange start action; active mode hides setup controls; paused state; restrained completion card with next actions | `FocusUi.kt` helpers; `FocusController` untouched |
| **App rules** | Glass cards | Focus Guard explainer with status + disclosure route, suggested setup, per-app Allow/Smart/Block, emergency apps locked, keyword chips | Rules → Permission health now shows the accessibility disclosure first |
| **Break gate** | Hard-coded dark theme, neon scrim | Theme-aware calm card with focus illustration; Back to studying (primary), earned break (secondary), always-visible Emergency — open anyway | Theme default SYSTEM; flow-in-composition lint error fixed |
| **Study** | Tabs + large deck hero | Due-reviews card, **My subjects** (chapter count, completion %, next-topic Continue), expandable chapter progress, flashcard decks, 7-day study history (real review log); 2 columns ≥ 560 dp | `StudyUiModels.kt` (aggregation, ranking) |
| **Deck / Review / Quiz / Import / AI cards** | Shake animation, colour-only correctness, sticky actions missing | Sticky bottom action bar (keyboard-aware), accessible flip card, 4 grade buttons with intervals, icon+text correctness, honest AI engine badge | None to repositories/AI |
| **AI Tutor** | Large empty canvas | Compact intro with mascot + 4 quick actions wired to real features (Explain, Quiz me → quiz, Summarise, Plan my revision), formatted replies (bullets, numbered, code), Copy, Retry on failure, Stop, honest engine/model states | `AiPresentation.kt`; chat remains memory-only |
| **Insights** | Fixed 7-day view, decorative cards | 7D / 30D / 12W filter driving every metric; zero-based labelled chart with goal line and tap selection; 12-week heatmap with month labels and day detail; data-derived observations only; empty state | `InsightsMath.kt`; query window 168 days |
| **Settings / Permission health / Privacy** | Mixed glass styles | Grouped cards, ≥ 48 dp rows/sliders, theme Follow system/Light/Dark, permission explanations with icon+text status and "what stops working", illustrated privacy policy | None |
| **App shell** | Floating overlay bar + magic 110 dp padding | M3 `NavigationBar` / `NavigationRail` (≥ 600 dp), insets applied once, content clipped below top bars | Deep-link race fixed |

## Bugs fixed
1. **Deep-link crash** — notification route navigated before the nav graph existed (`MainActivity`).
2. **Flow operator invoked in composition** (lint error) in `GateActivity` — a new flow per recomposition.
3. **Content under the top bar** — scrolled controls remained focusable/touch-overlapping beneath the opaque bar; `StudINScaffold` now starts content below the bar.
4. **Sub-48 dp chart/heatmap targets** — replaced with one tap surface per chart/row; values still announced per bar/day.
5. **Low-contrast flashcard answer** — inherited `onSurfaceVariant`; now `onSurface`.
6. **Truncated AI tab label / two-line buttons** at 360 dp — adaptive label and stacking thresholds.
