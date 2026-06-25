# Perk Tree UX Readability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve the custom STAT Mod perk tree so node state, blocker reasons, and unlock affordance are obvious at a glance without changing perk mechanics.

**Architecture:** Extract perk-node presentation logic into a small pure helper that classifies unlock state and builds tooltip lines, then make `TalentTreePanel` consume that model for rendering and click feedback. Keep gameplay authority in existing perk data and caches; only the client presentation layer changes.

**Tech Stack:** Java 21, NeoForge 1.21.1 client GUI classes, JUnit 5, existing `ClientPerkCache` / `ClientStatCache` / `RaceEffectApplier` client state

---

## File Structure

- Create: `src/main/java/tong/statmod/client/gui/PerkNodeVisualState.java`
  - Enum for `UNLOCKED`, `AVAILABLE`, `LOCKED_STAT`, `LOCKED_POINTS`, `LOCKED_PREREQ`, `LOCKED_MIXED`
- Create: `src/main/java/tong/statmod/client/gui/PerkNodePresentation.java`
  - Pure helper that resolves visual state, tooltip lines, blocker priority, and final status text
- Modify: `src/main/java/tong/statmod/client/gui/PerkNodeWidget.java`
  - Centralize node border/fill/text colors by visual state while keeping tier color helpers
- Modify: `src/main/java/tong/statmod/client/gui/TalentTreePanel.java`
  - Use `PerkNodePresentation`, track selected node, render state-aware nodes, build stable tooltips, and keep click validation consistent with the visible blockers
- Test: `src/test/java/tong/statmod/client/gui/PerkNodePresentationTest.java`
  - Verify classification and tooltip line generation
- Modify: `src/test/java/tong/statmod/client/gui/TalentTreePanelTest.java`
  - Keep the existing outside-click regression and add lightweight state/selection assertions if practical

### Task 1: Add pure perk-node presentation model

**Files:**
- Create: `src/main/java/tong/statmod/client/gui/PerkNodeVisualState.java`
- Create: `src/main/java/tong/statmod/client/gui/PerkNodePresentation.java`
- Test: `src/test/java/tong/statmod/client/gui/PerkNodePresentationTest.java`

- [ ] **Step 1: Write the failing classification and tooltip tests**

```java
@Test
void resolvesAvailableStateWhenLevelPointsAndPrereqPass() {
    PerkNodePresentation model = PerkNodePresentation.resolve(
            false,
            true,
            true,
            true,
            12,
            3,
            12,
            2,
            "Heavy Swing");

    assertEquals(PerkNodeVisualState.AVAILABLE, model.state());
    assertEquals("Click to unlock", model.statusLine().getString());
}

@Test
void resolvesMixedLockedStateWhenSeveralRequirementsFail() {
    PerkNodePresentation model = PerkNodePresentation.resolve(
            false,
            false,
            false,
            false,
            8,
            1,
            12,
            3,
            "Heavy Swing");

    assertEquals(PerkNodeVisualState.LOCKED_MIXED, model.state());
    assertEquals("Blocked: prerequisite perk missing", model.statusLine().getString());
    assertTrue(model.detailLines().stream().anyMatch(line -> line.getString().contains("Stat requirement: 8 / 12")));
    assertTrue(model.detailLines().stream().anyMatch(line -> line.getString().contains("Perk points: 1 / 3")));
    assertTrue(model.detailLines().stream().anyMatch(line -> line.getString().contains("Requires: Heavy Swing")));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.client.gui.PerkNodePresentationTest`
Expected: FAIL because `PerkNodePresentation` and `PerkNodeVisualState` do not exist yet

- [ ] **Step 3: Write the minimal presentation implementation**

```java
public enum PerkNodeVisualState {
    UNLOCKED,
    AVAILABLE,
    LOCKED_STAT,
    LOCKED_POINTS,
    LOCKED_PREREQ,
    LOCKED_MIXED
}

public record PerkNodePresentation(
        PerkNodeVisualState state,
        List<Component> detailLines,
        Component statusLine) {

    public static PerkNodePresentation resolve(
            boolean unlocked,
            boolean meetsLevel,
            boolean canAfford,
            boolean hasPrerequisite,
            int currentLevel,
            int currentPoints,
            int requiredLevel,
            int requiredPoints,
            @Nullable String prerequisiteName) {
        // Resolve dominant state, then build detail lines in stable order.
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.client.gui.PerkNodePresentationTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/client/gui/PerkNodeVisualState.java src/main/java/tong/statmod/client/gui/PerkNodePresentation.java src/test/java/tong/statmod/client/gui/PerkNodePresentationTest.java
git commit -m "feat(perks): add node presentation model"
```

### Task 2: Make node rendering reflect readable state

**Files:**
- Modify: `src/main/java/tong/statmod/client/gui/PerkNodeWidget.java`
- Modify: `src/main/java/tong/statmod/client/gui/TalentTreePanel.java`
- Modify: `src/test/java/tong/statmod/client/gui/TalentTreePanelTest.java`
- Create: `src/test/java/tong/statmod/client/gui/PerkNodeWidgetTest.java`

- [ ] **Step 1: Write the failing UI-state rendering tests**

```java
@Test
void selectedNodeUsesDifferentFillThanHoveredNode() {
    int hovered = PerkNodeWidget.fillColor(PerkNodeVisualState.AVAILABLE, true, false);
    int selected = PerkNodeWidget.fillColor(PerkNodeVisualState.AVAILABLE, false, true);

    assertNotEquals(hovered, selected);
}

@Test
void ignoresClicksOutsidePanel() {
    TalentTreePanel panel = new TalentTreePanel(List.of(StatType.values()), 0, 0, 300, 300);
    assertFalse(panel.mouseClicked(350, 350, 0));
    assertFalse(panel.mouseClicked(-10, -10, 0));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.client.gui.PerkNodeWidgetTest --tests tong.statmod.client.gui.TalentTreePanelTest`
Expected: FAIL because `fillColor(...)` does not exist yet

- [ ] **Step 3: Implement state-aware node styling**

```java
public static int fillColor(PerkNodeVisualState state, boolean hovered, boolean selected) {
    int base = switch (state) {
        case UNLOCKED -> 0xFF243226;
        case AVAILABLE -> 0xFF1B2438;
        case LOCKED_STAT -> 0xFF241A1A;
        case LOCKED_POINTS -> 0xFF241F1A;
        case LOCKED_PREREQ -> 0xFF201A24;
        case LOCKED_MIXED -> 0xFF1A1A1A;
    };
    if (selected) return brighten(base, 0x00181818);
    if (hovered) return brighten(base, 0x000E0E0E);
    return base;
}
```

```java
PerkNodePresentation presentation = PerkNodePresentation.resolve(...);
boolean selected = perk == selectedPerk;
renderNode(graphics, font, sx, ny, perk, presentation, hovered, selected);
```

- [ ] **Step 4: Run tests to verify behavior**

Run: `.\gradlew.bat test --tests tong.statmod.client.gui.PerkNodeWidgetTest --tests tong.statmod.client.gui.TalentTreePanelTest --tests tong.statmod.client.gui.PerkNodePresentationTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/client/gui/PerkNodeWidget.java src/main/java/tong/statmod/client/gui/TalentTreePanel.java src/test/java/tong/statmod/client/gui/PerkNodeWidgetTest.java src/test/java/tong/statmod/client/gui/TalentTreePanelTest.java
git commit -m "feat(perks): restyle perk tree node states"
```

### Task 3: Replace ad-hoc tooltips with stable blocker-first tooltips

**Files:**
- Modify: `src/main/java/tong/statmod/client/gui/TalentTreePanel.java`
- Modify: `src/main/java/tong/statmod/client/gui/PerkNodePresentation.java`
- Test: `src/test/java/tong/statmod/client/gui/PerkNodePresentationTest.java`

- [ ] **Step 1: Extend the failing tooltip test for stable order**

```java
@Test
void buildsTooltipInStableReadableOrder() {
    PerkNodePresentation model = PerkNodePresentation.resolve(
            false,
            true,
            false,
            true,
            12,
            1,
            12,
            3,
            null);

    List<Component> tooltip = model.tooltipLines(
            Component.literal("Heavy Swing"),
            Component.literal("A heavy melee finisher"),
            Component.literal("ACTIVE"),
            Component.literal("Cost: 3 points"));

    assertEquals("Heavy Swing", tooltip.get(0).getString());
    assertEquals("A heavy melee finisher", tooltip.get(1).getString());
    assertEquals("ACTIVE", tooltip.get(2).getString());
    assertEquals("Cost: 3 points", tooltip.get(3).getString());
    assertEquals("Blocked: not enough perk points", tooltip.get(tooltip.size() - 1).getString());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests tong.statmod.client.gui.PerkNodePresentationTest`
Expected: FAIL because stable tooltip composition is not implemented yet

- [ ] **Step 3: Implement stable tooltip composition in panel rendering**

```java
List<Component> tooltip = presentation.tooltipLines(
        Component.literal(perk.name),
        Component.literal(perk.description),
        Component.literal(perk.tier.name()),
        Component.literal("Cost: " + perk.tier.cost + " point" + (perk.tier.cost > 1 ? "s" : "")));
renderTooltipBox(graphics, font, tooltipX, tooltipY, tooltip, PerkNodeWidget.tierColor(perk.tier));
```

- [ ] **Step 4: Run tests to verify it passes**

Run: `.\gradlew.bat test --tests tong.statmod.client.gui.PerkNodePresentationTest --tests tong.statmod.client.gui.TalentTreePanelTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/client/gui/TalentTreePanel.java src/main/java/tong/statmod/client/gui/PerkNodePresentation.java src/test/java/tong/statmod/client/gui/PerkNodePresentationTest.java
git commit -m "feat(perks): stabilize perk tree tooltips"
```

### Task 4: Final verification and cleanup

**Files:**
- Modify: `docs/superpowers/plans/2026-06-25-perk-tree-ux-readability-implementation.md`
  - Optionally check off completed steps during execution

- [ ] **Step 1: Run focused GUI tests**

Run: `.\gradlew.bat test --tests tong.statmod.client.gui.PerkNodePresentationTest --tests tong.statmod.client.gui.TalentTreePanelTest`
Expected: PASS

- [ ] **Step 2: Run broader project verification**

Run: `.\gradlew.bat build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Review git diff for accidental noise**

Run: `git status --short`
Expected: only intended perk-tree UX files staged or modified for commit

- [ ] **Step 4: Commit the finished implementation**

```bash
git add src/main/java/tong/statmod/client/gui/PerkNodeVisualState.java src/main/java/tong/statmod/client/gui/PerkNodePresentation.java src/main/java/tong/statmod/client/gui/PerkNodeWidget.java src/main/java/tong/statmod/client/gui/TalentTreePanel.java src/test/java/tong/statmod/client/gui/PerkNodePresentationTest.java src/test/java/tong/statmod/client/gui/PerkNodeWidgetTest.java src/test/java/tong/statmod/client/gui/TalentTreePanelTest.java
git commit -m "feat(perks): improve perk tree readability"
```

## Self-Review

- **Spec coverage:** The plan covers visual state classification, stable tooltip order, family-aware rendering reuse, selected/hovered readability, and targeted tests. It intentionally does not change perk costs, balance, or gameplay rules.
- **Placeholder scan:** No `TODO`, `TBD`, or implicit “write tests later” steps remain. Each task has explicit files, commands, and expected outcomes.
- **Type consistency:** The plan consistently uses `PerkNodeVisualState`, `PerkNodePresentation`, `tooltipLines(...)`, and `selectedPerk` semantics across all tasks.
