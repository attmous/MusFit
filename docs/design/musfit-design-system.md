# MusFit Design System

Snapshot date: 2026-06-28.

This document defines MusFit's current app-wide design tokens and practical UI
rules. It complements the implementation files under
`app/src/main/java/com/musfit/ui/theme/`.

## Product Feel

MusFit should feel like a premium daily tracking tool: calm, practical,
rounded, and data-rich. The design can be warm and expressive, but it should
not feel like a landing page, social app, or decorative wellness poster.

Design priorities:

1. Fast repeated logging.
2. Clear health and fitness status.
3. Dense but readable information.
4. Local-first trust and privacy.
5. Original visual language inspired by modern health apps, without copying
   another product's assets or exact layouts.

## Token Files

| File | Responsibility |
| --- | --- |
| `Color.kt` | Raw color constants for light, dark, macros, and tab accents. |
| `MusFitColors.kt` | Semantic color model exposed through `MusFitTheme.colors`. |
| `Theme.kt` | Material 3 theme bridge and light/dark color scheme mapping. |
| `Type.kt` | Google Sans Flex display/title scale over Material 3 typography. |
| `Shape.kt` | M3E-scale rounded shapes. |
| `Spacing.kt` | Shared spacing scale. |
| `Motion.kt` | Stable Compose spring specs for M3E-style motion. |
| `TabAccent.kt` | Fixed per-destination accents with light/dark variants. |

## Color System

MusFit uses semantic tokens, not direct screen-level color literals. New UI
should prefer `MusFitTheme.colors` and `tabAccentFor(destination)`.

### Light Palette

| Token | Value | Use |
| --- | --- | --- |
| `background` | `Cream #FBF7F1` | App background. |
| `surface` | `CardWhite #FFFFFF` | Main cards and sheets. |
| `surfaceVariant` | `WarmFill #F4EEE6` | Secondary panels and subtle fills. |
| `onSurface` | `WarmInk #2A2420` | Primary text. |
| `onSurfaceVariant` | `WarmMuted #8C8178` | Secondary text. |
| `brand` | `Emerald #1E7A53` | Food/brand emphasis and positive action. |
| `accent` | `Coral #FF7A66` | Today accent and warm emphasis. |

### Dark Palette

| Token | Value | Use |
| --- | --- | --- |
| `background` | `DarkBg #14110F` | App background. |
| `surface` | `DarkSurface #221E1A` | Main cards and sheets. |
| `surfaceVariant` | `DarkSurfaceVariant #2B2621` | Secondary panels. |
| `onSurface` | `DarkOnSurface #F3EDE6` | Primary text. |
| `onSurfaceVariant` | `DarkOnSurfaceVariant #B0A79E` | Secondary text. |
| `brand` | `EmeraldBright #3CCB9B` | Food/brand emphasis. |
| `accent` | `CoralBright #FF8A66` | Today accent and warm emphasis. |

### Macro Colors

| Macro | Light | Dark |
| --- | --- | --- |
| Protein | `#0D9488` | `#2DD4BF` |
| Carbs | `#F59E0B` | `#FBBF24` |
| Fat | `#6D5BD0` | `#A78BFA` |

Keep macro colors stable across Food surfaces. Users learn these mappings.

### Tab Accents

| Destination | Light | Dark |
| --- | --- | --- |
| Today | Coral | CoralBright |
| Food | Emerald | EmeraldBright |
| Training | Indigo | IndigoBright |
| Profile | Teal | TealBright |

Use tab accents for active navigation, key actions, selected states, and major
charts. Avoid using a tab accent as the default fill for every card.

## Typography

MusFit uses:

- Google Sans Flex for display, headline, and title roles.
- Roboto/system default for body and label roles.

Rules:

- Use headline/display roles for hero numbers and screen-level summaries.
- Use title roles for card headings and section headings.
- Use body roles for descriptions, food rows, form hints, and explanatory copy.
- Use label roles for chips, metadata, controls, and compact values.
- Do not reduce letter spacing below zero.
- Do not scale font size with viewport width.

## Shape

Current shape scale:

| Token | Radius | Use |
| --- | --- | --- |
| `extraSmall` | 8dp | Tiny chips, thumbnails, icon wells. |
| `small` | 12dp | Compact inputs, tags, small panels. |
| `medium` | 16dp | Dense cards, controls, inner groups. |
| `large` | 28dp | Main content cards. |
| `extraLarge` | 28dp | Hero cards, sheets, large surfaces. |

Do not make every small element 28dp. Large shapes are expressive because they
contrast with tighter dense controls.

## Spacing

Current spacing scale:

| Token | Value |
| --- | --- |
| `xs` | 4dp |
| `sm` | 8dp |
| `md` | 12dp |
| `lg` | 16dp |
| `xl` | 20dp |
| `xxl` | 24dp |

Use 16dp as the default screen horizontal padding. Use 20-24dp for hero
surfaces and modal sheet interiors. Use 8-12dp for dense repeated rows.

## Motion

`MusFitMotion` exposes stable Compose spring specs:

| Token | Use |
| --- | --- |
| `spatial()` | Bounds, size, offset, shape, large transitions. |
| `spatialFast()` | Selection feedback and smaller movement. |
| `effects()` | Color and alpha changes. |

Motion should be subtle in database/search/editing flows and more visible in
summary charts, navigation selection, and major state changes.

## Layout Rules

- Build real app surfaces first; do not add marketing-style hero layouts.
- Use full-width sections or direct constrained layouts for page structure.
- Cards are for repeated items, modals, dashboard panels, or framed tools.
- Do not put cards inside cards.
- Use stable dimensions for charts, rings, tab rows, icon buttons, and tiles.
- Keep text inside controls from clipping in both portrait and larger font
  settings.
- Treat mobile portrait as the primary layout; prepare larger screens through
  adaptive layout only when the workflow benefits.

## Dark Mode Rules

- New colors must have light and dark behavior.
- Prefer semantic tokens over raw color literals.
- Any raw color literal in UI code needs a reason.
- Charts must be checked in dark mode, especially grid lines, labels, and
  selected markers.
- Scrims and shadows may use raw black only when they are truly overlay effects.

## Implementation Rules

- Keep visual changes local to Compose UI and theme tokens unless behavior
  changes are explicitly requested.
- Do not introduce accounts, cloud sync, analytics, social features, or
  subscriptions as part of design work.
- For Food work, prefer existing `FoodScreen`, `FoodComponents`,
  `FoodModalSheets`, `FoodAddPanelUi`, and token patterns.
- Use the stable Material 3 dependency set unless a plan explicitly approves an
  alpha migration.
- Verify with Gradle and, for meaningful UI changes, a real device or emulator.

## QA Checklist

- Light/dark screenshots reviewed.
- No unreadable accent-on-container combinations.
- Touch targets are large enough.
- Charts remain legible.
- Bottom navigation and sheets avoid system bars.
- Keyboard-heavy forms avoid IME overlap.
- Screen still supports fast repeated tracking.

