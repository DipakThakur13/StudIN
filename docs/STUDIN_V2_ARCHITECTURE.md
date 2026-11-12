# StudIN V2 — architecture

V2 is a **presentation-layer** redesign. Domain, data, AI and platform layers are unchanged in
behaviour; additions are UI-state helpers and one bug fix.

```
:core:domain  (pure Kotlin/Java, unchanged)
:app
  core/designsystem   NEW  Tokens · Theme · Components · StudyComponents (+ Legacy shims removed at the end)
  core/data           unchanged (Room v1, DataStore; ThemeMode default → SYSTEM)
  core/di             + AppEntryPoint.focusController() (tests / non-injected code)
  ai/                 unchanged
  platform/*          unchanged logic; GateActivity UI rebuilt
  ui/navigation       Scaffold + NavigationBar/NavigationRail (was: floating overlay)
  feature/*           presentation rebuilt on StudIN* components; ViewModels gain UI-only state
design/               NEW  SVG sources + svg2vd.py (brand, illustrations)
```

## Decisions (ADR summary)
| # | Decision | Why |
|---|---|---|
| V2-1 | Material 3 colour roles + a small `StudINColors` extension, delivered via `CompositionLocal` | Screens stay on semantic roles; light/dark are complete independent palettes |
| V2-2 | Content-height `StudINTopAppBar` instead of M3 `TopAppBar` | M3 bars have fixed height; 28 sp titles + subtitles clipped at 1.3× font scale |
| V2-3 | App-level `Scaffold(contentWindowInsets = 0)` + `consumeWindowInsets(innerPadding)`; each screen owns a `StudINScaffold` | Insets applied exactly once; bar never overlaps content; IME works per screen |
| V2-4 | Window width ≥ 600 dp → `NavigationRail` (+ max content width 720 dp) | Medium/expanded window classes without adding the adaptive library (no new dependencies) |
| V2-5 | Vector logo & illustrations generated from SVG (`design/svg2vd.py`) | Crisp at every density, tiny APK cost, themable backdrops, editable sources |
| V2-6 | Reduced motion via `Settings.Global.ANIMATOR_DURATION_SCALE` → `motionDuration()` | Honour the system setting everywhere with one helper |
| V2-7 | Theme preference default changed DARK → SYSTEM | Light-first design that still follows the user's system choice; persisted in DataStore |
| V2-8 | V1 component API kept as restyled shims during migration, then deleted | Allowed parallel screen migration without a broken build |

## Bug fixed during the migration
`MainActivity` navigated to a notification deep-link route before the `NavHost` graph existed
(crash on cold start from the focus notification once the NavHost moved inside a Scaffold).
It now awaits `nav.currentBackStackEntryFlow.first()` before navigating.

## State management
Unchanged pattern: `@HiltViewModel` + `StateFlow` + `collectAsStateWithLifecycle`. New UI-only
state (Insights period, AI chat errors/retry, onboarding phase) lives in ViewModels or
`rememberSaveable`; nothing is persisted that wasn't before (chat stays in memory only).
