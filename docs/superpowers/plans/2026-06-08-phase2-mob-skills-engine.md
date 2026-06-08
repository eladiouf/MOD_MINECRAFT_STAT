# Phase 2 — Mob Skills Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Donner aux mobs des skills actifs scalés par leurs `MobStats`. Voir `docs/superpowers/specs/2026-06-08-phase2-mob-skills-engine-design.md` pour le design complet.

**Architecture:** Engine extensible (`MobSkillRegistry` + `MobSkillState` capability + `MobSkillTickHandler`) + 8 skills hardcoded (`Charge`, `Whirlwind`, `Lunge`, `Fireball`, `Curse`, `MagicMissile`, `BattleCry`, `Reflect`). JSON loadouts par mob. L2H integration optionnelle.

**Tech Stack:** Java 17, Forge 1.20.1, Forge Capabilities, `SimpleJsonResourceReloadListener`, JUnit 5.

**Prérequis :** Phase 1 terminée (MobStats fonctionne).

---

## Sommaire des tâches

| # | Tâche | Sortie |
|---|-------|--------|
| T1 | `MobSkill` interface + `MobSkillRegistry` | Fondation |
| T2 | `MobSkillState` capability + provider | Cooldowns + mana persistants |
| T3 | `MobSkillLoadout` + `MobSkillReloadListener` | JSON loader |
| T4 | `MobSkillTickHandler` + Config entries | Driver |
| T5 | 4 skills combat : Charge, Whirlwind, Lunge, Reflect | Contenu mêlée + reactif |
| T6 | 4 skills magie/mental : Fireball, Curse, MagicMissile, BattleCry | Contenu cast |
| T7 | 8 JSON loadouts vanilla mobs | Data |
| T8 | `CapabilityHandler` + `STATMod.java` wiring | Intégration |
| T9 | `L2HostilityMobSkillSync` (optionnel L2H) | Bridge |
| T10 | `ARCHITECTURE.md` update | Docs |

---

## Task 1 — `MobSkill` interface + `MobSkillRegistry`

**Fichiers :**
- Créer : `src/main/java/tong/statmod/combat/skills/MobSkill.java`
- Créer : `src/main/java/tong/statmod/combat/skills/MobSkillRegistry.java`
- Créer : `src/test/java/tong/statmod/combat/skills/MobSkillRegistryTest.java`

- [ ] **Step 1.1 : Créer le test**

`src/test/java/tong/statmod/combat/skills/MobSkillRegistryTest.java` :

```java
package tong.statmod.combat.skills;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tong.statmod.capability.MobStats;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.*;

class MobSkillRegistryTest {

    @AfterEach
    void clear() { MobSkillRegistry.clear(); }

    private static MobSkill stub(String id) {
        return new MobSkill() {
            public ResourceLocation id() { return new ResourceLocation("statmod", id); }
            public StatType requiredStat() { return StatType.BRUTE_FORCE; }
            public int requiredLevel() { return 0; }
            public int baseCooldownTicks() { return 20; }
            public int manaCost() { return 0; }
            public double maxRange() { return 10; }
            public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) { return true; }
            public void execute(Mob mob, LivingEntity target, MobStats stats) {}
        };
    }

    @Test
    void register_thenGet_returnsSameInstance() {
        MobSkill s = stub("alpha");
        MobSkillRegistry.register(s);
        assertSame(s, MobSkillRegistry.get(new ResourceLocation("statmod", "alpha")));
    }

    @Test
    void get_unknownId_returnsNull() {
        assertNull(MobSkillRegistry.get(new ResourceLocation("statmod", "unknown")));
    }

    @Test
    void register_duplicateId_overwritesAndLogsWarn() {
        MobSkill first  = stub("dup");
        MobSkill second = stub("dup");
        MobSkillRegistry.register(first);
        MobSkillRegistry.register(second);
        assertSame(second, MobSkillRegistry.get(new ResourceLocation("statmod", "dup")));
    }

    @Test
    void values_returnsAllRegistered() {
        MobSkillRegistry.register(stub("a"));
        MobSkillRegistry.register(stub("b"));
        assertEquals(2, MobSkillRegistry.values().size());
    }
}
```

- [ ] **Step 1.2 : Lancer pour vérifier l'échec**

```
gradlew.bat test --tests tong.statmod.combat.skills.MobSkillRegistryTest
```

Attendu : compile error / class not found.

- [ ] **Step 1.3 : Créer `MobSkill.java`**

```java
package tong.statmod.combat.skills;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import tong.statmod.capability.MobStats;
import tong.statmod.stats.StatType;

/**
 * An active or reactive ability that a mob can perform.
 * Stat-gated by {@link #requiredStat()} ≥ {@link #requiredLevel()}.
 * Executed server-side only.
 */
public interface MobSkill {

    ResourceLocation id();

    StatType requiredStat();
    int requiredLevel();

    int baseCooldownTicks();
    int manaCost();

    /** Max distance to target in blocks. */
    double maxRange();

    /** Final guard called by the tick handler after generic checks. */
    boolean canExecute(Mob mob, LivingEntity target, MobStats stats);

    /** Server-side effect. Must check {@code mob.isAlive()} for delayed actions. */
    void execute(Mob mob, LivingEntity target, MobStats stats);

    /** If true, the skill is driven by LivingHurtEvent rather than the tick handler. */
    default boolean isReactive() { return false; }
}
```

- [ ] **Step 1.4 : Créer `MobSkillRegistry.java`**

```java
package tong.statmod.combat.skills;

import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static registry of all MobSkill instances.
 * Populated at FMLCommonSetupEvent.
 */
public final class MobSkillRegistry {

    private static final Map<ResourceLocation, MobSkill> SKILLS = new LinkedHashMap<>();

    private MobSkillRegistry() {}

    public static void register(MobSkill skill) {
        ResourceLocation id = skill.id();
        if (SKILLS.containsKey(id)) {
            STATMod.LOGGER.warn("[MobSkillRegistry] Overwriting existing skill: {}", id);
        }
        SKILLS.put(id, skill);
    }

    public static MobSkill get(ResourceLocation id) {
        return SKILLS.get(id);
    }

    public static Collection<MobSkill> values() {
        return Collections.unmodifiableCollection(SKILLS.values());
    }

    /** Test only — clears the registry. */
    public static void clear() {
        SKILLS.clear();
    }
}
```

- [ ] **Step 1.5 : Lancer les tests**

```
gradlew.bat test --tests tong.statmod.combat.skills.MobSkillRegistryTest
```

Attendu : 4 tests passent.

- [ ] **Step 1.6 : Build**

```
gradlew.bat build
```

- [ ] **Step 1.7 : Commit**

```bash
git add src/main/java/tong/statmod/combat/skills/MobSkill.java \
        src/main/java/tong/statmod/combat/skills/MobSkillRegistry.java \
        src/test/java/tong/statmod/combat/skills/MobSkillRegistryTest.java
git commit -m "feat(phase2): add MobSkill interface + MobSkillRegistry"
```

---

## Task 2 — `MobSkillState` capability

**Fichiers :**
- Créer : `src/main/java/tong/statmod/capability/MobSkillState.java`
- Créer : `src/main/java/tong/statmod/capability/MobSkillStateProvider.java`
- Créer : `src/test/java/tong/statmod/capability/MobSkillStateTest.java`

- [ ] **Step 2.1 : Créer le test**

`MobSkillStateTest.java` :

```java
package tong.statmod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobSkillStateTest {

    @Test
    void defaultMana_isZero() {
        assertEquals(0, new MobSkillState().getMana());
    }

    @Test
    void setMana_clampsToZero() {
        MobSkillState s = new MobSkillState();
        s.setMana(-5);
        assertEquals(0, s.getMana());
    }

    @Test
    void cooldown_get_unknownId_returnsZero() {
        assertEquals(0L, new MobSkillState().getCooldownEndTick(
            new ResourceLocation("statmod", "x")));
    }

    @Test
    void cooldown_setThenGet_roundTrip() {
        MobSkillState s = new MobSkillState();
        ResourceLocation id = new ResourceLocation("statmod", "x");
        s.setCooldownEndTick(id, 1234L);
        assertEquals(1234L, s.getCooldownEndTick(id));
    }

    @Test
    void globalCooldown_default_isZero() {
        assertEquals(0L, new MobSkillState().getGlobalCooldownEndTick());
    }

    @Test
    void nbtRoundTrip_preservesAllFields() {
        MobSkillState original = new MobSkillState();
        original.setMana(42);
        original.setGlobalCooldownEndTick(500L);
        ResourceLocation id = new ResourceLocation("statmod", "charge");
        original.setCooldownEndTick(id, 750L);

        CompoundTag tag = original.serializeNBT();
        MobSkillState loaded = new MobSkillState();
        loaded.deserializeNBT(tag);

        assertEquals(42, loaded.getMana());
        assertEquals(500L, loaded.getGlobalCooldownEndTick());
        assertEquals(750L, loaded.getCooldownEndTick(id));
    }

    @Test
    void deserialize_sanitizesNegativeValues() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Mana", -10);
        tag.putLong("GlobalCD", -50L);
        MobSkillState s = new MobSkillState();
        s.deserializeNBT(tag);
        assertEquals(0, s.getMana());
        assertEquals(0L, s.getGlobalCooldownEndTick());
    }
}
```

- [ ] **Step 2.2 : Créer `MobSkillState.java`**

```java
package tong.statmod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;

public class MobSkillState implements INBTSerializable<CompoundTag> {

    private int mana = 0;
    private long globalCooldownEndTick = 0L;
    private final Map<ResourceLocation, Long> cooldownEnd = new HashMap<>();

    public int getMana() { return mana; }

    public void setMana(int mana) {
        this.mana = Math.max(0, mana);
    }

    public void addMana(int amount) {
        setMana(this.mana + amount);
    }

    public long getCooldownEndTick(ResourceLocation skillId) {
        Long v = cooldownEnd.get(skillId);
        return v == null ? 0L : v;
    }

    public void setCooldownEndTick(ResourceLocation skillId, long endTick) {
        cooldownEnd.put(skillId, Math.max(0L, endTick));
    }

    public long getGlobalCooldownEndTick() { return globalCooldownEndTick; }

    public void setGlobalCooldownEndTick(long tick) {
        this.globalCooldownEndTick = Math.max(0L, tick);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Mana", mana);
        tag.putLong("GlobalCD", globalCooldownEndTick);
        ListTag list = new ListTag();
        for (var entry : cooldownEnd.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putString("Id", entry.getKey().toString());
            c.putLong("End", entry.getValue());
            list.add(c);
        }
        tag.put("Cooldowns", list);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        setMana(tag.getInt("Mana"));
        setGlobalCooldownEndTick(tag.getLong("GlobalCD"));
        cooldownEnd.clear();
        ListTag list = tag.getList("Cooldowns", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            cooldownEnd.put(new ResourceLocation(c.getString("Id")), Math.max(0L, c.getLong("End")));
        }
    }
}
```

- [ ] **Step 2.3 : Créer `MobSkillStateProvider.java`**

```java
package tong.statmod.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MobSkillStateProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<MobSkillState> MOB_SKILL_STATE =
        CapabilityManager.get(new CapabilityToken<>() {});

    private MobSkillState instance = null;
    private final LazyOptional<MobSkillState> optional = LazyOptional.of(this::getOrCreate);

    private MobSkillState getOrCreate() {
        if (instance == null) instance = new MobSkillState();
        return instance;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == MOB_SKILL_STATE ? optional.cast() : LazyOptional.empty();
    }

    @Override public CompoundTag serializeNBT() { return getOrCreate().serializeNBT(); }
    @Override public void deserializeNBT(CompoundTag nbt) { getOrCreate().deserializeNBT(nbt); }

    public void invalidate() { optional.invalidate(); }
}
```

- [ ] **Step 2.4 : Tests + build + commit**

```
gradlew.bat test --tests tong.statmod.capability.MobSkillStateTest
gradlew.bat build
```

```bash
git add src/main/java/tong/statmod/capability/MobSkillState.java \
        src/main/java/tong/statmod/capability/MobSkillStateProvider.java \
        src/test/java/tong/statmod/capability/MobSkillStateTest.java
git commit -m "feat(phase2): add MobSkillState capability (cooldowns + mana)"
```

---

## Task 3 — `MobSkillLoadout` + `MobSkillReloadListener`

**Fichiers :**
- Créer : `src/main/java/tong/statmod/combat/skills/MobSkillLoadout.java`
- Créer : `src/main/java/tong/statmod/reload/MobSkillReloadListener.java`
- Créer : `src/test/java/tong/statmod/combat/skills/MobSkillLoadoutTest.java`

- [ ] **Step 3.1 : Créer le test**

```java
package tong.statmod.combat.skills;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobSkillLoadoutTest {

    @Test
    void parseValidJson_extractsAllFields() {
        String json = """
            {
              "entity_type": "minecraft:zombie",
              "global_cooldown_ticks": 60,
              "skills": [
                { "id": "statmod:charge", "weight": 100 },
                { "id": "statmod:battle_cry", "weight": 30 }
              ]
            }
            """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        MobSkillLoadout loadout = MobSkillLoadout.fromJson(obj);

        assertEquals(new ResourceLocation("minecraft", "zombie"), loadout.entityType());
        assertEquals(60, loadout.globalCooldownTicks());
        assertEquals(2, loadout.entries().size());
        assertEquals(100, loadout.entries().get(0).weight());
    }

    @Test
    void parseEntryWithoutWeight_defaultsTo100() {
        String json = """
            {
              "entity_type": "minecraft:zombie",
              "skills": [ { "id": "statmod:charge" } ]
            }
            """;
        MobSkillLoadout l = MobSkillLoadout.fromJson(JsonParser.parseString(json).getAsJsonObject());
        assertEquals(100, l.entries().get(0).weight());
    }

    @Test
    void parseWithoutGlobalCooldown_defaultsTo20() {
        String json = """
            {
              "entity_type": "minecraft:zombie",
              "skills": []
            }
            """;
        MobSkillLoadout l = MobSkillLoadout.fromJson(JsonParser.parseString(json).getAsJsonObject());
        assertEquals(20, l.globalCooldownTicks());
    }
}
```

- [ ] **Step 3.2 : Créer `MobSkillLoadout.java`**

```java
package tong.statmod.combat.skills;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record MobSkillLoadout(
    ResourceLocation entityType,
    int globalCooldownTicks,
    List<Entry> entries
) {

    public record Entry(ResourceLocation skillId, int weight) {}

    public static MobSkillLoadout fromJson(JsonObject obj) {
        ResourceLocation entityType = new ResourceLocation(obj.get("entity_type").getAsString());
        int gcd = obj.has("global_cooldown_ticks") ? obj.get("global_cooldown_ticks").getAsInt() : 20;
        List<Entry> entries = new ArrayList<>();
        if (obj.has("skills")) {
            JsonArray arr = obj.getAsJsonArray("skills");
            for (var el : arr) {
                JsonObject e = el.getAsJsonObject();
                ResourceLocation id = new ResourceLocation(e.get("id").getAsString());
                int weight = e.has("weight") ? e.get("weight").getAsInt() : 100;
                entries.add(new Entry(id, weight));
            }
        }
        return new MobSkillLoadout(entityType, gcd, List.copyOf(entries));
    }
}
```

- [ ] **Step 3.3 : Créer `MobSkillReloadListener.java`**

```java
package tong.statmod.reload;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import tong.statmod.STATMod;
import tong.statmod.combat.skills.MobSkillLoadout;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MobSkillReloadListener extends SimpleJsonResourceReloadListener {

    public static final MobSkillReloadListener INSTANCE = new MobSkillReloadListener();

    private final Map<ResourceLocation, MobSkillLoadout> loadouts = new HashMap<>();

    private MobSkillReloadListener() {
        super(new GsonBuilder().create(), "mob_skills");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects,
                         ResourceManager manager, ProfilerFiller profiler) {
        loadouts.clear();
        for (var entry : objects.entrySet()) {
            try {
                JsonObject obj = entry.getValue().getAsJsonObject();
                MobSkillLoadout loadout = MobSkillLoadout.fromJson(obj);
                loadouts.put(loadout.entityType(), loadout);
            } catch (Exception e) {
                STATMod.LOGGER.error("[MobSkills] Failed to load {}", entry.getKey(), e);
            }
        }
        STATMod.LOGGER.info("[MobSkills] Loaded {} mob skill loadouts", loadouts.size());
    }

    public Optional<MobSkillLoadout> get(ResourceLocation entityType) {
        return Optional.ofNullable(loadouts.get(entityType));
    }
}
```

- [ ] **Step 3.4 : Tests + commit**

```
gradlew.bat test --tests tong.statmod.combat.skills.MobSkillLoadoutTest
gradlew.bat build
```

```bash
git add src/main/java/tong/statmod/combat/skills/MobSkillLoadout.java \
        src/main/java/tong/statmod/reload/MobSkillReloadListener.java \
        src/test/java/tong/statmod/combat/skills/MobSkillLoadoutTest.java
git commit -m "feat(phase2): add MobSkillLoadout + JSON reload listener"
```

---

## Task 4 — `MobSkillTickHandler` + Config

**Fichiers :**
- Créer : `src/main/java/tong/statmod/combat/MobSkillTickHandler.java`
- Modifier : `src/main/java/tong/statmod/Config.java`

- [ ] **Step 4.1 : Ajouter 3 entries dans `Config.java`**

Insérer avant `static final ForgeConfigSpec SPEC = BUILDER.build();` :

```java
    // ── Mob Skills (Phase 2) ──
    public static final ForgeConfigSpec.BooleanValue MOB_SKILLS_ENABLED = BUILDER
        .comment("Enable Phase 2 mob skill engine. False = mobs use only Phase 1 stat bonuses.")
        .define("mobSkillsEnabled", true);

    public static final ForgeConfigSpec.DoubleValue MOB_SKILL_COOLDOWN_MULT = BUILDER
        .comment("Global cooldown multiplier on all mob skills (lower = mobs cast more often)")
        .defineInRange("mobSkillCooldownMult", 1.0, 0.1, 5.0);

    public static final ForgeConfigSpec.IntValue MOB_SKILL_TICK_INTERVAL = BUILDER
        .comment("How often the mob skill tick handler runs, in ticks (10 = twice per second)")
        .defineInRange("mobSkillTickInterval", 10, 1, 200);
```

Cached primitives + assignment dans `onLoad` :

```java
public static boolean mobSkillsEnabled;
public static double mobSkillCooldownMult;
public static int mobSkillTickInterval;

// in onLoad():
mobSkillsEnabled = MOB_SKILLS_ENABLED.get();
mobSkillCooldownMult = MOB_SKILL_COOLDOWN_MULT.get();
mobSkillTickInterval = MOB_SKILL_TICK_INTERVAL.get();
```

- [ ] **Step 4.2 : Créer `MobSkillTickHandler.java`**

```java
package tong.statmod.combat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import tong.statmod.Config;
import tong.statmod.STATMod;
import tong.statmod.capability.MobSkillState;
import tong.statmod.capability.MobSkillStateProvider;
import tong.statmod.capability.MobStats;
import tong.statmod.capability.MobStatsProvider;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.combat.skills.MobSkillLoadout;
import tong.statmod.combat.skills.MobSkillRegistry;
import tong.statmod.reload.MobSkillReloadListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class MobSkillTickHandler {

    private static final Random RNG = new Random();
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Config.mobSkillsEnabled) return;

        tickCounter++;
        if (tickCounter < Config.mobSkillTickInterval) return;
        tickCounter = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, mob -> true)) {
                tickMob(mob);
            }
        }
    }

    private static void tickMob(Mob mob) {
        if (!mob.isAlive()) return;

        MobStats stats = mob.getCapability(MobStatsProvider.MOB_STATS).orElse(null);
        if (stats == null) return;

        MobSkillState state = mob.getCapability(MobSkillStateProvider.MOB_SKILL_STATE).orElse(null);
        if (state == null) return;

        // Regen 1 mana per tick interval (~2/s at default settings).
        state.addMana(1);

        long now = mob.level().getGameTime();
        if (state.getGlobalCooldownEndTick() > now) return;

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (entityType == null) return;

        MobSkillLoadout loadout = MobSkillReloadListener.INSTANCE.get(entityType).orElse(null);
        if (loadout == null || loadout.entries().isEmpty()) return;

        List<Candidate> castable = new ArrayList<>();
        int totalWeight = 0;
        for (var entry : loadout.entries()) {
            MobSkill skill = MobSkillRegistry.get(entry.skillId());
            if (skill == null) continue;
            if (skill.isReactive()) continue;
            if (stats.getLevel(skill.requiredStat().index) < skill.requiredLevel()) continue;
            if (state.getMana() < skill.manaCost()) continue;
            if (state.getCooldownEndTick(skill.id()) > now) continue;
            if (mob.distanceTo(target) > skill.maxRange()) continue;
            if (!skill.canExecute(mob, target, stats)) continue;
            castable.add(new Candidate(skill, entry.weight()));
            totalWeight += entry.weight();
        }
        if (castable.isEmpty()) return;

        int pick = RNG.nextInt(totalWeight);
        MobSkill chosen = null;
        int acc = 0;
        for (var c : castable) {
            acc += c.weight;
            if (pick < acc) { chosen = c.skill; break; }
        }
        if (chosen == null) chosen = castable.get(0).skill;

        try {
            chosen.execute(mob, target, stats);
        } catch (Exception e) {
            STATMod.LOGGER.error("[MobSkills] {} crashed during execute on {}",
                chosen.id(), mob.getType(), e);
            return;
        }

        long cd = Math.round(chosen.baseCooldownTicks() * Config.mobSkillCooldownMult);
        state.setCooldownEndTick(chosen.id(), now + cd);
        state.setMana(state.getMana() - chosen.manaCost());
        state.setGlobalCooldownEndTick(now + loadout.globalCooldownTicks());
    }

    private record Candidate(MobSkill skill, int weight) {}
}
```

- [ ] **Step 4.3 : Build + commit**

```
gradlew.bat build
```

```bash
git add src/main/java/tong/statmod/combat/MobSkillTickHandler.java src/main/java/tong/statmod/Config.java
git commit -m "feat(phase2): add MobSkillTickHandler + Config entries"
```

---

## Task 5 — 4 skills mêlée + réactif

**Fichiers :**
- Créer : `src/main/java/tong/statmod/combat/skills/impl/ChargeSkill.java`
- Créer : `src/main/java/tong/statmod/combat/skills/impl/WhirlwindSkill.java`
- Créer : `src/main/java/tong/statmod/combat/skills/impl/LungeSkill.java`
- Créer : `src/main/java/tong/statmod/combat/skills/impl/ReflectSkill.java`
- Créer : `src/main/java/tong/statmod/combat/ReflectSkillHandler.java` (event handler pour Reflect)

- [ ] **Step 5.1 : `ChargeSkill`**

```java
package tong.statmod.combat.skills.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import tong.statmod.STATMod;
import tong.statmod.capability.MobStats;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.stats.StatType;

public class ChargeSkill implements MobSkill {

    public static final ResourceLocation ID = new ResourceLocation(STATMod.MODID, "charge");

    @Override public ResourceLocation id() { return ID; }
    @Override public StatType requiredStat() { return StatType.BRUTE_FORCE; }
    @Override public int requiredLevel() { return 30; }
    @Override public int baseCooldownTicks() { return 200; }
    @Override public int manaCost() { return 8; }
    @Override public double maxRange() { return 12; }

    @Override
    public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) {
        return mob.hasLineOfSight(target);
    }

    @Override
    public void execute(Mob mob, LivingEntity target, MobStats stats) {
        Vec3 toTarget = target.position().subtract(mob.position()).normalize();
        double speed = mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * 6.0;
        mob.setDeltaMovement(toTarget.x * speed, 0.3, toTarget.z * speed);
        mob.hurtMarked = true;

        mob.level().playSound(null, mob.blockPosition(),
            SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0f, 1.0f);

        // Damage on contact handled by vanilla AI; bonus knockback applied here.
        double damageBonus = stats.getLevel(StatType.BRUTE_FORCE.index) * 0.05;
        if (mob.distanceTo(target) < 2.5) {
            target.hurt(mob.damageSources().mobAttack(mob), (float) damageBonus);
            target.knockback(2.0, -toTarget.x, -toTarget.z);
        }
    }
}
```

- [ ] **Step 5.2 : `WhirlwindSkill`**

```java
package tong.statmod.combat.skills.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import tong.statmod.STATMod;
import tong.statmod.capability.MobStats;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.stats.StatType;

public class WhirlwindSkill implements MobSkill {

    public static final ResourceLocation ID = new ResourceLocation(STATMod.MODID, "whirlwind");

    @Override public ResourceLocation id() { return ID; }
    @Override public StatType requiredStat() { return StatType.BLADE_TECHNIQUE; }
    @Override public int requiredLevel() { return 40; }
    @Override public int baseCooldownTicks() { return 240; }
    @Override public int manaCost() { return 12; }
    @Override public double maxRange() { return 4; }

    @Override public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) { return true; }

    @Override
    public void execute(Mob mob, LivingEntity target, MobStats stats) {
        double base = mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
        AABB box = mob.getBoundingBox().inflate(4.0);
        for (Player p : mob.level().getEntitiesOfClass(Player.class, box)) {
            p.hurt(mob.damageSources().mobAttack(mob), (float) (base * 1.5));
        }
        mob.level().playSound(null, mob.blockPosition(),
            SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.HOSTILE, 1.2f, 0.8f);
    }
}
```

- [ ] **Step 5.3 : `LungeSkill`**

```java
package tong.statmod.combat.skills.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import tong.statmod.STATMod;
import tong.statmod.capability.MobStats;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.stats.StatType;

public class LungeSkill implements MobSkill {

    public static final ResourceLocation ID = new ResourceLocation(STATMod.MODID, "lunge");

    @Override public ResourceLocation id() { return ID; }
    @Override public StatType requiredStat() { return StatType.RAPIDITE; }
    @Override public int requiredLevel() { return 35; }
    @Override public int baseCooldownTicks() { return 100; }
    @Override public int manaCost() { return 5; }
    @Override public double maxRange() { return 10; }

    @Override public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) { return mob.onGround(); }

    @Override
    public void execute(Mob mob, LivingEntity target, MobStats stats) {
        Vec3 toTarget = target.position().subtract(mob.position()).normalize();
        mob.setDeltaMovement(toTarget.x * 1.2, 0.6, toTarget.z * 1.2);
        mob.hurtMarked = true;
        mob.level().playSound(null, mob.blockPosition(),
            SoundEvents.WOLF_GROWL, SoundSource.HOSTILE, 0.8f, 1.4f);
    }
}
```

- [ ] **Step 5.4 : `ReflectSkill`**

```java
package tong.statmod.combat.skills.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import tong.statmod.STATMod;
import tong.statmod.capability.MobStats;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.stats.StatType;

public class ReflectSkill implements MobSkill {

    public static final ResourceLocation ID = new ResourceLocation(STATMod.MODID, "reflect");
    public static final float REFLECT_FRACTION = 0.30f;

    @Override public ResourceLocation id() { return ID; }
    @Override public StatType requiredStat() { return StatType.PHYSICAL_RESISTANCE; }
    @Override public int requiredLevel() { return 50; }
    @Override public int baseCooldownTicks() { return 40; }
    @Override public int manaCost() { return 0; }
    @Override public double maxRange() { return 0; }
    @Override public boolean isReactive() { return true; }

    @Override public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) { return false; }
    @Override public void execute(Mob mob, LivingEntity target, MobStats stats) { /* triggered by ReflectSkillHandler */ }
}
```

- [ ] **Step 5.5 : `ReflectSkillHandler`**

```java
package tong.statmod.combat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import tong.statmod.Config;
import tong.statmod.STATMod;
import tong.statmod.capability.MobSkillState;
import tong.statmod.capability.MobSkillStateProvider;
import tong.statmod.capability.MobStats;
import tong.statmod.capability.MobStatsProvider;
import tong.statmod.combat.skills.impl.ReflectSkill;
import tong.statmod.reload.MobSkillReloadListener;
import tong.statmod.stats.StatType;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class ReflectSkillHandler {

    @SubscribeEvent
    public static void onMobHurt(LivingHurtEvent event) {
        if (!Config.mobSkillsEnabled) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        MobStats stats = mob.getCapability(MobStatsProvider.MOB_STATS).orElse(null);
        if (stats == null) return;
        if (stats.getLevel(StatType.PHYSICAL_RESISTANCE.index) < 50) return;

        ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (entityType == null) return;

        var loadout = MobSkillReloadListener.INSTANCE.get(entityType).orElse(null);
        if (loadout == null) return;
        boolean hasReflect = loadout.entries().stream()
            .anyMatch(e -> e.skillId().equals(ReflectSkill.ID));
        if (!hasReflect) return;

        MobSkillState state = mob.getCapability(MobSkillStateProvider.MOB_SKILL_STATE).orElse(null);
        if (state == null) return;

        long now = mob.level().getGameTime();
        if (state.getCooldownEndTick(ReflectSkill.ID) > now) return;

        float reflected = event.getAmount() * ReflectSkill.REFLECT_FRACTION;
        attacker.hurt(attacker.damageSources().thorns(mob), reflected);
        state.setCooldownEndTick(ReflectSkill.ID, now + 40);
    }
}
```

- [ ] **Step 5.6 : Build + commit**

```
gradlew.bat build
```

```bash
git add src/main/java/tong/statmod/combat/skills/impl/ChargeSkill.java \
        src/main/java/tong/statmod/combat/skills/impl/WhirlwindSkill.java \
        src/main/java/tong/statmod/combat/skills/impl/LungeSkill.java \
        src/main/java/tong/statmod/combat/skills/impl/ReflectSkill.java \
        src/main/java/tong/statmod/combat/ReflectSkillHandler.java
git commit -m "feat(phase2): 4 mêlée/reactive skills (Charge, Whirlwind, Lunge, Reflect)"
```

---

## Task 6 — 4 skills magie / mental

**Fichiers :**
- Créer : `src/main/java/tong/statmod/combat/skills/impl/FireballSkill.java`
- Créer : `src/main/java/tong/statmod/combat/skills/impl/CurseSkill.java`
- Créer : `src/main/java/tong/statmod/combat/skills/impl/MagicMissileSkill.java`
- Créer : `src/main/java/tong/statmod/combat/skills/impl/BattleCrySkill.java`

- [ ] **Step 6.1 : `FireballSkill`**

```java
package tong.statmod.combat.skills.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.Vec3;
import tong.statmod.STATMod;
import tong.statmod.capability.MobStats;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.stats.StatType;

public class FireballSkill implements MobSkill {

    public static final ResourceLocation ID = new ResourceLocation(STATMod.MODID, "fireball");

    @Override public ResourceLocation id() { return ID; }
    @Override public StatType requiredStat() { return StatType.FIRE_AFFINITY; }
    @Override public int requiredLevel() { return 40; }
    @Override public int baseCooldownTicks() { return 150; }
    @Override public int manaCost() { return 10; }
    @Override public double maxRange() { return 20; }
    @Override public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) { return mob.hasLineOfSight(target); }

    @Override
    public void execute(Mob mob, LivingEntity target, MobStats stats) {
        Vec3 dir = target.position().add(0, target.getBbHeight() / 2, 0)
            .subtract(mob.position().add(0, mob.getBbHeight() / 2, 0)).normalize();
        SmallFireball fb = new SmallFireball(mob.level(), mob, dir.x, dir.y, dir.z);
        fb.setPos(mob.getX(), mob.getY() + mob.getBbHeight() * 0.7, mob.getZ());
        mob.level().addFreshEntity(fb);
        mob.level().playSound(null, mob.blockPosition(),
            SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0f, 1.0f);
    }
}
```

- [ ] **Step 6.2 : `CurseSkill`**

```java
package tong.statmod.combat.skills.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import tong.statmod.STATMod;
import tong.statmod.capability.MobStats;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.stats.StatType;

public class CurseSkill implements MobSkill {

    public static final ResourceLocation ID = new ResourceLocation(STATMod.MODID, "curse");

    @Override public ResourceLocation id() { return ID; }
    @Override public StatType requiredStat() { return StatType.ARCANE_POWER; }
    @Override public int requiredLevel() { return 45; }
    @Override public int baseCooldownTicks() { return 300; }
    @Override public int manaCost() { return 15; }
    @Override public double maxRange() { return 16; }
    @Override public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) { return true; }

    @Override
    public void execute(Mob mob, LivingEntity target, MobStats stats) {
        AABB box = new AABB(target.blockPosition()).inflate(3.0);
        for (Player p : mob.level().getEntitiesOfClass(Player.class, box)) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1));
        }
        mob.level().playSound(null, mob.blockPosition(),
            SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 1.0f, 0.9f);
    }
}
```

- [ ] **Step 6.3 : `MagicMissileSkill`**

```java
package tong.statmod.combat.skills.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import tong.statmod.STATMod;
import tong.statmod.capability.MobStats;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.stats.StatType;

public class MagicMissileSkill implements MobSkill {

    public static final ResourceLocation ID = new ResourceLocation(STATMod.MODID, "magic_missile");

    @Override public ResourceLocation id() { return ID; }
    @Override public StatType requiredStat() { return StatType.CASTING_SPEED; }
    @Override public int requiredLevel() { return 30; }
    @Override public int baseCooldownTicks() { return 80; }
    @Override public int manaCost() { return 6; }
    @Override public double maxRange() { return 24; }
    @Override public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) { return mob.hasLineOfSight(target); }

    @Override
    public void execute(Mob mob, LivingEntity target, MobStats stats) {
        // Direct hit: magic damage that ignores armor.
        float damage = stats.getLevel(StatType.CASTING_SPEED.index) * 0.06f;
        target.hurt(mob.damageSources().magic(), damage);
        mob.level().playSound(null, mob.blockPosition(),
            SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 0.8f, 1.2f);
    }
}
```

> Implementation note: a true homing projectile would be a separate Entity type. For Phase 2, we apply the magic-damage hit directly to keep scope small. Upgrade to a projectile in Phase 3 if needed.

- [ ] **Step 6.4 : `BattleCrySkill`**

```java
package tong.statmod.combat.skills.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import tong.statmod.STATMod;
import tong.statmod.capability.MobStats;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.stats.StatType;

public class BattleCrySkill implements MobSkill {

    public static final ResourceLocation ID = new ResourceLocation(STATMod.MODID, "battle_cry");

    @Override public ResourceLocation id() { return ID; }
    @Override public StatType requiredStat() { return StatType.INTIMIDATION; }
    @Override public int requiredLevel() { return 40; }
    @Override public int baseCooldownTicks() { return 400; }
    @Override public int manaCost() { return 8; }
    @Override public double maxRange() { return 8; }
    @Override public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) { return true; }

    @Override
    public void execute(Mob mob, LivingEntity target, MobStats stats) {
        AABB box = mob.getBoundingBox().inflate(8.0);
        for (Player p : mob.level().getEntitiesOfClass(Player.class, box)) {
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 0));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
        }
        mob.level().playSound(null, mob.blockPosition(),
            SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5f, 0.7f);
    }
}
```

- [ ] **Step 6.5 : Build + commit**

```bash
gradlew.bat build
git add src/main/java/tong/statmod/combat/skills/impl/FireballSkill.java \
        src/main/java/tong/statmod/combat/skills/impl/CurseSkill.java \
        src/main/java/tong/statmod/combat/skills/impl/MagicMissileSkill.java \
        src/main/java/tong/statmod/combat/skills/impl/BattleCrySkill.java
git commit -m "feat(phase2): 4 magic/mental skills (Fireball, Curse, MagicMissile, BattleCry)"
```

---

## Task 7 — JSON loadouts vanilla mobs

**Fichiers :** 8 fichiers JSON dans `src/main/resources/data/statmod/mob_skills/minecraft/`

- [ ] **Step 7.1 : zombie.json** → charge
- [ ] **Step 7.2 : wither_skeleton.json** → charge + whirlwind + reflect
- [ ] **Step 7.3 : spider.json** → lunge
- [ ] **Step 7.4 : pillager.json** → lunge
- [ ] **Step 7.5 : witch.json** → curse + magic_missile
- [ ] **Step 7.6 : evoker.json** → curse + magic_missile + battle_cry
- [ ] **Step 7.7 : blaze.json** → fireball
- [ ] **Step 7.8 : wither.json** → fireball + battle_cry

Format type :
```json
{
  "entity_type": "minecraft:zombie",
  "global_cooldown_ticks": 60,
  "skills": [
    { "id": "statmod:charge", "weight": 100 }
  ]
}
```

> Note pour les autres mobs (ravager, enderman, elder_guardian) : on les profile dans Phase 3 quand on aura aussi des skills propres à eux. Pour Phase 2 on se limite aux 8 mobs explicites.

- [ ] **Step 7.9 : Build + commit**

```bash
git add src/main/resources/data/statmod/mob_skills/
git commit -m "feat(phase2): 8 JSON skill loadouts for vanilla mobs"
```

---

## Task 8 — Wiring : CapabilityHandler + STATMod

**Fichiers :**
- Modifier : `src/main/java/tong/statmod/capability/CapabilityHandler.java`
- Modifier : `src/main/java/tong/statmod/STATMod.java`

- [ ] **Step 8.1 : `CapabilityHandler.java`**

Dans `registerCapabilities`, ajouter :
```java
event.register(MobSkillState.class);
```

Dans `attachCapabilities`, dans le bloc `instanceof Mob`, ajouter :
```java
event.addCapability(
    ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "mob_skill_state"),
    new MobSkillStateProvider());
```

- [ ] **Step 8.2 : `STATMod.java`** — registry init + reload listener

Dans `commonSetup`, ajouter (avant le bloc L2H) :

```java
// Register all hardcoded mob skills
MobSkillRegistry.register(new ChargeSkill());
MobSkillRegistry.register(new WhirlwindSkill());
MobSkillRegistry.register(new LungeSkill());
MobSkillRegistry.register(new ReflectSkill());
MobSkillRegistry.register(new FireballSkill());
MobSkillRegistry.register(new CurseSkill());
MobSkillRegistry.register(new MagicMissileSkill());
MobSkillRegistry.register(new BattleCrySkill());
LOGGER.info("[MobSkills] Registered {} skills", MobSkillRegistry.values().size());
```

Dans `onAddReloadListeners` :
```java
event.addListener(MobSkillReloadListener.INSTANCE);
```

- [ ] **Step 8.3 : Build + commit**

```bash
gradlew.bat build
git add src/main/java/tong/statmod/capability/CapabilityHandler.java src/main/java/tong/statmod/STATMod.java
git commit -m "feat(phase2): wire MobSkill engine into CapabilityHandler + STATMod"
```

---

## Task 9 — L2HostilityMobSkillSync (optionnel)

**Fichiers :**
- Créer : `src/main/java/tong/statmod/integration/L2HostilityMobSkillSync.java`
- Modifier : `src/main/java/tong/statmod/STATMod.java`

- [ ] **Step 9.1 : `L2HostilityMobSkillSync.java`**

```java
package tong.statmod.integration;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import tong.statmod.STATMod;
import tong.statmod.combat.skills.MobSkillLoadout;
import tong.statmod.combat.skills.impl.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Augments a mob's loadout with extra skill entries unlocked by L2H traits.
 * Loaded only when l2hostility is present.
 */
public class L2HostilityMobSkillSync {

    public static MobSkillLoadout augment(Mob mob, MobSkillLoadout base) {
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        if (cap == null) return base;

        List<MobSkillLoadout.Entry> entries = new ArrayList<>(base.entries());
        for (MobTrait trait : cap.traits.keySet()) {
            ResourceLocation id = trait.getRegistryName();
            if (id == null) continue;
            switch (id.getPath()) {
                case "aura"        -> entries.add(new MobSkillLoadout.Entry(BattleCrySkill.ID, 60));
                case "killer_aura" -> entries.add(new MobSkillLoadout.Entry(MagicMissileSkill.ID, 80));
                case "fiery"       -> entries.add(new MobSkillLoadout.Entry(FireballSkill.ID, 70));
                case "gravity"     -> entries.add(new MobSkillLoadout.Entry(ChargeSkill.ID, 60));
                case "dispell"     -> entries.add(new MobSkillLoadout.Entry(CurseSkill.ID, 70));
                case "master"      -> {
                    entries.add(new MobSkillLoadout.Entry(BattleCrySkill.ID, 50));
                    entries.add(new MobSkillLoadout.Entry(MagicMissileSkill.ID, 50));
                    entries.add(new MobSkillLoadout.Entry(FireballSkill.ID, 50));
                }
                default -> {}
            }
        }
        if (entries.size() == base.entries().size()) return base;
        return new MobSkillLoadout(base.entityType(), base.globalCooldownTicks(), List.copyOf(entries));
    }
}
```

- [ ] **Step 9.2 : Modifier `MobSkillTickHandler.tickMob`**

Remplacer la ligne `MobSkillLoadout loadout = MobSkillReloadListener.INSTANCE.get(entityType).orElse(null);` par :

```java
MobSkillLoadout loadout = MobSkillReloadListener.INSTANCE.get(entityType).orElse(null);
if (loadout == null) {
    if (!net.minecraftforge.fml.ModList.get().isLoaded("l2hostility")) return;
    loadout = new MobSkillLoadout(entityType, 20, java.util.List.of());
}
if (net.minecraftforge.fml.ModList.get().isLoaded("l2hostility")) {
    loadout = tong.statmod.integration.L2HostilityMobSkillSync.augment(mob, loadout);
}
if (loadout.entries().isEmpty()) return;
```

> Note pour `L2HostilityMobSkillSync.augment` : la méthode contient une référence à `MobTraitCap`. Elle sera chargée par le JVM seulement quand le code y arrive, ce qui n'a lieu que dans la branche `ModList.isLoaded("l2hostility")`. Pattern déjà validé en Phase 1.

- [ ] **Step 9.3 : Build + commit**

```bash
gradlew.bat build
git add src/main/java/tong/statmod/integration/L2HostilityMobSkillSync.java src/main/java/tong/statmod/combat/MobSkillTickHandler.java
git commit -m "feat(phase2): L2HostilityMobSkillSync — L2H traits unlock extra skills"
```

---

## Task 10 — ARCHITECTURE.md

**Fichier :** `ARCHITECTURE.md`

- [ ] **Step 10.1 : Ajouter la section Mob Skills Engine dans le graphe**

Sous `Mob Stats System (Phase 1)`, ajouter un bloc :

```
├── Mob Skills Engine (Phase 2)
│   ├── MobSkill (interface) + MobSkillRegistry (8 hardcoded)
│   ├── MobSkillState (capability — cooldowns + mana NBT)
│   ├── MobSkillLoadout + MobSkillReloadListener (data/statmod/mob_skills/)
│   ├── MobSkillTickHandler (server tick driver, 10-tick interval)
│   ├── ReflectSkillHandler (reactive via LivingHurtEvent)
│   └── 8 skills: Charge, Whirlwind, Lunge, Reflect, Fireball, Curse, MagicMissile, BattleCry
```

Et sous `integration/`, ajouter :
```
│   ├── L2HostilityMobSkillSync — L2H traits unlock extra skills (PHASE 2)
```

Update extension points :
```
7. **Add a mob skill**: implement `MobSkill`, register via `MobSkillRegistry.register(...)` in `STATMod.commonSetup()` or addon plugin
8. **Add a mob skill loadout**: drop a JSON in `data/statmod/mob_skills/<ns>/<mob>.json`
```

- [ ] **Step 10.2 : Commit**

```bash
git add ARCHITECTURE.md
git commit -m "docs(phase2): update ARCHITECTURE.md with Mob Skills Engine"
```

---

## Critères de succès Phase 2

- [x] `BUILD SUCCESSFUL` avec et sans L2H deps compileOnly
- [x] Tests unitaires passent (au moins T1, T2, T3)
- [x] Sans L2H, en jeu : un zombie de niveau player ≥ 30 charge le joueur (visuel + son)
- [x] Sans L2H, en jeu : une witch lance Curse, le joueur prend WEAKNESS + SLOWNESS
- [x] Sans L2H, en jeu : un Blaze tire des Fireball plus fréquemment
- [x] `mobSkillsEnabled=false` dans config → aucun cast
- [x] Avec L2H, en jeu : zombie avec trait `gravity` → ajoute Charge à son loadout même si déjà présent (poids cumulé)

**Hors scope (Phase 3+) :**
- Animations Epic Fight pour mobs
- UI telegraph/cast bar côté joueur
- Adaptive difficulty
- Commande de debug `/statmod debug cast`
- Particules custom (réutilise les vanilla ANGRY_VILLAGER, SMOKE, etc.)
