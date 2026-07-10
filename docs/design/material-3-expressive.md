# Material 3 Expressive Reference

This document is MusFit's local working reference for Material 3 Expressive
(M3E). It is not a copy of Google's docs. It translates the official guidance
into stable rules for this Android app.

Exact tokens live under `app/src/main/java/com/musfit/ui/theme/` and are not
duplicated here. See the source-owned
[`musfit-design-system.md`](musfit-design-system.md) map.

## Official Sources

- [M3 Expressive: Engaging UX Design](https://m3.material.io/blog/building-with-m3-expressive)
- [Applying M3 Expressive](https://m3.material.io/foundations/usability/applying-m-3-expressive)
- [Material Design 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Compose Material 3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping)

## What M3E Means Here

Material 3 Expressive is an expansion of Material 3 focused on more engaging
interfaces through stronger type, richer motion, more varied shape, and clearer
visual hierarchy. For MusFit, that means:

- Health data should feel approachable, not clinical.
- Important numbers get confident type and more space.
- Cards, sheets, and controls use soft rounded geometry.
- Motion communicates change without slowing repeated workflows.
- Color is semantic and useful, not decorative.
- Dense tracking screens stay practical and scan-friendly.

M3E is a direction, not permission to make the app noisy. MusFit is a daily-use
fitness and nutrition tracker; speed, clarity, and trust are higher priority
than visual surprise.

## Stability Policy

MusFit uses Jetpack Compose Material 3 through the Compose BOM. The app should
stay on stable Compose dependencies unless a specific feature plan explicitly
approves an alpha dependency.

Current approach:

- Keep `androidx.compose.material3.MaterialTheme`.
- Use stable `lightColorScheme` and `darkColorScheme`.
- Build the expressive look through MusFit tokens:
  - `MusFitColors`
  - `MusFitTypography`
  - `MusFitShapes`
  - `MusFitSpacing`
  - `MusFitMotion`
- Avoid direct dependency on alpha-only expressive APIs such as
  `MaterialExpressiveTheme`, `MotionScheme`, or alpha-only shape/component APIs.

If Compose Material 3 Expressive APIs become stable later, migrate only after a
small compatibility review of component behavior, screenshots, and test impact.

## Design Principles

### 1. Expressive Hierarchy

Use expressive treatment for the user's main decision points:

- Today's calorie balance.
- Macro status.
- Training readiness or active workout state.
- Food add/log actions.
- Warnings that need correction, such as missing nutrition data.

Do not apply hero-scale type or high-emphasis containers to every card. A
screen where everything is loud has no hierarchy.

### 2. Rounded But Structured

Use the shape roles from `MusFitTheme.shapes`. Large outer or hero surfaces may
use the most expressive role; smaller controls should step down so dense UI does
not turn into a stack of oversized pills.

Recommended shape roles:

| Role | Shape |
| --- | --- |
| Full-width dashboard/content card | `MusFitTheme.shapes.large` or `extraLarge` |
| Bottom sheet container | `extraLarge` top corners |
| Segmented controls and major chips | Pill or `large` shape |
| Text fields, compact cards, small panels | `medium` |
| Thumbnails, icon wells, tags | `small` |

### 3. Tonal Color With Fixed Accents

Use fixed MusFit accents instead of wallpaper-driven Dynamic Color. This keeps
nutrition, training, and profile surfaces visually consistent across devices.

Current app accents:

| Area | Accent |
| --- | --- |
| Today | Coral |
| Food | Emerald |
| Training | Indigo |
| Profile / Settings | Teal |

Accent color should clarify ownership and state. It should not be the default
background for every element on a screen.

### 4. Confident Type

Use the bundled Roboto Flex family and the roles defined in `Type.kt` for all
display, headline, title, body, and label text. Weight and tracking contrast make
hero values expressive while dense rows remain readable.

Good uses of display/headline roles:

- Calorie totals.
- Macro totals.
- Weekly score.
- Body weight trend.
- Workout volume summaries.

Avoid display type inside compact list rows, dense modal forms, or repeated
nutrient tables.

### 5. Purposeful Motion

Motion should explain where the UI changed:

- Chart bars and rings can load in.
- Sheets can expand with springy spatial motion.
- Selection changes can use fast spring feedback.
- Color/alpha changes should avoid overshoot.

Motion should not block repeated logging. Food entry, workout set logging, and
settings edits should remain fast.

### 6. Edge-To-Edge And Insets

All top-level screens should work edge-to-edge. Content must respect status
bars, navigation bars, and IME/keyboard insets. Bottom bars and sheets need
careful padding so controls are never hidden behind system UI.

Use the Android `edge-to-edge` skill when changing app chrome, modal sheets,
bottom navigation, or keyboard-heavy forms.

## Component Guidance

### Navigation

- Bottom navigation uses a clear active pill/indicator.
- Each destination keeps its fixed accent.
- Labels stay short and stable: Today, Food, Training, Profile.

### Cards

- Cards group real information or actions.
- Do not nest cards inside cards.
- Use surface and surfaceVariant roles before inventing new colors.
- Full-width cards are acceptable; floating decorative page sections are not.

### Buttons And FABs

- Primary actions use icon plus text when the action may be ambiguous.
- Food add and active-workout actions may use more expressive shape.
- Destructive actions require clear label, confirmation where needed, and
  strong state feedback.

### Sheets

- Sheets are work surfaces, not marketing panels.
- Put the current task at the top.
- Keep dense editors readable with clear section headings and enough spacing.
- Respect IME insets for manual food, recipe, and profile forms.

### Charts

- Charts use app tokens and remain legible in dark mode.
- Animate chart reveal only when it improves comprehension.
- Do not use gradients that reduce data readability.

## Accessibility Rules

- Maintain readable contrast in light and dark.
- Hit targets should be at least 48dp for primary controls.
- Do not rely on color alone for nutrition status.
- Use clear content descriptions for icons and charts where relevant.
- Keep text scalable; never rely on viewport-width font sizing.

## Verification Checklist

Run this checklist for any M3E-related UI change:

- Light mode and dark mode both look intentional.
- No hardcoded colors bypass `MusFitTheme.colors` unless explicitly justified.
- Text does not clip at large font sizes.
- Bottom controls are not obscured by navigation bars.
- Forms remain usable with the keyboard open.
- Repeated workflows are still fast.
- The screen has one clear visual priority.

