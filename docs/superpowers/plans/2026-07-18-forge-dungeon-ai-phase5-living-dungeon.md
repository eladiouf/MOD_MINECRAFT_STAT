# Dungeon AI Phase 5 — Living Dungeon Actors Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Populate combat floors with bounded neutral inhabitants and specialized hostile actors while preserving sequential room spawning and completion counts.

**Architecture:** A pure deterministic policy selects living roles by floor, room and wave ordinal. Neutral actors spawn only in the safehouse during floor construction and carry a non-combat marker. Hostile living roles replace existing wave slots, so they appear only when their chamber becomes active and remain part of that chamber's completion count. Small goals and one interaction handler implement scavenging, rescue, survival and engineering behavior.

**Tech Stack:** Java 17, Forge 47.4.4 events and AI Goals, Minecraft 1.20.1 entities/NBT, JUnit 5.

## Global Constraints

- Hostile actors never add to wave size; they replace an existing queued mob.
- Neutral inhabitants spawn only in room `DungeonLayout.ROOM_COUNT / 2` and never count toward room or floor completion.
- Every hostile actor uses squad ID `floor:room:roomIndex` and spawns only with that active room's deferred queue.
- Neutral actors use faction `INHABITANTS`; the encounter director never targets players for them.
- Prisoner release requires a deliberate player interaction and persists in NBT.
- Population is deterministic and capped at four neutral inhabitants plus three special hostile slots per combat floor.
- No Cataclysm identifiers or addon class imports.

---

### Task 1: Deterministic living-role policy

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/ai/living/DungeonLivingRole.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/living/DungeonLivingEventPolicy.java`
- Test: `src/test/java/tong/statmod/dungeon/ai/living/DungeonLivingEventPolicyTest.java`

**Interfaces:**
- Produces `List<DungeonLivingRole> safehouseRoles(int floor)`.
- Produces `Optional<DungeonLivingRole> combatRole(int floor, int roomIndex, int ordinal)`.

- [ ] Write tests requiring early floors to have at most one survivor, floor 31+ to introduce prisoner/rival roles, floor 51+ ritualists, floor 71+ engineers, deterministic output and all-role coverage through floor 100; verify red.
- [ ] Implement roles `RIVAL_EXPLORER`, `WOUNDED_SURVIVOR`, `PRISONER`, `WANDERING_MERCHANT`, `SCAVENGER`, `RITUALIST`, and `ENGINEER` with the stated thresholds and fixed room/ordinal slots.
- [ ] Run the focused test and commit `feat: select bounded living dungeon events`.

### Task 2: Neutral safehouse population

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/ai/living/DungeonLivingActor.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/living/DungeonLivingPopulation.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonRoomChain.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java`
- Test: `src/test/java/tong/statmod/dungeon/ai/living/DungeonLivingPopulationContractTest.java`

**Interfaces:**
- Adds persistent tags `LIVING_ROLE_TAG`, `NON_COMBAT_TAG`, and `PRISONER_RELEASED_TAG`.
- Produces `populateSafehouse(ServerLevel level, BlockPos floorSpawn, int floor)`.

- [ ] Write a failing source contract requiring safehouse-only population, authorized spawning, persistence, a four-actor cap, and non-combat exclusions in both floor and room counts.
- [ ] Spawn player-model adventurers for survivors/prisoners, a wandering trader for merchants and a fox for scavengers at deterministic offsets inside the safehouse.
- [ ] Tag actors as `INHABITANTS`, set their exact living-room squad, and call population after room construction only for combat floors.
- [ ] Exclude `NON_COMBAT_TAG` actors from count, purge and room-completion predicates.
- [ ] Run focused and room-progression tests; commit `feat: populate dungeon safehouses with inhabitants`.

### Task 3: Persistent living behaviors

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/ai/living/DungeonLivingGoals.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/living/goal/SurvivorGoal.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/living/goal/PrisonerFollowGoal.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/living/goal/ScavengerGoal.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/living/goal/EngineerRepairGoal.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/living/DungeonLivingInteractionEvents.java`
- Modify: `src/main/java/tong/statmod/dungeon/ai/DungeonEncounterDirector.java`
- Test: `src/test/java/tong/statmod/dungeon/ai/living/DungeonLivingBehaviorContractTest.java`

**Interfaces:**
- Produces idempotent `DungeonLivingGoals.ensureAttached(Mob actor)`.
- Prisoner interaction sets `PRISONER_RELEASED_TAG`; released prisoners follow only the interacting player's UUID.
- Engineers heal damaged same-squad constructs on a 100-tick cooldown.

- [ ] Write a failing contract for goal recovery, neutral targeting exclusion, explicit prisoner interaction, player ownership, item scavenging and exact-squad engineer repair.
- [ ] Implement survivor retreat/follow behavior, released-prisoner following, scavenger item approach without deleting protected loot, merchant danger avoidance and engineer construct repair.
- [ ] Make the occupied-floor director reattach living goals and skip player aggro for `INHABITANTS`.
- [ ] Run focused and director tests; commit `feat: add persistent living dungeon behaviors`.

### Task 4: Hostile living roles inside room waves

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java`
- Modify: `src/main/java/tong/statmod/dungeon/ai/living/DungeonLivingActor.java`
- Test: `src/test/java/tong/statmod/dungeon/ai/living/DungeonLivingWaveContractTest.java`

**Interfaces:**
- Encodes queued living roles as `statmod:living:<ROLE>` in the existing pending spawn metadata.
- Rival explorer maps to party assassin/hunter, ritualist to mage/hexer, and engineer to tank/warden plus repair goal.

- [ ] Write a failing contract requiring `combatRole` selection before queue insertion, `statmod:adventurer` replacement, no wave-size increment, and exact room squad initialization during deferred spawn.
- [ ] Replace only the policy-selected wave slots, configure compatible party/tactical roles, and preserve scaling, authorization, room index and delayed spawn behavior.
- [ ] Run all dungeon AI/party/room tests; commit `feat: seed living actors into chamber waves`.

### Task 5: Full phase validation

**Files:**
- Modify: `docs/forge-1.20.1-server-validation.md`

- [ ] Run `.\gradlew.bat clean test build --console=plain`; require exit code 0.
- [ ] Scan active dungeon source/resources for `cataclysm:`; require zero matches.
- [ ] Record test totals, artifact size/hash, safehouse cap and wave-replacement invariant.
- [ ] Commit `docs: validate living dungeon AI` and push `forge-1.20.1`.
