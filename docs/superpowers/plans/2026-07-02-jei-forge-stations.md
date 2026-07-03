# JEI Forge Stations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add full JEI integration for `infusion_forge`, `infusion_forge` assembly mode, and `enchantment_anvil`.

**Architecture:** Register one JEI plugin with three recipe categories. Feed infusion and enchantment recipes from the existing Java catalogs, and feed assembly recipes from the generated `data/statmod/recipe/assembly/**` JSONs with filtering for forge-compatible inputs.

**Tech Stack:** NeoForge 1.21.1, JEI `19.27.0.343`, Java 21, JUnit 5

## Global Constraints

- Keep existing station behavior unchanged.
- Reuse existing catalogs instead of duplicating recipe logic.
- Only expose `statmod:assembly/*` recipes that match the forge workflow.
- Use TDD: failing tests first.

---

### Task 1: Add JEI plugin scaffolding
- [ ] Add failing source tests for JEI plugin, categories, and catalysts.
- [ ] Implement `@JeiPlugin` registration and 3 category ids.
- [ ] Verify tests pass.

### Task 2: Expose infusion and enchantment recipes
- [ ] Add failing tests for recipe wrappers/loaders from Java catalogs.
- [ ] Implement recipe record wrappers and category layouts.
- [ ] Verify tests pass.

### Task 3: Expose assembly recipes inside infusion forge
- [ ] Add failing tests for assembly loader filtering.
- [ ] Implement JSON assembly recipe loader and JEI category population.
- [ ] Verify tests pass.

### Task 4: Verify integration
- [ ] Run focused JEI/plugin tests.
- [ ] Run compile verification.
