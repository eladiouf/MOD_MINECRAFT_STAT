# STAT MOD — Remaining Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete all remaining STAT MOD features: config, stat effects, perk effects, GUI, polish.

**Architecture:** ModConfigSpec → Stat effects (8 missing) → Perk effects (64 missing) → Perk GUI → SkillPerkGate mappings → Polish. Each phase is independent except where noted.

**Tech Stack:** NeoForge 21.1.219, Java 21, Mojang mappings, JUnit 5.10

---

## File Structure

### Config System
- Modify: `src/main/java/tong/statmod/config/Config.java` — Replace hardcoded constants with NeoForge ModConfigSpec (server + client)
- New: `src/main/java/tong/statmod/config/ConfigHolder.java` — Static holder for config registration + loading
- Modify: `src/main/java/tong/statmod/STATMod.java` — Register config event

### Stat Effects (8 missing)
- Modify: `src/main/java/tong/statmod/stats/StatEffectApplier.java` — Add PHYSICAL_ENDURANCE, TRACKING, KEEN_SENSES, FORGING, COOKING, ALCHEMY, MAGICAL_POWER, MANA_REGENERATION

### Perk Effects (64 missing)
- Modify: `src/main/java/tong/statmod/perks/PerkEffectHandler.java` — Add all missing perk effects organized by tier

### Perk GUI
- New: `src/main/java/tong/statmod/client/gui/PerkScreen.java` — Main perk tree screen
- New: `src/main/java/tong/statmod/client/gui/PerkNodeWidget.java` — Perk node widget
- New: `src/main/java/tong/statmod/client/gui/TalentTreePanel.java` — Skill tree panel per stat

### SkillPerkGate Mappings
- Modify: `src/main/java/tong/statmod/integration/SkillPerkGate.java` — Add real skill→perk and race→perk mappings

### Polish
- New: `src/main/java/tong/statmod/sound/ModSounds.java` — Custom sound events
- Modify: `src/main/java/tong/statmod/client/StatTabScreen.java` — Add shortcut to perk screen
- New: `src/main/java/tong/statmod/item/ModCreativeTab.java` — Creative tab for items

---
