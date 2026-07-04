# Unified App Back Stack Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one app-level back-stack model so bottom-tab navigation across Today, Food, Training, and Profile follows the user's actual page sequence.

**Architecture:** Keep AndroidX Navigation as the route renderer, but stop treating bottom-tab back behavior as Navigation's default start-destination pop. Add a small pure stack coordinator for bottom destinations, wire all app-shell tab opens through it, and let route-level screens such as scanners/settings still pop through `NavController`.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX Navigation Compose, JUnit.

---

### Task 1: Pure Stack Contract

**Files:**
- Create: `app/src/main/java/com/musfit/ui/AppNavigationStack.kt`
- Test: `app/src/test/java/com/musfit/ui/AppNavigationStackTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
@Test
fun selectingTabs_pushesDistinctDestinations_andBackPopsInReverseOrder() {
    val stack = AppNavigationStack()

    stack.select(AppDestination.Food)
    stack.select(AppDestination.Training)
    stack.select(AppDestination.Profile)

    assertEquals(AppDestination.Profile, stack.current)
    assertEquals(AppDestination.Training, stack.pop())
    assertEquals(AppDestination.Food, stack.pop())
    assertEquals(AppDestination.Today, stack.pop())
    assertNull(stack.pop())
}
```

- [ ] **Step 2: Run test red**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.AppNavigationStackTest" --no-daemon --console=plain
```

Expected: compile failure because `AppNavigationStack` does not exist.

- [ ] **Step 3: Implement minimal stack**

Create `AppNavigationStack` with `current`, `canPop`, `select(destination)`, `replace(destination)`, and `pop()`.

- [ ] **Step 4: Run test green**

Run the same focused test command and confirm it passes.

### Task 2: AppNavGraph Wiring

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/AppNavGraph.kt`
- Modify: `app/src/main/java/com/musfit/ui/AppDestination.kt`
- Test: `app/src/test/java/com/musfit/ui/AppDestinationTest.kt`

- [ ] **Step 1: Extend tests**

Add route ownership assertions so Food-owned scanner routes keep the Food tab selected.

- [ ] **Step 2: Wire app-level back**

Use `remember { AppNavigationStack() }` in `AppNavGraph`, route bottom-tab clicks and cross-tab callbacks through `select`, and add a `BackHandler` that pops the app stack only while the current route is a bottom destination.

- [ ] **Step 3: Preserve route-level pop**

Keep barcode scanner, nutrition-label scanner, and profile settings on `NavController` so gesture back first exits those pages.

### Task 3: Child Route-Like Back Handling

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`

- [ ] **Step 1: Add child back handler**

When Training is showing the active workout route-like surface, consume system back and call `closeActiveWorkoutRoute()` before the app-level tab stack can pop to a previous tab.

### Task 4: Verification

- [ ] Run focused unit tests:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.AppNavigationStackTest" --tests "com.musfit.ui.AppDestinationTest" --no-daemon --console=plain
```

- [ ] Run full verification:

```powershell
.\scripts\dev\verify-musfit.ps1 -Preset Full
```

- [ ] Install and seed emulator:

```powershell
.\scripts\android\install-seed-musfit.ps1 -Reset
```

- [ ] Manually verify app flow: Today -> Food -> Training -> Profile, then gesture back to Training, Food, Today. Also verify Profile Settings and Food scanners pop back to their owning tab first.
