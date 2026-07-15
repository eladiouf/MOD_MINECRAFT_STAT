# Forge 1.20.1 Native Stats Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a localized, read-only native statistics screen on `P` plus bounded XP and level-up notices for all 23 synchronized statistics.

**Architecture:** Keep progression server-authoritative and build the screen from immutable revisions of `ClientStatsCache`. Plain-Java presentation and notice models own ordering, formatting, bounds, and lifetimes; Forge-only classes own key registration, screen rendering, overlay rendering, and clientbound packet wiring.

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.10, Forge `SimpleChannel`, JUnit 5, vanilla `Screen`/`GuiGraphics`/`KeyMapping`.

## Global Constraints

- Work only on branch `forge-1.20.1` with Minecraft 1.20.1, Forge 47.4.10, and Java 17.
- Keep Epic Fight 20.14.17 and Pufferfish's Attributes 0.8.2 mandatory; do not import their APIs in client UI code.
- Keep the screen read-only: no stat mutation packet, perk action, or server capability lookup may originate from it.
- Use stable existing `StatType` IDs and `StatProgress.requiredXp`; do not change the XP curve, NBT schema, or stat roster.
- Register client classes behind `Dist.CLIENT`; a dedicated server must not load input or rendering classes.
- Use `P` as the configurable default key and provide complete French and English text.
- Render with Minecraft primitives and vanilla widgets; do not add textures, shaders, or another UI dependency.
- Keep at most four notices; merge same-stat awards within 20 ticks; show XP for 50 ticks and level gains for 80 ticks with a 15-tick fade.
- Preserve the two known non-fatal upstream Iron's Spells 3.16.2 loot-table errors; do not patch third-party JARs.

---

### Task 1: Revisioned Client Snapshot

**Files:**
- Modify: `src/main/java/tong/statmod/client/ClientStatsCache.java`
- Create: `src/main/java/tong/statmod/client/ClientStatsState.java`
- Create: `src/test/java/tong/statmod/client/ClientStatsCacheTest.java`

**Interfaces:**
- Consumes: `Map<StatType, StatValue>` from `StatsSnapshotMessage`.
- Produces: `ClientStatsState(long revision, Map<StatType, StatValue> values)`, `ClientStatsCache.state()`, and the preserved `snapshot()` API.

- [ ] **Step 1: Write the failing cache tests**

```java
class ClientStatsCacheTest {
    @AfterEach void clear() { ClientStatsCache.clear(); }

    @Test void replacePublishesOneImmutableRevision() {
        long before = ClientStatsCache.state().revision();
        ClientStatsCache.replace(Map.of(StatType.AGILITY, new StatValue(12, 34)));
        ClientStatsState state = ClientStatsCache.state();
        assertEquals(before + 1, state.revision());
        assertEquals(new StatValue(12, 34), state.values().get(StatType.AGILITY));
        assertEquals(23, state.values().size());
        assertThrows(UnsupportedOperationException.class,
                () -> state.values().put(StatType.AGILITY, new StatValue(0, 0)));
    }

    @Test void clearPublishesACompleteZeroSnapshot() {
        ClientStatsCache.replace(Map.of(StatType.AGILITY, new StatValue(12, 34)));
        ClientStatsCache.clear();
        assertEquals(23, ClientStatsCache.state().values().size());
        assertTrue(ClientStatsCache.state().values().values().stream()
                .allMatch(value -> value.equals(new StatValue(0, 0))));
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew test --tests tong.statmod.client.ClientStatsCacheTest`

Expected: compilation fails because `ClientStatsState` and `state()` do not exist.

- [ ] **Step 3: Implement immutable revision publication**

```java
public record ClientStatsState(long revision, Map<StatType, StatValue> values) {
    public ClientStatsState {
        EnumMap<StatType, StatValue> copy = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            copy.put(type, values.getOrDefault(type, new StatValue(0, 0)));
        }
        values = Collections.unmodifiableMap(copy);
    }
}
```

Change the cache to one volatile state and preserve `snapshot()`:

```java
private static volatile ClientStatsState state = zeroState(0);

public static void replace(Map<StatType, StatValue> next) {
    state = new ClientStatsState(state.revision() + 1, next);
}

public static void clear() {
    state = zeroState(state.revision() + 1);
}

public static ClientStatsState state() { return state; }
public static Map<StatType, StatValue> snapshot() { return state.values(); }
```

- [ ] **Step 4: Run the focused test and existing snapshot tests**

Run: `./gradlew test --tests tong.statmod.client.ClientStatsCacheTest --tests tong.statmod.network.StatsSnapshotMessageTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/client/ClientStatsCache.java src/main/java/tong/statmod/client/ClientStatsState.java src/test/java/tong/statmod/client/ClientStatsCacheTest.java
git commit -m "feat: publish revisioned client stats"
```

---

### Task 2: Exhaustive Stats Presentation Model

**Files:**
- Create: `src/main/java/tong/statmod/client/stats/StatDisplayState.java`
- Create: `src/main/java/tong/statmod/client/stats/StatPresentation.java`
- Create: `src/main/java/tong/statmod/client/stats/StatsScreenModel.java`
- Create: `src/test/java/tong/statmod/client/stats/StatsScreenModelTest.java`

**Interfaces:**
- Consumes: `ClientStatsState`, `StatType`, `StatFamily`, `StatProgress.requiredXp(int)`.
- Produces: `StatsScreenModel.from(ClientStatsState)` and immutable `FamilySection` / `StatCard` records.

- [ ] **Step 1: Write failing model tests**

```java
@Test void containsEveryStatOnceInFamilyAndEnumOrder() {
    StatsScreenModel model = StatsScreenModel.from(new ClientStatsState(7, Map.of()));
    List<StatType> flattened = model.families().stream()
            .flatMap(family -> family.cards().stream()).map(StatsScreenModel.StatCard::type).toList();
    assertEquals(List.of(StatType.values()), flattened);
    assertEquals(6, model.families().size());
}

@Test void computesBoundedProgressAndMaxState() {
    Map<StatType, StatValue> values = Map.of(
            StatType.AGILITY, new StatValue(0, 5),
            StatType.MANA_POOL, new StatValue(100, 999));
    StatsScreenModel model = StatsScreenModel.from(new ClientStatsState(3, values));
    assertEquals(0.5, model.card(StatType.AGILITY).progress());
    assertEquals(10, model.card(StatType.AGILITY).requiredXp());
    assertTrue(model.card(StatType.MANA_POOL).maxLevel());
    assertEquals(0, model.card(StatType.MANA_POOL).requiredXp());
}

@Test void magicalStatsAreFoundationAndCurrentGameplayStatsAreActive() {
    assertEquals(StatDisplayState.ACTIVE, StatPresentation.of(StatType.BRUTE_FORCE).state());
    assertEquals(StatDisplayState.FOUNDATION, StatPresentation.of(StatType.ARCANE_POWER).state());
}
```

- [ ] **Step 2: Run tests and verify RED**

Run: `./gradlew test --tests tong.statmod.client.stats.StatsScreenModelTest`

Expected: compilation fails because the presentation types do not exist.

- [ ] **Step 3: Implement exhaustive presentation metadata**

```java
public record StatPresentation(String nameKey, String descriptionKey, StatDisplayState state) {
    private static final EnumSet<StatType> FOUNDATION = EnumSet.of(
            StatType.ARCANE_POWER, StatType.CASTING_SPEED, StatType.MANA_POOL,
            StatType.ERUDITION, StatType.MAGIC_RESISTANCE, StatType.FIRE_AFFINITY,
            StatType.WATER_AFFINITY, StatType.EARTH_AFFINITY, StatType.AIR_AFFINITY);

    public static StatPresentation of(StatType type) {
        return new StatPresentation("stat.statmod." + type.id(),
                "stat.statmod." + type.id() + ".description",
                FOUNDATION.contains(type) ? StatDisplayState.FOUNDATION : StatDisplayState.ACTIVE);
    }
}
```

Implement `StatsScreenModel` with `revision`, stable family order, a lookup map, `requiredXp`, `maxLevel`, and `progress = clamp(xp / requiredXp, 0, 1)`. Treat negative XP as zero and level 100 as `MAX`.

- [ ] **Step 4: Run focused model tests**

Run: `./gradlew test --tests tong.statmod.client.stats.StatsScreenModelTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/client/stats src/test/java/tong/statmod/client/stats
git commit -m "feat: model stats screen presentation"
```

---

### Task 3: Complete French and English UI Language

**Files:**
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
- Create: `src/test/java/tong/statmod/client/stats/StatsLanguageResourcesTest.java`

**Interfaces:**
- Consumes: translation-key conventions from `StatPresentation` and every `StatFamily.slug()`.
- Produces: complete bilingual keys for names, descriptions, screen, key mapping, statuses, narration, and notices.

- [ ] **Step 1: Write the failing resource completeness test**

```java
@Test void bothLanguagesContainEveryRequiredStatsKey() throws Exception {
    for (String locale : List.of("fr_fr", "en_us")) {
        String json = Files.readString(Path.of("src/main/resources/assets/statmod/lang/" + locale + ".json"));
        for (StatType type : StatType.values()) {
            assertTrue(json.contains("\"stat.statmod." + type.id() + "\""));
            assertTrue(json.contains("\"stat.statmod." + type.id() + ".description\""));
        }
        for (StatFamily family : StatFamily.values()) {
            assertTrue(json.contains("\"family.statmod." + family.slug() + "\""));
        }
        for (String key : REQUIRED_UI_KEYS) assertTrue(json.contains("\"" + key + "\""));
    }
}
```

`REQUIRED_UI_KEYS` contains `key.categories.statmod`, `key.statmod.open_stats`, `screen.statmod.stats.title`, `screen.statmod.level`, `screen.statmod.xp`, `screen.statmod.max`, `screen.statmod.status.active`, `screen.statmod.status.foundation`, `notice.statmod.xp`, and `notice.statmod.level_up`.

- [ ] **Step 2: Run test and verify RED**

Run: `./gradlew test --tests tong.statmod.client.stats.StatsLanguageResourcesTest`

Expected: FAIL on the first missing stat translation key.

- [ ] **Step 3: Add all bilingual strings**

Use stable keys such as:

```json
"key.categories.statmod": "STAT Mod",
"key.statmod.open_stats": "Ouvrir les statistiques",
"screen.statmod.stats.title": "Statistiques du personnage",
"stat.statmod.brute_force": "Force brute",
"stat.statmod.brute_force.description": "Améliore les dégâts des armes lourdes classées.",
"stat.statmod.arcane_power.description": "Intégration magique prévue avec Iron's Spells.",
"notice.statmod.xp": "+%s XP — %s",
"notice.statmod.level_up": "%s atteint le niveau %s"
```

Provide equivalent natural English strings and distinct current-behavior descriptions for all 23 stats.

- [ ] **Step 4: Run resource test**

Run: `./gradlew test --tests tong.statmod.client.stats.StatsLanguageResourcesTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/resources/assets/statmod/lang src/test/java/tong/statmod/client/stats/StatsLanguageResourcesTest.java
git commit -m "feat: localize stats presentation"
```

---

### Task 4: Register `P` and Open the Read-Only Screen

**Files:**
- Create: `src/main/java/tong/statmod/client/ClientKeyMappings.java`
- Create: `src/main/java/tong/statmod/client/ClientInputEvents.java`
- Create: `src/main/java/tong/statmod/client/stats/StatsOverviewScreen.java`
- Create: `src/main/java/tong/statmod/client/stats/StatCardRenderer.java`
- Create: `src/test/java/tong/statmod/client/StatsScreenWiringContractTest.java`

**Interfaces:**
- Consumes: `ClientStatsCache.state()` and `StatsScreenModel.from(...)`.
- Produces: `ClientKeyMappings.OPEN_STATS`, `StatsOverviewScreen`, and client-only event wiring.

- [ ] **Step 1: Write failing source/wiring contracts**

Assert the key class contains `GLFW.GLFW_KEY_P`, `RegisterKeyMappingsEvent`, and `event.register(OPEN_STATS)`. Assert the input class contains `TickEvent.ClientTickEvent`, `Phase.END`, `consumeClick`, and `minecraft.setScreen(new StatsOverviewScreen())`. Assert the screen source does not contain `StatNetwork`, `sendToServer`, `StatCapabilities`, or mutation command strings.

- [ ] **Step 2: Run contract and verify RED**

Run: `./gradlew test --tests tong.statmod.client.StatsScreenWiringContractTest`

Expected: FAIL because the client classes do not exist.

- [ ] **Step 3: Register the key on the mod event bus**

```java
@Mod.EventBusSubscriber(modid = StatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ClientKeyMappings {
    public static final KeyMapping OPEN_STATS = new KeyMapping(
            "key.statmod.open_stats", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, "key.categories.statmod");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) { event.register(OPEN_STATS); }
}
```

- [ ] **Step 4: Add tick input routing and screen toggling**

On `ClientTickEvent` `END`, consume all queued clicks. Open only when a local player exists and no other screen is open. Inside `StatsOverviewScreen.keyPressed`, close when `OPEN_STATS.matches(keyCode, scanCode)` or the inventory mapping matches; otherwise delegate to `super`.

- [ ] **Step 5: Implement responsive screen rendering**

Build family buttons with `Button.builder`, keep a static session-only selected family, and rebuild only the model when `ClientStatsCache.state().revision()` changes. Render a centered parchment-colored panel, title, family selector, and one or two card columns based on available width. Track a clamped `scrollOffset`, update it from `mouseScrolled`, apply it only to the clipped card area, and leave the title/family selector fixed. `StatCardRenderer` draws name, level or `MAX`, status, XP text, and a clamped progress bar using `GuiGraphics.fill` and `drawString`. Keep vanilla narration on family buttons, expose each visible card as a non-interactive `NarratableEntry` using its name/level/status text, and add mouse tooltips with `renderComponentTooltip`.

- [ ] **Step 6: Run focused contracts and full compile**

Run: `./gradlew test --tests tong.statmod.client.StatsScreenWiringContractTest --tests tong.statmod.client.stats.StatsScreenModelTest`

Expected: PASS and Forge client classes compile under 1.20.1 mappings.

- [ ] **Step 7: Commit**

```powershell
git add src/main/java/tong/statmod/client/ClientKeyMappings.java src/main/java/tong/statmod/client/ClientInputEvents.java src/main/java/tong/statmod/client/stats/StatsOverviewScreen.java src/main/java/tong/statmod/client/stats/StatCardRenderer.java src/test/java/tong/statmod/client/StatsScreenWiringContractTest.java
git commit -m "feat: add native stats screen"
```

---

### Task 5: Bounded Clientbound Progress Notice Packet

**Files:**
- Create: `src/main/java/tong/statmod/network/StatProgressNoticeMessage.java`
- Modify: `src/main/java/tong/statmod/network/StatNetwork.java`
- Modify: `src/main/java/tong/statmod/StatModRuntime.java`
- Create: `src/test/java/tong/statmod/network/StatProgressNoticeMessageTest.java`

**Interfaces:**
- Produces: `StatProgressNoticeMessage(StatType stat, int awardedXp, int newLevel, int levelsGained)`, `valid()`, and `StatNetwork.sendProgressNotice(...)`.
- Consumes later: `ClientProgressNotices.offer(message)`.

- [ ] **Step 1: Write RED packet tests**

```java
@Test void roundTripsStableIdAndNumbers() {
    var original = new StatProgressNoticeMessage(StatType.AGILITY, 25, 7, 1);
    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    StatProgressNoticeMessage.encode(original, buffer);
    assertEquals(original, StatProgressNoticeMessage.decode(buffer));
}

@Test void malformedPayloadIsBoundedAndInvalidUnknownIdIsRejected() {
    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    buffer.writeUtf("missing_stat");
    buffer.writeVarInt(Integer.MAX_VALUE);
    buffer.writeVarInt(Integer.MAX_VALUE);
    buffer.writeVarInt(Integer.MAX_VALUE);
    var decoded = StatProgressNoticeMessage.decode(buffer);
    assertFalse(decoded.valid());
    assertTrue(decoded.awardedXp() <= StatProgressNoticeMessage.MAX_AWARDED_XP);
    assertEquals(100, decoded.newLevel());
    assertEquals(100, decoded.levelsGained());
}
```

- [ ] **Step 2: Run test and verify RED**

Run: `./gradlew test --tests tong.statmod.network.StatProgressNoticeMessageTest`

Expected: compilation fails because the packet does not exist.

- [ ] **Step 3: Implement validation and registration**

Decode the stable ID with `StatType.fromId(id).orElse(null)`. Clamp XP to `0..1_000_000`, level to `0..100`, and levels gained to `0..100`. Register message discriminator `1`, direction `PLAY_TO_CLIENT`, after the existing snapshot discriminator `0`. The handler enqueues work and calls `ClientProgressNotices.offer(message)` only when `valid()`.

Change `StatModRuntime.NETWORK_PROTOCOL` from `"1"` to `"2"` because clients without discriminator `1` are not wire-compatible with progression notices. Extend the packet test to assert the exact protocol value.

Add:

```java
public static void sendProgressNotice(ServerPlayer player, StatType stat,
        int awardedXp, int newLevel, int levelsGained) {
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new StatProgressNoticeMessage(stat, awardedXp, newLevel, levelsGained));
}
```

- [ ] **Step 4: Run packet and network tests**

Run: `./gradlew test --tests tong.statmod.network.StatProgressNoticeMessageTest --tests tong.statmod.network.StatsSnapshotMessageTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/StatModRuntime.java src/main/java/tong/statmod/network/StatProgressNoticeMessage.java src/main/java/tong/statmod/network/StatNetwork.java src/test/java/tong/statmod/network/StatProgressNoticeMessageTest.java
git commit -m "feat: add progression notice packet"
```

---

### Task 6: Notice Queue and HUD Overlay

**Files:**
- Create: `src/main/java/tong/statmod/client/notice/ProgressNotice.java`
- Create: `src/main/java/tong/statmod/client/notice/ProgressNoticeQueue.java`
- Create: `src/main/java/tong/statmod/client/notice/ClientProgressNotices.java`
- Create: `src/main/java/tong/statmod/client/notice/ProgressNoticeOverlay.java`
- Modify: `src/main/java/tong/statmod/client/ClientStatsEvents.java`
- Create: `src/test/java/tong/statmod/client/notice/ProgressNoticeQueueTest.java`

**Interfaces:**
- Consumes: valid `StatProgressNoticeMessage` packets.
- Produces: immutable notice snapshots, `tick()`, `clear()`, and an above-all Forge overlay.

- [ ] **Step 1: Write RED queue tests**

Test that same-stat offers at ticks 10 and 29 merge XP; tick 50 creates a second notice because it is 21 ticks after the latest merge; a fifth distinct notice removes the oldest; XP notices expire after 50 ticks; level notices expire after 80; fade alpha is `1` before the final 15 ticks and reaches `0` at expiry; `clear()` empties the queue.

- [ ] **Step 2: Run test and verify RED**

Run: `./gradlew test --tests tong.statmod.client.notice.ProgressNoticeQueueTest`

Expected: compilation fails because the queue types do not exist.

- [ ] **Step 3: Implement the pure queue**

```java
public record ProgressNotice(StatType stat, int awardedXp, int newLevel,
        int levelsGained, long updatedAt, int remainingTicks) {
    ProgressNotice merge(StatProgressNoticeMessage message, long tick) {
        int xp = (int) Math.min(Integer.MAX_VALUE, (long) awardedXp + message.awardedXp());
        int levels = Math.min(100, levelsGained + message.levelsGained());
        int duration = levels > 0 ? ProgressNoticeQueue.LEVEL_DURATION
                : ProgressNoticeQueue.XP_DURATION;
        return new ProgressNotice(stat, xp, message.newLevel(), levels, tick, duration);
    }

    ProgressNotice nextTick() {
        return new ProgressNotice(stat, awardedXp, newLevel, levelsGained,
                updatedAt, remainingTicks - 1);
    }

    public float alpha() {
        return remainingTicks >= ProgressNoticeQueue.FADE_TICKS ? 1.0F
                : Math.max(0.0F, remainingTicks / (float) ProgressNoticeQueue.FADE_TICKS);
    }
}

public final class ProgressNoticeQueue {
    public static final int MAX_NOTICES = 4;
    public static final int MERGE_WINDOW = 20;
    public static final int XP_DURATION = 50;
    public static final int LEVEL_DURATION = 80;
    public static final int FADE_TICKS = 15;

    private final ArrayDeque<ProgressNotice> notices = new ArrayDeque<>();

    public void offer(StatProgressNoticeMessage message, long tick) {
        if (message == null || !message.valid()) return;
        ProgressNotice newest = notices.peekLast();
        if (newest != null && newest.stat() == message.stat()
                && tick - newest.updatedAt() < MERGE_WINDOW) {
            notices.removeLast();
            notices.addLast(newest.merge(message, tick));
        } else {
            int duration = message.levelsGained() > 0 ? LEVEL_DURATION : XP_DURATION;
            notices.addLast(new ProgressNotice(message.stat(), message.awardedXp(),
                    message.newLevel(), message.levelsGained(), tick, duration));
        }
        while (notices.size() > MAX_NOTICES) notices.removeFirst();
    }

    public void tick() {
        int currentSize = notices.size();
        for (int index = 0; index < currentSize; index++) {
            ProgressNotice next = notices.removeFirst().nextTick();
            if (next.remainingTicks() > 0) notices.addLast(next);
        }
    }

    public List<ProgressNotice> snapshot() { return List.copyOf(notices); }
    public void clear() { notices.clear(); }
}
```

`ProgressNotice` stores stat, accumulated XP, new level, accumulated levels gained, creation/update tick, remaining duration, and a clamped `alpha()`.

- [ ] **Step 4: Wire client ticks, logout clearing, and overlay registration**

`ClientProgressNotices` owns one queue and monotonically increasing client tick. Tick it on `ClientTickEvent.END`; clear it from `ClientStatsEvents.logout`. Register `ProgressNoticeOverlay::render` through `RegisterGuiOverlaysEvent.registerAboveAll("progress_notices", ...)` on the client mod bus.

The overlay returns early for an empty queue or `minecraft.options.renderDebug`. Render at upper-right with a dark translucent background, localized stat name, gold level-up color, white XP color, and alpha applied to every ARGB color.

- [ ] **Step 5: Run queue tests and compile Forge wiring**

Run: `./gradlew test --tests tong.statmod.client.notice.ProgressNoticeQueueTest --tests tong.statmod.client.StatsScreenWiringContractTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add src/main/java/tong/statmod/client/notice src/main/java/tong/statmod/client/ClientStatsEvents.java src/test/java/tong/statmod/client/notice
git commit -m "feat: render progression notices"
```

---

### Task 7: Emit Notices from Accepted Server Awards

**Files:**
- Modify: `src/main/java/tong/statmod/progression/xp/XpAwardService.java`
- Modify: `src/test/java/tong/statmod/progression/xp/XpAwardServiceContractTest.java`
- Create: `src/test/java/tong/statmod/progression/xp/XpNoticeCalculationTest.java`
- Create: `src/main/java/tong/statmod/progression/xp/XpNoticeCalculation.java`

**Interfaces:**
- Consumes: `XpAwardResult.accepted()`, pre-award snapshot, and post-award `PlayerStats`.
- Produces: one `Notice(stat, awardedXp, newLevel, levelsGained)` per changed stat.

- [ ] **Step 1: Write RED calculation tests**

```java
@Test void calculatesLevelGainAcrossXpWrap() {
    Map<StatType, StatValue> before = Map.of(StatType.AGILITY, new StatValue(4, 240));
    PlayerStats after = new PlayerStats();
    after.setLevel(StatType.AGILITY, 5);
    after.addXp(StatType.AGILITY, 15);
    var notices = XpNoticeCalculation.from(before, after, Map.of(StatType.AGILITY, 25));
    assertEquals(new XpNoticeCalculation.Notice(StatType.AGILITY, 25, 5, 1), notices.get(0));
}
```

Also test multiple stats preserve `StatType` order and unchanged/zero awards are omitted.

- [ ] **Step 2: Run test and verify RED**

Run: `./gradlew test --tests tong.statmod.progression.xp.XpNoticeCalculationTest`

Expected: compilation fails because the calculation type does not exist.

- [ ] **Step 3: Implement pure calculation and service emission**

Before `XpAwardCoordinator.apply`, take `Map<StatType, StatValue> before = stats.snapshot()`. After a changed result, calculate notices, refresh attributes when needed, send the authoritative snapshot, then call `StatNetwork.sendProgressNotice` for each notice. Administrative `/stat set` and raw snapshot sync remain notice-free.

- [ ] **Step 4: Run XP and notice tests**

Run: `./gradlew test --tests 'tong.statmod.progression.xp.*' --tests tong.statmod.network.StatProgressNoticeMessageTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/tong/statmod/progression/xp/XpAwardService.java src/main/java/tong/statmod/progression/xp/XpNoticeCalculation.java src/test/java/tong/statmod/progression/xp/XpAwardServiceContractTest.java src/test/java/tong/statmod/progression/xp/XpNoticeCalculationTest.java
git commit -m "feat: notify accepted stat progression"
```

---

### Task 8: Full Verification, Documentation, and `test-vrai` Deployment

**Files:**
- Modify: `README.md`
- Modify: `docs/compatibility/forge-1.20.1-supported-runtime.md`
- Modify: `scripts/verify-clean-foundation.ps1` only if the JAR manifest gains intentional required entries.

**Interfaces:**
- Consumes: completed screen, packet, notice queue, and exact ten-JAR `test-vrai` profile.
- Produces: verified build and deployed `statmod-0.1.0+1.20.1.jar`.

- [ ] **Step 1: Update user documentation**

Document `P`, six families, 23 read-only stats, progression notices, active/foundation labels, and the fact that magical integration is still deferred. Do not claim Iron's spell gameplay validation.

- [ ] **Step 2: Run complete automated verification**

Run:

```powershell
./gradlew clean test build --console=plain
powershell -ExecutionPolicy Bypass -File scripts/verify-clean-foundation.ps1 -Mode After
git diff --check
```

Expected: Gradle `BUILD SUCCESSFUL`; verifier reports `jars=74 manifest=74`; no diff whitespace errors.

- [ ] **Step 3: Run required-provider GameTest**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-gametest-server.ps1 `
  -ProviderModsDirectory 'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test-vrai\mods'
```

Expected: `OK required-provider Forge GameTest server smoke`.

- [ ] **Step 4: Deploy transactionally to `test-vrai`**

Back up the old STAT Mod JAR below `.minecraft/statmod-backups/test-vrai/<timestamp>-before-stats-ui`, copy only `build/libs/statmod-0.1.0+1.20.1.jar` into the existing ten-JAR profile, and compare SHA-256 source/destination. Do not add or remove any provider JAR.

- [ ] **Step 5: Perform client acceptance**

Launch `test-vrai`, create or enter a disposable world, press `P`, inspect all six families at two GUI scales, award XP through one normal action and one operator command, confirm only the normal action creates a notice, reconnect, and check the newest log. Accept the known two Iron's Spells loot-table parse errors; reject any crash, missing dependency, mixin failure, STAT Mod error, or ParCool/Patchouli guide error.

- [ ] **Step 6: Commit documentation**

```powershell
git add README.md docs/compatibility/forge-1.20.1-supported-runtime.md scripts/verify-clean-foundation.ps1
git commit -m "docs: validate native stats screen"
```

- [ ] **Step 7: Final status**

Run: `git status --short` and `git log -10 --oneline`.

Expected: no uncommitted project changes; the worktree remains on `forge-1.20.1`; `test-vrai/mods` contains exactly ten validated JARs.
