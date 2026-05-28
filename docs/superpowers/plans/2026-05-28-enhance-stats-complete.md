# Stat Effects Enhancement Implementation Plan

> **For agentic workers:** Execute tasks in order. Each builds on the previous.

**Goal:** Make all 14 non-magic stats have meaningful gameplay effects with proper progression.

**Architecture:** Extend existing `StatEffectApplier` + `StatPassiveEffects` + `StatCalculator`. Add stat caching for efficiency.

**Tech Stack:** Minecraft Forge 1.20.1, Epic Fight

---

### Task 1: Enhance StatCalculator formulas

**Modify:** `src/main/java/tong/statmod/stats/StatCalculator.java`

- [ ] Add missing formulas: Keen Senses, better progression curves

### Task 2: Keen Senses + Intimidation + Endurance effects

**Modify:** `src/main/java/tong/statmod/stats/StatPassiveEffects.java`

- [ ] Keen Senses: nearby mobs glow when sneaking
- [ ] Intimidation: mob panic AoE on hit
- [ ] Physical Endurance: natural regen boost

### Task 3: Precision bow bonus + Forging durability + Cooking nutrition

**Modify:** `src/main/java/tong/statmod/stats/StatEffectApplier.java`
**Modify:** `src/main/java/tong/statmod/stats/StatPassiveEffects.java`

- [ ] Precision: arrow damage multiplier
- [ ] Forging: durability preservation on item break
- [ ] Cooking: extra nutrition from food

### Task 4: Efficiency - stat caching

**Modify:** `src/main/java/tong/statmod/stats/StatCache.java` (new)
**Modify:** `src/main/java/tong/statmod/stats/StatEffectApplier.java`
**Modify:** `src/main/java/tong/statmod/stats/StatPassiveEffects.java`
**Modify:** `src/main/java/tong/statmod/fatigue/FatigueHandler.java`

- [ ] Create StatCache for per-player level caching
- [ ] Replace raw capability calls in hot paths with cache

### Task 5: Build + commit

- [ ] Build and verify
- [ ] Commit and push
