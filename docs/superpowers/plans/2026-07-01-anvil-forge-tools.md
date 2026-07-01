# Enchantment Anvil Forge Tools Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add two real forge support tools and make advanced enchantment-anvil recipes require them instead of grips.

**Architecture:** Register a focused `ForgingTools` item group, expose the tools in the creative tab, migrate the anvil support rules and recipe catalog to explicit tool ids, and keep JSON fallback essence recipes aligned with the same support requirements. Use tests first to lock the support behavior before touching recipe logic.

**Tech Stack:** NeoForge 1.21.1, Java 21, Minecraft item/menu APIs, JUnit 5, Gradle

## Global Constraints

- Add two forge support items now.
- Make them real registered items, visible in the creative tab, with lang/model/recipe resources.
- Keep them non-consumable.
- Require them in the `enchantment_anvil` support slot for all current essence recipes.
- Do not add durability yet.
- Do not add tool tiers yet.
- Keep `infusion_forge` grip-based.
- Update current essence JSON fallback resources so they stay aligned with the Java anvil catalog.

---

### Task 1: Add Forge Tool Definitions And Support Rules

### Task 2: Migrate Enchantment Anvil Recipes From Grips To Tools

### Task 3: Add Tool Resources And Verify Drift Coverage
