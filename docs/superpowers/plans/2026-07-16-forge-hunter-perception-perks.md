# Forge Hunter Perception Perks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add six deterministic automatic perks that give Tracking a personal marked-prey contour and Keen Senses a crouched personal threat scan.

**Architecture:** Extend the canonical automatic-perk catalog, calculate all ranges and durations in a loader-neutral pure rules class, and send only committed server marks through one bounded packet. Keep scanning and world-space contours entirely client-side, with no entity metadata mutation, then retain the existing XP sources and stat screen.

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.10, JUnit 5.10.2, Forge SimpleChannel, Iron's Spells 3.16.2, Epic Fight 20.14.17, Pufferfish's Attributes 0.8.2, Gradle 8.8.

## Global Constraints

- Work only in the clean `.worktrees/forge-combat-perks` worktree on the Forge 1.20.1 lineage.
- Do not modify, stage, commit, import, or merge any file under `tong/statmod/dungeon`.
- Preserve the existing 33 perk IDs, order, requirements, effects, amounts, and localization byte-for-byte.
- Append exactly six IDs in the approved order for a total of 39.
- Keep thresholds exactly 25, 50, and 75 and fixed normalized amount `0.25` per milestone.
- Keep `AutomaticPerkBonuses.MAX_AGGREGATE` at `0.75`; do not add server-config keys.
- Do not modify XP sources, limits, save schema, affinity removal, the `P` screen, spell targeting, combat damage, loot, dodge, or provider attributes.
- Never call `setGlowingTag`, mutate entity metadata, manipulate teams, or send vanilla metadata packets.
- Register client rendering only on `Dist.CLIENT`; dedicated-server class loading must remain safe.
- Change protocol from `"6"` to `"7"` and snapshot maximum from 33 to 39.
- Deploy only after clean unit tests, artifact inspection, and required-provider GameTest smoke pass.

---

### Task 1: Extend the catalog and implement pure perception rules

**Files:**
- Modify: `src/main/java/tong/statmod/perks/AutomaticPerkEffect.java`
- Modify: `src/main/java/tong/statmod/perks/AutomaticPerkCatalog.java`
- Modify: `src/main/java/tong/statmod/perks/AutomaticPerkBonuses.java`
- Create: `src/main/java/tong/statmod/effects/HunterPerceptionRules.java`
- Modify: `src/test/java/tong/statmod/perks/AutomaticPerkCatalogTest.java`
- Modify: `src/test/java/tong/statmod/perks/AutomaticPerkResolverTest.java`
- Modify: `src/test/java/tong/statmod/perks/AutomaticPerkBonusesTest.java`
- Create: `src/test/java/tong/statmod/effects/HunterPerceptionRulesTest.java`

**Interfaces:**
- Produces enum values `TRACKING_FOCUS` and `KEEN_SENSES_AWARENESS`.
- Produces `HunterPerceptionRules.milestoneCount(double): int`.
- Produces `HunterPerceptionRules.trackingDurationTicks(int, double): int`.
- Produces `HunterPerceptionRules.trackingRangeBlocks(int, double): double`.
- Produces `HunterPerceptionRules.keenSensesRangeBlocks(int, double): double`.
- Preserves `AutomaticPerkResolver.active(Map<StatType,Integer>)` and all existing bonus accessors.

- [ ] **Step 1: Write failing catalog and boundary tests**

Update the catalog assertions to:

```java
assertEquals(39, definitions.size());
assertEquals(39, definitions.stream().map(AutomaticPerkDefinition::id)
        .distinct().count());
assertEquals("statmod:physical_resistance_75", definitions.get(32).id());
assertEquals("statmod:tracking_25", definitions.get(33).id());
assertEquals("statmod:tracking_75", definitions.get(35).id());
assertEquals("statmod:keen_senses_25", definitions.get(36).id());
assertEquals("statmod:keen_senses_75", definitions.get(38).id());
```

In `AutomaticPerkResolverTest`, call the existing milestone helper for:

```java
assertMilestones(StatType.TRACKING, "tracking");
assertMilestones(StatType.KEEN_SENSES, "keen_senses");
```

Add bonus assertions with Forge defaults loaded:

```java
assertEquals(0.25, bonusesAt(StatType.TRACKING, 25)
        .amount(AutomaticPerkEffect.TRACKING_FOCUS), 1.0e-9);
assertEquals(0.50, bonusesAt(StatType.TRACKING, 50)
        .amount(AutomaticPerkEffect.TRACKING_FOCUS), 1.0e-9);
assertEquals(0.75, bonusesAt(StatType.TRACKING, 75)
        .amount(AutomaticPerkEffect.TRACKING_FOCUS), 1.0e-9);
assertEquals(0.75, bonusesAt(StatType.KEEN_SENSES, 75)
        .amount(AutomaticPerkEffect.KEEN_SENSES_AWARENESS), 1.0e-9);
```

- [ ] **Step 2: Write failing pure-rules tests**

Create `HunterPerceptionRulesTest` with exact anchors:

```java
@Test
void convertsOnlyFiniteBoundedNormalizedScoresToMilestones() {
    assertEquals(0, HunterPerceptionRules.milestoneCount(Double.NaN));
    assertEquals(0, HunterPerceptionRules.milestoneCount(-1.0));
    assertEquals(1, HunterPerceptionRules.milestoneCount(0.25));
    assertEquals(2, HunterPerceptionRules.milestoneCount(0.50));
    assertEquals(3, HunterPerceptionRules.milestoneCount(0.75));
    assertEquals(3, HunterPerceptionRules.milestoneCount(50.0));
}

@Test
void followsExactTrackingAnchors() {
    assertEquals(0, HunterPerceptionRules.trackingDurationTicks(0, 0.0));
    assertEquals(0.0, HunterPerceptionRules.trackingRangeBlocks(0, 0.0), 1.0e-9);
    assertEquals(61, HunterPerceptionRules.trackingDurationTicks(1, 0.0));
    assertEquals(12.12, HunterPerceptionRules.trackingRangeBlocks(1, 0.0), 1.0e-9);
    assertEquals(125, HunterPerceptionRules.trackingDurationTicks(25, 0.25));
    assertEquals(19.0, HunterPerceptionRules.trackingRangeBlocks(25, 0.25), 1.0e-9);
    assertEquals(190, HunterPerceptionRules.trackingDurationTicks(50, 0.50));
    assertEquals(26.0, HunterPerceptionRules.trackingRangeBlocks(50, 0.50), 1.0e-9);
    assertEquals(255, HunterPerceptionRules.trackingDurationTicks(75, 0.75));
    assertEquals(33.0, HunterPerceptionRules.trackingRangeBlocks(75, 0.75), 1.0e-9);
    assertEquals(280, HunterPerceptionRules.trackingDurationTicks(500, 50.0));
    assertEquals(36.0, HunterPerceptionRules.trackingRangeBlocks(500, 50.0), 1.0e-9);
}

@Test
void followsExactKeenSensesAnchors() {
    assertEquals(0.0, HunterPerceptionRules.keenSensesRangeBlocks(0, 0.0), 1.0e-9);
    assertEquals(6.1, HunterPerceptionRules.keenSensesRangeBlocks(1, 0.0), 1.0e-9);
    assertEquals(10.5, HunterPerceptionRules.keenSensesRangeBlocks(25, 0.25), 1.0e-9);
    assertEquals(15.0, HunterPerceptionRules.keenSensesRangeBlocks(50, 0.50), 1.0e-9);
    assertEquals(19.5, HunterPerceptionRules.keenSensesRangeBlocks(75, 0.75), 1.0e-9);
    assertEquals(22.0, HunterPerceptionRules.keenSensesRangeBlocks(500, 50.0), 1.0e-9);
}
```

- [ ] **Step 3: Run focused tests and verify RED**

Run:

```powershell
.\gradlew.bat test --tests "tong.statmod.perks.*" --tests "tong.statmod.effects.HunterPerceptionRulesTest" --console=plain
```

Expected: test compilation fails because the two enum constants and
`HunterPerceptionRules` do not exist; catalog-count assertions also fail.

- [ ] **Step 4: Add the exact catalog entries and normalized effects**

Append the enum values and then append these groups after Physical Resistance:

```java
addMilestones(definitions, "tracking", StatType.TRACKING,
        AutomaticPerkEffect.TRACKING_FOCUS, 0.25);
addMilestones(definitions, "keen_senses", StatType.KEEN_SENSES,
        AutomaticPerkEffect.KEEN_SENSES_AWARENESS, 0.25);
```

Extend `configuredAmount` without a config key:

```java
case TRACKING_FOCUS, KEEN_SENSES_AWARENESS -> 0.25;
```

Do not change `MAX_AGGREGATE`.

- [ ] **Step 5: Implement the pure rules**

Create:

```java
package tong.statmod.effects;

import tong.statmod.stats.StatProgress;

public final class HunterPerceptionRules {
    private static final double SCORE_PER_MILESTONE = 0.25;
    private static final double MAX_SCORE = 0.75;

    private HunterPerceptionRules() {
    }

    public static int milestoneCount(double normalizedScore) {
        double bounded = Double.isFinite(normalizedScore)
                ? Math.max(0.0, Math.min(MAX_SCORE, normalizedScore)) : 0.0;
        return Math.max(0, Math.min(3,
                (int) Math.round(bounded / SCORE_PER_MILESTONE)));
    }

    public static int trackingDurationTicks(int level, double normalizedScore) {
        int bounded = boundedLevel(level);
        return bounded == 0 ? 0 : 60 + bounded + 40 * milestoneCount(normalizedScore);
    }

    public static double trackingRangeBlocks(int level, double normalizedScore) {
        int bounded = boundedLevel(level);
        return bounded == 0 ? 0.0
                : 12.0 + 0.12 * bounded + 4.0 * milestoneCount(normalizedScore);
    }

    public static double keenSensesRangeBlocks(int level, double normalizedScore) {
        int bounded = boundedLevel(level);
        return bounded == 0 ? 0.0
                : 6.0 + 0.10 * bounded + 2.0 * milestoneCount(normalizedScore);
    }

    private static int boundedLevel(int level) {
        return Math.max(0, Math.min(StatProgress.MAX_LEVEL, level));
    }
}
```

- [ ] **Step 6: Verify GREEN and commit**

Run the Step 3 command. Expected: all perk and hunter-rule tests pass.

```powershell
git add src/main/java/tong/statmod/perks src/main/java/tong/statmod/effects/HunterPerceptionRules.java src/test/java/tong/statmod/perks src/test/java/tong/statmod/effects/HunterPerceptionRulesTest.java
git commit -m "feat: define hunter perception perks"
```

---

### Task 2: Add the bounded prey packet and client mark cache

**Files:**
- Create: `src/main/java/tong/statmod/network/TrackedPreyMessage.java`
- Create: `src/main/java/tong/statmod/client/hunter/TrackedPreyCache.java`
- Create: `src/main/java/tong/statmod/client/hunter/ClientHunterPerception.java`
- Modify: `src/main/java/tong/statmod/network/StatNetwork.java`
- Modify: `src/main/java/tong/statmod/StatModRuntime.java`
- Modify: `src/main/java/tong/statmod/network/StatsSnapshotMessage.java`
- Modify: `src/test/java/tong/statmod/StatModRuntimeTest.java`
- Modify: `src/test/java/tong/statmod/network/StatProgressNoticeMessageTest.java`
- Modify: `src/test/java/tong/statmod/network/StatsSnapshotMessageTest.java`
- Create: `src/test/java/tong/statmod/network/TrackedPreyMessageTest.java`
- Create: `src/test/java/tong/statmod/client/hunter/TrackedPreyCacheTest.java`

**Interfaces:**
- Produces `TrackedPreyMessage(int entityId, int durationTicks)`.
- Produces `TrackedPreyMessage.clear(): TrackedPreyMessage` and `isClear(): boolean`.
- Produces `TrackedPreyCache.accept(TrackedPreyMessage, long)` and
  `entityId(long): OptionalInt`.
- Produces `ClientHunterPerception.accept(TrackedPreyMessage)` and `clear()`.
- Produces `StatNetwork.sendTrackedPrey(ServerPlayer, int, int)` and
  `sendTrackedPreyClear(ServerPlayer)`.
- Sets protocol `"7"`, `StatsSnapshotMessage.MAX_PERKS == 39`, and message ID 4.

- [ ] **Step 1: Write failing packet and cache tests**

Create packet tests:

```java
@Test
void roundTripsMarkAndClear() {
    assertRoundTrip(new TrackedPreyMessage(42, 280));
    assertRoundTrip(TrackedPreyMessage.clear());
}

@Test
void rejectsMalformedBoundsBeforeCacheMutation() {
    assertThrows(IllegalArgumentException.class,
            () -> new TrackedPreyMessage(-2, 0));
    assertThrows(IllegalArgumentException.class,
            () -> new TrackedPreyMessage(3, -1));
    assertThrows(IllegalArgumentException.class,
            () -> new TrackedPreyMessage(3, 401));
    assertThrows(IllegalArgumentException.class,
            () -> new TrackedPreyMessage(-1, 1));
    assertThrows(IllegalArgumentException.class,
            () -> new TrackedPreyMessage(3, 0));
}
```

Create cache tests:

```java
@Test
void replacesRefreshesExpiresAndClearsOneMark() {
    TrackedPreyCache cache = new TrackedPreyCache();
    cache.accept(new TrackedPreyMessage(7, 20), 100);
    assertEquals(7, cache.entityId(119).orElseThrow());
    assertTrue(cache.entityId(120).isEmpty());

    cache.accept(new TrackedPreyMessage(8, 40), 200);
    cache.accept(new TrackedPreyMessage(9, 40), 210);
    assertEquals(9, cache.entityId(249).orElseThrow());
    cache.accept(TrackedPreyMessage.clear(), 220);
    assertTrue(cache.entityId(220).isEmpty());
}
```

- [ ] **Step 2: Update protocol/snapshot tests and verify RED**

Change both protocol assertions to `"7"`, maximum to 39, and extend the full
snapshot setup with Tracking and Keen Senses at level 75. Assert:

```java
assertEquals(39, decoded.activePerkIds().size());
assertEquals("statmod:rapidite_25", decoded.activePerkIds().get(0));
assertEquals("statmod:keen_senses_75", decoded.activePerkIds().get(38));
```

Run:

```powershell
.\gradlew.bat test --tests "tong.statmod.StatModRuntimeTest" --tests "tong.statmod.network.*" --tests "tong.statmod.client.hunter.TrackedPreyCacheTest" --console=plain
```

Expected: compilation fails for missing prey types and protocol/count assertions
fail against 6/33.

- [ ] **Step 3: Implement the bounded record and pure cache**

Create the record:

```java
public record TrackedPreyMessage(int entityId, int durationTicks) {
    public static final int CLEAR_ENTITY_ID = -1;
    public static final int MAX_DURATION_TICKS = 400;

    public TrackedPreyMessage {
        boolean clear = entityId == CLEAR_ENTITY_ID && durationTicks == 0;
        boolean mark = entityId >= 0 && durationTicks > 0
                && durationTicks <= MAX_DURATION_TICKS;
        if (!clear && !mark) {
            throw new IllegalArgumentException("invalid tracked prey payload");
        }
    }

    public static TrackedPreyMessage clear() {
        return new TrackedPreyMessage(CLEAR_ENTITY_ID, 0);
    }

    public boolean isClear() {
        return entityId == CLEAR_ENTITY_ID;
    }

    public static void encode(TrackedPreyMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeVarInt(message.durationTicks);
    }

    public static TrackedPreyMessage decode(FriendlyByteBuf buffer) {
        return new TrackedPreyMessage(buffer.readVarInt(), buffer.readVarInt());
    }
}
```

Implement `TrackedPreyCache` with two primitive fields and no Minecraft import:

```java
public final class TrackedPreyCache {
    private int entityId = TrackedPreyMessage.CLEAR_ENTITY_ID;
    private long expiresAt = Long.MIN_VALUE;

    public void accept(TrackedPreyMessage message, long currentTick) {
        if (message == null || message.isClear()) {
            clear();
            return;
        }
        entityId = message.entityId();
        long duration = message.durationTicks();
        expiresAt = currentTick > Long.MAX_VALUE - duration
                ? Long.MAX_VALUE : currentTick + duration;
    }

    public OptionalInt entityId(long currentTick) {
        if (entityId < 0 || currentTick >= expiresAt) {
            clear();
            return OptionalInt.empty();
        }
        return OptionalInt.of(entityId);
    }

    public void clear() {
        entityId = TrackedPreyMessage.CLEAR_ENTITY_ID;
        expiresAt = Long.MIN_VALUE;
    }
}
```

- [ ] **Step 4: Register message ID 4 and update bounds**

Set protocol 7 and maximum 39. Register:

```java
CHANNEL.registerMessage(4, TrackedPreyMessage.class,
        TrackedPreyMessage::encode,
        TrackedPreyMessage::decode,
        (message, contextSupplier) -> {
            var context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientHunterPerception.accept(message)));
            context.setPacketHandled(true);
        },
        Optional.of(NetworkDirection.PLAY_TO_CLIENT));
```

Add player-targeted send methods using `PacketDistributor.PLAYER`. Create the
initial `ClientHunterPerception` facade; Task 4 will add scan state:

```java
private static final TrackedPreyCache MARK = new TrackedPreyCache();
private static long clientTick;

public static void accept(TrackedPreyMessage message) {
    MARK.accept(message, clientTick);
}

public static void tickClock() {
    if (clientTick < Long.MAX_VALUE) {
        clientTick++;
    }
}

public static OptionalInt markedEntityId() {
    return MARK.entityId(clientTick);
}

public static void clear() {
    MARK.clear();
    clientTick = 0L;
}
```

- [ ] **Step 5: Verify GREEN and commit**

Run the Step 2 command. Expected: packet, cache, protocol, canonical filtering,
39-item round trip, and oversized-payload rejection all pass.

```powershell
git add src/main/java/tong/statmod/StatModRuntime.java src/main/java/tong/statmod/network src/main/java/tong/statmod/client/hunter src/test/java/tong/statmod/StatModRuntimeTest.java src/test/java/tong/statmod/network src/test/java/tong/statmod/client/hunter
git commit -m "feat: sync tracked prey state"
```

---

### Task 3: Acquire marks from committed hostile damage

**Files:**
- Create: `src/main/java/tong/statmod/event/HunterPerceptionEvents.java`
- Create: `src/main/java/tong/statmod/effects/HunterTrackingService.java`
- Create: `src/test/java/tong/statmod/event/HunterPerceptionEventsContractTest.java`
- Create: `src/test/java/tong/statmod/effects/HunterTrackingServiceContractTest.java`

**Interfaces:**
- Consumes `AutomaticPerkBonuses.from(PlayerStats)` and
  `HunterPerceptionRules.trackingDurationTicks(int, double)`.
- Consumes `StatNetwork.sendTrackedPrey(ServerPlayer, int, int)`.
- Produces `HunterTrackingService.mark(ServerPlayer, LivingEntity): boolean`.
- Adds one read-only `LivingDamageEvent` observer at `EventPriority.LOWEST`.

- [ ] **Step 1: Write failing event and service contracts**

Require the event source to contain:

```java
@SubscribeEvent(priority = EventPriority.LOWEST)
public static void damage(LivingDamageEvent event)
```

and all of these guards:

```java
Float.isFinite(event.getAmount())
event.getAmount() > 0F
event.getSource().getEntity() instanceof ServerPlayer
target instanceof Enemy
XpAwardService.isEligible(player)
HunterTrackingService.mark(player, target)
```

Assert the source contains none of:

```text
event.setAmount(
event.setCanceled(
setGlowingTag(
tong.statmod.dungeon
```

Require the service source to read `StatType.TRACKING`, resolve
`TRACKING_FOCUS`, calculate duration, reject duration zero, and call exactly one
`StatNetwork.sendTrackedPrey`.

- [ ] **Step 2: Run focused contracts and verify RED**

```powershell
.\gradlew.bat test --tests "tong.statmod.event.HunterPerceptionEventsContractTest" --tests "tong.statmod.effects.HunterTrackingServiceContractTest" --console=plain
```

Expected: both tests fail because their production files are absent.

- [ ] **Step 3: Implement the minimal read-only event observer**

Use this event flow:

```java
@SubscribeEvent(priority = EventPriority.LOWEST)
public static void damage(LivingDamageEvent event) {
    float amount = event.getAmount();
    LivingEntity target = event.getEntity();
    if (!Float.isFinite(amount) || amount <= 0F
            || !(event.getSource().getEntity() instanceof ServerPlayer player)
            || !(target instanceof Enemy)
            || !XpAwardService.isEligible(player)) {
        return;
    }
    HunterTrackingService.mark(player, target);
}
```

The handler must not inspect weapon classes, spell schools, dungeon tags, or
cancel/mutate damage.

- [ ] **Step 4: Implement server-authoritative duration resolution**

`HunterTrackingService.mark` must:

```java
PlayerStats stats = player.getCapability(StatCapabilities.PLAYER_STATS)
        .resolve().orElse(null);
if (stats == null || !(target instanceof Enemy) || !target.isAlive()) {
    return false;
}
int level = stats.get(StatType.TRACKING).level();
double score = AutomaticPerkBonuses.from(stats)
        .amount(AutomaticPerkEffect.TRACKING_FOCUS);
int duration = HunterPerceptionRules.trackingDurationTicks(level, score);
if (duration == 0) {
    return false;
}
StatNetwork.sendTrackedPrey(player, target.getId(), duration);
return true;
```

Do not add persisted or static per-player maps.

- [ ] **Step 5: Verify GREEN, run combat regressions, and commit**

```powershell
.\gradlew.bat test --tests "tong.statmod.event.*" --tests "tong.statmod.effects.*" --tests "tong.statmod.progression.xp.*" --console=plain
```

Expected: hunter contracts and all existing combat/XP regressions pass.

```powershell
git add src/main/java/tong/statmod/event/HunterPerceptionEvents.java src/main/java/tong/statmod/effects/HunterTrackingService.java src/test/java/tong/statmod/event/HunterPerceptionEventsContractTest.java src/test/java/tong/statmod/effects/HunterTrackingServiceContractTest.java
git commit -m "feat: mark committed hostile prey"
```

---

### Task 4: Scan threats and render personal client contours

**Files:**
- Create: `src/main/java/tong/statmod/client/hunter/ThreatCandidate.java`
- Create: `src/main/java/tong/statmod/client/hunter/HunterThreatSelection.java`
- Modify: `src/main/java/tong/statmod/client/hunter/ClientHunterPerception.java`
- Create: `src/main/java/tong/statmod/client/hunter/HunterPerceptionRenderer.java`
- Modify: `src/main/java/tong/statmod/client/ClientInputEvents.java`
- Modify: `src/main/java/tong/statmod/client/ClientStatsEvents.java`
- Create: `src/test/java/tong/statmod/client/hunter/HunterThreatSelectionTest.java`
- Create: `src/test/java/tong/statmod/client/hunter/ClientHunterPerceptionContractTest.java`
- Create: `src/test/java/tong/statmod/client/hunter/HunterPerceptionRendererContractTest.java`

**Interfaces:**
- Produces record `ThreatCandidate(int entityId, double distanceSquared,
  boolean enemy, boolean alive, boolean removed)`.
- Produces `HunterThreatSelection.select(List<ThreatCandidate>, double, int): List<Integer>`.
- Produces `ClientHunterPerception.tick(Minecraft)`, `markedEntityId()`,
  `threatEntityIds()`, and `clear()`.
- Produces one `Dist.CLIENT` `RenderLevelStageEvent.Stage.AFTER_ENTITIES` renderer.

- [ ] **Step 1: Write failing pure selection tests**

Create candidates that prove exact spherical filtering, nearest-first order,
hostile/alive/removal guards, and cap behavior:

```java
@Test
void keepsOnlyNearestEligibleThreatsInsideExactRadius() {
    List<ThreatCandidate> input = List.of(
            new ThreatCandidate(1, 25.0, true, true, false),
            new ThreatCandidate(2, 4.0, true, true, false),
            new ThreatCandidate(3, 1.0, false, true, false),
            new ThreatCandidate(4, 9.0, true, false, false),
            new ThreatCandidate(5, 16.0, true, true, true),
            new ThreatCandidate(6, 36.1, true, true, false));

    assertEquals(List.of(2, 1), HunterThreatSelection.select(input, 6.0, 64));
    assertEquals(List.of(2), HunterThreatSelection.select(input, 6.0, 1));
    assertEquals(List.of(), HunterThreatSelection.select(input, 0.0, 64));
    assertEquals(List.of(), HunterThreatSelection.select(input, Double.NaN, 64));
}
```

- [ ] **Step 2: Write failing client and renderer contracts**

Require client logic to:

- call `ClientHunterPerception.tick(minecraft)` from END client tick;
- clear on logout through `ClientStatsEvents`;
- scan only when `player.isCrouching()` and not spectator;
- scan every five ticks with `SCAN_INTERVAL_TICKS = 5`;
- use `Enemy`, exact range, `HunterPerceptionRules`, and a 64-result cap;
- clear on missing player/level, death, level 0, non-crouching, and level identity change;
- count only canonical `tracking_25/50/75` and `keen_senses_25/50/75` IDs.

Require the renderer to contain:

```java
RenderLevelStageEvent.Stage.AFTER_ENTITIES
LevelRenderer.renderLineBox
RenderSystem.disableDepthTest()
RenderSystem.enableDepthTest()
```

Require amber `(1.0F, 0.62F, 0.10F)` and red `(1.0F, 0.15F, 0.15F)`, marked
ID precedence, camera-relative interpolation, and `Dist.CLIENT`. Reject all
global glow, team, metadata-packet, sound, particle, toast, and action-bar calls.

- [ ] **Step 3: Run focused client tests and verify RED**

```powershell
.\gradlew.bat test --tests "tong.statmod.client.hunter.*" --tests "tong.statmod.client.ClientStatsCacheTest" --console=plain
```

Expected: compilation fails for candidate/selector types and source contracts
fail because ticking, scanning, and rendering are absent.

- [ ] **Step 4: Implement pure nearest-first selection**

Implement `select` as:

```java
public static List<Integer> select(
        List<ThreatCandidate> candidates, double range, int maximum) {
    if (candidates == null || candidates.isEmpty()
            || !Double.isFinite(range) || range <= 0.0 || maximum <= 0) {
        return List.of();
    }
    double rangeSquared = range * range;
    int limit = Math.min(64, maximum);
    return candidates.stream()
            .filter(Objects::nonNull)
            .filter(candidate -> candidate.enemy() && candidate.alive()
                    && !candidate.removed())
            .filter(candidate -> Double.isFinite(candidate.distanceSquared())
                    && candidate.distanceSquared() >= 0.0
                    && candidate.distanceSquared() <= rangeSquared)
            .sorted(Comparator.comparingDouble(ThreatCandidate::distanceSquared)
                    .thenComparingInt(ThreatCandidate::entityId))
            .limit(limit)
            .map(ThreatCandidate::entityId)
            .toList();
}
```

- [ ] **Step 5: Extend the client cache and throttled scan**

On each END client tick:

1. increment the monotonic mark-cache clock;
2. clear all state if player/level is missing, player is dead/spectator, or the
   `ClientLevel` identity changed;
3. read levels and active IDs from `ClientStatsCache.state()`;
4. derive normalized score as `0.25 * canonicalActiveIdCount`;
5. validate the marked entity against alive/removed/`Enemy`, same level, expiry,
   and Tracking range;
6. when crouching and Keen Senses level is positive, rescan every five ticks;
7. otherwise clear the threat ID list immediately.

Use this core scan structure:

```java
List<LivingEntity> nearby = level.getEntitiesOfClass(
        LivingEntity.class, player.getBoundingBox().inflate(range));
List<ThreatCandidate> candidates = nearby.stream()
        .map(entity -> new ThreatCandidate(
                entity.getId(), player.distanceToSqr(entity),
                entity instanceof Enemy, entity.isAlive(), entity.isRemoved()))
        .toList();
threatEntityIds = HunterThreatSelection.select(candidates, range, 64);
```

The enclosing `tick(Minecraft minecraft)` must implement the seven ordered
guards above, use `clientTick % SCAN_INTERVAL_TICKS == 0` for rescan cadence,
and publish `List.copyOf(...)` so render code cannot mutate the cache.

- [ ] **Step 6: Implement personal world-space contours**

Register a `Dist.CLIENT` Forge-bus subscriber. At `AFTER_ENTITIES`, resolve the
current marked ID first, render it amber, then render red threat IDs while
skipping the marked ID. For every entity:

```java
double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
AABB box = entity.getBoundingBox().move(
        x - entity.getX() - camera.x,
        y - entity.getY() - camera.y,
        z - entity.getZ() - camera.z).inflate(0.05);
LevelRenderer.renderLineBox(poseStack, vertexConsumer, box, red, green, blue, 1.0F);
```

Use `RenderType.lines()`, end only that batch, and restore depth testing in a
`finally` block. Do not change depth mask, blend mode, entity flags, or teams.
Before calculating the box, define:

```java
Vec3 camera = event.getCamera().getPosition();
float partialTick = event.getPartialTick();
```

- [ ] **Step 7: Verify GREEN and commit**

Run the Step 3 command, then:

```powershell
.\gradlew.bat test --tests "tong.statmod.client.*" --tests "tong.statmod.network.*" --console=plain
```

Expected: selector, cache, renderer contracts, existing stats UI, book study,
notices, and packet tests all pass.

```powershell
git add src/main/java/tong/statmod/client src/test/java/tong/statmod/client
git commit -m "feat: render personal hunter perception"
```

---

### Task 5: Localize, document, verify, deploy, and hand off

**Files:**
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`
- Modify: `src/test/java/tong/statmod/client/stats/AutomaticPerkLanguageTest.java`
- Modify: `README.md`
- Modify: `docs/compatibility/forge-1.20.1-supported-runtime.md`
- Modify: `src/test/java/tong/statmod/SupportedRuntimeContractTest.java`
- Deploy: `build/libs/statmod-0.1.0+1.20.1.jar`

**Interfaces:**
- Consumes the complete 39-perk catalog and protocol 7 runtime.
- Produces complete English/French presentation and one verified `test-vrai` JAR.

- [ ] **Step 1: Write failing localization and runtime contracts**

Extend the catalog-driven language test and require exact first-tier fragments:

```java
assertTrue(english.contains(
        "\"perk.statmod.tracking_25.description\": \"+2 seconds and +4 blocks"));
assertTrue(french.contains(
        "\"perk.statmod.tracking_25.description\": \"+2 secondes et +4 blocs"));
assertTrue(english.contains(
        "\"perk.statmod.keen_senses_25.description\": \"+2 blocks"));
assertTrue(french.contains(
        "\"perk.statmod.keen_senses_25.description\": \"+2 blocs"));
```

Update `SupportedRuntimeContractTest` to require:

```text
39 automatic perks
6 hunter perception perks
protocol 7
personal marked-prey contour
crouched personal threat scan
no damage, dodge, loot, or global glowing state
```

- [ ] **Step 2: Run documentation tests and verify RED**

```powershell
.\gradlew.bat test --tests "tong.statmod.client.stats.AutomaticPerkLanguageTest" --tests "tong.statmod.SupportedRuntimeContractTest" --console=plain
```

Expected: six missing perk localizations and outdated 33/protocol-6 runtime text
fail.

- [ ] **Step 3: Add all twelve localized entries**

Add names I/II/III for Tracking and Keen Senses. Each Tracking description
states that this milestone adds two seconds and four blocks to the personal
prey mark. Each Keen Senses description states that this milestone adds two
blocks to the crouched personal hostile scan. Do not mention damage, loot,
dodge, global glow, or notifications.

- [ ] **Step 4: Update runtime documentation and verify GREEN**

Document formulas, eligibility, personal rendering, scan throttle/cap, protocol
7, catalog 39, unchanged XP sources, provider boundaries, and explicit dungeon
non-integration in README and the supported-runtime record. Run Step 2 and
expect all documentation contracts to pass.

- [ ] **Step 5: Commit localization and documentation**

```powershell
git add src/main/resources/assets/statmod/lang src/test/java/tong/statmod/client/stats/AutomaticPerkLanguageTest.java README.md docs/compatibility/forge-1.20.1-supported-runtime.md src/test/java/tong/statmod/SupportedRuntimeContractTest.java
git commit -m "docs: document hunter perception perks"
```

- [ ] **Step 6: Run complete clean verification twice**

```powershell
.\gradlew.bat clean test build --console=plain
.\gradlew.bat test --rerun-tasks --console=plain
```

Expected: both commands exit 0; every XML suite reports zero failures and zero
errors.

- [ ] **Step 7: Inspect the artifact**

Require exactly one JAR and exactly one class entry for catalog, hunter rules,
tracked-prey message, client perception cache, renderer, and server event.
Require zero duplicate archive entries and zero bundled classes under Iron's
Spells, Epic Fight, or Pufferfish namespaces. Record SHA-256.

- [ ] **Step 8: Run the required-provider GameTest smoke**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-gametest-server.ps1 -ProviderModsDirectory 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods'
```

Expected: `OK required-provider Forge GameTest server smoke`.

- [ ] **Step 9: Back up and deploy only STAT Mod**

Confirm no Minecraft/Forge Java process is running. Require exactly one existing
`statmod-*.jar`; preserve total mod count. Back it up under:

```text
C:\Users\El Hadji\AppData\Roaming\.minecraft\statmod-backups\test-vrai\<timestamp>-before-hunter-perception-perks
```

Replace only the STAT Mod JAR and require source/target SHA-256 equality.

- [ ] **Step 10: Preserve parallel dungeon work and push**

Require the hunter worktree to be clean. Do not merge into, reset, stash, stage,
or clean the active dungeon worktree. Push the verified hunter HEAD as a
fast-forward to `origin/forge-1.20.1`, then require:

```powershell
git ls-remote origin refs/heads/forge-1.20.1
```

to equal local hunter `HEAD`. Keep the hunter worktree until dungeon integration
is explicitly complete.

## Completion audit

- Confirm every design section maps to a task.
- Confirm all first 33 perk definitions are unchanged.
- Confirm no file under `tong/statmod/dungeon` appears in any commit.
- Confirm there is one new packet only, ID 4, protocol 7, and maximum 39.
- Confirm no damage mutation, dodge, loot modification, global glow, entity
  metadata, teams, targeting, notifications, particles, sounds, or key bindings.
- Confirm all client rendering classes are isolated behind `Dist.CLIENT`.
- Confirm personal mark replacement, expiry, range, scan throttle, 64 cap,
  nearest-first order, amber precedence, and clear paths are tested.
