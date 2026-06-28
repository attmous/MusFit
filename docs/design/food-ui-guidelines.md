# Food UI Guidelines

Snapshot date: 2026-06-28.

This document translates the MusFit design system into Food miniapp rules. The
Food miniapp is the active priority and should remain clean, dense, and
Lifesum-like in information architecture without copying Lifesum's assets or
exact layouts.

Authoritative Food architecture remains
[`docs/architecture/food-system.md`](../architecture/food-system.md).

## Food Product Intent

Food is the daily nutrition command center:

- Show today's nutrition status at a glance.
- Make logging fast from saved foods, recents, barcode, manual entry, recipes,
  templates, and quick calories.
- Keep nutrition details trustworthy and editable.
- Support planning, shopping, water, and Health Connect export without making
  the primary diary feel crowded.

Food should feel practical and premium: polished enough to trust, dense enough
for daily use.

## Screen Hierarchy

Food screen priority:

1. Date and mode: what day am I editing, and am I planning or logging?
2. Calorie status: total, target, remaining/over, training-calorie inclusion.
3. Macro status: protein, carbs, fat with learned color mapping.
4. Meal sections: breakfast/lunch/dinner/snacks/custom meals.
5. Secondary trackers: micronutrients, water, insights, ratings, planning,
   shopping, Health Connect.

Do not let secondary feature cards push the diary below the fold on common
phones. The user should see their day status and first meal quickly.

## Top Summary

The top summary can use the most expressive treatment in Food:

- Use Food emerald as the dominant accent.
- Use headline/display type for the primary calorie number.
- Keep macro progress visible and color-coded.
- Show planning/logged state clearly.
- Keep date navigation compact.

Avoid adding explanatory text about what the screen does. The UI should be
self-evident.

## Meal Sections

Meal sections are the workhorse of Food. They should be easy to scan and fast
to operate.

Rules:

- Meal headers show name, optional time, calories, and a clear add action.
- Per-item rows show food name first, then serving amount and macro summary.
- Planned and logged states need visible but restrained distinction.
- Empty meals should invite adding without becoming large promotional cards.
- Custom meals should feel native, not bolted on.
- Copy, move, edit, delete, and mark planned-to-logged actions should remain
  discoverable through existing sheet/menu patterns.

Use dense rows, not oversized cards, for repeated foods. Save expressive card
treatment for meal summaries or high-value insights.

## Add Flow

The add flow has many modes. It should feel like one coherent task, not several
unrelated tools.

Mode guidance:

| Mode | UI priority |
| --- | --- |
| Saved | Search, recents, same-as-yesterday, favorites, templates, recipes. |
| Manual | Clear nutrition fields, serving model, save/log decision. |
| Barcode | Scan/result confidence, product review, edit before save. |
| Quick calories | Fast amount entry and favorite presets. |
| AI text shell | Editable draft first; never pretend estimates are certain. |

Keep "Keep adding" visible near the final logging action. It is a workflow
accelerator and should not be buried.

## Editors

Food editors are data-entry surfaces. Expressive style should not reduce
precision.

Rules:

- Use section headings for identity, serving, macros, micros, and quality/trust.
- Align numeric inputs consistently.
- Make units explicit.
- Show per-100g versus per-serving state clearly.
- For imported or scanned foods, make review/edit state obvious.
- Destructive actions belong at the bottom or in an overflow menu with
  confirmation/undo behavior.

Avoid playful motion or oversized shapes inside dense nutrition forms.

## Nutrition Visuals

Food uses several nutrition visual systems:

- Calorie ring.
- Macro progress bars.
- Advanced nutrient and micronutrient progress.
- Daily insights.
- Day, meal, and food ratings.
- Water progress.

Visual rules:

- Macro colors never change meaning.
- Progress visuals must show both value and target when the target matters.
- Ratings should expose reasons, not only a score.
- Warnings use semantic warning colors and plain language.
- Do not rely on color alone for good/bad/over/under states.

## Data Trust

Food data can come from manual entry, Open Food Facts barcode lookup, starter
foods, local edits, nutrition-label OCR, and AI text drafts. The UI must be
honest about confidence.

Rules:

- Barcode imports should be editable before logging/saving.
- Nutrition-label OCR is best-effort and must show values for review.
- AI text logging is a local draft helper, not an authoritative nutrition
  source.
- Duplicate detection and merge flows should explain what changes.
- Local user edits are treated as trusted for that device.

Do not make imported data look more certain than it is.

## Sheets And Modal Surfaces

Food has many sheets, so consistency matters:

- Use a clear title and immediate task controls.
- Keep the primary action fixed or easy to reach when the sheet is long.
- Use `MusFitTheme.shapes.extraLarge` for major sheet/container rounding.
- Respect IME insets for search and numeric input.
- Keep sheet internals structured with spacing, not nested cards.

Long sheets should remain scrollable and should not hide final actions behind
the navigation bar.

## Planning, Shopping, And Water

Planning and shopping are secondary workflows. They should support the diary,
not replace it.

Rules:

- Planned items should visually connect to their future meal.
- Shopping groups should be checkable and grouped by useful food categories.
- Manual shopping items should be visually distinct from generated ingredients.
- Water tracking can be compact; it should not compete with calories/macros.

## Empty, Loading, And Error States

Food states should be useful and compact:

- Empty database/search: show the next useful action.
- Barcode no match: offer manual creation with barcode carried forward.
- Network failure: explain that local foods still work.
- OCR parse uncertainty: show editable fields, not a dead end.
- Duplicate detection: show the competing saved item and import clearly.

Avoid large illustrations or marketing copy in utility states.

## Accessibility

- Food item rows need readable names at large font sizes.
- Numeric values need units.
- Controls that only use icons need content descriptions.
- Touch targets for add/edit/delete/move actions should be comfortable.
- Color-coded macro/rating states need text labels or values.
- Scanners need non-camera fallback paths.

## Food QA Checklist

Use this checklist after visual Food changes:

- Today summary visible quickly on common phone height.
- First meal section is reachable without excessive scrolling.
- Add flow modes are still easy to distinguish.
- Manual food and recipe forms work with keyboard open.
- Barcode and OCR review screens clearly invite correction.
- Light and dark modes both preserve macro color meaning.
- No dense editor text clips.
- Bottom sheets and final actions avoid nav bar overlap.
- Repeated logging remains fast.

