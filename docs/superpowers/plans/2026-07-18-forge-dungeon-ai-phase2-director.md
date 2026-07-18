# Dungeon AI Phase 2 — Floor Director Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add persistent factions and alert states plus one bounded tactical director for each occupied dungeon floor.

**Architecture:** Pure policies calculate state transitions and alert propagation. A server tick subscriber scans only occupied floors every 10 ticks, tags authorized dungeon actors, reconstructs transient squad state after reload, and shares last-known player positions within strict range and time limits.

**Tech Stack:** Java 17, Forge 47.4.4, Minecraft 1.20.1, JUnit 5, existing dungeon position and spawn-tag APIs.

## Global Constraints

- Never scan unoccupied floors.
- Never reveal current player positions through walls; share only observed last-known positions.
- Keep all collections bounded and clear them on server stop.
- Keep unrelated teams independent.
- Do not reference Cataclysm entities.

---

### Task 1: AI domain and pure transition policy

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/ai/DungeonFaction.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/DungeonAlertState.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/DungeonAlertPolicy.java`
- Test: `src/test/java/tong/statmod/dungeon/ai/DungeonAlertPolicyTest.java`

**Interfaces:**
- Produces: `DungeonAlertState next(DungeonAlertState current, boolean seesEnemy, boolean heardAlert, boolean hasRecentMemory, boolean lowHealth)`.
- Produces: `boolean canShare(long sourceTick, long nowTick, double distanceSquared)`.

- [ ] Write tests proving sight enters combat, squad alerts enter alerted state, stale memory returns to idle, low health retreats, and alerts expire after 100 ticks or 48 blocks.
- [ ] Run the focused test and confirm compilation fails because the types are absent.
- [ ] Add factions `ADVENTURER_RIVALS`, `CULT_OF_CINDERS`, `FROZEN_COVEN`, `ARCANE_ORDER`, `RESTLESS_DEAD`, `DUNGEON_CONSTRUCTS`, `BEAST_PACKS`, `INHABITANTS`.
- [ ] Add states `IDLE`, `SUSPICIOUS`, `ALERTED`, `COMBAT`, `RETREATING`, `REGROUPING`.
- [ ] Implement `next` with priority `lowHealth > seesEnemy > heardAlert > recentMemory > idle`; recent memory maps to `SUSPICIOUS`.
- [ ] Implement `canShare` as `nowTick-sourceTick <= 100 && distanceSquared <= 48*48`.
- [ ] Run focused tests and commit `feat: add dungeon AI alert domain`.

---

### Task 2: Persistent actor metadata

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/ai/DungeonAiActor.java`
- Modify: `src/main/java/tong/statmod/dungeon/party/AdventurerPartyHelper.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonBossAltarBlock.java`
- Test: `src/test/java/tong/statmod/dungeon/ai/DungeonAiActorContractTest.java`

**Interfaces:**
- Produces constants `FACTION_TAG`, `ALERT_TAG`, `FLOOR_TAG`, `SQUAD_TAG`, `LAST_SEEN_X/Y/Z/TICK`.
- Produces `void initialize(Mob mob, DungeonFaction faction, int floor, String squadId)` and safe getters.

- [ ] Write a source contract asserting all persistent keys exist and both ordinary and boss spawn boundaries call `DungeonAiActor.initialize`.
- [ ] Verify the contract fails.
- [ ] Implement initialization with stable enum names, floor, squad ID and `IDLE` state.
- [ ] Initialize adventurer parties as `ADVENTURER_RIVALS`; derive other factions from namespaces and Iron's entity paths in a pure `factionFor(entityId)` method.
- [ ] Use `floor + ":room:" + roomIndex` for ordinary squads and `floor + ":boss"` for boss squads.
- [ ] Run tests and commit `feat: persist dungeon AI actor metadata`.

---

### Task 3: Bounded occupied-floor director

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/ai/DungeonEncounterDirector.java`
- Create: `src/main/java/tong/statmod/dungeon/ai/DungeonDirectorPolicy.java`
- Test: `src/test/java/tong/statmod/dungeon/ai/DungeonDirectorPolicyTest.java`
- Modify: `src/main/java/tong/statmod/StatMod.java` only if explicit registration is required.

**Interfaces:**
- Produces: `Set<Integer> occupiedFloors(List<PlayerSnapshot>)` pure helper.
- Consumes: `DungeonTeleportHandler.floorAtPos`, authorized spawn tag, actor metadata and alert policy.

- [ ] Test that creative/spectator/dead players are ignored, two teams on one floor yield one floor scan, and players on two floors yield two scans.
- [ ] Verify the test fails because the policy is absent.
- [ ] Implement the pure player snapshot policy.
- [ ] Add an `@Mod.EventBusSubscriber` director ticking every 10 server ticks.
- [ ] For each occupied floor, scan once within `DungeonMobSpawner.FLOOR_SCAN_RADIUS`, limited to 64 tagged living actors.
- [ ] Actors with line of sight store last-known coordinates/tick and enter `COMBAT`; squad allies within 48 blocks receive `ALERTED` and the same memory only when `canShare` passes.
- [ ] Actors without current sight use memory for at most 100 ticks, then transition to `IDLE`; actors below 20% health enter `RETREATING`.
- [ ] Clear transient floor caches on `ServerStoppedEvent`.
- [ ] Run all `dungeon.ai` and `dungeon.party` tests, then full `clean test build`.
- [ ] Commit `feat: coordinate AI on occupied dungeon floors` and push `forge-1.20.1`.
