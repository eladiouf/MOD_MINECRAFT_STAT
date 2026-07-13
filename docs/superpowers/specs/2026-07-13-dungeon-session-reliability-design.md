# Dungeon Session Reliability Design

**Date:** 2026-07-13
**Scope:** Trial Dungeon runtime reliability on NeoForge 1.21.1

## Goal

Make dungeon encounters reliable across the full physical floor, reset interrupted floors cleanly after every server restart, and prevent rival FTB teams from sharing one physical floor.

## Confirmed Product Rules

- Only one team may occupy a physical challenge floor (`floor > 0`) at a time.
- Floor 0 remains a shared city hub for all teams.
- Members of the occupying team may join that floor and play cooperatively.
- A combat floor interrupted by a server restart starts again from its first required room.
- A boss floor interrupted by a server restart is reset: surviving authorized mobs are removed, boss tracking is cleared, and its altar becomes active again.
- Completed player progression remains persistent. A session reset never lowers `dungeonFloorReached`, removes points, or removes boss rewards already granted.
- Without FTB Teams, the existing fallback remains: all players are considered one cooperative team.

## Root Causes

### Floor scans no longer match the generated layout

`DungeonMobSpawner.FLOOR_SCAN_RADIUS` is 85 blocks, but `DungeonArchitect` generates rooms inside an envelope of `HX=134` and `HZ=122`. Mobs in peripheral rooms can therefore be omitted from alive counts and cleanup operations. A scalar radius is also the wrong abstraction for a rectangular fortress.

### Runtime state is implicitly ephemeral

Room encounter progress and boss UUID tracking live in static maps. Those maps disappear on restart, while entities and the inactive altar remain saved in the world. The resulting mixed state can skip rooms, leave stale mobs, or leave a boss altar permanently inactive.

### Team separation occurs too late

Completion rewards are filtered by FTB team, but entry and encounter ownership are not. Rival teams can reach the same physical floor and mutate the same floor-wide encounter state.

## Architecture

### 1. Shared floor bounds

Introduce `DungeonFloorBounds` as the single authority for runtime entity queries.

It exposes a rectangular `AABB mobArea(BlockPos floorCenter)` covering:

- X: `DungeonArchitect.HX` plus a small entity margin;
- Z: `DungeonArchitect.HZ` plus a small entity margin;
- Y: the lowest generated room/arena surface through the highest imported-arena and flying-mob allowance.

The horizontal bounds must remain strictly inside half of `FLOOR_SPACING` so adjacent floors can never contaminate one another. All mob counts, cleanup passes, boss cooldown player queries, and altar pre-summon cleanup use this authority.

### 2. Explicit server-session initialization

Introduce `DungeonFloorSession`, containing a set of floors initialized during the current server process.

`prepareOnEntry(ServerLevel, floor)` runs once per floor per server session, after block generation and before the player is teleported. It performs the following atomic reset on the logical server thread:

1. discard authorized non-merchant mobs inside `DungeonFloorBounds.mobArea`;
2. remove queued spawns and queued L2 applications belonging to that floor;
3. remove the floor's `RoomEncounterProgress` state;
4. clear `DungeonBossTracker` for that floor;
5. on a boss floor, locate the central boss altar and set `ACTIVE=true`;
6. mark the floor initialized for this server session.

Repeated entry during the same server process is a no-op, so active encounters are not reset when a teammate joins or a player reconnects.

The session set is cleared on server stop for integrated-server safety and test isolation. A new dedicated-server process naturally starts empty as well.

### 3. Team-exclusive admission

Before generation or session initialization, `DungeonTeleportHandler.enterFloor` examines living players already assigned to the requested challenge floor. Floor 0 bypasses admission control and remains shared.

- Empty floor: entry is allowed.
- At least one occupant in the same FTB team: entry is allowed.
- Any occupant from another FTB team: entry is refused with a translated player-facing message.

This is an admission rule, not persisted ownership. Ownership disappears naturally when the final team member leaves the floor. Because teleportation and event handling run on the logical server thread, checking current occupants is atomic for normal entry paths.

The existing `FTBTeamsBridge.sameTeam` fallback remains unchanged: without FTB Teams, all players are treated as cooperative.

### 4. Encounter reset interfaces

Add narrow reset operations instead of exposing internal collections:

- `DungeonMobSpawner.resetFloorRuntime(ServerLevel, int)` clears floor queues and authorized mobs using shared bounds.
- `DungeonRoomEncounterDirector.resetFloor(int)` removes all room progress for one floor.
- `DungeonBossTracker.clear(int)` remains the boss-tracking reset authority.
- `DungeonBossArenaFloor.reactivateAltar(ServerLevel, BlockPos)` scans only the known central altar column and reactivates an existing altar without rebuilding the arena.

No runtime map is serialized. Restart behavior is deliberately reset-based.

## Data Flow

```text
enterFloor(player, floor)
  -> validate unlocked floor and target dimension
  -> reject if a rival team occupies the floor
  -> generate blocks if the floor does not exist
  -> DungeonFloorSession.prepareOnEntry
       -> purge stale authorized mobs and queues
       -> reset room and boss runtime state
       -> reactivate boss altar when applicable
  -> teleport player
  -> begin the floor encounter/session timers
```

During combat, all alive counts use `DungeonFloorBounds`. Clearing the active room advances `RoomEncounterProgress`; clearing every required room calls `DungeonProgress.completeFloor`. Persistent player progression remains unchanged.

## Failure Handling

- If the dungeon dimension cannot be resolved, entry continues to fail as it does today.
- If no altar is found during a boss reset, log a warning and allow entry; do not regenerate the entire island automatically.
- If FTB Teams throws or is absent, preserve its current cooperative fallback so optional integration failure cannot block the dungeon.
- Session initialization only marks a floor initialized after reset operations finish.
- Merchants are excluded from reset cleanup through `DungeonMerchant.MERCHANT_TAG`.

## Tests

### Pure/unit tests

- Floor bounds include every legal room center and room edge used for mob spawning.
- Floor bounds never overlap the neighboring floor at `FLOOR_SPACING=300`.
- Session state initializes a floor once, can be cleared at server stop, and initializes again afterward.
- Admission policy allows an empty floor and teammates, but rejects a rival team.
- Existing sequential room encounter tests continue to pass after adding `resetFloor` behavior.

### Integration-facing verification

- Compile all NeoForge sources with Java 21.
- Run the complete JUnit suite.
- Run the dungeon-focused JUnit suite.
- Manual GameTest/in-game follow-up remains appropriate for block/entity persistence: start a room or boss encounter, restart the server, enter again, and verify a clean first encounter or active altar.

## Non-Goals

- Separate physical dungeon instances per team.
- Persistence of partial room or boss encounters.
- Changing dungeon geometry, themes, loot, mob pools, scaling, or rewards.
- Ejecting players placed onto a floor through external administrator commands; the supported dungeon entry path enforces exclusivity.
