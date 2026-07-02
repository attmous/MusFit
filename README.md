# MusFit

**Your AI strength coach — with a real app around it.**
MusFit is an Android strength-training and nutrition tracker built to become the rich, hands-on
interface for a personal AI coach that *you* run: a locally hosted model, your own API key, or a
personal agent you already operate. No account. No subscription. Nobody else's cloud.

## The idea

The best coach sees the whole picture — what you ate, how you trained, how you slept — and
tells you what to do next: what to eat, when to go to bed, and what to put on the bar.
AI models are finally good enough to be that coach, and personal
agents (OpenClaw, Hermes, and friends) already run on people's own machines with their own
data. What's missing is the interface: a plain chat window can't photograph a plate, scan a
barcode, time a rest period, read your wearable, or draw a calorie ring.

MusFit is that interface — a feature-rich native app that gives a personal AI eyes and hands:

- **A serious tracker underneath.** Meals, workouts, body measurements, water, goals — all
  structured data in a local Room database. The coach reasons over real numbers, not vibes,
  and everything it suggests lands back in the same diary you can edit by hand.
- **Bring your own AI.** The intelligence layer is pluggable: an on-device model, any
  API-compatible endpoint with your own key, or a bridge to a local agent that already knows
  you. MusFit itself has no AI vendor lock-in and no middleman.
- **The coach comes to you.** The **Today** tab is the coach's feed — proactive, contextual
  cues: when to head to bed, how much to eat today, how hard to train given yesterday's
  session and last night's sleep. Separately, a **floating chat** gives you a full
  ChatGPT/Claude-style conversation with the same coach whenever you want to ask, plan, or
  push back.
- **Local-first, still.** Your data lives on the phone. AI access happens on your terms —
  local inference or your own key — never through a MusFit account, because there isn't one.

And the coach has a specialty: MusFit is **strength-first**. The training half is a serious gym
log, and the programming, recovery, and nutrition intelligence all orbit one goal — steady
progress under the bar.

## The daily loop

1. **Photograph your meals.** The coach analyzes the photo, estimates calories and the
   protein/carb/fat split, and drafts the diary entry for your review — barcode scanning,
   saved foods, and manual entry remain for precision.
2. **Train with the built-in logger.** Sets, supersets, RPE, rest timer — the coach observes
   volume, PRs, and calories burned.
3. **Wear whatever you wear.** Steps, sleep duration, and sleep quality flow in from your
   watch or band (Fitbit, Pixel Watch, or similar) via Android Health Connect.
4. **Get coached.** Today's feed turns the combined picture into concrete cues; the floating
   chat answers anything on demand, with full context.

## Where it stands today

The tracker foundation is shipped and daily-drivable; the AI layer is the active frontier.

**Shipped:** the four-tab app described below, a deterministic (rule-based) coach feed on
Today, AI logging shells in Food (text-draft logging works; photo and voice are UX shells),
and Health Connect integration that reads steps, weight, and heart data and exports workouts,
nutrition, and hydration.

**The AI coach roadmap:**

- [ ] Pluggable AI backend — on-device model, OpenAI/Anthropic-compatible API key, or a bridge
      to a locally running agent (OpenClaw, Hermes, or similar)
- [ ] Meal-photo analysis → calories + macro split drafted into the diary
- [ ] Voice logging through the mic
- [ ] Sleep duration and quality import from wearables via Health Connect
- [ ] AI-generated coach feed on Today — bedtime, intake, and training-intensity cues from the
      combined nutrition/training/sleep picture
- [ ] Floating coach chat with full local context

## The end game — a complete strength coach

The roadmap above is the plumbing. The product it enables is a coach that can stand in for a
good human strength coach, end to end — all on the same bring-your-own-AI, local-first terms.

**Programming that adapts**

- A chat-based onboarding interview — goals, experience, injuries, equipment, schedule — that
  produces a real periodized program, not a template picked from a list.
- Autoregulation: the next session's targets adjust to logged RPE, recent stalls, and last
  night's sleep ("you slept five hours — keep the volume, drop the intensity 10%").
- Fatigue management: the coach notices RPE creep and stalled lifts, schedules deloads, and
  rebalances weekly volume per muscle group before problems become injuries.
- Plateau busting: per-lift e1RM trends with strength-standard context, and concrete fixes —
  exercise variations, volume changes, technique cues.
- The whole athlete: programmed warm-up and mobility blocks per session, and conditioning days
  driven by heart-rate data from the wearable — supporting the lifting, never replacing it.

**Eyes and ears in the gym**

- Form check: record a set and on-device pose estimation gives bar-path, depth, and tempo
  feedback — no video ever leaves the phone.
- Hands-free logging: "bench, eighty kilos, eight reps, RPE eight" — voice logging mid-set
  with the rest timer talking back, so the phone stays in the pocket.
- Machine taken? Ask the coach for an equivalent substitution based on the equipment your gym
  actually has.
- Gym profiles: per-gym equipment inventories (home rack vs. commercial gym vs. hotel), so
  substitutions, plate math, and program generation match wherever you're training.
- On the wrist: a Wear OS companion built for mid-workout use — the current exercise and
  target reps at a glance, the rest timer counting down between sets right on the watch face,
  and one-tap set logging, so the phone can stay in the bag.

**Nutrition in service of training**

- Adaptive targets: the coach re-estimates your real energy expenditure weekly from logged
  intake against the weight trend, and adjusts calories and macros — no static formula.
- Phase management: structured bulk / cut / recomp cycles with scheduled check-ins and exit
  criteria.
- Training-day awareness: macro cycling between training and rest days, and pre-/post-workout
  meal timing tied to the session on the calendar.
- "What should I eat?" — meal and recipe suggestions that fit the macros you have left today,
  drawn from foods you actually log and what's on the shopping list.

**A supplement loop that closes itself**

- Supplements as a first-class diary: creatine, protein, vitamins — dose, timing, and
  adherence streaks logged alongside food.
- Impact, measured: the coach correlates supplement adherence with the trends it already
  tracks — strength progress, sleep quality, body weight — and surfaces what actually seems
  to be working for you.
- Auto-replenishment: an opt-in Stripe integration reorders your staples before they run out,
  against a budget and schedule you set — driven by tracked usage so orders match real
  consumption, and cancellable like everything else.

**Recovery as a first-class input**

- A morning readiness picture — sleep, resting heart rate, and a quick soreness check-in on a
  body map — feeding directly into today's training cue.
- Injury-aware programming: log a tweaked shoulder and the program routes around it, then
  ramps back.

**A relationship, not a dashboard**

- Weekly coach reports: adherence, what moved, what stalled, and what changes next week — like
  a real coaching check-in.
- Progress photos, stored on-device, compared side-by-side over time alongside the weight and
  measurement trends.
- Proactive by default: cues arrive as notifications from the coach at the moment they matter,
  not as stats you have to remember to look at — plus home-screen widgets for the day's
  headline numbers.
- Bring your history: import workouts and diaries from Hevy, Strong, or MyFitnessPal exports,
  so the coach starts out knowing your training age instead of treating you as a blank slate.

## What's inside

Four tabs, each a focused miniapp with its own accent color on a shared design language.

### 📅 Today — the coach's home

The daily dashboard and the coach's mouthpiece: a configurable metric carousel (calories,
macros, steps, water, weight, training volume, …), progress rings, weekly goals, and the coach
feed — today deterministic cues generated locally from your own data, tomorrow the AI coach.

### 🍽 Food — calories in

A full food diary with the depth of the big trackers:

- **Diary** — date navigation, calorie ring, macro + advanced-nutrient + micronutrient
  progress, custom meals with times, deterministic daily insights, and a day-rating card.
- **Add flow** — saved foods, recents, favorites, "same as yesterday", templates, recipes,
  manual entry, quick calories with presets, and barcode scanning with Open Food Facts lookup.
- **Food database** — a full editor (per-100 g or per-serving, custom serving units, complete
  macros and micros), local + online search, duplicate detection and merge, starter foods.
- **Recipes & templates** — recipes with ingredients, cooked yield, and per-serving nutrition;
  reusable meal templates; both editable, favoritable, and loggable in fractional servings.
- **Goals & diet modes** — calorie/macro/advanced-nutrient targets, Balanced / High-Protein /
  Keto / Muscle-Gain / Weight-Loss / Custom modes, net-carbs toggle, optional
  include-training-calories.
- **Planning** — plan future days, planned-vs-logged tracking, copy day/meal, a 7-day plan
  strip, and a shopping list generated from planned meals.
- **Water tracking** with goals, and nutrition/hydration export to Health Connect.
- **Experimental** — nutrition-label OCR (camera scan with user review) and AI text-draft
  logging, the seed of the photo/voice flows above.

### 🏋️ Training — calories out

A structured strength log in the spirit of dedicated workout apps:

- Routine builder with an exercise library (muscle groups, instructions, personal notes).
- Active workout logging — sets, reps, weight, RPE, set types, supersets, and a rest timer.
- Plate-loading hints, PR detection, and a workout finish flow with recaps.
- Workout history and progress views, plus workout export to Health Connect.

### 👤 Profile — the long-term trend

The body and progress hub: a weight hero card with trend, body-measurement tiles with
sparklines, goal and target-weight management, plan launchers, and app settings.

## How it's built

Single-module Android app (`:app`, `com.musfit`) with strict one-direction layering:

```text
Compose screen → ViewModel (StateFlow) → Repository interface → Room DAO / Open Food Facts
```

| Area | Choice |
| --- | --- |
| Language / UI | Kotlin, Jetpack Compose, Material 3 (Expressive), single activity |
| State | Hilt ViewModels exposing immutable `StateFlow`, date-scoped `flatMapLatest` streams |
| Storage | Room (schema v27, exported schemas, migration-only — no destructive fallback) |
| Domain | Pure Kotlin calculators (`NutritionCalculator`, `WorkoutCalculator`) with no Android deps |
| Integrations | Retrofit + Moshi (Open Food Facts), CameraX + ML Kit (barcode/label scan), Health Connect behind a fakeable gateway |
| Testing | TDD culture: JUnit ViewModel tests with hand-written fakes, Robolectric repository/DAO tests against in-memory Room with real migrations, pure domain tests |
| Min / target SDK | 28 (Android 9) / 37 |

The full architecture map lives in [`docs/architecture/`](docs/architecture/README.md), with a
deep dive into the Food miniapp in
[`docs/architecture/food-system.md`](docs/architecture/food-system.md). The design system
(shared header/summary-card language, per-tab accents, spacing/shape/type tokens) is documented
under [`docs/design/`](docs/design/musfit-design-system.md). Feature specs and implementation
plans are kept in [`docs/superpowers/`](docs/superpowers/) — the repo is developed spec-first,
and every shipped slice has a written plan.

## Building from source

Requirements: **JDK 17**, an Android SDK with **API 37**, Windows PowerShell (the repo's
tooling is PowerShell-first; a POSIX setup works with the equivalent Gradle invocations).

Set up the local Android toolchain for the shell (local, untracked bootstrap — if it's missing,
point `JAVA_HOME`/`ANDROID_HOME` at JDK 17 and your SDK yourself):

```powershell
. .\.superpowers\sdd\android-env.ps1
```

Run the full verification build:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

Install the debug APK on a connected device or emulator:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell monkey -p com.musfit -c android.intent.category.LAUNCHER 1
```

CI (GitHub Actions, [`android.yml`](.github/workflows/android.yml)) runs the same tests, lint,
and assembly on every push and uploads the debug APK as the `musfit-debug-apk` artifact.

## Status

MusFit is a personal project under active development. The Food miniapp has shipped nearly all
of its original 24-slice roadmap; Training, Today, and Profile are shipped and being polished
under a unified cross-tab design language. The AI coach layer is the next chapter. There are
no store releases — grab the CI artifact or build from source.

## Privacy

All health, meal, body, and workout data stays on-device. MusFit has no accounts, no cloud
sync, no analytics or tracking, no subscriptions, and no social features. AI is strictly
opt-in and bring-your-own: on-device inference, your own API key, or your own local agent —
requests go where you point them and nowhere else. The only other integrations are Open Food
Facts (outbound product lookups only) and Android Health Connect (on-device,
permission-gated).
