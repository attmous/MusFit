# MusFit Design System

This is the low-drift design contract for MusFit. Exact color values, radii,
spacing values, typography sizes, and motion parameters live in source under
`app/src/main/java/com/musfit/ui/theme/`; do not duplicate those volatile values
here.

Related guidance:

- [Material 3 Expressive interpretation](material-3-expressive.md)
- [Food UI guidelines](food-ui-guidelines.md)
- [Screen and navigation contracts](../architecture/screen-contracts.md)

## Product Feel

MusFit should feel like a premium daily tracking tool: calm, practical, rounded,
and data-rich. It can be warm and expressive, but app screens must not feel like
landing pages, social feeds, or decorative wellness posters.

Priorities:

1. Fast repeated logging.
2. Clear fitness, nutrition, and health status.
3. Dense but readable information.
4. Local-first trust and explicit external-data boundaries.
5. An original visual language; never copy another product's assets or exact
   layouts.

## Source-Owned Tokens

| Source | Responsibility |
| --- | --- |
| `Color.kt` | Raw light/dark palette, macro colors, integration colors, and tab accents. |
| `MusFitColors.kt` | Semantic colors exposed through `MusFitTheme.colors`. |
| `Theme.kt` | Material 3 light/dark bridge and composition locals. |
| `Type.kt` | Bundled Roboto Flex type scale for display, headline, title, body, and label roles. |
| `Shape.kt` | Material 3 shape-role radii. |
| `Spacing.kt` | Shared spacing scale. |
| `Motion.kt` | Compose spring specifications. |
| `TabAccent.kt` | Destination-owned light/dark accents. |

When a token changes, source and visual tests/screenshots are authoritative.
Update prose only when the design rule changes, not when a literal value changes.

## Color And Ownership

- Use `MusFitTheme.colors` for semantic surfaces, text, state, and integration
  colors.
- Use `tabAccentFor(destination)` for destination ownership and primary actions.
- Keep macro colors consistent across Food surfaces so the learned mapping does
  not change from screen to screen.
- Avoid raw color literals in feature UI. A literal is acceptable only for a
  truly local rendering need with a documented reason.
- Every new semantic color needs light and dark behavior plus contrast review.
- Accent color clarifies ownership, selection, and key action; it is not the
  default background of every card.

Current destination ownership is Today/coral, Food/green, Training/indigo, and
Profile/teal. Exact variants come from `TabAccent.kt` and `Color.kt`.

## Typography

MusFit applies the bundled Roboto Flex family across Material typography roles.
The implemented system uses strong weight and tighter tracking for display,
headline, and hero-number roles, with quieter body and label roles from the same
family.

- Use display/headline roles for hero values and screen summaries.
- Use title roles for cards and sections.
- Use body roles for descriptions, rows, hints, and explanatory copy.
- Use label roles for controls, chips, metadata, and compact values.
- Use the role defined in `Type.kt`; do not recreate font size, weight, line
  height, or tracking at call sites without a concrete exception.
- Do not scale type from viewport width. Validate font scaling and localization
  instead.

## Shape, Spacing, And Motion

- Use `MusFitTheme.shapes`, `MusFitTheme.spacing`, and `MusFitMotion` rather than
  copying numeric tokens into feature files.
- Preserve contrast between tighter inner controls and more expressive outer or
  hero surfaces; not every element should be a large rounded card.
- Use spring motion to explain spatial or state change. Search, database, and
  editing flows should remain calm; navigation, summaries, and major state
  changes may be more expressive.
- Motion must not delay logging, hide state, or make repeated actions tiring.

## Layout And Components

- Build app workflows, not marketing compositions.
- Prefer shared theme/components when they express the same semantics; avoid
  false abstraction across unrelated feature behavior.
- Cards are appropriate for repeated items, dashboard panels, modals, or framed
  tools. Avoid card-inside-card hierarchies.
- Keep dense repeated rows scan-friendly with clear alignment and hierarchy.
- Preserve system-bar, gesture, and IME insets.
- Treat phone portrait as a required baseline, then adapt layouts where larger
  windows materially improve the workflow.
- Keep destination ownership visible without making every surface accent-filled.

## Accessibility And QA

For meaningful UI work, verify:

- light and dark themes;
- text and icon contrast;
- touch-target size and non-overlapping actions;
- font scaling and long/localized text;
- system-bar and keyboard insets;
- chart labels, selected markers, and non-color state cues;
- the affected workflow on the seeded emulator with screenshot or UI-tree
  evidence.

Run the focused tests and the standard debug gate from `AGENTS.md`. A token or
shared-component change needs cross-tab review because Today, Food, Training,
Profile, scanners, and sheets consume the same theme surface.
