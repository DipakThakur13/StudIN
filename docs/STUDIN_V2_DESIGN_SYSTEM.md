# StudIN V2 design system — "Warm Academic, Premium Edition"

Source of truth: `app/src/main/java/com/studin/app/core/designsystem/`
(`Tokens.kt`, `Theme.kt`, `Components.kt`, `StudyComponents.kt`). Screens must not use raw
colour literals, ad-hoc radii or arbitrary spacing.

## 1. Principles
Light-first, warm ivory surfaces, forest-green brand, **restrained** burnt-orange accents,
flat cards with 1 dp borders, balanced whitespace, calm motion. No glassmorphism, neon,
glow, blur, particle effects or floating overlays.

## 2. Colour tokens
Material 3 roles are filled from the spec palette; StudIN-only roles live in `StudINColors`
(`StudINTheme.colors`).

| Token | Light | Dark | M3 role / usage |
|---|---|---|---|
| Background | `#FAFAF7` | `#111914` | `background` |
| Surface | `#FFFFFF` | `#1B2720` | `surface`, cards, nav bar |
| Surface secondary | `#F4F3EE` | `#243128` | `surfaceVariant`, illustration backdrops, tonal buttons |
| Primary (forest) | `#243F32` | `#B4DEC0` | `primary` — brand, primary buttons, selection |
| Primary dark | `#183126` | — | `onPrimaryContainer` |
| Primary container | `#DCE7DF` | `#2F4A3B` | selected nav indicator, selected exam card |
| Accent (orange) | `#E66B38` | `#F4A273` | `tertiary` / `StudINTheme.colors.accent` — focus progress, start-focus action, highlights |
| Accent light | `#FFF0E7` | `#3D2A1E` | `tertiaryContainer` / `accentContainer` |
| Text primary | `#202823` | `#F4F6F1` | `onBackground`, `onSurface` |
| Text secondary | `#626D65` | `#BAC5BB` | `onSurfaceVariant`, `textSecondary` |
| Border | `#E5E8E1` | `#34463A` | `outlineVariant`, card borders |
| Success | `#287A55` | `#81C8A0` | correct answers, granted permissions |
| Warning | `#B86B20` | `#E8B66C` | advisory verdicts |
| Error | `#B84040` | `#FFAAA2` | `error`, destructive actions |
| AI accent | `#7865A5` | `#C2B3EC` | AI Tutor only |
| Focus track | `#E7EAE3` | `#2A382F` | progress tracks |

Contrast is unit-tested (`DesignSystemTest.textContrastMeetsWcagAA`): text primary/secondary
and primary ≥ 4.5:1 on every background in both themes; white on forest ≥ 4.5:1; white on
orange ≥ 3:1 (UI component / large text use only).

**Orange policy** — only for: active focus progress, the main "Start focus" action, streaks,
selected chart bars and small highlights. Never for every card or button.

### Gradient policy
Only two gradients exist: forest `#243F32 → #355846` (exam countdown hero / milestone
banners) and warm accent `#E66B38 → #F3A16D` (achievement states). Everything else is flat.

## 3. Typography (system sans-serif, scalable `sp`)
| Role | Style | Size / weight |
|---|---|---|
| Main page title | `headlineMedium` | 28 sp Bold |
| Hero value | `StudINType.heroValue` / `displaySmall` | 36 sp Bold (tabular) |
| Focus timer | `StudINType.timer` | 52 sp Bold (tabular) |
| Section heading | `titleLarge` | 20 sp SemiBold |
| Card heading | `titleMedium` | 16 sp SemiBold |
| Body | `bodyLarge` / `bodyMedium` | 16 / 14 sp Regular |
| Supporting | `bodySmall` | 12 sp Regular |
| Button label | `labelLarge` | 14 sp SemiBold |
| Navigation label | `labelMedium` | 12 sp Medium |

Text never has fixed heights; containers use `heightIn(min = …)` and wrap at 1.3× font scale.

## 4. Spacing (`Spacing`)
4 · 8 · 12 · 16 · 20 · 24 · 32 · 40 · 48 dp. Screen horizontal padding **20 dp**, card padding
**16 dp**, section spacing **24 dp**, related elements **8–12 dp**, touch target **48 dp**,
max content width **720 dp** (tablets/landscape).

## 5. Shapes (`Radius`, `StudINShapes`)
Standard card 16 · small card 12 · button 12 · input 12 · dialog 24 · bottom sheet 24 (top) ·
badge 8. Elevation is 0 for cards (1 dp border instead); only dialogs, menus and sheets elevate.

## 6. Motion (`Motion`, `motionDuration()`)
Small state 150 ms · card 210 ms · screen 260 ms · success 380 ms. `motionDuration()` returns 0
when the system "Remove animations" setting is on (`LocalReducedMotion`). Allowed: timer
progress, press feedback, tab selection, card expansion, flashcard flip, progress updates,
completion feedback, loading indicators. Not allowed: infinite decorative motion, springs,
bouncing, blur, particles, animated gradients.

## 7. Components
| Component | Purpose |
|---|---|
| `StudINScaffold`, `StudINTopAppBar` | Screen frame; content-height bar (no clipping at large fonts), status-bar inset handled once |
| `screenContentPadding()`, `Modifier.studinContentWidth()` | 20 dp gutters + scaffold insets; centred max-width content |
| `StudINCard` | Flat bordered card, optional click |
| `StudINPrimaryButton` (tone Primary/Accent), `StudINSecondaryButton`, `StudINOutlinedButton`, `StudINTextButton`, `StudINIconButton` | Buttons with loading/disabled states, ≥ 48 dp |
| `StudINSectionHeader` | Heading semantics + optional action |
| `StudINBadge`, `StudINChip`, `StudINFilterChip`, `StudINSegmentedTabs` | Status labels, info chips, selection |
| `StudINLinearProgress`, `StudINProgressRing` | Accessible progress (range semantics) |
| `StudINStatCard`, `StudINIconTile`, `StudINListItem` | Metrics, icon anchors, settings rows |
| `StudINEmptyState`, `StudINErrorState`, `StudINLoadingState` | Consistent states (empty states can use illustrations) |
| `StudINTextField`, `StudINDialog`, `StudINBottomSheet` | Inputs and overlays (12 / 24 / 24 dp) |
| `StudINExamCountdownCard` | Compact forest hero (gradient permitted) |
| `StudINStudyTaskCard` | Subject · chapter · duration · state · action |
| `StudINFocusTimer` | Ring that scales 200–300 dp with width |
| `StudINInsightChart` (`ChartBar`, `niceStep`) | Zero-based, labelled, goal line, selectable, accessible bars |
| `StudINFlashcard` | Accessible reveal + restrained flip |
| `StudINPermissionExplanation` | What / why / status (icon + text) / action |
| `StudINLogo`, `StudINWordmark`, `StudINIllustration` | Brand + illustrations on theme backdrops |

## 8. Brand & illustrations
* Logo "Rising Sun Book" — vector mark (`ic_studin_mark`), adaptive + monochrome launcher
  icon, splash mark, notification icon. Sources and generator: `design/`.
* Six illustrations (`ill_welcome`, `ill_focus`, `ill_recall`, `ill_ai_tutor`, `ill_insights`,
  `ill_privacy`) explain the product in onboarding and power empty states. They carry no
  backdrop; `StudINIllustration` draws a theme-coloured circle so the art works in both themes.

## 9. Navigation shell
`StudinNavHost`: Material 3 `Scaffold` with `NavigationBar` (< 600 dp) or `NavigationRail`
(≥ 600 dp). The bar is part of the layout and its insets are consumed before screens apply
their own, so content is never covered and insets are never applied twice.
