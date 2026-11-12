# StudIN V2 — accessibility report

Target: WCAG 2.2 AA principles as applicable to Android.

## Automated checks (all passing)
| Check | How | Result |
|---|---|---|
| Text contrast ≥ 4.5:1 (text primary, text secondary, primary) on background/surface/surface-secondary, light **and** dark; white on forest ≥ 4.5:1; white on orange ≥ 3:1 | `DesignSystemTest.textContrastMeetsWcagAA` | ✅ |
| Every clickable has an accessible label | `AccessibilityAuditTest` (semantics tree) | ✅ 0 findings |
| Every visible control ≥ 48 dp, or its 48 dp touch area overlaps no other control | same | ✅ 0 findings |
| Each screen exposes at least one heading | same | ✅ |
| Coverage | 13 screens × light, dark, 1.3× font = 39 renders, each scrolled to the bottom (6 swipes), with realistic data seeded | — |
| Rules are live | `A11yRulesControlTest`: two adjacent 12 dp unlabeled targets and no heading **must** be reported; StudIN header + button **must** pass | ✅ |

Report artifact: `app/build/reports/a11y/a11y-audit.txt` ("Audited 39 screen renders … No findings.").

### Findings fixed by the audit
* Selectable chart bars 40–41 dp wide with overlapping touch areas → chart-level tap target.
* Heatmap day cells 20–26 dp clickable → row-level tap target; per-day semantics retained.
* Controls scrolled beneath the opaque top bar remained focusable → content now starts below the bar.

## Built-in support
* **Dynamic type**: all text in `sp`; content-height top bar; no fixed text heights; verified at 1.3× in screenshots (`docs/screenshots/v2/font130`) and the audit.
* **Semantics**: headings on page titles and sections; `stateDescription` on flashcards, onboarding slides and selectable items; range semantics on progress; merged descriptions for stat cards and the countdown hero; chart bars/heatmap days announce "Tuesday 11 November: 90 min".
* **Non-colour status**: correctness, permissions, verdicts and badges always pair icon + text.
* **Reduced motion**: `motionDuration()` returns 0 when *Remove animations* is on; no infinite or decorative animations remain (12 → 0).
* **Dialogs**: Material 3 `AlertDialog`/`DatePickerDialog` (focus trapping and announcement handled by the platform).
* **Keyboard**: chat input and sticky action bars use `imePadding()`; `adjustResize` + edge-to-edge.

## Not verified (needs a device)
* TalkBack walkthrough (focus order, announcements while the timer runs).
* Google Accessibility Test Framework — it does not evaluate Compose under Robolectric (verified: a known-bad 12 dp control passed), so it was not used as evidence.
* Soft-keyboard overlap, display-size scaling, high-contrast text, landscape on real hardware.

### Manual test script
1. Enable TalkBack; complete onboarding using only swipe navigation.
2. Start/pause/end a focus session from the Focus screen and the notification.
3. Review 3 cards and answer a quiz question; confirm correct/incorrect is announced.
4. In Insights, swipe across the chart and heatmap; each bar/day should announce date and minutes.
5. Repeat at Settings → Display size *Largest* and font size *Largest*.
