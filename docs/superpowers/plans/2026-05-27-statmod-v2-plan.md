# STAT Mod v2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a Minecraft Forge mod adding 23 stats across 5 categories with Epic Fight skill unlocks, fatigue, and weapon mastery.

**Architecture:** Three-layer design — stat definition (enum + registry), progression storage (Forge capability), effects (LivingHurtEvent modifiers + Epic Fight skill unlocks). Clean separation: one capability per system (Stats, Fatigue, Weapon Mastery), event-driven XP gain, dirty-flag networking.

**Tech Stack:** Minecraft Forge 1.20.1 (47.4.20), Parchment mappings, Epic Fight 20.9.5, Gradle 8.8, Java 17

---

## File Inventory (all new files)

### Phase 1: Foundation (capability + network)
- Creates: `src/main/java/tong/statmod/stats/StatType.java`, `StatRegistry.java`
- Creates: `src/main/java/tong/statmod/capability/PlayerStats.java`, `PlayerStatsProvider.java`
- Creates: `src/main/java/tong/statmod/network/NetworkHandler.java`, `SyncAllStatsPacket.java`, `StatUpdatePacket.java`
- Modifies: `src/main/java/tong/statmod/StatMod.java`

### Phase 2: Core Progression (XP + effects)
- Creates: `src/main/java/tong/statmod/progression/ActionType.java`, `CombatXPHandler.java`, `NonCombatXPHandler.java`
- Creates: `src/main/java/tong/statmod/stats/StatCalculator.java`, `StatEffectApplier.java`

### Phase 3: Fatigue
- Creates: `src/main/java/tong/statmod/fatigue/FatigueManager.java`, `FatigueHandler.java`, `FatigueEffects.java`
- Creates: `src/main/java/tong/statmod/network/FatiguePacket.java`

### Phase 4: Weapon Mastery
- Creates: `src/main/java/tong/statmod/weapon/WeaponType.java`, `WeaponMasteryManager.java`, `WeaponXPHandler.java`

### Phase 5: Skill Unlocks + EF Integration
- Creates: `src/main/java/tong/statmod/integration/EpicFightCompat.java`
- Creates: `src/main/java/tong/statmod/skills/SkillUnlockRegistry.java`, `SkillUnlockHandler.java`
- Creates: `src/main/java/tong/statmod/skills/skills/EarthSplitterSkill.java`, `IaijutsuSkill.java`, `ShadowStepSkill.java`, `IronWallSkill.java`, `HawkEyeSkill.java`

### Phase 6: Client
- Creates: `src/main/java/tong/statmod/client/ClientSetup.java`, `ClientStatsCache.java`
- Creates: `src/main/java/tong/statmod/client/hud/HUDManager.java`, `ComboOverlay.java`, `FatigueOverlay.java`
- Creates: `src/main/java/tong/statmod/client/gui/CharacterScreen.java`, `StatWidget.java`
- Creates: `src/main/java/tong/statmod/client/notification/LevelUpToast.java`

### Phase 7: Config & Commands
- Modifies: `src/main/java/tong/statmod/Config.java`, `ConfigClient.java`
- Creates: `src/main/java/tong/statmod/command/StatsCommands.java`

### Phase 8: Polish
- Creates: `src/main/resources/assets/statmod/lang/en_us.json`, `fr_fr.json`
- Creates: `src/main/resources/data/statmod/advancements/` (3 JSONs)

---

## Phase 1: Foundation

### Task 1.1: Create StatType Enum

**Files:**
- Create: `src/main/java/tong/statmod/stats/StatType.java`

- [ ] **Step 1: Create the enum class**

```java
package tong.statmod.stats;

public enum StatType {
    // Combat (7)
    BRUTE_FORCE(0, StatCategory.COMBAT, "Brute Force", 0, 100),
    BLADE_TECHNIQUE(1, StatCategory.COMBAT, "Blade Technique", 0, 100),
    RAPIDITE(2, StatCategory.COMBAT, "Rapidité", 0, 100),
    AGILITY(3, StatCategory.COMBAT, "Agility", 0, 100),
    PHYSICAL_RESISTANCE(4, StatCategory.COMBAT, "Physical Resistance", 0, 100),
    PHYSICAL_ENDURANCE(5, StatCategory.COMBAT, "Physical Endurance", 0, 100),
    PRECISION(6, StatCategory.COMBAT, "Precision", 0, 100),
    // Magic (9)
    ARCANE_POWER(7, StatCategory.MAGIC, "Arcane Power", 0, 100),
    WATER_AFFINITY(8, StatCategory.MAGIC, "Water Affinity", 0, 100),
    EARTH_AFFINITY(9, StatCategory.MAGIC, "Earth Affinity", 0, 100),
    FIRE_AFFINITY(10, StatCategory.MAGIC, "Fire Affinity", 0, 100),
    AIR_AFFINITY(11, StatCategory.MAGIC, "Air Affinity", 0, 100),
    MAGIC_RESISTANCE(12, StatCategory.MAGIC, "Magic Resistance", 0, 100),
    CASTING_SPEED(13, StatCategory.MAGIC, "Casting Speed", 0, 100),
    MANA_POOL(14, StatCategory.MAGIC, "Mana Pool", 0, 100),
    ERUDITION(15, StatCategory.MAGIC, "Erudition", 0, 100),
    // Survival (2)
    TRACKING(16, StatCategory.SURVIVAL, "Tracking", 0, 100),
    KEEN_SENSES(17, StatCategory.SURVIVAL, "Keen Senses", 0, 100),
    // Crafting (3)
    FORGING(18, StatCategory.CRAFTING, "Forging", 0, 100),
    COOKING(19, StatCategory.CRAFTING, "Cooking", 0, 100),
    ALCHEMY(20, StatCategory.CRAFTING, "Alchemy", 0, 100),
    // Mental (2)
    INTIMIDATION(21, StatCategory.MENTAL, "Intimidation", 0, 100),
    WILLPOWER(22, StatCategory.MENTAL, "Willpower", 0, 100);

    public final int index;
    public final StatCategory category;
    public final String displayName;
    public final int minLevel;
    public final int maxLevel;

    StatType(int index, StatCategory category, String displayName, int minLevel, int maxLevel) {
        this.index = index;
        this.category = category;
        this.displayName = displayName;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    public static StatType byIndex(int index) {
        for (StatType s : values()) {
            if (s.index == index) return s;
        }
        return BRUTE_FORCE;
    }
}
```

- [ ] **Step 2: Create StatCategory enum**

```java
package tong.statmod.stats;

public enum StatCategory {
    COMBAT("combat"),
    MAGIC("magic"),
    SURVIVAL("survival"),
    CRAFTING("crafting"),
    MENTAL("mental");

    public final String key;

    StatCategory(String key) {
        this.key = key;
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd STAT_MOD; .\gradlew.bat build 2>&1 | Select-String "error"`
Expected: No errors related to StatType or StatCategory

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/stats/
git commit -m "feat: add StatType enum with 23 stats across 5 categories"
```

### Task 1.2: Create StatRegistry (EF Integration)

**Files:**
- Create: `src/main/java/tong/statmod/stats/StatRegistry.java`
- Modify: `src/main/java/tong/statmod/StatMod.java`

- [ ] **Step 1: Create StatRegistry**

```java
package tong.statmod.stats;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import tong.statmod.StatMod;
import yesman.epicfight.api.utils.math.ValueCorrector;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.entity.ai.attribute.AttributeStat;

public class StatRegistry {
    public static final DeferredRegister<AttributeStat> STATS = DeferredRegister.create(
            new ResourceLocation(EpicFightMod.MODID, "attributes"), StatMod.MODID);

    public static final RegistryObject<AttributeStat> BRUTE_FORCE = register(StatType.BRUTE_FORCE);
    public static final RegistryObject<AttributeStat> BLADE_TECHNIQUE = register(StatType.BLADE_TECHNIQUE);
    public static final RegistryObject<AttributeStat> RAPIDITE = register(StatType.RAPIDITE);
    public static final RegistryObject<AttributeStat> AGILITY = register(StatType.AGILITY);
    public static final RegistryObject<AttributeStat> PHYSICAL_RESISTANCE = register(StatType.PHYSICAL_RESISTANCE);
    public static final RegistryObject<AttributeStat> PHYSICAL_ENDURANCE = register(StatType.PHYSICAL_ENDURANCE);
    public static final RegistryObject<AttributeStat> PRECISION = register(StatType.PRECISION);
    public static final RegistryObject<AttributeStat> ARCANE_POWER = register(StatType.ARCANE_POWER);
    public static final RegistryObject<AttributeStat> WATER_AFFINITY = register(StatType.WATER_AFFINITY);
    public static final RegistryObject<AttributeStat> EARTH_AFFINITY = register(StatType.EARTH_AFFINITY);
    public static final RegistryObject<AttributeStat> FIRE_AFFINITY = register(StatType.FIRE_AFFINITY);
    public static final RegistryObject<AttributeStat> AIR_AFFINITY = register(StatType.AIR_AFFINITY);
    public static final RegistryObject<AttributeStat> MAGIC_RESISTANCE = register(StatType.MAGIC_RESISTANCE);
    public static final RegistryObject<AttributeStat> CASTING_SPEED = register(StatType.CASTING_SPEED);
    public static final RegistryObject<AttributeStat> MANA_POOL = register(StatType.MANA_POOL);
    public static final RegistryObject<AttributeStat> ERUDITION = register(StatType.ERUDITION);
    public static final RegistryObject<AttributeStat> TRACKING = register(StatType.TRACKING);
    public static final RegistryObject<AttributeStat> KEEN_SENSES = register(StatType.KEEN_SENSES);
    public static final RegistryObject<AttributeStat> FORGING = register(StatType.FORGING);
    public static final RegistryObject<AttributeStat> COOKING = register(StatType.COOKING);
    public static final RegistryObject<AttributeStat> ALCHEMY = register(StatType.ALCHEMY);
    public static final RegistryObject<AttributeStat> INTIMIDATION = register(StatType.INTIMIDATION);
    public static final RegistryObject<AttributeStat> WILLPOWER = register(StatType.WILLPOWER);

    private static RegistryObject<AttributeStat> register(StatType type) {
        return STATS.register(type.name().toLowerCase(), () -> new AttributeStat(
                new ResourceLocation(StatMod.MODID, type.name().toLowerCase()),
                0.0, type.minLevel, type.maxLevel, null));
    }

    public static void register(IEventBus modEventBus) {
        STATS.register(modEventBus);
        StatMod.LOGGER.info("Registered {} AttributeStats for Epic Fight", StatType.values().length);
    }
}
```

- [ ] **Step 2: Update StatMod.java to call StatRegistry**

Read existing `StatMod.java` first:

```bash
cat src/main/java/tong/statmod/StatMod.java
```

Add the registration call inside the constructor, before `MinecraftForge.EVENT_BUS.register(this)`:

```java
ModStats.register(modEventBus);
```

Wait — we renamed to StatRegistry. The actual line should be:

```java
StatRegistry.register(modEventBus);
```

Full modified constructor should look like:

```java
public StatMod() {
    IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
    StatRegistry.register(modEventBus);
    MinecraftForge.EVENT_BUS.register(this);
}
```

- [ ] **Step 3: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/stats/StatRegistry.java src/main/java/tong/statmod/StatMod.java
git commit -m "feat: add StatRegistry with 23 AttributeStats for Epic Fight"
```

### Task 1.3: Create PlayerStats Capability

**Files:**
- Create: `src/main/java/tong/statmod/capability/PlayerStats.java`
- Create: `src/main/java/tong/statmod/capability/PlayerStatsProvider.java`

- [ ] **Step 1: Create PlayerStats (storage)**

```java
package tong.statmod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import tong.statmod.StatMod;

public class PlayerStats implements INBTSerializable<CompoundTag> {
    public static final int STAT_COUNT = 23;
    private final int[] levels = new int[STAT_COUNT];
    private final int[] xp = new int[STAT_COUNT];

    public int getLevel(int index) { return levels[index]; }
    public int getXp(int index) { return xp[index]; }

    public void addXp(int index, int amount) {
        if (index < 0 || index >= STAT_COUNT) return;
        this.xp[index] += amount;
        while (this.xp[index] >= getXpForNextLevel(levels[index]) && levels[index] < 100) {
            levels[index]++;
            this.xp[index] -= getXpForNextLevel(levels[index]);
            StatMod.LOGGER.debug("Level up! Stat {} → level {}", index, levels[index]);
        }
    }

    public static int getXpForNextLevel(int level) {
        return (level + 1) * (level + 1) * 10;
    }

    public void setLevel(int index, int level) { this.levels[index] = level; }
    public void setXp(int index, int xp) { this.xp[index] = xp; }

    public void copyFrom(PlayerStats source) {
        System.arraycopy(source.levels, 0, this.levels, 0, STAT_COUNT);
        System.arraycopy(source.xp, 0, this.xp, 0, STAT_COUNT);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Levels", levels);
        tag.putIntArray("XP", xp);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        int[] loadedLevels = tag.getIntArray("Levels");
        int[] loadedXp = tag.getIntArray("XP");
        System.arraycopy(loadedLevels, 0, levels, 0, Math.min(loadedLevels.length, STAT_COUNT));
        System.arraycopy(loadedXp, 0, xp, 0, Math.min(loadedXp.length, STAT_COUNT));
    }
}
```

- [ ] **Step 2: Create PlayerStatsProvider**

```java
package tong.statmod.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerStatsProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<PlayerStats> PLAYER_STATS = CapabilityManager.get(new CapabilityToken<>() {});

    private PlayerStats stats;
    private final LazyOptional<PlayerStats> lazyOptional = LazyOptional.of(this::getOrCreate);

    private PlayerStats getOrCreate() {
        if (this.stats == null) this.stats = new PlayerStats();
        return this.stats;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == PLAYER_STATS ? lazyOptional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return getOrCreate().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        getOrCreate().deserializeNBT(nbt);
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/capability/
git commit -m "feat: add PlayerStats capability with 23 stats storage"
```

### Task 1.4: Register Capability & Attach to Player

**Files:**
- Modify: `src/main/java/tong/statmod/StatMod.java`
- Create: `src/main/java/tong/statmod/capability/CapabilityHandler.java`

- [ ] **Step 1: Create CapabilityHandler**

```java
package tong.statmod.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;

@Mod.EventBusSubscriber(modid = StatMod.MODID)
public class CapabilityHandler {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerStats.class);
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                new ResourceLocation(StatMod.MODID, "player_stats"),
                new PlayerStatsProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(oldStats -> {
                event.getEntity().getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(newStats -> {
                    newStats.copyFrom(oldStats);
                });
            });
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/capability/CapabilityHandler.java src/main/java/tong/statmod/StatMod.java
git commit -m "feat: register PlayerStats capability and attach to players"
```

### Task 1.5: Create Network Infrastructure

**Files:**
- Create: `src/main/java/tong/statmod/network/NetworkHandler.java`
- Create: `src/main/java/tong/statmod/network/SyncAllStatsPacket.java`
- Create: `src/main/java/tong/statmod/network/StatUpdatePacket.java`

- [ ] **Step 1: Create NetworkHandler**

```java
package tong.statmod.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import tong.statmod.StatMod;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(StatMod.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(packetId++, SyncAllStatsPacket.class,
            SyncAllStatsPacket::encode, SyncAllStatsPacket::decode, SyncAllStatsPacket::handle);
        CHANNEL.registerMessage(packetId++, StatUpdatePacket.class,
            StatUpdatePacket::encode, StatUpdatePacket::decode, StatUpdatePacket::handle);
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
```

- [ ] **Step 2: Create SyncAllStatsPacket**

```java
package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.client.ClientStatsCache;

import java.util.function.Supplier;

public class SyncAllStatsPacket {
    private final int[] levels;
    private final int[] xp;

    public SyncAllStatsPacket(int[] levels, int[] xp) {
        this.levels = levels;
        this.xp = xp;
    }

    public static void encode(SyncAllStatsPacket packet, FriendlyByteBuf buf) {
        buf.writeVarIntArray(packet.levels);
        buf.writeVarIntArray(packet.xp);
    }

    public static SyncAllStatsPacket decode(FriendlyByteBuf buf) {
        return new SyncAllStatsPacket(buf.readVarIntArray(), buf.readVarIntArray());
    }

    public static void handle(SyncAllStatsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientStatsCache.updateAll(packet.levels, packet.xp));
        ctx.get().setPacketHandled(true);
    }
}
```

- [ ] **Step 3: Create StatUpdatePacket**

```java
package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.client.ClientStatsCache;

import java.util.function.Supplier;

public class StatUpdatePacket {
    private final int[] updates; // [index, level, xp] tuples

    public StatUpdatePacket(int index, int level, int xp) {
        this.updates = new int[]{index, level, xp};
    }

    public StatUpdatePacket(int[] updates) {
        this.updates = updates;
    }

    public static void encode(StatUpdatePacket packet, FriendlyByteBuf buf) {
        buf.writeVarIntArray(packet.updates);
    }

    public static StatUpdatePacket decode(FriendlyByteBuf buf) {
        return new StatUpdatePacket(buf.readVarIntArray());
    }

    public static void handle(StatUpdatePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            for (int i = 0; i < packet.updates.length; i += 3) {
                ClientStatsCache.updateStat(packet.updates[i], packet.updates[i+1], packet.updates[i+2]);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
```

- [ ] **Step 4: Create ClientStatsCache (skeleton for now, will be expanded in Phase 6)**

```java
package tong.statmod.client;

import tong.statmod.stats.StatType;

public class ClientStatsCache {
    private static final int[] levels = new int[23];
    private static final int[] xp = new int[23];

    public static void updateAll(int[] newLevels, int[] newXp) {
        System.arraycopy(newLevels, 0, levels, 0, Math.min(newLevels.length, 23));
        System.arraycopy(newXp, 0, xp, 0, Math.min(newXp.length, 23));
    }

    public static void updateStat(int index, int level, int statXp) {
        if (index >= 0 && index < 23) {
            levels[index] = level;
            xp[index] = statXp;
        }
    }

    public static int getLevel(int index) { return levels[index]; }
    public static int getXp(int index) { return xp[index]; }
    public static int getLevel(StatType type) { return levels[type.index]; }
    public static int getXp(StatType type) { return xp[type.index]; }
}
```

- [ ] **Step 5: Trigger full sync on player login (update StatMod.java)**

Add to `StatMod.java` constructor or create a login handler:

```java
@Mod.EventBusSubscriber(modid = StatMod.MODID)
public static class LoginHandler {
    @SubscribeEvent
    public static void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                int[] levels = new int[23];
                int[] xp = new int[23];
                for (int i = 0; i < 23; i++) {
                    levels[i] = stats.getLevel(i);
                    xp[i] = stats.getXp(i);
                }
                NetworkHandler.sendToPlayer(new SyncAllStatsPacket(levels, xp), serverPlayer);
            });
        }
    }
}
```

- [ ] **Step 6: Call NetworkHandler.register() in StatMod constructor**

```java
NetworkHandler.register();
```

- [ ] **Step 7: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 8: Commit**

```bash
git add src/main/java/tong/statmod/network/ src/main/java/tong/statmod/client/ src/main/java/tong/statmod/StatMod.java
git commit -m "feat: add network handlers, sync all stats on login, client cache"
```

---

## Phase 2: Core Progression

### Task 2.1: Create ActionType Enum

**Files:**
- Create: `src/main/java/tong/statmod/progression/ActionType.java`

- [ ] **Step 1: Create ActionType with stat associations**

```java
package tong.statmod.progression;

import tong.statmod.stats.StatType;

public enum ActionType {
    MELEE_HEAVY(StatType.BRUTE_FORCE, 5),
    MELEE_SLASH(StatType.BLADE_TECHNIQUE, 5),
    MELEE_RAPID(StatType.RAPIDITE, 3),
    DODGE(StatType.AGILITY, 5),
    TAKE_DAMAGE(StatType.PHYSICAL_RESISTANCE, 2),
    GUARD(StatType.PHYSICAL_ENDURANCE, 5),
    RANGED_HIT(StatType.PRECISION, 5),
    MAGIC_DAMAGE(StatType.ARCANE_POWER, 5),
    UNDERWATER_ACTION(StatType.WATER_AFFINITY, 3),
    MINE_BLOCK(StatType.EARTH_AFFINITY, 2),
    FIRE_ACTION(StatType.FIRE_AFFINITY, 3),
    AIR_ACTION(StatType.AIR_AFFINITY, 3),
    POTION_HIT(StatType.MAGIC_RESISTANCE, 2),
    USE_ITEM(StatType.CASTING_SPEED, 2),
    ENCHANT(StatType.ERUDITION, 5),
    KILL_MOB(StatType.TRACKING, 10),
    EXPLORE(StatType.KEEN_SENSES, 3),
    CRAFT(StatType.FORGING, 3),
    COOK(StatType.COOKING, 3),
    BREW(StatType.ALCHEMY, 5),
    KILL_BOSS(StatType.INTIMIDATION, 25),
    SURVIVE_LOW_HP(StatType.WILLPOWER, 5);

    public final StatType primaryStat;
    public final int baseXp;

    ActionType(StatType primaryStat, int baseXp) {
        this.primaryStat = primaryStat;
        this.baseXp = baseXp;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/progression/ActionType.java
git commit -m "feat: add ActionType enum with 22 action-to-stat mappings"
```

### Task 2.2: Create CombatXPHandler

**Files:**
- Create: `src/main/java/tong/statmod/progression/CombatXPHandler.java`

- [ ] **Step 1: Create the handler**

```java
package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.StatUpdatePacket;
import yesman.epicfight.api.forgeevent.PlayerEvent;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

@Mod.EventBusSubscriber(modid = StatMod.MODID)
public class CombatXPHandler {
    @SubscribeEvent
    public static void onAttack(PlayerEvent.ServerPlayerOnAttackEvent event) {
        if (!(event.getPlayerPatch().getOriginal() instanceof ServerPlayer player)) return;

        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            WeaponCategory category = event.getPlayerPatch().getHoldingItemCapability().getWeaponCategory();
            ActionType action = mapWeaponToAction(category);
            if (action != null) {
                int xp = action.baseXp + player.getRandom().nextInt(6);
                stats.addXp(action.primaryStat.index, xp);
                StatMod.LOGGER.debug("Awarded {} XP to {}", xp, action.primaryStat);
                // Send update (will be batched, for now send immediately)
                NetworkHandler.sendToPlayer(
                    new StatUpdatePacket(action.primaryStat.index, stats.getLevel(action.primaryStat.index), stats.getXp(action.primaryStat.index)),
                    player);
            }
        });
    }

    private static ActionType mapWeaponToAction(WeaponCategory category) {
        if (category == WeaponCategory.AXE || category == WeaponCategory.GREATSWORD)
            return ActionType.MELEE_HEAVY;
        if (category == WeaponCategory.SWORD || category == WeaponCategory.KATANA ||
            category == WeaponCategory.DAGGER || category == WeaponCategory.TRIDENT)
            return ActionType.MELEE_SLASH;
        if (category == WeaponCategory.FIST)
            return ActionType.MELEE_RAPID;
        if (category == WeaponCategory.BOW || category == WeaponCategory.CROSSBOW)
            return ActionType.RANGED_HIT;
        if (category == WeaponCategory.SHIELD)
            return ActionType.GUARD;
        if (category == WeaponCategory.SPEAR)
            return ActionType.MELEE_SLASH;
        return null;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors (ignore warnings about epicfight API usage)

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/progression/CombatXPHandler.java
git commit -m "feat: add CombatXPHandler granting XP from Epic Fight attacks"
```

### Task 2.3: Create NonCombatXPHandler

**Files:**
- Create: `src/main/java/tong/statmod/progression/NonCombatXPHandler.java`

- [ ] **Step 1: Create the handler**

```java
package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.StatUpdatePacket;

@Mod.EventBusSubscriber(modid = StatMod.MODID)
public class NonCombatXPHandler {

    @SubscribeEvent
    public static void onMobKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            LivingEntity killed = event.getEntity();
            ActionType action;
            if (killed instanceof EnderDragon || killed instanceof WitherBoss || killed.getMaxHealth() > 200) {
                action = ActionType.KILL_BOSS;
            } else if (killed instanceof Monster) {
                action = ActionType.KILL_MOB;
            } else {
                return; // passive mobs, animals
            }
            awardXp(player, action);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && isPickaxe(event.getPlayer().getMainHandItem())) {
            awardXp(player, ActionType.MINE_BLOCK);
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            awardXp(player, ActionType.CRAFT);
        }
    }

    @SubscribeEvent
    public static void onPlayerSmelt(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            awardXp(player, ActionType.COOK);
        }
    }

    private static void awardXp(ServerPlayer player, ActionType action) {
        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            stats.addXp(action.primaryStat.index, action.baseXp);
            NetworkHandler.sendToPlayer(
                new StatUpdatePacket(action.primaryStat.index, stats.getLevel(action.primaryStat.index), stats.getXp(action.primaryStat.index)),
                player);
        });
    }

    private static boolean isPickaxe(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.PickaxeItem;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/progression/NonCombatXPHandler.java
git commit -m "feat: add NonCombatXPHandler for kill, craft, mine XP"
```

### Task 2.4: Create StatCalculator + StatEffectApplier

**Files:**
- Create: `src/main/java/tong/statmod/stats/StatCalculator.java`
- Create: `src/main/java/tong/statmod/stats/StatEffectApplier.java`

- [ ] **Step 1: Create StatCalculator**

```java
package tong.statmod.stats;

public class StatCalculator {

    public static int getXpForNextLevel(int level) {
        return (level + 1) * (level + 1) * 10;
    }

    public static float getDamageBonus(StatType stat, int level) {
        return switch (stat) {
            case BRUTE_FORCE, BLADE_TECHNIQUE -> 0.01f * level;     // +1% per point
            case ARCANE_POWER -> 0.01f * level;
            default -> 0;
        };
    }

    public static float getDamageReduction(int resistanceLevel) {
        return 0.005f * resistanceLevel; // -0.5% per point, max 50%
    }

    public static float getAttackSpeedBonus(int rapLevel) {
        return 0.005f * rapLevel; // +0.5% per point
    }

    public static float getMoveSpeedBonus(int agilityLevel) {
        return 0.003f * agilityLevel; // +0.3% per point
    }

    public static float getCritChance(int precisionLevel) {
        return 0.005f * precisionLevel; // +0.5% per point
    }

    public static float getEnduranceHearts(int enduranceLevel) {
        return 0.2f * enduranceLevel; // +0.2 hearts per point
    }

    public static float getFatigueReduction(int willpowerLevel) {
        return 0.005f * willpowerLevel; // -0.5% fatigue gain per point, max 50%
    }
}
```

- [ ] **Step 2: Create StatEffectApplier**

```java
package tong.statmod.stats;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.capability.PlayerStatsProvider;

@Mod.EventBusSubscriber(modid = StatMod.MODID)
public class StatEffectApplier {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // Attacker: bonus damage
        if (event.getSource().getEntity() instanceof Player player) {
            player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                float multiplier = 1.0f;
                multiplier += StatCalculator.getDamageBonus(StatType.BRUTE_FORCE, stats.getLevel(StatType.BRUTE_FORCE.index));
                multiplier += StatCalculator.getDamageBonus(StatType.BLADE_TECHNIQUE, stats.getLevel(StatType.BLADE_TECHNIQUE.index));
                event.setAmount(event.getAmount() * multiplier);
            });
        }

        // Defender: damage reduction
        if (event.getEntity() instanceof Player player) {
            player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                float reduction = StatCalculator.getDamageReduction(stats.getLevel(StatType.PHYSICAL_RESISTANCE.index));
                event.setAmount(event.getAmount() * (1.0f - reduction));
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                // Apply endurance hearts
                AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    int enduranceLevel = stats.getLevel(StatType.PHYSICAL_ENDURANCE.index);
                    float bonusHearts = StatCalculator.getEnduranceHearts(enduranceLevel);
                    if (bonusHearts > 0) {
                        // Remove old modifier, add new
                        maxHealth.removeModifiers();
                        maxHealth.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            new net.minecraft.resources.ResourceLocation(StatMod.MODID, "endurance_bonus"),
                            bonusHearts,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
                    }
                }
            });
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/stats/
git commit -m "feat: add StatCalculator, StatEffectApplier (damage, resist, endurance)"
```

---

## Phase 3: Fatigue

### Task 3.1: Create FatigueManager (Capability)

**Files:**
- Create: `src/main/java/tong/statmod/fatigue/FatigueManager.java`
- Create: `src/main/java/tong/statmod/fatigue/FatigueProvider.java`

- [ ] **Step 1: Create FatigueManager**

```java
package tong.statmod.fatigue;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class FatigueManager implements INBTSerializable<CompoundTag> {
    private float fatigue = 0;
    private static final float MAX_FATIGUE = 100;

    public float getFatigue() { return fatigue; }
    public float getFatiguePercent() { return fatigue / MAX_FATIGUE; }
    public boolean isExhausted() { return fatigue >= MAX_FATIGUE; }

    public void addFatigue(float amount) {
        this.fatigue = Math.min(MAX_FATIGUE, this.fatigue + amount);
    }

    public void reduceFatigue(float amount) {
        this.fatigue = Math.max(0, this.fatigue - amount);
    }

    public void reset() { this.fatigue = 0; }

    public FatigueThreshold getThreshold() {
        float pct = getFatiguePercent();
        if (pct >= 1.0f) return FatigueThreshold.EXHAUSTED;
        if (pct >= 0.9f) return FatigueThreshold.CRITICAL;
        if (pct >= 0.75f) return FatigueThreshold.SEVERE;
        if (pct >= 0.5f) return FatigueThreshold.MODERATE;
        if (pct >= 0.25f) return FatigueThreshold.LIGHT;
        return FatigueThreshold.NONE;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Fatigue", fatigue);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.fatigue = tag.getFloat("Fatigue");
    }

    public enum FatigueThreshold {
        NONE, LIGHT, MODERATE, SEVERE, CRITICAL, EXHAUSTED
    }
}
```

- [ ] **Step 2: Create FatigueProvider**

```java
package tong.statmod.fatigue;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FatigueProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<FatigueManager> FATIGUE = CapabilityManager.get(new CapabilityToken<>() {});

    private FatigueManager manager;
    private final LazyOptional<FatigueManager> lazyOptional = LazyOptional.of(this::getOrCreate);

    private FatigueManager getOrCreate() {
        if (this.manager == null) this.manager = new FatigueManager();
        return this.manager;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == FATIGUE ? lazyOptional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() { return getOrCreate().serializeNBT(); }

    @Override
    public void deserializeNBT(CompoundTag nbt) { getOrCreate().deserializeNBT(nbt); }
}
```

- [ ] **Step 3: Register fatigue capability in CapabilityHandler**

Add to `CapabilityHandler.registerCapabilities`:
```java
event.register(FatigueManager.class);
```

Add to `CapabilityHandler.attachCapabilities`:
```java
if (event.getObject() instanceof Player) {
    event.addCapability(new ResourceLocation(StatMod.MODID, "fatigue"), new FatigueProvider());
}
```

- [ ] **Step 4: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add src/main/java/tong/statmod/fatigue/ src/main/java/tong/statmod/capability/CapabilityHandler.java
git commit -m "feat: add FatigueManager capability"
```

### Task 3.2: Create Fatigue Handler (Event Costs)

**Files:**
- Create: `src/main/java/tong/statmod/fatigue/FatigueHandler.java`

- [ ] **Step 1: Create FatigueHandler**

```java
package tong.statmod.fatigue;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.FatiguePacket;

@Mod.EventBusSubscriber(modid = StatMod.MODID)
public class FatigueHandler {

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
                if (player.isSleeping()) {
                    fatigue.reset();
                    return;
                }

                // Recovery when idle
                if (fatigue.getFatigue() > 0) {
                    if (player.isShiftKeyDown()) {
                        fatigue.reduceFatigue(3.0f);
                    } else if (!player.isSprinting() && !player.swinging) {
                        fatigue.reduceFatigue(2.0f);
                    }
                }

                // Sprint cost
                if (player.isSprinting()) {
                    fatigue.addFatigue(1.0f);
                }

                // Jump cost
                if (player.jumping && player.onGround()) {
                    fatigue.addFatigue(2.0f);
                }

                syncIfChanged(player, fatigue);
            });
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
                fatigue.addFatigue(5.0f);
                syncIfChanged(player, fatigue);
            });
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
                fatigue.addFatigue(1.0f);
                syncIfChanged(player, fatigue);
            });
        }
    }

    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
                fatigue.reset();
                syncIfChanged(player, fatigue);
            });
        }
    }

    private static float lastSync = 0;

    private static void syncIfChanged(ServerPlayer player, FatigueManager fatigue) {
        float current = fatigue.getFatigue();
        if (Math.abs(current - lastSync) > 5.0f) {
            NetworkHandler.sendToPlayer(new FatiguePacket(current), player);
            lastSync = current;
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/fatigue/FatigueHandler.java
git commit -m "feat: add FatigueHandler with sprint/jump/damage costs and recovery"
```

### Task 3.3: Create Fatigue Effects (Debuffs)

**Files:**
- Create: `src/main/java/tong/statmod/fatigue/FatigueEffects.java`

- [ ] **Step 1: Create FatigueEffects**

```java
package tong.statmod.fatigue;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;

@Mod.EventBusSubscriber(modid = StatMod.MODID)
public class FatigueEffects {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;

        player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
            FatigueManager.FatigueThreshold threshold = fatigue.getThreshold();

            switch (threshold) {
                case LIGHT -> applyDebuff(player, 0, 0, 0, 0); // warning level, no debuff yet
                case MODERATE -> applyDebuff(player, 1, 0, 0, 0);
                case SEVERE -> applyDebuff(player, 2, 1, 0, 0);
                case CRITICAL -> applyDebuff(player, 3, 1, 1, 0);
                case EXHAUSTED -> applyDebuff(player, 4, 2, 1, 1);
                default -> clearDebuffs(player);
            }
        });
    }

    private static void applyDebuff(ServerPlayer player, int weakness, int slowness, int fatigue, int miningFatigue) {
        if (weakness > 0) player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, weakness - 1, true, false));
        if (slowness > 0) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, slowness - 1, true, false));
        if (fatigue > 0) player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, fatigue - 1, true, false));
        if (miningFatigue > 0) player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, miningFatigue + 1, true, false));
    }

    private static void clearDebuffs(ServerPlayer player) {
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
    }
}
```

- [ ] **Step 2: Create FatiguePacket**

```java
package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.client.ClientStatsCache;

import java.util.function.Supplier;

public class FatiguePacket {
    private final float fatigue;

    public FatiguePacket(float fatigue) { this.fatigue = fatigue; }

    public static void encode(FatiguePacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.fatigue);
    }

    public static FatiguePacket decode(FriendlyByteBuf buf) {
        return new FatiguePacket(buf.readFloat());
    }

    public static void handle(FatiguePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientStatsCache.updateFatigue(packet.fatigue));
        ctx.get().setPacketHandled(true);
    }
}
```

Add to `ClientStatsCache`:
```java
private static float fatigue = 0;

public static void updateFatigue(float f) { fatigue = f; }
public static float getFatigue() { return fatigue; }
```

Register FatiguePacket in `NetworkHandler.register()`:
```java
CHANNEL.registerMessage(packetId++, FatiguePacket.class,
    FatiguePacket::encode, FatiguePacket::decode, FatiguePacket::handle);
```

- [ ] **Step 3: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/fatigue/FatigueEffects.java src/main/java/tong/statmod/network/FatiguePacket.java src/main/java/tong/statmod/client/ src/main/java/tong/statmod/network/NetworkHandler.java
git commit -m "feat: add FatigueEffects with threshold debuffs, FatiguePacket, client cache"
```

---

## Phase 4: Weapon Mastery

### Task 4.1: Create WeaponType Enum + WeaponMasteryManager

**Files:**
- Create: `src/main/java/tong/statmod/weapon/WeaponType.java`
- Create: `src/main/java/tong/statmod/weapon/WeaponMasteryManager.java`
- Create: `src/main/java/tong/statmod/weapon/WeaponMasteryProvider.java`

- [ ] **Step 1: Create WeaponType enum**

```java
package tong.statmod.weapon;

import yesman.epicfight.world.capabilities.item.WeaponCategory;

public enum WeaponType {
    SWORD(WeaponCategory.SWORD),
    GREATSWORD(WeaponCategory.GREATSWORD),
    KATANA(WeaponCategory.KATANA),
    SPEAR(WeaponCategory.SPEAR),
    DAGGER(WeaponCategory.DAGGER),
    AXE(WeaponCategory.AXE),
    FIST(WeaponCategory.FIST),
    BOW(WeaponCategory.BOW),
    CROSSBOW(WeaponCategory.CROSSBOW),
    TRIDENT(WeaponCategory.TRIDENT),
    SHIELD(WeaponCategory.SHIELD);

    public final WeaponCategory epicFightCategory;

    WeaponType(WeaponCategory epicFightCategory) {
        this.epicFightCategory = epicFightCategory;
    }

    public static WeaponType fromEpicFightCategory(WeaponCategory category) {
        for (WeaponType type : values()) {
            if (type.epicFightCategory == category) return type;
        }
        return null;
    }
}
```

- [ ] **Step 2: Create WeaponMasteryManager**

```java
package tong.statmod.weapon;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import tong.statmod.stats.StatCalculator;

public class WeaponMasteryManager implements INBTSerializable<CompoundTag> {
    public static final int WEAPON_COUNT = 11;
    private static final int MAX_LEVEL = 50;
    private final int[] levels = new int[WEAPON_COUNT];
    private final int[] xp = new int[WEAPON_COUNT];

    public int getLevel(int index) { return levels[index]; }
    public int getXp(int index) { return xp[index]; }

    public void addXp(int index, int amount) {
        if (index < 0 || index >= WEAPON_COUNT) return;
        this.xp[index] += amount;
        while (this.xp[index] >= StatCalculator.getXpForNextLevel(levels[index]) && levels[index] < MAX_LEVEL) {
            levels[index]++;
            this.xp[index] -= StatCalculator.getXpForNextLevel(levels[index]);
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("WeaponLevels", levels);
        tag.putIntArray("WeaponXP", xp);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        int[] loadedLevels = tag.getIntArray("WeaponLevels");
        int[] loadedXp = tag.getIntArray("WeaponXP");
        System.arraycopy(loadedLevels, 0, levels, 0, Math.min(loadedLevels.length, WEAPON_COUNT));
        System.arraycopy(loadedXp, 0, xp, 0, Math.min(loadedXp.length, WEAPON_COUNT));
    }
}
```

- [ ] **Step 3: Create WeaponMasteryProvider**

```java
package tong.statmod.weapon;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WeaponMasteryProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<WeaponMasteryManager> WEAPON_MASTERY = CapabilityManager.get(new CapabilityToken<>() {});

    private WeaponMasteryManager manager;
    private final LazyOptional<WeaponMasteryManager> lazyOptional = LazyOptional.of(this::getOrCreate);

    private WeaponMasteryManager getOrCreate() {
        if (this.manager == null) this.manager = new WeaponMasteryManager();
        return this.manager;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == WEAPON_MASTERY ? lazyOptional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() { return getOrCreate().serializeNBT(); }

    @Override
    public void deserializeNBT(CompoundTag nbt) { getOrCreate().deserializeNBT(nbt); }
}
```

- [ ] **Step 4: Register weapon mastery capability in CapabilityHandler**

```java
event.register(WeaponMasteryManager.class);
...
event.addCapability(new ResourceLocation(StatMod.MODID, "weapon_mastery"), new WeaponMasteryProvider());
```

- [ ] **Step 5: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 6: Commit**

```bash
git add src/main/java/tong/statmod/weapon/ src/main/java/tong/statmod/capability/CapabilityHandler.java
git commit -m "feat: add WeaponMastery capability with 11 weapon types"
```

### Task 4.2: Create WeaponXPHandler

**Files:**
- Create: `src/main/java/tong/statmod/weapon/WeaponXPHandler.java`

- [ ] **Step 1: Create WeaponXPHandler**

```java
package tong.statmod.weapon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.stats.StatType;
import tong.statmod.capability.PlayerStatsProvider;
import yesman.epicfight.api.forgeevent.PlayerEvent;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

@Mod.EventBusSubscriber(modid = StatMod.MODID)
public class WeaponXPHandler {

    @SubscribeEvent
    public static void onAttack(PlayerEvent.ServerPlayerOnAttackEvent event) {
        if (!(event.getPlayerPatch().getOriginal() instanceof ServerPlayer player)) return;

        WeaponCategory category = event.getPlayerPatch().getHoldingItemCapability().getWeaponCategory();
        WeaponType weaponType = WeaponType.fromEpicFightCategory(category);
        if (weaponType == null) return;

        player.getCapability(WeaponMasteryProvider.WEAPON_MASTERY).ifPresent(mastery -> {
            int xp = 5 + player.getRandom().nextInt(6);
            mastery.addXp(weaponType.ordinal(), xp);

            // Synergy: 50% weapon XP also goes to associated combat stat
            StatType primaryStat = mapWeaponToStat(weaponType);
            if (primaryStat != null) {
                player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                    stats.addXp(primaryStat.index, xp / 2);
                });
            }
        });
    }

    private static StatType mapWeaponToStat(WeaponType weapon) {
        return switch (weapon) {
            case AXE, GREATSWORD -> StatType.BRUTE_FORCE;
            case SWORD, KATANA, DAGGER -> StatType.BLADE_TECHNIQUE;
            case FIST -> StatType.RAPIDITE;
            case BOW, CROSSBOW -> StatType.PRECISION;
            case SPEAR, TRIDENT -> StatType.AGILITY;
            case SHIELD -> StatType.PHYSICAL_ENDURANCE;
        };
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/weapon/WeaponXPHandler.java
git commit -m "feat: add WeaponXPHandler with stat synergy"
```

---

## Phase 5: Skill Unlocks + EF Integration

### Task 5.1: Create EpicFightCompat

**Files:**
- Create: `src/main/java/tong/statmod/integration/EpicFightCompat.java`

- [ ] **Step 1: Create the compatibility layer**

```java
package tong.statmod.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import tong.statmod.StatMod;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public class EpicFightCompat {
    private static boolean epicFightLoaded = false;

    public static void init() {
        epicFightLoaded = ModList.get().isLoaded(EpicFightMod.MODID);
        if (epicFightLoaded) {
            StatMod.LOGGER.info("Epic Fight detected — deep integration enabled");
        } else {
            StatMod.LOGGER.info("Epic Fight not detected — running in standalone mode");
        }
    }

    public static boolean isEpicFightLoaded() {
        return epicFightLoaded;
    }

    public static void grantSkill(ServerPlayer player, Skill skill) {
        if (!epicFightLoaded) return;

        player.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY).ifPresent(cap -> {
            if (cap instanceof ServerPlayerPatch playerPatch) {
                SkillContainer container = playerPatch.getSkill(skill.getCategory());
                if (container != null) {
                    container.setSkill(skill);
                    StatMod.LOGGER.info("Granted Epic Fight skill: {}", skill.getName());
                }
            }
        });
    }

    public static void removeSkill(ServerPlayer player, Skill skill) {
        if (!epicFightLoaded) return;

        player.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY).ifPresent(cap -> {
            if (cap instanceof ServerPlayerPatch playerPatch) {
                SkillContainer container = playerPatch.getSkill(skill.getCategory());
                if (container != null && container.getSkill() == skill) {
                    container.setSkill(null);
                }
            }
        });
    }
}
```

- [ ] **Step 2: Call EpicFightCompat.init() in StatMod constructor**

```java
EpicFightCompat.init();
```

- [ ] **Step 3: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/integration/ src/main/java/tong/statmod/StatMod.java
git commit -m "feat: add EpicFightCompat layer for soft dependency management"
```

### Task 5.2: Create SkillUnlockRegistry + Handler

**Files:**
- Create: `src/main/java/tong/statmod/skills/SkillUnlockRegistry.java`
- Create: `src/main/java/tong/statmod/skills/SkillUnlockHandler.java`

- [ ] **Step 1: Create SkillUnlockRegistry**

```java
package tong.statmod.skills;

import tong.statmod.stats.StatType;
import yesman.epicfight.skill.Skill;

import java.util.HashMap;
import java.util.Map;

public class SkillUnlockRegistry {
    private static final Map<StatType, Skill[]> statSkills = new HashMap<>();

    public static void register(StatType stat, Skill tier1, Skill tier2, Skill tier3, Skill tier4) {
        statSkills.put(stat, new Skill[]{tier1, tier2, tier3, tier4});
    }

    public static Skill getSkill(StatType stat, int tier) {
        Skill[] skills = statSkills.get(stat);
        if (skills == null || tier < 0 || tier >= skills.length) return null;
        return skills[tier];
    }

    public static boolean hasSkill(StatType stat, int tier) {
        return getSkill(stat, tier) != null;
    }
}
```

- [ ] **Step 2: Create SkillUnlockHandler**

```java
package tong.statmod.skills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.integration.EpicFightCompat;
import tong.statmod.stats.StatType;

@Mod.EventBusSubscriber(modid = StatMod.MODID)
public class SkillUnlockHandler {

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            for (StatType stat : StatType.values()) {
                int level = stats.getLevel(stat.index);
                checkAndUnlock(player, stat, level);
            }
        });
    }

    private static final int[] TIERS = {10, 25, 50, 75};

    private static void checkAndUnlock(ServerPlayer player, StatType stat, int level) {
        for (int i = 0; i < TIERS.length; i++) {
            if (level >= TIERS[i]) {
                var skill = SkillUnlockRegistry.getSkill(stat, i);
                if (skill != null) {
                    EpicFightCompat.grantSkill(player, skill);
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/skills/
git commit -m "feat: add SkillUnlockRegistry + Handler, triggers on tick"
```

### Task 5.3: Create Initial EF Skill Classes

**Files:**
- Create: `src/main/java/tong/statmod/skills/skills/EarthSplitterSkill.java`
- Create: `src/main/java/tong/statmod/skills/skills/IaijutsuSkill.java`
- Create: `src/main/java/tong/statmod/skills/skills/ShadowStepSkill.java`
- Create: `src/main/java/tong/statmod/skills/skills/IronWallSkill.java`
- Create: `src/main/java/tong/statmod/skills/skills/HawkEyeSkill.java`

- [ ] **Step 1: Create EarthSplitterSkill (Brute Force 50, common)**

```java
package tong.statmod.skills.skills;

import net.minecraft.resources.ResourceLocation;
import tong.statmod.StatMod;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillCategory;

public class EarthSplitterSkill extends Skill {
    public static final EarthSplitterSkill INSTANCE = new EarthSplitterSkill();

    private EarthSplitterSkill() {
        super(new ResourceLocation(StatMod.MODID, "earth_splitter"), SkillCategories.WEAPON_INNATE);
    }
}
```

- [ ] **Step 2: Create IaijutsuSkill (Blade Technique 25, common)**

```java
package tong.statmod.skills.skills;

import net.minecraft.resources.ResourceLocation;
import tong.statmod.StatMod;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;

public class IaijutsuSkill extends Skill {
    public static final IaijutsuSkill INSTANCE = new IaijutsuSkill();

    private IaijutsuSkill() {
        super(new ResourceLocation(StatMod.MODID, "iaijutsu"), SkillCategories.WEAPON_INNATE);
    }
}
```

- [ ] **Step 3: Create ShadowStepSkill (Agility 25, dodge)**

```java
package tong.statmod.skills.skills;

import net.minecraft.resources.ResourceLocation;
import tong.statmod.StatMod;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;

public class ShadowStepSkill extends Skill {
    public static final ShadowStepSkill INSTANCE = new ShadowStepSkill();

    private ShadowStepSkill() {
        super(new ResourceLocation(StatMod.MODID, "shadow_step"), SkillCategories.DODGE);
    }
}
```

- [ ] **Step 4: Create IronWallSkill (Endurance 25, guard)**

```java
package tong.statmod.skills.skills;

import net.minecraft.resources.ResourceLocation;
import tong.statmod.StatMod;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;

public class IronWallSkill extends Skill {
    public static final IronWallSkill INSTANCE = new IronWallSkill();

    private IronWallSkill() {
        super(new ResourceLocation(StatMod.MODID, "iron_wall"), SkillCategories.GUARD);
    }
}
```

- [ ] **Step 5: Create HawkEyeSkill (Precision 10, passive)**

```java
package tong.statmod.skills.skills;

import net.minecraft.resources.ResourceLocation;
import tong.statmod.StatMod;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;

public class HawkEyeSkill extends Skill {
    public static final HawkEyeSkill INSTANCE = new HawkEyeSkill();

    private HawkEyeSkill() {
        super(new ResourceLocation(StatMod.MODID, "hawk_eye"), SkillCategories.PASSIVE);
    }
}
```

- [ ] **Step 6: Register all 5 skills in SkillUnlockRegistry**

Create a registration method in a new class or add to SkillUnlockRegistry:

```java
// Add this static initializer to SkillUnlockRegistry
public static void init() {
    register(StatType.BRUTE_FORCE, null, null, EarthSplitterSkill.INSTANCE, null);
    register(StatType.BLADE_TECHNIQUE, null, IaijutsuSkill.INSTANCE, null, null);
    register(StatType.AGILITY, null, ShadowStepSkill.INSTANCE, null, null);
    register(StatType.PHYSICAL_ENDURANCE, null, IronWallSkill.INSTANCE, null, null);
    register(StatType.PRECISION, HawkEyeSkill.INSTANCE, null, null, null);
}
```

Call `SkillUnlockRegistry.init()` from `StatMod` constructor after `EpicFightCompat.init()`.

- [ ] **Step 7: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 8: Commit**

```bash
git add src/main/java/tong/statmod/skills/skills/ src/main/java/tong/statmod/skills/SkillUnlockRegistry.java
git commit -m "feat: add 5 initial EF skill classes (Earth Splitter, Iaijutsu, Shadow Step, Iron Wall, Hawk Eye)"
```

---

## Phase 6: Client

### Task 6.1: Create ClientSetup + ClientStatsCache (finalize)

**Files:**
- Create: `src/main/java/tong/statmod/client/ClientSetup.java`

- [ ] **Step 1: Create ClientSetup with keybinding**

```java
package tong.statmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.Minecraft;
import tong.statmod.StatMod;
import tong.statmod.client.gui.CharacterScreen;

@Mod.EventBusSubscriber(modid = StatMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    public static final KeyMapping OPEN_STATS_KEY = new KeyMapping(
        "key.statmod.open_stats", InputConstants.KEY_P, "key.categories.statmod");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(ClientEventHandler.class);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STATS_KEY);
    }

    @Mod.EventBusSubscriber(modid = StatMod.MODID, value = Dist.CLIENT)
    public static class ClientEventHandler {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (OPEN_STATS_KEY.consumeClick()) {
                Minecraft.getInstance().setScreen(new CharacterScreen());
            }
        }
    }
}
```

- [ ] **Step 1b: Update ClientStatsCache to be final**

Already created in Task 1.5. Ensure it has the fatigue field:

```java
// Already has:
private static float fatigue = 0;
public static void updateFatigue(float f) { fatigue = f; }
public static float getFatigue() { return fatigue; }
```

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add src/main/java/tong/statmod/client/ClientSetup.java
git commit -m "feat: add ClientSetup with P key binding for character screen"
```

### Task 6.2: Create HUD Overlays (Combo + Fatigue)

**Files:**
- Create: `src/main/java/tong/statmod/client/hud/HUDManager.java`
- Create: `src/main/java/tong/statmod/client/hud/ComboOverlay.java`
- Create: `src/main/java/tong/statmod/client/hud/FatigueOverlay.java`

- [ ] **Step 1: Create HUDManager**

```java
package tong.statmod.client.hud;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;

@Mod.EventBusSubscriber(modid = StatMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class HUDManager {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "combo", ComboOverlay.INSTANCE);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "fatigue", FatigueOverlay.INSTANCE);
    }
}
```

- [ ] **Step 2: Create ComboOverlay**

```java
package tong.statmod.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import tong.statmod.client.ClientStatsCache;

import java.awt.Color;

public class ComboOverlay implements IGuiOverlay {
    public static final ComboOverlay INSTANCE = new ComboOverlay();

    private int comboCount = 0;
    private long lastHitTime = 0;
    private static final long FADE_OUT = 3000; // ms

    public void registerHit() {
        this.comboCount++;
        this.lastHitTime = System.currentTimeMillis();
    }

    public void reset() {
        this.comboCount = 0;
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        long elapsed = System.currentTimeMillis() - lastHitTime;
        if (elapsed > FADE_OUT || comboCount == 0) {
            comboCount = 0;
            return;
        }

        String text = comboCount + " hits";
        float alpha = Math.max(0, 1.0f - (elapsed / (float) FADE_OUT));
        int color = switch ((comboCount / 10) % 4) {
            case 0 -> Color.WHITE.getRGB();
            case 1 -> Color.YELLOW.getRGB();
            case 2 -> Color.ORANGE.getRGB();
            default -> Color.RED.getRGB();
        };

        int x = screenWidth / 2 - 20;
        int y = screenHeight / 4 - 10;
        graphics.setColor(1, 1, 1, alpha);
        graphics.drawString(Minecraft.getInstance().font, text, x, y, color);
        graphics.setColor(1, 1, 1, 1);
    }
}
```

- [ ] **Step 3: Create FatigueOverlay**

```java
package tong.statmod.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import tong.statmod.client.ClientStatsCache;

import java.awt.Color;

public class FatigueOverlay implements IGuiOverlay {
    public static final FatigueOverlay INSTANCE = new FatigueOverlay();
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 6;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        float fatigue = ClientStatsCache.getFatigue();
        if (fatigue <= 0) return;

        int x = screenWidth / 2 - BAR_WIDTH / 2;
        int y = screenHeight / 4 - 25;
        int filled = (int) ((fatigue / 100.0f) * BAR_WIDTH);

        Color barColor;
        if (fatigue < 25) barColor = Color.GREEN;
        else if (fatigue < 50) barColor = Color.YELLOW;
        else if (fatigue < 75) barColor = Color.ORANGE;
        else barColor = Color.RED;

        // Background
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, Color.DARK_GRAY.darker().getRGB());
        // Filled portion
        graphics.fill(x, y, x + filled, y + BAR_HEIGHT, barColor.getRGB());
        // Border
        graphics.fill(x, y, x + BAR_WIDTH, y + 1, Color.GRAY.getRGB());
        graphics.fill(x, y, x + 1, y + BAR_HEIGHT, Color.GRAY.getRGB());
        graphics.fill(x + BAR_WIDTH - 1, y, x + BAR_WIDTH, y + BAR_HEIGHT, Color.GRAY.getRGB());
        graphics.fill(x, y + BAR_HEIGHT - 1, x + BAR_WIDTH, y + BAR_HEIGHT, Color.GRAY.getRGB());

        // Percentage text
        String text = String.format("%d%%", (int) fatigue);
        graphics.drawString(Minecraft.getInstance().font, text, x + BAR_WIDTH + 5, y, Color.WHITE.getRGB());
    }
}
```

- [ ] **Step 4: Update CombatXPHandler to register combo hits**

Add `ComboOverlay.INSTANCE.registerHit()` when a hit is registered:

```java
// Inside CombatXPHandler.onAttack, after XP award:
if (event.getPlayerPatch().getOriginal().level().isClientSide) {
    ComboOverlay.INSTANCE.registerHit();
}
```

Wait — `ServerPlayerOnAttackEvent` is server-side only. We need to send combo data through a packet or track it server-side. For simplicity, track combo server-side and sync via the existing stat update packet.

Actually, let's keep it simple: the combo overlay is purely client-side for now, driven by the player's own attack detection. We'll handle it differently.

Better approach: don't try to sync combo — just display it client-side based on player attack animation events. This is simpler. We'll rework this during Phase 8 if needed.

For now, the combo counter will be a visual placeholder. We'll sync it properly when the EF integration is more mature.

- [ ] **Step 5: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 6: Commit**

```bash
git add src/main/java/tong/statmod/client/hud/
git commit -m "feat: add HUD overlays (combo + fatigue bar)"
```

### Task 6.3: Create CharacterScreen GUI

**Files:**
- Create: `src/main/java/tong/statmod/client/gui/CharacterScreen.java`
- Create: `src/main/java/tong/statmod/client/gui/StatWidget.java`

- [ ] **Step 1: Create StatWidget**

```java
package tong.statmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.stats.StatType;

import java.awt.Color;

public class StatWidget extends AbstractWidget {
    private final StatType stat;

    public StatWidget(StatType stat, int x, int y) {
        super(x, y, 200, 20, Component.literal(stat.displayName));
        this.stat = stat;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int level = ClientStatsCache.getLevel(stat);
        int xp = ClientStatsCache.getXp(stat);
        int needed = (level + 1) * (level + 1) * 10;

        // Stat name and level
        graphics.drawString(this.font, stat.displayName + "  Lv." + level, getX(), getY(), 0xFFFFFF);

        // XP bar
        int barWidth = 120;
        int barHeight = 4;
        int barX = getX() + 80;
        int barY = getY() + 12;
        int filled = needed > 0 ? (int) ((float) xp / needed * barWidth) : 0;

        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, Color.DARK_GRAY.getRGB());
        if (level < 100) {
            graphics.fill(barX, barY, barX + Math.min(filled, barWidth), barY + barHeight, Color.CYAN.getRGB());
        }

        // XP text
        String xpText = level < 100 ? xp + "/" + needed + " XP" : "MAX";
        graphics.drawString(this.font, xpText, getX() + 80 + barWidth + 5, barY - 2, Color.GRAY.getRGB());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
```

- [ ] **Step 2: Create CharacterScreen (5 tabs)**

```java
package tong.statmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tong.statmod.stats.StatCategory;
import tong.statmod.stats.StatType;

import java.util.ArrayList;
import java.util.List;

public class CharacterScreen extends Screen {
    private static final int TAB_COUNT = 5;
    private int selectedTab = 0;
    private final String[] tabNames = {"⚔ Combat", "✦ Magic", "🌿 Survival", "🔨 Crafting", "🧠 Mental"};
    private final StatCategory[] tabCategories = {
        StatCategory.COMBAT, StatCategory.MAGIC, StatCategory.SURVIVAL,
        StatCategory.CRAFTING, StatCategory.MENTAL};
    private List<StatWidget> widgets = new ArrayList<>();

    protected CharacterScreen() {
        super(Component.translatable("screen.statmod.character"));
    }

    @Override
    protected void init() {
        super.init();
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        this.clearWidgets();
        widgets.clear();

        StatCategory category = tabCategories[selectedTab];
        int y = 40;
        for (StatType stat : StatType.values()) {
            if (stat.category == category) {
                StatWidget widget = new StatWidget(stat, 20, y);
                addRenderableWidget(widget);
                widgets.add(widget);
                y += 30;
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        // Tab bar
        int tabX = 20;
        for (int i = 0; i < TAB_COUNT; i++) {
            int color = i == selectedTab ? 0xFFFFFF : 0x888888;
            graphics.drawString(this.font, tabNames[i], tabX, 20, color);
            tabX += this.font.width(tabNames[i]) + 15;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Tab switching via click
        int tabX = 20;
        for (int i = 0; i < TAB_COUNT; i++) {
            int tabWidth = this.font.width(tabNames[i]) + 15;
            if (mouseX >= tabX && mouseX <= tabX + tabWidth && mouseY >= 20 && mouseY <= 35) {
                if (selectedTab != i) {
                    selectedTab = i;
                    rebuildWidgets();
                }
                return true;
            }
            tabX += tabWidth;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
```

- [ ] **Step 3: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/client/gui/
git commit -m "feat: add CharacterScreen with 5 tabs and StatWidget"
```

### Task 6.4: Create Notifications

**Files:**
- Create: `src/main/java/tong/statmod/client/notification/LevelUpToast.java`

- [ ] **Step 1: Create toast notification**

```java
package tong.statmod.client.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;

import java.awt.Color;

@Mod.EventBusSubscriber(modid = StatMod.MODID, value = Dist.CLIENT)
public class LevelUpToast {
    private static String activeText = "";
    private static long startTime = 0;
    private static final long DURATION = 3000;

    public static void show(String text) {
        activeText = text;
        startTime = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onRenderOverlay(CustomizeGuiOverlayEvent event) {
        if (activeText.isEmpty()) return;

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > DURATION) {
            activeText = "";
            return;
        }

        float alpha = 1.0f;
        if (elapsed > DURATION - 500) {
            alpha = (DURATION - elapsed) / 500.0f;
        }

        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        GuiGraphics graphics = event.getGuiGraphics();
        int x = screenWidth / 2 - 60;
        int y = screenHeight / 2 + 40;

        graphics.setColor(1, 1, 1, alpha);
        graphics.drawString(Minecraft.getInstance().font, activeText, x, y, Color.YELLOW.getRGB());
        graphics.setColor(1, 1, 1, 1);
    }

    // Called from StatUpdatePacket handler
    public static void onLevelUp(String statName, int newLevel) {
        show("§6" + statName + " → Level " + newLevel + "!");
    }
}
```

- [ ] **Step 2: Integrate with StatUpdatePacket handling**

In `ClientStatsCache` or `StatUpdatePacket.handle()`, detect level ups and show toast:

```java
// In ClientStatsCache.updateStat, add:
if (level > oldLevel) {
    LevelUpToast.onLevelUp(StatType.byIndex(index).displayName, level);
}
```

- [ ] **Step 3: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/client/notification/
git commit -m "feat: add LevelUpToast notification system"
```

---

## Phase 7: Config & Commands

### Task 7.1: Expand Server Config

**Files:**
- Modify: `src/main/java/tong/statmod/Config.java`

- [ ] **Step 1: Read current Config.java**

```bash
cat src/main/java/tong/statmod/Config.java
```

- [ ] **Step 2: Expand with all config options**

```java
package tong.statmod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = StatMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // Progression
    public static final ForgeConfigSpec.IntValue XP_BASE = BUILDER
        .comment("Base multiplier for XP formula")
        .defineInRange("progression.xp_base", 10, 1, 1000);

    public static final ForgeConfigSpec.IntValue XP_PER_HIT = BUILDER
        .comment("Base XP per successful hit")
        .defineInRange("progression.xp_per_hit", 5, 1, 100);

    public static final ForgeConfigSpec.DoubleValue DEATH_XP_LOSS = BUILDER
        .comment("Fraction of XP lost on death (0.0 - 1.0)")
        .defineInRange("progression.death_xp_loss", 0.1, 0.0, 1.0);

    // Fatigue
    public static final ForgeConfigSpec.IntValue SPRINT_COST = BUILDER
        .comment("Fatigue cost per second of sprinting")
        .defineInRange("fatigue.sprint_cost", 1, 0, 100);

    public static final ForgeConfigSpec.IntValue JUMP_COST = BUILDER
        .comment("Fatigue cost per jump")
        .defineInRange("fatigue.jump_cost", 2, 0, 100);

    public static final ForgeConfigSpec.IntValue ATTACK_COST = BUILDER
        .comment("Fatigue cost per attack")
        .defineInRange("fatigue.attack_cost", 3, 0, 100);

    public static final ForgeConfigSpec.IntValue DAMAGE_COST = BUILDER
        .comment("Fatigue cost per damage taken")
        .defineInRange("fatigue.damage_cost", 5, 0, 100);

    public static final ForgeConfigSpec.IntValue RECOVERY_RATE = BUILDER
        .comment("Fatigue recovery per second while idle")
        .defineInRange("fatigue.recovery_rate", 2, 0, 100);

    public static final ForgeConfigSpec.IntValue SNEAK_RECOVERY = BUILDER
        .comment("Fatigue recovery per second while sneaking")
        .defineInRange("fatigue.sneak_recovery", 3, 0, 100);

    // Effects
    public static final ForgeConfigSpec.DoubleValue FORCE_DAMAGE_PER_POINT = BUILDER
        .comment("Damage bonus per Brute Force level (multiplier)")
        .defineInRange("effects.force_damage", 0.01, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue RESISTANCE_PER_POINT = BUILDER
        .comment("Damage reduction per Physical Resistance level")
        .defineInRange("effects.resistance", 0.005, 0.0, 1.0);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enableDebug = false;

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        enableDebug = true; // Allow debug mode config later
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/Config.java
git commit -m "feat: expand server config with progression, fatigue, effects options"
```

### Task 7.2: Create Commands

**Files:**
- Create: `src/main/java/tong/statmod/command/StatsCommands.java`

- [ ] **Step 1: Create the command class**

```java
package tong.statmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.capability.PlayerStats;
import tong.statmod.fatigue.FatigueProvider;
import tong.statmod.stats.StatType;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.SyncAllStatsPacket;
import tong.statmod.network.FatiguePacket;

import java.util.Arrays;

public class StatsCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stats")
            .then(Commands.literal("get")
                .then(Commands.argument("stat", StringArgumentType.word())
                    .executes(ctx -> getStat(ctx.getSource(), StringArgumentType.getString(ctx, "stat"))))
                .executes(ctx -> getAllStats(ctx.getSource())))
            .then(Commands.literal("set")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("stat", StringArgumentType.word())
                    .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> setStat(ctx.getSource(), StringArgumentType.getString(ctx, "stat"),
                            IntegerArgumentType.getInteger(ctx, "level"))))))
            .then(Commands.literal("reset")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> resetStats(ctx.getSource())))
        );

        dispatcher.register(Commands.literal("fatigue")
            .executes(ctx -> getFatigue(ctx.getSource()))
            .then(Commands.literal("reset")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> resetFatigue(ctx.getSource())))
        );
    }

    private static int getStat(CommandSourceStack source, String statName) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;

        StatType stat = findStat(statName);
        if (stat == null) {
            source.sendFailure(Component.literal("Unknown stat: " + statName));
            return 0;
        }

        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            int level = stats.getLevel(stat.index);
            int xp = stats.getXp(stat.index);
            int needed = PlayerStats.getXpForNextLevel(level);
            source.sendSuccess(() -> Component.literal(
                "§6" + stat.displayName + "§r: Lv." + level + " (" + xp + "/" + needed + " XP)"), false);
        });
        return 1;
    }

    private static int getAllStats(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;

        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            StringBuilder sb = new StringBuilder("§6=== Stats ===\n");
            for (StatType stat : StatType.values()) {
                sb.append("§e").append(stat.displayName).append("§r: Lv.")
                    .append(stats.getLevel(stat.index)).append("\n");
            }
            source.sendSuccess(() -> Component.literal(sb.toString()), false);
        });
        return 1;
    }

    private static int setStat(CommandSourceStack source, String statName, int level) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;

        StatType stat = findStat(statName);
        if (stat == null) {
            source.sendFailure(Component.literal("Unknown stat: " + statName));
            return 0;
        }

        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            stats.setLevel(stat.index, level);
            stats.setXp(stat.index, 0);
            syncPlayer(player);
            source.sendSuccess(() -> Component.literal("Set " + stat.displayName + " to Lv." + level), true);
        });
        return 1;
    }

    private static int resetStats(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;

        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            for (int i = 0; i < PlayerStats.STAT_COUNT; i++) {
                stats.setLevel(i, 0);
                stats.setXp(i, 0);
            }
            syncPlayer(player);
            source.sendSuccess(() -> Component.literal("All stats reset"), true);
        });
        return 1;
    }

    private static int getFatigue(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;

        player.getCapability(FatigueProvider.FATIGUE).ifPresent(f -> {
            source.sendSuccess(() -> Component.literal("Fatigue: " + (int) f.getFatigue() + "%"), false);
        });
        return 1;
    }

    private static int resetFatigue(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;

        player.getCapability(FatigueProvider.FATIGUE).ifPresent(f -> {
            f.reset();
            NetworkHandler.sendToPlayer(new FatiguePacket(0), player);
            source.sendSuccess(() -> Component.literal("Fatigue reset"), true);
        });
        return 1;
    }

    private static StatType findStat(String name) {
        for (StatType stat : StatType.values()) {
            if (stat.name().equalsIgnoreCase(name) || stat.displayName.equalsIgnoreCase(name)) {
                return stat;
            }
        }
        return null;
    }

    private static void syncPlayer(ServerPlayer player) {
        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            int[] levels = new int[PlayerStats.STAT_COUNT];
            int[] xp = new int[PlayerStats.STAT_COUNT];
            for (int i = 0; i < PlayerStats.STAT_COUNT; i++) {
                levels[i] = stats.getLevel(i);
                xp[i] = stats.getXp(i);
            }
            NetworkHandler.sendToPlayer(new SyncAllStatsPacket(levels, xp), player);
        });
    }
}
```

- [ ] **Step 2: Register commands in StatMod**

Add to StatMod:

```java
@SubscribeEvent
public static void onServerStarting(net.minecraftforge.event.server.ServerStartingEvent event) {
    StatsCommands.register(event.getServer().getCommands().getDispatcher());
}
```

- [ ] **Step 3: Verify compilation**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add src/main/java/tong/statmod/command/ src/main/java/tong/statmod/StatMod.java
git commit -m "feat: add /stats and /fatigue commands"
```

---

## Phase 8: Polish

### Task 8.1: Add Localization Files

**Files:**
- Create: `src/main/resources/assets/statmod/lang/en_us.json`
- Create: `src/main/resources/assets/statmod/lang/fr_fr.json`

- [ ] **Step 1: Create en_us.json**

```json
{
  "key.categories.statmod": "STAT Mod",
  "key.statmod.open_stats": "Open Character Screen",
  "screen.statmod.character": "Character Stats",

  "statmod.stat.combat": "Combat",
  "statmod.stat.magic": "Magic",
  "statmod.stat.survival": "Survival",
  "statmod.stat.crafting": "Crafting",
  "statmod.stat.mental": "Mental",

  "statmod.stat.brute_force": "Brute Force",
  "statmod.stat.blade_technique": "Blade Technique",
  "statmod.stat.rapidite": "Rapidité",
  "statmod.stat.agility": "Agility",
  "statmod.stat.physical_resistance": "Physical Resistance",
  "statmod.stat.physical_endurance": "Physical Endurance",
  "statmod.stat.precision": "Precision",
  "statmod.stat.arcane_power": "Arcane Power",
  "statmod.stat.water_affinity": "Water Affinity",
  "statmod.stat.earth_affinity": "Earth Affinity",
  "statmod.stat.fire_affinity": "Fire Affinity",
  "statmod.stat.air_affinity": "Air Affinity",
  "statmod.stat.magic_resistance": "Magic Resistance",
  "statmod.stat.casting_speed": "Casting Speed",
  "statmod.stat.mana_pool": "Mana Pool",
  "statmod.stat.erudition": "Erudition",
  "statmod.stat.tracking": "Tracking",
  "statmod.stat.keen_senses": "Keen Senses",
  "statmod.stat.forging": "Forging",
  "statmod.stat.cooking": "Cooking",
  "statmod.stat.alchemy": "Alchemy",
  "statmod.stat.intimidation": "Intimidation",
  "statmod.stat.willpower": "Willpower",

  "statmod.notification.level_up": "%s → Level %d!",
  "statmod.notification.skill_unlocked": "✦ New skill unlocked: %s",

  "statmod.command.stats": "View or manage character stats",
  "statmod.command.fatigue": "View or manage fatigue",
  "statmod.command.unknown_stat": "Unknown stat: %s",
  "statmod.command.stat_set": "Set %s to Lv.%d",
  "statmod.command.stats_reset": "All stats reset",
  "statmod.command.fatigue_value": "Fatigue: %d%%",
  "statmod.command.fatigue_reset": "Fatigue reset"
}
```

- [ ] **Step 2: Create fr_fr.json**

```json
{
  "key.categories.statmod": "STAT Mod",
  "key.statmod.open_stats": "Ouvrir l'écran de personnage",
  "screen.statmod.character": "Stats du personnage",

  "statmod.stat.combat": "Combat",
  "statmod.stat.magic": "Magie",
  "statmod.stat.survival": "Survie",
  "statmod.stat.crafting": "Artisanat",
  "statmod.stat.mental": "Mental",

  "statmod.stat.brute_force": "Force Brute",
  "statmod.stat.blade_technique": "Technique de Lame",
  "statmod.stat.rapidite": "Rapidité",
  "statmod.stat.agility": "Agilité",
  "statmod.stat.physical_resistance": "Résistance Physique",
  "statmod.stat.physical_endurance": "Endurance Physique",
  "statmod.stat.precision": "Précision",
  "statmod.stat.arcane_power": "Puissance Arcanique",
  "statmod.stat.water_affinity": "Affinité Aquatique",
  "statmod.stat.earth_affinity": "Affinité Terrestre",
  "statmod.stat.fire_affinity": "Affinité Ignée",
  "statmod.stat.air_affinity": "Affinité Aérienne",
  "statmod.stat.magic_resistance": "Résistance Magique",
  "statmod.stat.casting_speed": "Vitesse d'Incantation",
  "statmod.stat.mana_pool": "Réserve de Mana",
  "statmod.stat.erudition": "Érudition",
  "statmod.stat.tracking": "Pistage",
  "statmod.stat.keen_senses": "Sens Aiguisés",
  "statmod.stat.forging": "Forge",
  "statmod.stat.cooking": "Cuisine",
  "statmod.stat.alchemy": "Alchimie",
  "statmod.stat.intimidation": "Intimidation",
  "statmod.stat.willpower": "Volonté",

  "statmod.notification.level_up": "%s → Niveau %d !",
  "statmod.notification.skill_unlocked": "✦ Nouveau skill débloqué : %s",

  "statmod.command.stats": "Voir ou gérer les stats",
  "statmod.command.fatigue": "Voir ou gérer la fatigue",
  "statmod.command.unknown_stat": "Stat inconnue : %s",
  "statmod.command.stat_set": "%s défini au Nv.%d",
  "statmod.command.stats_reset": "Toutes les stats réinitialisées",
  "statmod.command.fatigue_value": "Fatigue : %d%%",
  "statmod.command.fatigue_reset": "Fatigue réinitialisée"
}
```

- [ ] **Step 3: Verify build**

Run: `.\gradlew.bat build 2>&1 | Select-String "error"`

Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/assets/statmod/lang/
git commit -m "i18n: add en_us and fr_fr localizations"
```

### Task 8.2: Full Build Test

- [ ] **Step 1: Run full build**

```bash
.\gradlew.bat build 2>&1 | Select-String -Pattern "(error|FAILURE|BUILD)"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Verify JAR contents**

```bash
jar tf build/libs/statmod-1.0.0.jar | grep tong/statmod
```

Expected: All 30+ class files present

- [ ] **Step 3: Commit final state**

```bash
git add -A
git commit -m "chore: full build verification"
```

---

## Implementation Order Summary

| Phase | Tasks | Files | Dependencies |
|-------|-------|-------|-------------|
| 1. Foundation | 1.1-1.5 | 10 files | None |
| 2. Progression | 2.1-2.4 | 5 files | Phase 1 |
| 3. Fatigue | 3.1-3.3 | 5 files | Phase 1 |
| 4. Weapon Mastery | 4.1-4.2 | 4 files | Phase 1 |
| 5. Skill Unlocks | 5.1-5.3 | 7 files | Phase 1-2, Epic Fight API |
| 6. Client | 6.1-6.4 | 8 files | Phase 1-3 |
| 7. Config & Commands | 7.1-7.2 | 3 files | Phase 1-3 |
| 8. Polish | 8.1-8.2 | 3 files | All phases |

Each phase builds on the foundation but can be tested independently with `./gradlew build`.
