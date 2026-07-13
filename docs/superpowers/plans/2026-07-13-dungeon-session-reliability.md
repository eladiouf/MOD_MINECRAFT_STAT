# Dungeon Session Reliability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reset interrupted dungeon encounters cleanly after a server restart, cover the complete fortress in runtime entity queries, and reserve each challenge floor to one FTB team at a time.

**Architecture:** Replace the obsolete scalar scan radius with a shared rectangular floor-boundary authority. Add an explicit per-server-session initializer that purges saved encounter entities and reactivates boss altars once, then enforce team admission before entering challenge floors. Partial encounters remain deliberately non-persistent; persistent player progression is untouched.

**Tech Stack:** Java 21, Minecraft 1.21.1, NeoForge 21.1.232, JUnit Jupiter 5.10, Gradle.

## Global Constraints

- Only one team may occupy a physical challenge floor (`floor > 0`) at a time; floor 0 remains a shared city hub.
- Members of the occupying team may join and play cooperatively.
- Combat and boss encounters interrupted by a server restart reset on first entry in the new server session.
- `dungeonFloorReached`, dungeon points, records, and granted boss rewards remain persistent and are never rolled back.
- Without FTB Teams, preserve the existing fallback in which all players are cooperative.
- Do not change dungeon geometry, themes, loot, mob pools, scaling, or rewards.
- Do not serialize partial room or boss encounters.

## File Structure

- Create `src/main/java/tong/statmod/dungeon/DungeonFloorBounds.java`: shared rectangular `AABB` construction.
- Create `src/main/java/tong/statmod/dungeon/DungeonSessionRegistry.java`: pure initialized-floor set.
- Create `src/main/java/tong/statmod/dungeon/DungeonFloorSession.java`: server lifecycle and first-entry reset orchestration.
- Create `src/main/java/tong/statmod/dungeon/DungeonFloorAdmission.java`: generic, unit-testable team admission policy.
- Modify `DungeonMobSpawner`, `DungeonBossHandler`, and `DungeonBossAltarBlock`: use shared bounds and expose an unconditional session reset.
- Modify `DungeonRoomEncounterDirector` and `DungeonBossArenaFloor`: expose narrow reset/reactivation operations.
- Modify `DungeonTeleportHandler`: enforce admission and call session preparation.
- Modify English and French language JSON: explain rival-team refusal.
- Create three focused tests for bounds, session registry, and admission policy.

---

### Task 1: Shared rectangular runtime bounds

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/DungeonFloorBounds.java`
- Create: `src/test/java/tong/statmod/dungeon/DungeonFloorBoundsTest.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java:43,122-150`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonBossHandler.java:145-163`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonBossAltarBlock.java:89-96`

**Interfaces:**
- Consumes: `DungeonArchitect.HX`, `DungeonArchitect.HZ`, and a floor-center `BlockPos`.
- Produces: `static AABB mobArea(BlockPos center)`, `static int halfX()`, and `static int halfZ()`.

- [ ] **Step 1: Write the failing bounds tests**

Create `DungeonFloorBoundsTest` with exact coverage and isolation assertions:

```java
package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonFloorBoundsTest {
    @Test
    void includesEveryGeneratedRoomEdgeAndVerticalAllowance() {
        BlockPos center = new BlockPos(0, 100, 0);
        AABB area = DungeonFloorBounds.mobArea(center);

        assertTrue(area.contains(DungeonArchitect.HX, 94, DungeonArchitect.HZ));
        assertTrue(area.contains(-DungeonArchitect.HX, 164, -DungeonArchitect.HZ));
        for (DungeonLayout.Room room : DungeonLayout.rooms()) {
            assertTrue(area.contains(room.minX(), 100 + DungeonRoomChain.roomYOffset(room.index(), 1), room.minZ()));
            assertTrue(area.contains(room.maxX(), 100 + DungeonRoomChain.roomYOffset(room.index(), 1), room.maxZ()));
        }
    }

    @Test
    void doesNotOverlapHorizontallyAdjacentFloors() {
        AABB first = DungeonFloorBounds.mobArea(new BlockPos(0, 100, 0));
        AABB second = DungeonFloorBounds.mobArea(
                new BlockPos(DungeonTeleportHandler.FLOOR_SPACING, 100, 0));

        assertTrue(first.maxX < second.minX);
    }
}
```

- [ ] **Step 2: Run the new test and confirm RED**

Run:

```powershell
.\gradlew.bat test --tests "tong.statmod.dungeon.DungeonFloorBoundsTest"
```

Expected: compilation fails because `DungeonFloorBounds` does not exist.

- [ ] **Step 3: Implement the shared bounds authority**

Create:

```java
package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/** Runtime query envelope for one physical dungeon floor. */
public final class DungeonFloorBounds {
    private static final int ENTITY_MARGIN = 8;
    private static final int BELOW_CENTER = 52;
    private static final int ABOVE_CENTER = 65;
    private static final int HALF_X = DungeonArchitect.HX + ENTITY_MARGIN;
    private static final int HALF_Z = DungeonArchitect.HZ + ENTITY_MARGIN;

    static {
        if (HALF_X * 2 >= DungeonTeleportHandler.FLOOR_SPACING
                || HALF_Z * 2 >= DungeonTeleportHandler.FLOOR_SPACING) {
            throw new IllegalStateException("Dungeon floor entity bounds overlap neighboring floors");
        }
    }

    private DungeonFloorBounds() {}

    public static AABB mobArea(BlockPos center) {
        return new AABB(
                center.getX() - HALF_X, center.getY() - BELOW_CENTER, center.getZ() - HALF_Z,
                center.getX() + HALF_X + 1, center.getY() + ABOVE_CENTER + 1, center.getZ() + HALF_Z + 1);
    }

    static int halfX() { return HALF_X; }
    static int halfZ() { return HALF_Z; }
}
```

Replace every `new AABB(floorCenter).inflate(DungeonMobSpawner.FLOOR_SCAN_RADIUS)` used for floor-wide mobs or players with `DungeonFloorBounds.mobArea(floorCenter)`. Remove `FLOOR_SCAN_RADIUS`. Do not change the attacker fallback radius of 60, which is local to the dying mob rather than floor-wide.

- [ ] **Step 4: Run bounds and existing dungeon tests**

Run:

```powershell
.\gradlew.bat test --tests "tong.statmod.dungeon.DungeonFloorBoundsTest" --tests "tong.statmod.dungeon.DungeonLayoutTest" --tests "tong.statmod.dungeon.DungeonBossTrackerTest"
```

Expected: all selected tests pass.

- [ ] **Step 5: Commit the isolated bounds fix**

```powershell
git add src/main/java/tong/statmod/dungeon/DungeonFloorBounds.java src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java src/main/java/tong/statmod/dungeon/DungeonBossHandler.java src/main/java/tong/statmod/dungeon/DungeonBossAltarBlock.java src/test/java/tong/statmod/dungeon/DungeonFloorBoundsTest.java
git commit -m "fix(dungeon): cover complete floors in entity scans"
```

---

### Task 2: Explicit per-session state registry

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/DungeonSessionRegistry.java`
- Create: `src/test/java/tong/statmod/dungeon/DungeonSessionRegistryTest.java`

**Interfaces:**
- Produces: `boolean needsPreparation(int floor)`, `void markPrepared(int floor)`, and `void clear()`.
- Consumed later by: `DungeonFloorSession.prepareOnEntry` and its server-stopping handler.

- [ ] **Step 1: Write the failing registry tests**

```java
package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonSessionRegistryTest {
    @Test
    void floorNeedsPreparationExactlyUntilMarked() {
        DungeonSessionRegistry registry = new DungeonSessionRegistry();

        assertTrue(registry.needsPreparation(12));
        registry.markPrepared(12);
        assertFalse(registry.needsPreparation(12));
        assertTrue(registry.needsPreparation(13));
    }

    @Test
    void clearingSessionMakesFloorsRequirePreparationAgain() {
        DungeonSessionRegistry registry = new DungeonSessionRegistry();
        registry.markPrepared(20);

        registry.clear();

        assertTrue(registry.needsPreparation(20));
    }
}
```

- [ ] **Step 2: Run the registry test and confirm RED**

```powershell
.\gradlew.bat test --tests "tong.statmod.dungeon.DungeonSessionRegistryTest"
```

Expected: compilation fails because `DungeonSessionRegistry` does not exist.

- [ ] **Step 3: Implement the pure registry**

```java
package tong.statmod.dungeon;

import java.util.HashSet;
import java.util.Set;

/** Floors reset successfully during the current logical-server session. */
final class DungeonSessionRegistry {
    private final Set<Integer> preparedFloors = new HashSet<>();

    boolean needsPreparation(int floor) {
        return !preparedFloors.contains(floor);
    }

    void markPrepared(int floor) {
        preparedFloors.add(floor);
    }

    void clear() {
        preparedFloors.clear();
    }
}
```

- [ ] **Step 4: Run the registry tests and confirm GREEN**

```powershell
.\gradlew.bat test --tests "tong.statmod.dungeon.DungeonSessionRegistryTest"
```

Expected: 2 tests pass.

- [ ] **Step 5: Commit the registry**

```powershell
git add src/main/java/tong/statmod/dungeon/DungeonSessionRegistry.java src/test/java/tong/statmod/dungeon/DungeonSessionRegistryTest.java
git commit -m "test(dungeon): define server session floor state"
```

---

### Task 3: Runtime reset and boss altar recovery

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/DungeonFloorSession.java`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java:141-168`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonRoomEncounterDirector.java:35-40`
- Modify: `src/main/java/tong/statmod/dungeon/DungeonBossArenaFloor.java:94-112`

**Interfaces:**
- Consumes: `DungeonFloorBounds.mobArea`, `DungeonSessionRegistry`, `DungeonBossTracker.clear`.
- Produces: `DungeonMobSpawner.resetFloorRuntime(ServerLevel, int)`, `DungeonRoomEncounterDirector.resetFloor(int)`, `DungeonBossArenaFloor.reactivateAltar(ServerLevel, BlockPos)`, and `DungeonFloorSession.prepareOnEntry(ServerLevel, int)`.

- [ ] **Step 1: Add the narrow room reset operation**

Add to `DungeonRoomEncounterDirector`:

```java
public static void resetFloor(int floor) {
    STATES.remove(floor);
}
```

Retain `resetActiveRoom(int)` for death/retry behavior; it resets only the current room, while the new method intentionally removes all cleared-room progress after a server restart.

- [ ] **Step 2: Add unconditional runtime cleanup to the spawner**

Add this method and use it from `clearFloorMobs` after the boss-tracking guard:

```java
public static int resetFloorRuntime(ServerLevel lv, int floor) {
    BlockPos center = DungeonTeleportHandler.floorSpawnPos(floor);
    int removed = 0;
    for (Mob mob : lv.getEntitiesOfClass(Mob.class, DungeonFloorBounds.mobArea(center),
            candidate -> candidate.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)
                    && !candidate.getPersistentData().getBoolean(DungeonMerchant.MERCHANT_TAG))) {
        mob.discard();
        removed++;
    }
    QUEUE.removeIf(pending -> pending.floor() == floor);
    L2_QUEUE.removeIf(pending -> DungeonTeleportHandler.floorAtPos(
            pending.mob().getBlockX(), pending.mob().getBlockZ()) == floor);
    PENDING_FLOORS.remove(floor);
    return removed;
}
```

After calling it from `clearFloorMobs`, keep the existing calls to `resetActiveRoom` and `DungeonBossTracker.clear`. This new method must not check `DungeonBossTracker`: first-entry recovery must be able to purge a saved boss from the previous server process.

- [ ] **Step 3: Add central-column altar recovery**

Add to `DungeonBossArenaFloor`:

```java
public static boolean reactivateAltar(ServerLevel lv, BlockPos center) {
    for (int y = center.getY() - 6; y <= center.getY() + 64; y++) {
        BlockPos pos = new BlockPos(center.getX(), y, center.getZ());
        BlockState state = lv.getBlockState(pos);
        if (!state.is(DungeonBlocks.BOSS_ALTAR.get())) continue;
        if (!state.getValue(DungeonBossAltarBlock.ACTIVE)) {
            lv.setBlock(pos, state.setValue(DungeonBossAltarBlock.ACTIVE, true), 3);
        }
        return true;
    }
    return false;
}
```

The scan covers both the generic altar at the center dais and imported arenas whose altar height is discovered dynamically.

- [ ] **Step 4: Implement the session orchestrator**

Create:

```java
package tong.statmod.dungeon;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import tong.statmod.STATMod;

@EventBusSubscriber(modid = STATMod.MODID)
public final class DungeonFloorSession {
    private static final DungeonSessionRegistry REGISTRY = new DungeonSessionRegistry();

    private DungeonFloorSession() {}

    public static void prepareOnEntry(ServerLevel level, int floor) {
        if (floor <= 0 || !REGISTRY.needsPreparation(floor)) return;

        int removed = DungeonMobSpawner.resetFloorRuntime(level, floor);
        DungeonRoomEncounterDirector.resetFloor(floor);
        DungeonBossTracker.clear(floor);
        if (DungeonObjective.forFloor(floor) == DungeonObjective.SLAY_BOSS
                && !DungeonBossArenaFloor.reactivateAltar(
                        level, DungeonTeleportHandler.floorSpawnPos(floor))) {
            STATMod.LOGGER.warn("[TrialDungeon] No boss altar found while resetting floor {}", floor);
        }
        REGISTRY.markPrepared(floor);
        STATMod.LOGGER.info("[TrialDungeon] Floor {} prepared for server session ({} stale mobs removed)",
                floor, removed);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        REGISTRY.clear();
    }
}
```

- [ ] **Step 5: Compile and run session-related tests**

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test --tests "tong.statmod.dungeon.DungeonSessionRegistryTest" --tests "tong.statmod.dungeon.RoomEncounterProgressTest" --tests "tong.statmod.dungeon.DungeonBossTrackerTest"
```

Expected: compilation succeeds and all selected tests pass.

- [ ] **Step 6: Commit runtime recovery**

```powershell
git add src/main/java/tong/statmod/dungeon/DungeonFloorSession.java src/main/java/tong/statmod/dungeon/DungeonMobSpawner.java src/main/java/tong/statmod/dungeon/DungeonRoomEncounterDirector.java src/main/java/tong/statmod/dungeon/DungeonBossArenaFloor.java
git commit -m "fix(dungeon): reset interrupted encounters per session"
```

---

### Task 4: Unit-tested team admission policy

**Files:**
- Create: `src/main/java/tong/statmod/dungeon/DungeonFloorAdmission.java`
- Create: `src/test/java/tong/statmod/dungeon/DungeonFloorAdmissionTest.java`

**Interfaces:**
- Produces: `static <T> boolean canEnter(T entrant, List<T> occupants, BiPredicate<T,T> sameTeam)`.
- Consumed later by: `DungeonTeleportHandler.enterFloor` with `FTBTeamsBridge::sameTeam`.

- [ ] **Step 1: Write failing admission tests**

```java
package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonFloorAdmissionTest {
    @Test
    void emptyFloorAllowsEntry() {
        assertTrue(DungeonFloorAdmission.canEnter("red:a", List.of(),
                (a, b) -> a.split(":")[0].equals(b.split(":")[0])));
    }

    @Test
    void teammatesMayJoinOccupiedFloor() {
        assertTrue(DungeonFloorAdmission.canEnter("red:b", List.of("red:a", "red:c"),
                (a, b) -> a.split(":")[0].equals(b.split(":")[0])));
    }

    @Test
    void anyRivalOccupantRejectsEntry() {
        assertFalse(DungeonFloorAdmission.canEnter("red:b", List.of("red:a", "blue:a"),
                (a, b) -> a.split(":")[0].equals(b.split(":")[0])));
    }
}
```

- [ ] **Step 2: Run the admission test and confirm RED**

```powershell
.\gradlew.bat test --tests "tong.statmod.dungeon.DungeonFloorAdmissionTest"
```

Expected: compilation fails because `DungeonFloorAdmission` does not exist.

- [ ] **Step 3: Implement the generic policy**

```java
package tong.statmod.dungeon;

import java.util.List;
import java.util.function.BiPredicate;

/** Pure policy for reserving one physical challenge floor to one team. */
final class DungeonFloorAdmission {
    private DungeonFloorAdmission() {}

    static <T> boolean canEnter(T entrant, List<T> occupants, BiPredicate<T, T> sameTeam) {
        for (T occupant : occupants) {
            if (!sameTeam.test(entrant, occupant)) return false;
        }
        return true;
    }
}
```

- [ ] **Step 4: Run admission tests and confirm GREEN**

```powershell
.\gradlew.bat test --tests "tong.statmod.dungeon.DungeonFloorAdmissionTest"
```

Expected: 3 tests pass.

- [ ] **Step 5: Commit the admission policy**

```powershell
git add src/main/java/tong/statmod/dungeon/DungeonFloorAdmission.java src/test/java/tong/statmod/dungeon/DungeonFloorAdmissionTest.java
git commit -m "test(dungeon): define exclusive floor admission"
```

---

### Task 5: Wire admission and session preparation into entry

**Files:**
- Modify: `src/main/java/tong/statmod/dungeon/DungeonTeleportHandler.java:147-185`
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`
- Test: `src/test/java/tong/statmod/dungeon/DungeonFloorAdmissionTest.java`

**Interfaces:**
- Consumes: `DungeonFloorAdmission.canEnter`, `FTBTeamsBridge.sameTeam`, and `DungeonFloorSession.prepareOnEntry`.
- Preserves: `enterFloor(ServerPlayer, int)` return contract and persistent unlock validation.

- [ ] **Step 1: Add the translated refusal message**

Add these JSON entries while preserving valid commas and existing formatting:

```json
// en_us.json
"dungeon.floor.occupied_by_other_team": "This floor is currently occupied by another team."

// fr_fr.json
"dungeon.floor.occupied_by_other_team": "Cet étage est actuellement occupé par une autre équipe."
```

- [ ] **Step 2: Enforce admission before generation**

After resolving `ServerLevel dungeon` and before saving the overworld return position, add:

```java
if (floor > 0) {
    java.util.List<ServerPlayer> occupants = playersOnFloor(dungeon, floor);
    occupants.remove(player);
    if (!DungeonFloorAdmission.canEnter(player, occupants,
            tong.statmod.integration.ftbteams.FTBTeamsBridge::sameTeam)) {
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "dungeon.floor.occupied_by_other_team"), true);
        return false;
    }
}
```

Floor 0 deliberately skips this block so the city remains a shared hub.

- [ ] **Step 3: Prepare the floor after block generation and before teleport**

Immediately after `IslandGenerator.generateFloor(dungeon, floor);`, add:

```java
DungeonFloorSession.prepareOnEntry(dungeon, floor);
```

This ordering guarantees the altar exists before recovery scans for it, and guarantees stale mobs are removed before the player begins tracking the floor.

- [ ] **Step 4: Run focused regression tests**

```powershell
.\gradlew.bat test --tests "tong.statmod.dungeon.DungeonFloorAdmissionTest" --tests "tong.statmod.dungeon.DungeonTeleportHandlerTest" --tests "tong.statmod.dungeon.DungeonCoopTest" --tests "tong.statmod.dungeon.DungeonRoomEncounterDirectorTest"
```

Expected: all selected tests pass.

- [ ] **Step 5: Commit entry wiring**

```powershell
git add src/main/java/tong/statmod/dungeon/DungeonTeleportHandler.java src/main/resources/assets/statmod/lang/en_us.json src/main/resources/assets/statmod/lang/fr_fr.json
git commit -m "fix(dungeon): reserve challenge floors by team"
```

---

### Task 6: Full verification and documentation alignment

**Files:**
- Modify: `CHANGELOG.md`
- Verify: all files changed by Tasks 1-5

**Interfaces:**
- Consumes the completed implementation.
- Produces a buildable NeoForge mod and a concise changelog entry.

- [ ] **Step 1: Add a changelog entry**

Under the current release heading, add:

```markdown
- Trial Dungeon reliability: interrupted encounters now reset cleanly after server restart,
  entity scans cover the complete fortress, and challenge floors reject rival teams while occupied.
```

- [ ] **Step 2: Check formatting and accidental edits**

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors; only intended source, test, language, changelog, spec, and plan files are changed. Leave the pre-existing untracked `logs/` directory untouched.

- [ ] **Step 3: Run the full unit-test suite**

```powershell
.\gradlew.bat test
```

Expected: `BUILD SUCCESSFUL` and zero failing tests.

- [ ] **Step 4: Run a clean production build**

```powershell
.\gradlew.bat clean build
```

Expected: `BUILD SUCCESSFUL`; the mod JAR is produced under `build/libs/`.

- [ ] **Step 5: Review the final diff against the specification**

```powershell
git diff 9b6e8c4 -- src/main/java/tong/statmod/dungeon src/test/java/tong/statmod/dungeon src/main/resources/assets/statmod/lang CHANGELOG.md
```

Confirm all five invariants: complete rectangular bounds, no adjacent-floor overlap, once-per-session reset, boss altar recovery, and team-exclusive challenge-floor entry.

- [ ] **Step 6: Commit final documentation**

```powershell
git add CHANGELOG.md
git commit -m "docs: record dungeon reliability fixes"
```

## Manual In-Game Acceptance

1. Enter a combat floor, clear at least one required room, stop the server, restart, and re-enter. The first required encounter must be available again and no saved authorized mob may remain.
2. Activate a boss altar, stop the server with the boss alive, restart, and re-enter. The old boss must be gone and the altar must be active.
3. Put one FTB team on a challenge floor and try to enter with a rival team. Entry must fail with the translated message.
4. Join the occupied floor with a teammate. Entry must succeed.
5. Put players from different teams in floor 0. The city must remain shared.
