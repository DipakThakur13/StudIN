# StudIN V2 — performance report

No emulator or device was available (no KVM), so **runtime** performance (startup, frame
timing, memory, battery) was **not measured**. Macrobenchmark and Baseline Profiles require a
device and are listed as follow-ups. Below are measured static/build metrics only.

## Measured
| Metric | V1 (`cb7d12f`) | V2 | Change |
|---|---|---|---|
| Release APK (minified, 3 ABIs) | 71,290,073 B | 70,681,413 B | −608,660 B (−0.85 %) |
| Debug APK | 92,144,089 B | 92,978,780 B | +834,691 B (new screens, tests' manifest) |
| Infinite animations (`rememberInfiniteTransition`) | 12 | 0 | removed |
| `Modifier.blur` (RenderEffect, GPU cost) | 3 | 0 | removed |
| Particle system (confetti Canvas) | 1 | 0 | removed |
| Raster drawables (`.png` in res) | 16 | 10 (legacy launcher mipmaps only) | logo/splash/illustrations are vectors |
| Distinct corner radii in code | 14 | 2 (+ token radii) | consistent |
| Android lint errors | 1 (flow in composition, pre-existing) | 0 | fixed |

## Changes with expected (unmeasured) benefit
* No continuously running animations → the UI can go idle (fewer frames, less battery).
* No blur/RenderEffect layers and no full-screen gradient/orb background.
* Lazy lists with stable keys for plans, sessions, cards, decks and chat messages.
* `collectAsStateWithLifecycle` everywhere; AI generation stays on `Dispatchers.IO` with a single
  active generation; chart/heatmap tap handling uses one `pointerInput` instead of 84 clickable nodes.
* Illustrations are drawn from vectors at ≤ 300 dp; intrinsic size reduced to 200 dp (lint `VectorRaster`).

## Remaining lint warnings (non-blocking)
`GradleDependency` / `NewerVersionAvailable` (deliberately no dependency upgrades),
`ConfigurationScreenWidthHeight` ×3 (window-width checks), `ObsoleteSdkInt` ×4, `UseKtx` ×4,
`UsableSpace`, `UnusedAttribute`, `AutoboxingStateCreation` hint.

## Follow-ups (device required)
1. Macrobenchmark cold/warm startup and Home → Focus → Study navigation frame timing.
2. Generate and ship a Baseline Profile.
3. Perfetto trace of a 25-minute session (service + accessibility events) for battery.
