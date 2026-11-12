# StudIN V2 — screenshot review

Tool: Robolectric native graphics + Roborazzi (`ScreenshotTour`) rendering the **real** app.
60 captures were generated **and visually inspected**; JPEG copies are in `docs/screenshots/v2/`.
Baseline (V1) captures: `docs/screenshots/v1/`; comparison: `docs/screenshots/v1_vs_v2.jpg`.

| Variant | Qualifiers | Screens |
|---|---|---|
| light | 400×880 dp, xxhdpi | all screens + onboarding (8), empty Home/Insights, focus active/paused/completed, review revealed, AI conversation, Insights 30D/12W, gate, deck |
| dark | same | Home, Focus, Study, AI, Insights, Settings, gate |
| compact | 360×640 dp | Home, Focus, Study, AI, Insights |
| large | 480×1000 dp | Home, Focus, Study, AI, Insights |
| tablet | 900×1280 dp (navigation rail) | Home, Focus, Study, AI, Insights |
| font130 | 400×880 dp, font scale 1.3 | Home, Focus, Study, AI, Insights |

## Issues found by inspection → fixed
| Issue | Where | Fix |
|---|---|---|
| Deep-link screenshots all showed Home; then crash "graph not set" | every routed capture | await back stack before navigating |
| Negative padding crash after removing the overlay bar | AI Tutor | removed `BottomBarPadding` arithmetic |
| Answer text grey on revealed flashcard | Review | explicit `onSurface` content colour |
| "Content ch…" tab label | AI, 360 dp | width threshold 340 → 390 dp |
| "Review / now" two-line buttons | Study, 360 dp | stacking threshold 140 → 156 dp |
| Busy 29-tick slider | onboarding goal | continuous slider snapped to 15 min |
| Stale "24:59", uneven stat heights, "…" custom chip (agent-reported) | Focus | completion card leads; equal-height rows; icon |

## Accepted / not applicable
* Active timer shows 25:00 — Robolectric clock is frozen (logic unit-tested).
* Rules lists no apps — Robolectric has no launcher apps installed.
* AI Model tab renders "Not supported" — Robolectric reports low memory.
* Pressed-state tint on onboarding "Next" — residue of the test's click.

## Not captured (needs a device)
Soft keyboard visible, real OEM status/navigation bars, TalkBack focus highlights.
