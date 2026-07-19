# Dungeon AI Phase 3 — Tactical Room Roles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Assign varied tactical roles to dungeon mobs and add six real supplemental behaviors while preserving strict room-level squad boundaries.

**Architecture:** A pure deterministic policy assigns one of twelve roles using floor, room and spawn ordinal. Role metadata persists in NBT. Small Forge `Goal` classes implement reusable behavior and are attached idempotently after spawn/reload.

**Tech Stack:** Java 17, Forge 47.4.4, Minecraft 1.20.1 AI Goals, JUnit 5.

## Global Constraints

- Squad IDs remain `floor:room:roomIndex`; alerts and support never cross squad IDs.
- Boss squads remain `floor:boss`.
- Goals must be idempotent after chunk reload.
- No role may modify mobs outside the Trial Dungeon.
- No Cataclysm entity identifiers.

---

### Task 1: Deterministic role catalog

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/ai/DungeonTacticalRole.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/DungeonTacticalRolePolicy.java`
- Test: `src/test/java/tong/statmod/dungeon/ai/DungeonTacticalRolePolicyTest.java`

**Interfaces:**
- Produces `DungeonTacticalRole roleFor(DungeonFaction faction, int floor, int room, int ordinal, boolean boss)`.

- [ ] Test determinism, early-floor restriction, deep-floor variety and boss command roles; verify red.
- [ ] Add roles `SHIELD_CAPTAIN`, `SPEAR_KEEPER`, `BERSERKER`, `WARDEN`, `SPELLBREAKER`, `ELEMENTAL_CASTER`, `BATTLE_CLERIC`, `NECROMANCER`, `HEXER`, `ARCANE_ARTILLERY`, `SAPPER`, `SCOUT`, `HUNTER`, `AMBUSHER`, `JAILER`.
- [ ] Floors 1–10 use captain, spear, scout and warden; floors 11–30 add berserker/hunter; floors 31–50 add spellbreaker/hexer/sapper; floors 51+ use all roles. Boss ordinal zero is always `SHIELD_CAPTAIN`, followed by `ELEMENTAL_CASTER` and `BATTLE_CLERIC`.
- [ ] Run tests and commit `feat: define deterministic dungeon tactical roles`.

---

### Task 2: Persist and recover tactical roles

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/ai/DungeonAiActor.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/DungeonTacticalGoals.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonBossAltarBlock.java`
- Modify: `src/main/java/tong/statmod/dungeon/ai/DungeonEncounterDirector.java`
- Test: `src/test/java/tong/statmod/dungeon/ai/DungeonTacticalWiringContractTest.java`

**Interfaces:**
- Adds `TACTICAL_ROLE_TAG` and `initialize(..., DungeonTacticalRole role)` overload.
- Produces `DungeonTacticalGoals.ensureAttached(Mob)`.

- [ ] Write a source contract requiring role initialization at both spawn boundaries and goal recovery in the director; verify red.
- [ ] Persist the role name and provide a safe getter defaulting to `WARDEN`.
- [ ] Assign roles from floor/room/position ordinal at ordinary spawn and from boss slot at altar spawn.
- [ ] Call `ensureAttached` after initialization and on every director cycle.
- [ ] Run contract tests and commit `feat: persist and recover tactical dungeon roles`.

---

### Task 3: Six supplemental behavior goals

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/ai/goal/ScoutGoal.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/goal/WardenGoal.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/goal/BerserkerGoal.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/goal/SpellbreakerGoal.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/goal/HunterGoal.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/goal/SapperGoal.java`
- Modify: `src/main/java/tong/statmod/dungeon/ai/DungeonTacticalGoals.java`

**Interfaces:**
- Scout retreats toward its room anchor after observing a player and spreads the existing alert.
- Warden returns to its spawn anchor when dragged more than 14 blocks.
- Berserker receives temporary speed and damage after dropping below 50% health.
- Spellbreaker prioritizes players currently using an item and interrupts with knockback on a 100-tick cooldown.
- Hunter follows valid last-known-position memory when it has no visible target.
- Sapper places a telegraphed, temporary damaging hazard no closer than 3 blocks to a player, at most once per 160 ticks.

- [ ] Implement each focused goal with `Goal.Flag.MOVE` or `TARGET` only where necessary.
- [ ] Map captain/spear/warden/jailer to WardenGoal; scout/ambusher to ScoutGoal; berserker to BerserkerGoal; spellbreaker/hexer to SpellbreakerGoal; hunter to HunterGoal; sapper/artillery to SapperGoal.
- [ ] Ensure no duplicate goal class is attached.
- [ ] Run all dungeon AI and party tests.

---

### Task 4: Full role validation

**Files:**
- Test: `src/test/java/tong/statmod/dungeon/ai/DungeonTacticalRoleCoverageTest.java`

- [ ] Test that the policy selects every role somewhere across floors 1–100, rooms 0–8 and ordinals 0–15.
- [ ] Run focused tests, then `.\gradlew.bat clean test build --console=plain`.
- [ ] Verify `rg -n -i "cataclysm:"` across active dungeon source returns zero matches.
- [ ] Commit `feat: add tactical room AI behaviors` and push `forge-1.20.1`.
