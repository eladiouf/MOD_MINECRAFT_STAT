# Forge 1.20.1 Player Statistics Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 23 persistent, server-authoritative player statistics with Forge capability storage, full client synchronization, and operator test commands.

**Architecture:** Plain Java domain types own the roster and progression invariants. A Forge player capability persists that model through NBT, lifecycle event handlers copy and synchronize it, and a versioned `SimpleChannel` sends immutable full snapshots to a client-only cache. Brigadier commands are the only mutation source in this slice.

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.10, Forge capabilities/events/networking, Brigadier, JUnit Jupiter 5.10.2, Gradle 8.8.

## Global Constraints

- Keep exactly 23 stable string stat identifiers grouped into exactly six families.
- Levels are inclusive from 0 through 100; level 100 always has zero XP.
- Use `requiredXp(level) = 10 * (level + 1)^2` below level 100.
- The server is the only authority; do not add a client-to-server mutation packet.
- Persist and transmit string identifiers, never enum ordinals or array indexes.
- Keep this slice independent from Iron's Spells, Epic Fight, Curios, Puffish Skills, Tensura, and every addon.
- Add no gameplay XP sources, stat bonuses, HUD, screen, key binding, fatigue, perks, or weapon mastery.
- Use translatable English and French command messages.

## File map

- `stats/StatFamily.java`: six stable stat families.
- `stats/StatType.java`: canonical 23-stat roster and ID lookup.
- `stats/StatValue.java`: immutable level/XP transport value.
- `stats/StatProgress.java`: validated mutable progression for one stat.
- `stats/PlayerStats.java`: aggregate, snapshots, copies, and NBT persistence.
- `capability/StatCapabilities.java`: capability token and registration.
- `capability/PlayerStatsProvider.java`: entity capability provider and invalidation.
- `network/StatsSnapshotMessage.java`: full snapshot wire format.
- `network/StatNetwork.java`: versioned Forge channel and server send helper.
- `client/ClientStatsCache.java`: client-side immutable snapshot cache.
- `event/PlayerStatsEvents.java`: attach, clone, login, respawn, dimension, and logout lifecycle.
- `command/StatsCommandRules.java`: testable permission and numeric policy.
- `command/StatsCommands.java`: Brigadier registration and feedback.

---

### Task 1: Define the canonical roster

**Files:**
- Create: `src/main/java/tong/statmod/stats/StatFamily.java`
- Create: `src/main/java/tong/statmod/stats/StatType.java`
- Create: `src/test/java/tong/statmod/stats/StatTypeTest.java`

**Interfaces:**
- Produces: `StatFamily.slug()`, `StatType.id()`, `StatType.family()`, and `StatType.fromId(String)`.
- Consumers: every later domain, persistence, packet, and command task.

- [ ] **Step 1: Write the failing roster test**

```java
package tong.statmod.stats;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class StatTypeTest {
    @Test
    void exposesTwentyThreeUniqueStableIdsInSixFamilies() {
        Set<String> ids = Arrays.stream(StatType.values())
                .map(StatType::id).collect(Collectors.toSet());
        Set<StatFamily> families = Arrays.stream(StatType.values())
                .map(StatType::family).collect(Collectors.toSet());
        assertEquals(23, StatType.values().length);
        assertEquals(23, ids.size());
        assertEquals(6, families.size());
    }

    @Test
    void resolvesIdsWithoutUsingOrdinals() {
        assertEquals(StatType.ARCANE_POWER, StatType.fromId("arcane_power").orElseThrow());
        assertTrue(StatType.fromId("ARCANE_POWER").isEmpty());
        assertTrue(StatType.fromId("removed_stat").isEmpty());
    }
}
```

- [ ] **Step 2: Run the test and observe RED**

Run: `.\gradlew.bat test --tests tong.statmod.stats.StatTypeTest --console=plain`  
Expected: compilation fails because `StatType` and `StatFamily` do not exist.

- [ ] **Step 3: Implement the six families and 23-stat enum**

```java
package tong.statmod.stats;

public enum StatFamily {
    FRONT_LINE_PHYSICAL("front_line_physical"),
    RANGED_HUNT("ranged_hunt"),
    MAGICAL_CORE("magical_core"),
    ELEMENTAL_SPECIALIZATION("elemental_specialization"),
    MENTAL_RESILIENCE("mental_resilience"),
    CRAFTING_SUPPORT("crafting_support");

    private final String slug;
    StatFamily(String slug) { this.slug = slug; }
    public String slug() { return slug; }
}
```

```java
package tong.statmod.stats;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum StatType {
    BRUTE_FORCE("brute_force", StatFamily.FRONT_LINE_PHYSICAL),
    BLADE_TECHNIQUE("blade_technique", StatFamily.FRONT_LINE_PHYSICAL),
    RAPIDITE("rapidite", StatFamily.FRONT_LINE_PHYSICAL),
    AGILITY("agility", StatFamily.FRONT_LINE_PHYSICAL),
    PHYSICAL_RESISTANCE("physical_resistance", StatFamily.FRONT_LINE_PHYSICAL),
    PHYSICAL_ENDURANCE("physical_endurance", StatFamily.FRONT_LINE_PHYSICAL),
    PRECISION("precision", StatFamily.RANGED_HUNT),
    TRACKING("tracking", StatFamily.RANGED_HUNT),
    KEEN_SENSES("keen_senses", StatFamily.RANGED_HUNT),
    ARCANE_POWER("arcane_power", StatFamily.MAGICAL_CORE),
    CASTING_SPEED("casting_speed", StatFamily.MAGICAL_CORE),
    MANA_POOL("mana_pool", StatFamily.MAGICAL_CORE),
    ERUDITION("erudition", StatFamily.MAGICAL_CORE),
    MAGIC_RESISTANCE("magic_resistance", StatFamily.MAGICAL_CORE),
    FIRE_AFFINITY("fire_affinity", StatFamily.ELEMENTAL_SPECIALIZATION),
    WATER_AFFINITY("water_affinity", StatFamily.ELEMENTAL_SPECIALIZATION),
    EARTH_AFFINITY("earth_affinity", StatFamily.ELEMENTAL_SPECIALIZATION),
    AIR_AFFINITY("air_affinity", StatFamily.ELEMENTAL_SPECIALIZATION),
    INTIMIDATION("intimidation", StatFamily.MENTAL_RESILIENCE),
    WILLPOWER("willpower", StatFamily.MENTAL_RESILIENCE),
    FORGING("forging", StatFamily.CRAFTING_SUPPORT),
    COOKING("cooking", StatFamily.CRAFTING_SUPPORT),
    ALCHEMY("alchemy", StatFamily.CRAFTING_SUPPORT);

    private static final Map<String, StatType> BY_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(StatType::id, Function.identity()));
    private final String id;
    private final StatFamily family;

    StatType(String id, StatFamily family) {
        this.id = id;
        this.family = family;
    }

    public String id() { return id; }
    public StatFamily family() { return family; }
    public static Optional<StatType> fromId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }
}
```

- [ ] **Step 4: Run the focused test and observe GREEN**

Run: `.\gradlew.bat test --tests tong.statmod.stats.StatTypeTest --console=plain`  
Expected: `BUILD SUCCESSFUL`, 2 tests pass.

- [ ] **Step 5: Commit the roster**

```powershell
git add src/main/java/tong/statmod/stats src/test/java/tong/statmod/stats/StatTypeTest.java
git commit -m "feat: define canonical player stat roster"
```

### Task 2: Implement progression, snapshots, and deep copies

**Files:**
- Create: `src/main/java/tong/statmod/stats/StatValue.java`
- Create: `src/main/java/tong/statmod/stats/StatProgress.java`
- Create: `src/main/java/tong/statmod/stats/PlayerStats.java`
- Create: `src/test/java/tong/statmod/stats/PlayerStatsTest.java`

**Interfaces:**
- Consumes: `StatType.values()` and stable IDs from Task 1.
- Produces: `StatValue(int level, int xp)`, `StatProgress.requiredXp(int)`, and `PlayerStats.get`, `setLevel`, `addXp`, `snapshot`, `copyFrom`.

- [ ] **Step 1: Write failing progression tests**

```java
package tong.statmod.stats;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PlayerStatsTest {
    @Test
    void startsWithAllStatsAtZero() {
        PlayerStats stats = new PlayerStats();
        assertEquals(23, stats.snapshot().size());
        assertEquals(new StatValue(0, 0), stats.get(StatType.BRUTE_FORCE));
    }

    @Test
    void usesQuadraticRequirementsAndHandlesMultipleLevels() {
        assertEquals(10, StatProgress.requiredXp(0));
        assertEquals(40, StatProgress.requiredXp(1));
        PlayerStats stats = new PlayerStats();
        stats.addXp(StatType.ARCANE_POWER, 55);
        assertEquals(new StatValue(2, 5), stats.get(StatType.ARCANE_POWER));
    }

    @Test
    void capsAtOneHundredAndClearsXp() {
        PlayerStats stats = new PlayerStats();
        stats.setLevel(StatType.WILLPOWER, 99);
        stats.addXp(StatType.WILLPOWER, Integer.MAX_VALUE);
        assertEquals(new StatValue(100, 0), stats.get(StatType.WILLPOWER));
    }

    @Test
    void snapshotsAndCopiesCannotMutateTheSource() {
        PlayerStats source = new PlayerStats();
        source.setLevel(StatType.COOKING, 12);
        PlayerStats copy = new PlayerStats();
        copy.copyFrom(source);
        source.setLevel(StatType.COOKING, 3);
        assertEquals(new StatValue(12, 0), copy.get(StatType.COOKING));
        assertThrows(UnsupportedOperationException.class,
                () -> copy.snapshot().put(StatType.ALCHEMY, new StatValue(1, 0)));
    }
}
```

- [ ] **Step 2: Run the test and observe RED**

Run: `.\gradlew.bat test --tests tong.statmod.stats.PlayerStatsTest --console=plain`  
Expected: compilation fails because the progression types do not exist.

- [ ] **Step 3: Implement the progression types**

```java
package tong.statmod.stats;

public record StatValue(int level, int xp) {}
```

```java
package tong.statmod.stats;

public final class StatProgress {
    static final int MAX_LEVEL = 100;
    private int level;
    private int xp;

    public static int requiredXp(int level) {
        if (level < 0 || level >= MAX_LEVEL) return 0;
        return 10 * (level + 1) * (level + 1);
    }

    StatValue value() { return new StatValue(level, xp); }

    void setLevel(int requestedLevel) {
        level = Math.max(0, Math.min(MAX_LEVEL, requestedLevel));
        xp = 0;
    }

    void load(int requestedLevel, int requestedXp) {
        level = Math.max(0, Math.min(MAX_LEVEL, requestedLevel));
        xp = 0;
        if (level < MAX_LEVEL) addXp(Math.max(0, requestedXp));
    }

    void addXp(long amount) {
        if (amount <= 0 || level == MAX_LEVEL) return;
        long remaining = (long) xp + amount;
        while (level < MAX_LEVEL) {
            int required = requiredXp(level);
            if (remaining < required) break;
            remaining -= required;
            level++;
        }
        xp = level == MAX_LEVEL ? 0 : (int) remaining;
    }

    void copyFrom(StatProgress source) {
        level = source.level;
        xp = source.xp;
    }
}
```

```java
package tong.statmod.stats;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class PlayerStats {
    private final EnumMap<StatType, StatProgress> values = new EnumMap<>(StatType.class);

    public PlayerStats() {
        for (StatType type : StatType.values()) values.put(type, new StatProgress());
    }

    public StatValue get(StatType type) { return values.get(type).value(); }
    public void setLevel(StatType type, int level) { values.get(type).setLevel(level); }
    public void addXp(StatType type, long amount) { values.get(type).addXp(amount); }

    public Map<StatType, StatValue> snapshot() {
        EnumMap<StatType, StatValue> copy = new EnumMap<>(StatType.class);
        values.forEach((type, progress) -> copy.put(type, progress.value()));
        return Collections.unmodifiableMap(copy);
    }

    public void copyFrom(PlayerStats source) {
        for (StatType type : StatType.values()) values.get(type).copyFrom(source.values.get(type));
    }

    void load(StatType type, int level, int xp) { values.get(type).load(level, xp); }
}
```

- [ ] **Step 4: Run both domain test classes and observe GREEN**

Run: `.\gradlew.bat test --tests 'tong.statmod.stats.*' --console=plain`  
Expected: `BUILD SUCCESSFUL`, all domain tests pass.

- [ ] **Step 5: Commit progression**

```powershell
git add src/main/java/tong/statmod/stats src/test/java/tong/statmod/stats/PlayerStatsTest.java
git commit -m "feat: add player stat progression model"
```

### Task 3: Add defensive NBT persistence and the player capability

**Files:**
- Modify: `src/main/java/tong/statmod/stats/PlayerStats.java`
- Create: `src/main/java/tong/statmod/capability/StatCapabilities.java`
- Create: `src/main/java/tong/statmod/capability/PlayerStatsProvider.java`
- Create: `src/test/java/tong/statmod/stats/PlayerStatsNbtTest.java`

**Interfaces:**
- Consumes: `PlayerStats.load(StatType,int,int)` and stable `StatType.id()` values.
- Produces: `PlayerStats.serializeNbt`, `deserializeNbt`, `StatCapabilities.PLAYER_STATS`, and a serializable provider.

- [ ] **Step 1: Write failing NBT tests**

```java
package tong.statmod.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class PlayerStatsNbtTest {
    @Test
    void roundTripsEveryKnownValue() {
        PlayerStats source = new PlayerStats();
        source.setLevel(StatType.FORGING, 15);
        source.addXp(StatType.FORGING, 37);
        PlayerStats loaded = new PlayerStats();
        loaded.deserializeNbt(source.serializeNbt());
        assertEquals(source.snapshot(), loaded.snapshot());
    }

    @Test
    void ignoresUnknownAndNormalizesInvalidEntries() {
        CompoundTag root = new CompoundTag();
        CompoundTag entries = new CompoundTag();
        CompoundTag known = new CompoundTag();
        known.putInt("level", -7);
        known.putInt("xp", -2);
        entries.put(StatType.AGILITY.id(), known);
        entries.putString("removed_stat", "bad-entry");
        root.put("stats", entries);
        PlayerStats loaded = new PlayerStats();
        loaded.deserializeNbt(root);
        assertEquals(new StatValue(0, 0), loaded.get(StatType.AGILITY));
        assertEquals(23, loaded.snapshot().size());
    }

    @Test
    void oversizedXpUsesNormalLevelUpRules() {
        CompoundTag root = new CompoundTag();
        CompoundTag entries = new CompoundTag();
        CompoundTag arcane = new CompoundTag();
        arcane.putInt("level", 0);
        arcane.putInt("xp", 55);
        entries.put(StatType.ARCANE_POWER.id(), arcane);
        root.put("stats", entries);
        PlayerStats loaded = new PlayerStats();
        loaded.deserializeNbt(root);
        assertEquals(new StatValue(2, 5), loaded.get(StatType.ARCANE_POWER));
    }
}
```

- [ ] **Step 2: Run the test and observe RED**

Run: `.\gradlew.bat test --tests tong.statmod.stats.PlayerStatsNbtTest --console=plain`  
Expected: compilation fails because the NBT methods do not exist.

- [ ] **Step 3: Add persistence to `PlayerStats`**

Add these methods and imports (`CompoundTag`, `Tag`) to `PlayerStats`:

```java
private static final String STATS_KEY = "stats";
private static final String LEVEL_KEY = "level";
private static final String XP_KEY = "xp";

public CompoundTag serializeNbt() {
    CompoundTag root = new CompoundTag();
    CompoundTag entries = new CompoundTag();
    for (StatType type : StatType.values()) {
        StatValue value = get(type);
        CompoundTag entry = new CompoundTag();
        entry.putInt(LEVEL_KEY, value.level());
        entry.putInt(XP_KEY, value.xp());
        entries.put(type.id(), entry);
    }
    root.put(STATS_KEY, entries);
    return root;
}

public void deserializeNbt(CompoundTag root) {
    if (!root.contains(STATS_KEY, Tag.TAG_COMPOUND)) return;
    CompoundTag entries = root.getCompound(STATS_KEY);
    for (StatType type : StatType.values()) {
        if (!entries.contains(type.id(), Tag.TAG_COMPOUND)) continue;
        CompoundTag entry = entries.getCompound(type.id());
        load(type, entry.getInt(LEVEL_KEY), entry.getInt(XP_KEY));
    }
}
```

- [ ] **Step 4: Add capability registration and provider**

```java
package tong.statmod.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import tong.statmod.stats.PlayerStats;

public final class StatCapabilities {
    public static final Capability<PlayerStats> PLAYER_STATS =
            CapabilityManager.get(new CapabilityToken<>() {});
    private StatCapabilities() {}
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(PlayerStats.class);
    }
}
```

```java
package tong.statmod.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tong.statmod.stats.PlayerStats;

public final class PlayerStatsProvider implements ICapabilitySerializable<CompoundTag> {
    private final PlayerStats stats = new PlayerStats();
    private final LazyOptional<PlayerStats> optional = LazyOptional.of(() -> stats);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull Capability<T> capability, @Nullable Direction side) {
        return capability == StatCapabilities.PLAYER_STATS ? optional.cast() : LazyOptional.empty();
    }

    @Override public CompoundTag serializeNBT() { return stats.serializeNbt(); }
    @Override public void deserializeNBT(CompoundTag tag) { stats.deserializeNbt(tag); }
    public void invalidate() { optional.invalidate(); }
}
```

- [ ] **Step 5: Run persistence tests and compile Forge code**

Run: `.\gradlew.bat test --tests tong.statmod.stats.PlayerStatsNbtTest --console=plain`  
Expected: `BUILD SUCCESSFUL`, 3 NBT tests pass.

Run: `.\gradlew.bat compileJava --console=plain`  
Expected: `BUILD SUCCESSFUL` with Forge 1.20.1 capability APIs resolved.

- [ ] **Step 6: Commit persistence and capability**

```powershell
git add src/main/java/tong/statmod/stats/PlayerStats.java src/main/java/tong/statmod/capability src/test/java/tong/statmod/stats/PlayerStatsNbtTest.java
git commit -m "feat: persist player stats in a Forge capability"
```

### Task 4: Add full snapshot networking and the client cache

**Files:**
- Create: `src/main/java/tong/statmod/network/StatsSnapshotMessage.java`
- Create: `src/main/java/tong/statmod/network/StatNetwork.java`
- Create: `src/main/java/tong/statmod/client/ClientStatsCache.java`
- Create: `src/test/java/tong/statmod/network/StatsSnapshotMessageTest.java`

**Interfaces:**
- Consumes: immutable `Map<StatType,StatValue>` snapshots.
- Produces: `StatsSnapshotMessage.from(PlayerStats)`, `encode`, `decode`, `values`, `StatNetwork.register`, `sendSnapshot`, and `ClientStatsCache.replace/clear/snapshot`.

- [ ] **Step 1: Write the failing packet round-trip test**

```java
package tong.statmod.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

class StatsSnapshotMessageTest {
    @Test
    void roundTripsByStableStringId() {
        PlayerStats stats = new PlayerStats();
        stats.setLevel(StatType.FIRE_AFFINITY, 17);
        stats.addXp(StatType.FIRE_AFFINITY, 30);
        StatsSnapshotMessage original = StatsSnapshotMessage.from(stats);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        StatsSnapshotMessage.encode(original, buffer);
        assertEquals(original.values(), StatsSnapshotMessage.decode(buffer).values());
    }
}
```

- [ ] **Step 2: Run the test and observe RED**

Run: `.\gradlew.bat test --tests tong.statmod.network.StatsSnapshotMessageTest --console=plain`  
Expected: compilation fails because `StatsSnapshotMessage` does not exist.

- [ ] **Step 3: Implement the immutable snapshot message**

```java
package tong.statmod.network;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

public record StatsSnapshotMessage(Map<StatType, StatValue> values) {
    public StatsSnapshotMessage {
        values = Collections.unmodifiableMap(new EnumMap<>(values));
    }

    public static StatsSnapshotMessage from(PlayerStats stats) {
        return new StatsSnapshotMessage(stats.snapshot());
    }

    public static void encode(StatsSnapshotMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.values.size());
        message.values.forEach((type, value) -> {
            buffer.writeUtf(type.id());
            buffer.writeVarInt(value.level());
            buffer.writeVarInt(value.xp());
        });
    }

    public static StatsSnapshotMessage decode(FriendlyByteBuf buffer) {
        EnumMap<StatType, StatValue> values = new EnumMap<>(StatType.class);
        int count = Math.min(buffer.readVarInt(), StatType.values().length);
        for (int index = 0; index < count; index++) {
            String id = buffer.readUtf(64);
            int level = buffer.readVarInt();
            int xp = buffer.readVarInt();
            StatType.fromId(id).ifPresent(type -> values.put(type, new StatValue(level, xp)));
        }
        for (StatType type : StatType.values()) values.putIfAbsent(type, new StatValue(0, 0));
        return new StatsSnapshotMessage(values);
    }
}
```

- [ ] **Step 4: Implement the cache and versioned channel**

```java
package tong.statmod.client;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

public final class ClientStatsCache {
    private static Map<StatType, StatValue> values = emptySnapshot();
    private ClientStatsCache() {}
    public static void replace(Map<StatType, StatValue> next) {
        values = Collections.unmodifiableMap(new EnumMap<>(next));
    }
    public static void clear() { values = emptySnapshot(); }
    public static Map<StatType, StatValue> snapshot() { return values; }
    private static Map<StatType, StatValue> emptySnapshot() {
        EnumMap<StatType, StatValue> empty = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) empty.put(type, new StatValue(0, 0));
        return Collections.unmodifiableMap(empty);
    }
}
```

```java
package tong.statmod.network;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import tong.statmod.StatMod;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.client.ClientStatsCache;

public final class StatNetwork {
    private static final String PROTOCOL = "1";
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(StatMod.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();
    private StatNetwork() {}

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        CHANNEL.registerMessage(0, StatsSnapshotMessage.class,
                StatsSnapshotMessage::encode, StatsSnapshotMessage::decode,
                (message, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientStatsCache.replace(message.values())));
                    context.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendSnapshot(ServerPlayer player) {
        player.getCapability(StatCapabilities.PLAYER_STATS).ifPresent(stats ->
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        StatsSnapshotMessage.from(stats)));
    }
}
```

- [ ] **Step 5: Run packet tests and compile the channel**

Run: `.\gradlew.bat test --tests tong.statmod.network.StatsSnapshotMessageTest --console=plain`  
Expected: `BUILD SUCCESSFUL`, packet round trip passes.

Run: `.\gradlew.bat compileJava --console=plain`  
Expected: `BUILD SUCCESSFUL` with Forge networking APIs resolved.

- [ ] **Step 6: Commit networking**

```powershell
git add src/main/java/tong/statmod/network src/main/java/tong/statmod/client src/test/java/tong/statmod/network
git commit -m "feat: synchronize complete player stat snapshots"
```

### Task 5: Wire Forge player lifecycle events

**Files:**
- Modify: `src/main/java/tong/statmod/StatMod.java`
- Create: `src/main/java/tong/statmod/event/PlayerStatsEvents.java`
- Create: `src/main/java/tong/statmod/client/ClientStatsEvents.java`
- Create: `src/test/java/tong/statmod/event/PlayerStatsWiringTest.java`

**Interfaces:**
- Consumes: provider, capability token, `PlayerStats.copyFrom`, and `StatNetwork.sendSnapshot`.
- Produces: all attachment, clone, synchronization, and cache-clear event registrations.

- [ ] **Step 1: Write the failing wiring test**

```java
package tong.statmod.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

class PlayerStatsWiringTest {
    @Test
    void cloneOperationCreatesAnIndependentExactCopy() {
        PlayerStats original = new PlayerStats();
        original.setLevel(StatType.PHYSICAL_ENDURANCE, 22);
        PlayerStats clone = new PlayerStats();
        PlayerStatsEvents.copyStats(original, clone);
        original.setLevel(StatType.PHYSICAL_ENDURANCE, 2);
        assertEquals(new StatValue(22, 0), clone.get(StatType.PHYSICAL_ENDURANCE));
    }
}
```

- [ ] **Step 2: Run the test and observe RED**

Run: `.\gradlew.bat test --tests tong.statmod.event.PlayerStatsWiringTest --console=plain`  
Expected: compilation fails because `PlayerStatsEvents` does not exist.

- [ ] **Step 3: Implement common lifecycle events**

```java
package tong.statmod.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.network.StatNetwork;
import tong.statmod.stats.PlayerStats;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class PlayerStatsEvents {
    private static final ResourceLocation CAPABILITY_ID =
            new ResourceLocation(StatMod.MOD_ID, "player_stats");
    private PlayerStatsEvents() {}

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerStatsProvider provider = new PlayerStatsProvider();
            event.addCapability(CAPABILITY_ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(StatCapabilities.PLAYER_STATS).ifPresent(source ->
                event.getEntity().getCapability(StatCapabilities.PLAYER_STATS).ifPresent(target ->
                        copyStats(source, target)));
        event.getOriginal().invalidateCaps();
    }

    public static void copyStats(PlayerStats source, PlayerStats target) { target.copyFrom(source); }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { sync(event.getEntity()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { sync(event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { sync(event.getEntity()); }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) StatNetwork.sendSnapshot(serverPlayer);
    }

    @Mod.EventBusSubscriber(modid = StatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {}
        @SubscribeEvent public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            StatCapabilities.register(event);
        }
    }
}
```

- [ ] **Step 4: Clear the client cache on logout and initialize networking**

```java
package tong.statmod.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID, value = Dist.CLIENT)
public final class ClientStatsEvents {
    private ClientStatsEvents() {}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientStatsCache.clear();
    }
}
```

Add the network initialization to `StatMod`:

```java
public StatMod() {
    StatNetwork.register();
    LOGGER.info("Initializing {} for Forge 1.20.1", MOD_NAME);
}
```

- [ ] **Step 5: Run lifecycle tests and compile event APIs**

Run: `.\gradlew.bat test --tests tong.statmod.event.PlayerStatsWiringTest --console=plain`  
Expected: `BUILD SUCCESSFUL`, copy test passes.

Run: `.\gradlew.bat compileJava --console=plain`  
Expected: `BUILD SUCCESSFUL` with every Forge event type resolved.

- [ ] **Step 6: Commit lifecycle wiring**

```powershell
git add src/main/java/tong/statmod/StatMod.java src/main/java/tong/statmod/event src/main/java/tong/statmod/client/ClientStatsEvents.java src/test/java/tong/statmod/event
git commit -m "feat: preserve stats through player lifecycle events"
```

### Task 6: Add administrative inspection and mutation commands

**Files:**
- Create: `src/main/java/tong/statmod/command/StatsCommandRules.java`
- Create: `src/main/java/tong/statmod/command/StatsCommands.java`
- Modify: `src/main/java/tong/statmod/event/PlayerStatsEvents.java`
- Modify: `src/main/resources/assets/statmod/lang/en_us.json`
- Modify: `src/main/resources/assets/statmod/lang/fr_fr.json`
- Create: `src/test/java/tong/statmod/command/StatsCommandRulesTest.java`

**Interfaces:**
- Consumes: capability mutation methods and `StatNetwork.sendSnapshot`.
- Produces: `/statmod stats`, `/statmod stat get`, `set`, and `addxp`.

- [ ] **Step 1: Write failing command policy tests**

```java
package tong.statmod.command;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StatsCommandRulesTest {
    @Test void fixesPermissionAndInputBounds() {
        assertEquals(2, StatsCommandRules.ADMIN_PERMISSION);
        assertTrue(StatsCommandRules.validLevel(0));
        assertTrue(StatsCommandRules.validLevel(100));
        assertFalse(StatsCommandRules.validLevel(-1));
        assertFalse(StatsCommandRules.validLevel(101));
        assertTrue(StatsCommandRules.validXpAmount(1));
        assertFalse(StatsCommandRules.validXpAmount(0));
    }
}
```

- [ ] **Step 2: Run the test and observe RED**

Run: `.\gradlew.bat test --tests tong.statmod.command.StatsCommandRulesTest --console=plain`  
Expected: compilation fails because `StatsCommandRules` does not exist.

- [ ] **Step 3: Implement command rules and Brigadier tree**

```java
package tong.statmod.command;

public final class StatsCommandRules {
    public static final int ADMIN_PERMISSION = 2;
    private StatsCommandRules() {}
    public static boolean validLevel(int level) { return level >= 0 && level <= 100; }
    public static boolean validXpAmount(int amount) { return amount > 0; }
}
```

Implement `StatsCommands` with these exact Brigadier branches and handlers:

```java
package tong.statmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.network.StatNetwork;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatProgress;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

public final class StatsCommands {
    private static final DynamicCommandExceptionType UNKNOWN_STAT =
            new DynamicCommandExceptionType(id -> Component.translatable(
                    "command.statmod.unknown_stat", id));
    private StatsCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("statmod")
                .then(Commands.literal("stats")
                        .executes(context -> showSelf(context.getSource()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> source.hasPermission(StatsCommandRules.ADMIN_PERMISSION))
                                .executes(context -> show(context.getSource(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("stat")
                        .then(Commands.literal("get")
                                .requires(source -> source.hasPermission(StatsCommandRules.ADMIN_PERMISSION))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(statArgument().executes(StatsCommands::get))))
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(StatsCommandRules.ADMIN_PERMISSION))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(statArgument().then(Commands.argument("level",
                                                IntegerArgumentType.integer(0, 100))
                                                .executes(StatsCommands::set)))))
                        .then(Commands.literal("addxp")
                                .requires(source -> source.hasPermission(StatsCommandRules.ADMIN_PERMISSION))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(statArgument().then(Commands.argument("amount",
                                                IntegerArgumentType.integer(1))
                                                .executes(StatsCommands::addXp))))))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> statArgument() {
        return Commands.argument("stat", StringArgumentType.word())
                .suggests((context, builder) -> {
                    Arrays.stream(StatType.values()).map(StatType::id).forEach(builder::suggest);
                    return builder.buildFuture();
                });
    }

    private static StatType stat(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "stat");
        return StatType.fromId(id).orElseThrow(() -> UNKNOWN_STAT.create(id));
    }

    private static int showSelf(CommandSourceStack source) throws CommandSyntaxException {
        return show(source, source.getPlayerOrException());
    }

    private static int show(CommandSourceStack source, ServerPlayer target) {
        var optional = target.getCapability(StatCapabilities.PLAYER_STATS).resolve();
        if (optional.isEmpty()) return missing(source, target);
        PlayerStats stats = optional.get();
        source.sendSuccess(() -> Component.translatable(
                "command.statmod.stats.header", target.getDisplayName()), false);
        for (StatType type : StatType.values()) {
            StatValue value = stats.get(type);
            source.sendSuccess(() -> Component.translatable("command.statmod.stats.entry",
                    type.id(), value.level(), value.xp(), required(value)), false);
        }
        return StatType.values().length;
    }

    private static int get(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        StatType type = stat(context);
        var optional = target.getCapability(StatCapabilities.PLAYER_STATS).resolve();
        if (optional.isEmpty()) return missing(source, target);
        StatValue value = optional.get().get(type);
        source.sendSuccess(() -> Component.translatable("command.statmod.stat.value",
                target.getDisplayName(), type.id(), value.level(), value.xp(), required(value)), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        StatType type = stat(context);
        int level = IntegerArgumentType.getInteger(context, "level");
        return mutate(context.getSource(), target, type, stats -> stats.setLevel(type, level));
    }

    private static int addXp(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        StatType type = stat(context);
        int amount = IntegerArgumentType.getInteger(context, "amount");
        return mutate(context.getSource(), target, type, stats -> stats.addXp(type, amount));
    }

    private static int mutate(CommandSourceStack source, ServerPlayer target, StatType type,
            java.util.function.Consumer<PlayerStats> mutation) {
        var optional = target.getCapability(StatCapabilities.PLAYER_STATS).resolve();
        if (optional.isEmpty()) return missing(source, target);
        PlayerStats stats = optional.get();
        mutation.accept(stats);
        StatNetwork.sendSnapshot(target);
        StatValue value = stats.get(type);
        source.sendSuccess(() -> Component.translatable("command.statmod.stat.updated",
                type.id(), target.getDisplayName(), value.level(), value.xp(), required(value)), true);
        return 1;
    }

    private static int required(StatValue value) {
        return StatProgress.requiredXp(value.level());
    }

    private static int missing(CommandSourceStack source, ServerPlayer target) {
        source.sendFailure(Component.translatable(
                "command.statmod.capability_missing", target.getDisplayName()));
        return 0;
    }
}
```

- [ ] **Step 4: Register commands and add complete translations**

Add to `PlayerStatsEvents`:

```java
@SubscribeEvent
public static void commands(RegisterCommandsEvent event) {
    StatsCommands.register(event.getDispatcher());
}
```

Merge these keys into both language files, translating the French values:

```json
{
  "command.statmod.unknown_stat": "Unknown stat: %s",
  "command.statmod.capability_missing": "Statistics are unavailable for %s",
  "command.statmod.stats.header": "Statistics for %s",
  "command.statmod.stats.entry": "%s: level %s, %s/%s XP",
  "command.statmod.stat.value": "%s — %s: level %s, %s/%s XP",
  "command.statmod.stat.updated": "Updated %s for %s: level %s, %s/%s XP"
}
```

French values:

```json
{
  "command.statmod.unknown_stat": "Statistique inconnue : %s",
  "command.statmod.capability_missing": "Les statistiques sont indisponibles pour %s",
  "command.statmod.stats.header": "Statistiques de %s",
  "command.statmod.stats.entry": "%s : niveau %s, %s/%s XP",
  "command.statmod.stat.value": "%s — %s : niveau %s, %s/%s XP",
  "command.statmod.stat.updated": "%s modifiée pour %s : niveau %s, %s/%s XP"
}
```

- [ ] **Step 5: Run command tests and compile registration**

Run: `.\gradlew.bat test --tests tong.statmod.command.StatsCommandRulesTest --console=plain`  
Expected: `BUILD SUCCESSFUL`, command policy tests pass.

Run: `.\gradlew.bat compileJava --console=plain`  
Expected: `BUILD SUCCESSFUL`; Brigadier and Forge command event APIs compile.

- [ ] **Step 6: Commit commands**

```powershell
git add src/main/java/tong/statmod/command src/main/java/tong/statmod/event/PlayerStatsEvents.java src/main/resources/assets/statmod/lang src/test/java/tong/statmod/command
git commit -m "feat: add player stat administration commands"
```

### Task 7: Add source-contract checks and complete verification

**Files:**
- Create: `src/test/java/tong/statmod/StatsFoundationContractTest.java`
- Modify: `README.md`
- Modify: `scripts/verify-clean-foundation.ps1`

**Interfaces:**
- Consumes: all preceding tasks.
- Produces: regression checks that guard registrations, independence, build output, and user documentation.

- [ ] **Step 1: Write the failing source-contract test**

```java
package tong.statmod;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StatsFoundationContractTest {
    @Test
    void entrypointRegistersNetworkAndLifecycleOwnsRequiredEvents() throws Exception {
        String entrypoint = Files.readString(Path.of("src/main/java/tong/statmod/StatMod.java"));
        String events = Files.readString(Path.of("src/main/java/tong/statmod/event/PlayerStatsEvents.java"));
        assertTrue(entrypoint.contains("StatNetwork.register()"));
        assertTrue(events.contains("PlayerLoggedInEvent"));
        assertTrue(events.contains("PlayerRespawnEvent"));
        assertTrue(events.contains("PlayerChangedDimensionEvent"));
        assertTrue(events.contains("RegisterCommandsEvent"));
    }

    @Test
    void activeSourceHasNoThirdPartyIntegrationImports() throws Exception {
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            String sources = paths.filter(path -> path.toString().endsWith(".java"))
                    .map(path -> {
                        try { return Files.readString(path); }
                        catch (Exception exception) { throw new RuntimeException(exception); }
                    }).reduce("", String::concat);
            assertFalse(sources.contains("irons_spellbooks"));
            assertFalse(sources.contains("epicfight"));
            assertFalse(sources.toLowerCase().contains("tensura"));
        }
    }
}
```

- [ ] **Step 2: Run the contract test**

Run: `.\gradlew.bat test --tests tong.statmod.StatsFoundationContractTest --console=plain`  
Expected: PASS if Tasks 1-6 are complete; otherwise RED identifies missing wiring.

- [ ] **Step 3: Document the functional foundation**

Add a `Player statistics foundation` section to `README.md` listing the 23-stat,
six-family model, the XP formula, the five command forms, permission level 2,
server authority, persistence lifecycle, and the explicit absence of automatic
XP, bonuses, HUD, and third-party integrations in this slice.

Extend `scripts/verify-clean-foundation.ps1 -Mode After` to require these JAR
entries in addition to the existing entrypoint and metadata checks:

```powershell
'tong/statmod/stats/PlayerStats.class',
'tong/statmod/capability/PlayerStatsProvider.class',
'tong/statmod/network/StatsSnapshotMessage.class',
'tong/statmod/command/StatsCommands.class'
```

- [ ] **Step 4: Run the complete fresh verification suite**

Run:

```powershell
.\gradlew.bat clean build --console=plain
& '.\scripts\verify-clean-foundation.ps1' -Mode After
git diff --check
git status --short
```

Expected:

- Gradle reports `BUILD SUCCESSFUL` and zero failed tests.
- The guard reports `OK mode=After branch=forge-1.20.1 jars=74 manifest=74`.
- `git diff --check` prints no errors.
- `git status --short` lists only the Task 7 README, guard, and test changes.

- [ ] **Step 5: Commit final documentation and guards**

```powershell
git add README.md scripts/verify-clean-foundation.ps1 src/test/java/tong/statmod/StatsFoundationContractTest.java
git commit -m "test: verify player stats foundation"
```

- [ ] **Step 6: Re-run verification from the committed tree**

Run:

```powershell
.\gradlew.bat clean build --console=plain
& '.\scripts\verify-clean-foundation.ps1' -Mode After
git status --short
```

Expected: build and guard succeed; status is empty.

## Self-review

- Spec coverage: Tasks 1-2 cover the roster and progression; Task 3 covers
  defensive persistence; Task 4 covers server-to-client full snapshots and the
  cache; Task 5 covers capability lifecycle synchronization; Task 6 covers all
  five command forms, permissions, validation, and translations; Task 7 covers
  independence, documentation, artifact contents, and fresh verification.
- Placeholder scan: the plan contains no unfinished requirement, deferred code
  marker, or reference to an undefined task.
- Type consistency: all later tasks consume the exact `StatType`, `StatValue`,
  `PlayerStats`, capability, snapshot, network, and cache names introduced by
  earlier tasks. Stable IDs are used in NBT, packets, and commands.
