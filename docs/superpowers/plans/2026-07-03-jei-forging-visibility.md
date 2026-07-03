# JEI Forging Visibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose the full forge flow in JEI by keeping existing `statmod` station categories and adding an Overgeared forging category for smithing anvil recipes.

**Architecture:** Extend the existing `StatModJeiPlugin` with one additional JEI recipe type, record, loader, and category dedicated to Overgeared `ForgingRecipe`. Keep runtime loading through `RecipeManager` and fallback source coverage through tests rather than adding a second dev-tree parser.

**Tech Stack:** NeoForge 1.21.1, JEI plugin API, Overgeared recipe runtime types, JUnit source/resource tests

## Global Constraints

- Keep the existing 3 `statmod` JEI categories intact.
- Add a fourth category only for Overgeared smithing-anvil recipes.
- Read Overgeared recipes from the runtime `RecipeManager`.
- Do not rely on an external Overgeared JEI plugin.

---

### Task 1: Add Overgeared JEI scaffolding

**Files:**
- Create: `src/main/java/tong/statmod/client/jei/OvergearedForgingJeiRecipe.java`
- Create: `src/main/java/tong/statmod/client/jei/OvergearedForgingJeiCategory.java`
- Create: `src/main/java/tong/statmod/client/jei/OvergearedForgingJeiRecipeLoader.java`
- Modify: `src/main/java/tong/statmod/client/jei/StatModJeiRecipeTypes.java`

### Task 2: Register the new JEI category and catalysts

**Files:**
- Modify: `src/main/java/tong/statmod/client/jei/StatModJeiPlugin.java`
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`

### Task 3: Lock the behavior with tests

**Files:**
- Modify: `src/test/java/tong/statmod/client/jei/StatModJeiPluginSourceTest.java`
- Create: `src/test/java/tong/statmod/client/jei/OvergearedForgingJeiRecipeLoaderSourceTest.java`

### Task 4: Verify JEI source coverage

**Files:**
- Verify existing `src/test/java/tong/statmod/client/jei/*.java`
- Verify existing forge resource tests still pass
