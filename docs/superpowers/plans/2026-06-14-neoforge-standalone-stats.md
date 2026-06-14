# Standalone Stats System — NeoForge 1.21.1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port STAT_MOD from Forge 1.20.1 + Epic Fight to NeoForge 1.21.1 standalone (stats, XP, leveling, 84 perks).

**Architecture:** NeoForge attachments for player data, vanilla Minecraft events for stat effects, custom payloads for networking, and the existing 84-perk system ported with Epic Fight coupling removed.

**Tech Stack:** NeoForge 1.21.1, JDK 21, Mojang mappings, NeoGradle 7+

---

## File Structure

```
src/main/java/tong/statmod/
├── STATMod.java                    ← @Mod entry point
├── storage/
│   ├── PlayerStatData.java         ← Player stat storage (levels, xp, perk points)
│   └── ModAttachments.java         ← AttachmentType registration
├── stats/
│   ├── StatType.java               ← Enum 22 stats
│   ├── StatEffectApplier.java      ← Apply stat effects via vanilla events
│   └── StatCommands.java           ← /stats, /level, /respec
├── progression/
│   └── LevelUpHandler.java         ← Milestones, perk point grants
├── perks/
│   ├── PerkTier.java               ← 6-tier enum
│   ├── Perk.java                   ← 84 perk entries
│   ├── PerkManager.java            ← Unlock logic, per-stat points
│   ├── PerkState.java              ← Runtime tracking maps
│   └── PerkEffectHandler.java      ← 84 perk effects via events
├── network/
│   ├── SyncPerksPayload.java       ← Sync perk IDs + points to client
│   ├── UnlockPerkPayload.java      ← Client→Server unlock request
│   ├── BatchSyncPayload.java       ← Full data sync on login
│   └── NetworkHandler.java         ← Payload registration
├── client/
│   ├── ClientStatCache.java        ← Client-side stat cache
│   ├── ClientPerkCache.java        ← Client-side perk cache
│   └── gui/
│       ├── PerkScreen.java         ← Main perk GUI
│       ├── perks/
│       │   ├── PerkNodeWidget.java ← Single perk node renderer
│       │   └── TalentTreePanel.java← 6-node stat tree panel
│       └── StatsOverviewScreen.java← Stats list screen
└── mixin/
    └── PlayerListMixin.java        ← Sync data on player join
```

```build.gradle```, ```gradle.properties```, ```settings.gradle``` at root.

---

### Task 1: Scaffold NeoForge Project

**Files:**
- Create: `build.gradle`
- Create: `gradle.properties`
- Create: `settings.gradle`
- Create: `src/main/resources/META-INF/neoforge.mods.toml`
- Create: `src/main/resources/pack.mcmeta`

- [ ] **Step 1: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx3G
org.gradle.daemon=false

minecraft_version=1.21.1
minecraft_version_range=[1.21.1,1.22)
neo_version=21.1.133
neo_version_range=[21.1,)
loader_version_range=[4,)

mod_id=statmod
mod_name=STAT Mod
mod_license=All Rights Reserved
mod_version=1.0.0
mod_group_id=tong.statmod
mod_authors=ela_juff
mod_description=Standalone stat system with XP, leveling, and 84 perks.
```

- [ ] **Step 2: Create `settings.gradle`**

```groovy
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven { url = 'https://maven.neoforged.net/releases' }
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
}
```

- [ ] **Step 3: Create `build.gradle`**

```groovy
plugins {
    id 'java-library'
    id 'eclipse'
    id 'idea'
    id 'maven-publish'
    id 'net.neoforged.gradle.userdev' version '7.0.173'
}

version = mod_version
group = mod_group_id

base {
    archivesName = mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

println "Java: ${System.getProperty 'java.version'}, JVM: ${System.getProperty 'java.vm.version'} (${System.getProperty 'java.vendor'}), Arch: ${System.getProperty 'os.arch'}"

runs {
    configureEach {
        systemProperty 'forge.logging.markers', 'REGISTRIES'
        systemProperty 'forge.logging.console.level', 'debug'
        modSource project.sourceSets.main
    }

    client {
        systemProperty 'forge.enabledGameTestNamespaces', mod_id
    }

    server {
        systemProperty 'forge.enabledGameTestNamespaces', mod_id
        programArgument '--nogui'
    }

    gameTestServer {
        systemProperty 'forge.enabledGameTestNamespaces', mod_id
    }

    data {
        programArguments.addAll '--mod', mod_id, '--all', '--output', file('src/generated/resources/').absolutePath, '--existing', file('src/main/resources/').absolutePath
    }
}

sourceSets.main.resources { srcDir 'src/generated/resources' }

repositories {
    mavenLocal()
    maven { url = 'https://maven.neoforged.net/releases' }
}

dependencies {
    implementation "net.neoforged:neoforge:${neo_version}"
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
}

tasks.named('processResources', ProcessResources).configure {
    var replaceProperties = [
            minecraft_version: minecraft_version, minecraft_version_range: minecraft_version_range,
            neo_version: neo_version, neo_version_range: neo_version_range,
            loader_version_range: loader_version_range,
            mod_id: mod_id, mod_name: mod_name, mod_license: mod_license, mod_version: mod_version,
            mod_authors: mod_authors, mod_description: mod_description,
    ]
    inputs.properties replaceProperties

    filesMatching(['META-INF/neoforge.mods.toml', 'pack.mcmeta']) {
        expand replaceProperties + [project: project]
    }
}

tasks.named('jar', Jar).configure {
    manifest {
        attributes([
                'Specification-Title'     : mod_id,
                'Specification-Vendor'    : mod_authors,
                'Specification-Version'   : '1',
                'Implementation-Title'    : project.name,
                'Implementation-Version'  : project.jar.archiveVersion,
                'Implementation-Vendor'   : mod_authors,
                'Implementation-Timestamp': new Date().format("yyyy-MM-dd'T'HH:mm:ssZ")
        ])
    }
}

publishing {
    publications {
        register('mavenJava', MavenPublication) {
            artifact jar
        }
    }
    repositories {
        maven {
            url "file://${project.projectDir}/mcmodsrepo"
        }
    }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

test {
    useJUnitPlatform()
}
```

- [ ] **Step 4: Create `src/main/resources/META-INF/neoforge.mods.toml`**

```toml
modLoader="javafml"
loaderVersion="${loader_version_range}"
license="${mod_license}"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"
authors="${mod_authors}"
description='''${mod_description}'''

[[dependencies.${mod_id}]]
    modId="minecraft"
    mandatory=true
    versionRange="${minecraft_version_range}"
    ordering="NONE"
    side="BOTH"

[[mixins]]
config = "statmod.mixins.json"
```

- [ ] **Step 5: Create `src/main/resources/pack.mcmeta`**

```json
{
  "pack": {
    "description": {
      "text": "${mod_id} resources"
    },
    "pack_format": 34
  }
}
```

- [ ] **Step 6: Verify project compiles**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL

---

### Task 2: Main Mod Class + Storage Layer

**Files:**
- Create: `src/main/java/tong/statmod/STATMod.java`
- Create: `src/main/java/tong/statmod/storage/PlayerStatData.java`
- Create: `src/main/java/tong/statmod/storage/ModAttachments.java`

- [ ] **Step 1: Create `STATMod.java`**

```java
package tong.statmod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tong.statmod.storage.ModAttachments;

@Mod(STATMod.MODID)
public class STATMod {
    public static final String MODID = "statmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(STATMod.class);

    public STATMod(IEventBus modBus) {
        ModAttachments.ATTACHMENTS.register(modBus);
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("STAT Mod initialized on NeoForge");
    }
}
```

- [ ] **Step 2: Create `PlayerStatData.java`**

```java
package tong.statmod.storage;

import tong.statmod.stats.StatType;

public class PlayerStatData {
    public static final int STAT_COUNT = 23;
    private final int[] levels = new int[STAT_COUNT];
    private final int[] xp = new int[STAT_COUNT];
    private final int[] perkPoints = new int[STAT_COUNT];

    public int[] getLevels() { return levels.clone(); }
    public int[] getXp() { return xp.clone(); }
    public int[] getPerkPoints() { return perkPoints.clone(); }

    public int getLevel(int index) { return index >= 0 && index < STAT_COUNT ? levels[index] : 0; }
    public int getXp(int index) { return index >= 0 && index < STAT_COUNT ? xp[index] : 0; }
    public int getPerkPointsForStat(int index) { return index >= 0 && index < STAT_COUNT ? perkPoints[index] : 0; }

    public void setLevel(int index, int value) { if (index >= 0 && index < STAT_COUNT) levels[index] = value; }
    public void setXp(int index, int value) { if (index >= 0 && index < STAT_COUNT) xp[index] = value; }
    public void setPerkPoints(int index, int value) { if (index >= 0 && index < STAT_COUNT) perkPoints[index] = value; }

    public boolean addXp(int index, int amount) {
        if (index < 0 || index >= STAT_COUNT || amount <= 0) return false;
        xp[index] += amount;
        int required = requiredXp(levels[index]);
        boolean leveledUp = false;
        while (xp[index] >= required && levels[index] < 100) {
            levels[index]++;
            xp[index] -= required;
            required = requiredXp(levels[index]);
            leveledUp = true;
        }
        return leveledUp;
    }

    public void addLevels(int index, int amount) {
        if (index >= 0 && index < STAT_COUNT) {
            levels[index] = Math.min(100, Math.max(0, levels[index] + amount));
        }
    }

    public void addPerkPointsForStat(int index, int amount) {
        if (index >= 0 && index < STAT_COUNT) {
            perkPoints[index] = Math.max(0, perkPoints[index] + amount);
        }
    }

    public int getGlobalLevel() {
        int sum = 0;
        for (int l : levels) sum += l;
        return sum / STAT_COUNT;
    }

    public static int requiredXp(int level) {
        return (level + 1) * (level + 1) * 10;
    }
}
```

- [ ] **Step 3: Create `ModAttachments.java`**

```java
package tong.statmod.storage;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import tong.statmod.STATMod;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, STATMod.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerStatData>> STATS =
            ATTACHMENTS.register("stats", () -> AttachmentType.builder(PlayerStatData::new).build());

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
```

- [ ] **Step 4: Build**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL

---

### Task 3: Stat System

**Files:**
- Create: `src/main/java/tong/statmod/stats/StatType.java`
- Create: `src/main/java/tong/statmod/stats/StatEffectApplier.java`
- Create: `src/main/java/tong/statmod/stats/StatCommands.java`

- [ ] **Step 1: Create `StatType.java`**

```java
package tong.statmod.stats;

public enum StatType {
    BRUTE_FORCE(0, "Brute Force", "Damage bonus for heavy weapons"),
    BLADE_TECHNIQUE(1, "Blade Technique", "Precision and finesse with blades"),
    RAPIDITE(2, "Rapidité", "Attack speed and fluidity"),
    AGILITY(3, "Agility", "Movement speed and evasion"),
    PHYSICAL_RESISTANCE(4, "Physical Resistance", "Incoming damage reduction"),
    PHYSICAL_ENDURANCE(5, "Physical Endurance", "Stamina and absorption"),
    PRECISION(6, "Precision", "Ranged accuracy and critical hits"),
    ARCANE_POWER(7, "Arcane Power", "Raw magical damage"),
    WATER_AFFINITY(8, "Water Affinity", "Water magic effectiveness"),
    EARTH_AFFINITY(9, "Earth Affinity", "Earth magic effectiveness"),
    FIRE_AFFINITY(10, "Fire Affinity", "Fire magic effectiveness"),
    AIR_AFFINITY(11, "Air Affinity", "Air magic effectiveness"),
    MAGIC_RESISTANCE(12, "Magic Resistance", "Magic damage reduction"),
    CASTING_SPEED(13, "Casting Speed", "Faster spell casting"),
    MANA_POOL(14, "Mana Pool", "Maximum mana"),
    ERUDITION(15, "Erudition", "Spell variety and learning"),
    TRACKING(16, "Tracking", "Mob detection and marking"),
    KEEN_SENSES(17, "Keen Senses", "Dodge and perception"),
    FORGING(18, "Forging", "Tool and weapon repair"),
    COOKING(19, "Cooking", "Food saturation"),
    ALCHEMY(20, "Alchemy", "Potion duration"),
    INTIMIDATION(21, "Intimidation", "Bonus damage to marked targets"),
    WILLPOWER(22, "Willpower", "Status effect resistance");

    private static final StatType[] BY_INDEX = new StatType[values().length];
    static {
        for (StatType s : values()) BY_INDEX[s.index] = s;
    }

    public final int index;
    public final String displayName;
    public final String description;

    StatType(int index, String displayName, String description) {
        this.index = index;
        this.displayName = displayName;
        this.description = description;
    }

    public static StatType byIndex(int index) {
        return index >= 0 && index < BY_INDEX.length ? BY_INDEX[index] : null;
    }

    public boolean hasPerks() {
        return index < 7 || index >= 16;
    }
}
```

- [ ] **Step 2: Create `StatEffectApplier.java`** (stub — effects wired later)

```java
package tong.statmod.stats;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import tong.statmod.STATMod;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;
import net.minecraft.world.entity.player.Player;

@EventBusSubscriber(modid = STATMod.MODID)
public class StatEffectApplier {
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player attacker) {
            attacker.getData(ModAttachments.STATS);
        }
        if (event.getEntity() instanceof Player victim) {
            victim.getData(ModAttachments.STATS);
        }
    }
}
```

- [ ] **Step 3: Create `StatCommands.java`** (barebones `/statlevel` command)

```java
package tong.statmod.stats;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import tong.statmod.storage.ModAttachments;

public class StatCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("statlevel")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("index", IntegerArgumentType.integer(0, 22))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> {
                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    if (ctx.getSource().getEntity() instanceof Player player) {
                                        player.getData(ModAttachments.STATS).addLevels(index, amount);
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("Stat level increased"), true);
                                    }
                                    return 1;
                                }))));
    }
}
```

- [ ] **Step 4: Wire commands in `STATMod.java`** (add)

```java
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import tong.statmod.stats.StatCommands;

// In STATMod constructor, after NeoForge.EVENT_BUS.register(this):
NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event ->
        StatCommands.register(event.getDispatcher()));
```

- [ ] **Step 5: Build**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL

---

### Task 4: XP & Leveling System

**Files:**
- Create: `src/main/java/tong/statmod/progression/LevelUpHandler.java`

- [ ] **Step 1: Create `LevelUpHandler.java`**

```java
package tong.statmod.progression;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.player.Player;
import tong.statmod.STATMod;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = STATMod.MODID)
public class LevelUpHandler {
    private static final Set<UUID> milestoneCheckCache = new HashSet<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        int globalLevel = data.getGlobalLevel();

        if (globalLevel > 0 && globalLevel % 10 == 0 && !milestoneCheckCache.contains(player.getUUID())) {
            for (StatType stat : StatType.values()) {
                if (stat.hasPerks()) {
                    data.addPerkPointsForStat(stat.index, 1);
                }
            }
            milestoneCheckCache.add(player.getUUID());
        }

        if (globalLevel % 10 != 0) {
            milestoneCheckCache.remove(player.getUUID());
        }
    }
}
```

- [ ] **Step 2: Build**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL

---

### Task 5: Perk System (Core Types)

**Files:**
- Create: `src/main/java/tong/statmod/perks/PerkTier.java`
- Create: `src/main/java/tong/statmod/perks/Perk.java`
- Create: `src/main/java/tong/statmod/perks/PerkManager.java`
- Create: `src/main/java/tong/statmod/perks/PerkState.java`

- [ ] **Step 1: Create `PerkTier.java`**

```java
package tong.statmod.perks;

public enum PerkTier {
    CORE(10, 1),
    ACTIVE(25, 1),
    SYNERGY(40, 2),
    SITUATIONAL(55, 1),
    MASTERY(75, 2),
    TRANSCENDENCE(95, 2);

    public final int requiredStatLevel;
    public final int cost;

    PerkTier(int requiredStatLevel, int cost) {
        this.requiredStatLevel = requiredStatLevel;
        this.cost = cost;
    }
}
```

- [ ] **Step 2: Create `Perk.java`** (full 84 entries)

```java
package tong.statmod.perks;

import tong.statmod.stats.StatType;

public enum Perk {
    BRUTE_CORE(0, StatType.BRUTE_FORCE, PerkTier.CORE, "Heavy Hitter", "+5% damage with axes and clubs"),
    BRUTE_ACTIVE(1, StatType.BRUTE_FORCE, PerkTier.ACTIVE, "Mighty Swing", "Charged attack deals +10% damage"),
    BRUTE_SYNERGY(2, StatType.BRUTE_FORCE, PerkTier.SYNERGY, "Crushing Force", "Synergy: +15% damage when below 50% HP", StatType.PHYSICAL_ENDURANCE),
    BRUTE_SITUATIONAL(3, StatType.BRUTE_FORCE, PerkTier.SITUATIONAL, "Berserker", "+20% damage when below 30% HP"),
    BRUTE_MASTERY(4, StatType.BRUTE_FORCE, PerkTier.MASTERY, "Colossus", "Attacks stun targets below 50% HP"),
    BRUTE_TRANSCENDENCE(5, StatType.BRUTE_FORCE, PerkTier.TRANSCENDENCE, "Titan's Wrath", "+50% damage, enemies explode on kill"),

    BLADE_CORE(6, StatType.BLADE_TECHNIQUE, PerkTier.CORE, "Sharp Edge", "+5% damage with swords"),
    BLADE_ACTIVE(7, StatType.BLADE_TECHNIQUE, PerkTier.ACTIVE, "Flowing Strike", "Every 3rd hit deals double damage"),
    BLADE_SYNERGY(8, StatType.BLADE_TECHNIQUE, PerkTier.SYNERGY, "Dance of Blades", "Synergy: combo hits grant speed", StatType.AGILITY),
    BLADE_SITUATIONAL(9, StatType.BLADE_TECHNIQUE, PerkTier.SITUATIONAL, "Parry Master", "Blocking reflects 50% damage"),
    BLADE_MASTERY(10, StatType.BLADE_TECHNIQUE, PerkTier.MASTERY, "Blade Storm", "AOE spin attack on kill"),
    BLADE_TRANSCENDENCE(11, StatType.BLADE_TECHNIQUE, PerkTier.TRANSCENDENCE, "One With the Blade", "Every hit is critical for 5s after kill"),

    RAPID_CORE(12, StatType.RAPIDITE, PerkTier.CORE, "Quick Hands", "+5% attack speed"),
    RAPID_ACTIVE(13, StatType.RAPIDITE, PerkTier.ACTIVE, "Double Strike", "10% chance to hit twice"),
    RAPID_SYNERGY(14, StatType.RAPIDITE, PerkTier.SYNERGY, "Blinding Speed", "Synergy: kills grant burst of speed", StatType.AGILITY),
    RAPID_SITUATIONAL(15, StatType.RAPIDITE, PerkTier.SITUATIONAL, "Flurry", "Attacks get faster as combo builds"),
    RAPID_MASTERY(16, StatType.RAPIDITE, PerkTier.MASTERY, "Time Skip", "Slow-motion effect on dodge"),
    RAPID_TRANSCENDENCE(17, StatType.RAPIDITE, PerkTier.TRANSCENDENCE, "Za Warudo", "Stop time for 2s on perfect dodge"),

    AGIL_CORE(18, StatType.AGILITY, PerkTier.CORE, "Light Feet", "+5% movement speed"),
    AGIL_ACTIVE(19, StatType.AGILITY, PerkTier.ACTIVE, "Agile Strikes", "Moving attacks deal +10% damage"),
    AGIL_SYNERGY(20, StatType.AGILITY, PerkTier.SYNERGY, "Wind Walker", "Synergy: dodge grants damage buff", StatType.RAPIDITE),
    AGIL_SITUATIONAL(21, StatType.AGILITY, PerkTier.SITUATIONAL, "Evasion", "20% dodge chance when hit"),
    AGIL_MASTERY(22, StatType.AGILITY, PerkTier.MASTERY, "Spectral Step", "Short teleport on dodge"),
    AGIL_TRANSCENDENCE(23, StatType.AGILITY, PerkTier.TRANSCENDENCE, "Untouchable", "100% dodge for 3s after taking damage"),

    RESIST_CORE(24, StatType.PHYSICAL_RESISTANCE, PerkTier.CORE, "Tough Skin", "-5% incoming damage"),
    RESIST_ACTIVE(25, StatType.PHYSICAL_RESISTANCE, PerkTier.ACTIVE, "Iron Guard", "Blocking reduces damage by 25%"),
    RESIST_SYNERGY(26, StatType.PHYSICAL_RESISTANCE, PerkTier.SYNERGY, "Fortress", "Synergy: armor bonus when still", StatType.PHYSICAL_ENDURANCE),
    RESIST_SITUATIONAL(27, StatType.PHYSICAL_RESISTANCE, PerkTier.SITUATIONAL, "Last Stand", "+30% resistance below 20% HP"),
    RESIST_MASTERY(28, StatType.PHYSICAL_RESISTANCE, PerkTier.MASTERY, "Diamond Skin", "Grants brief invulnerability after hit"),
    RESIST_TRANSCENDENCE(29, StatType.PHYSICAL_RESISTANCE, PerkTier.TRANSCENDENCE, "Immortal", "Survive fatal hit once per 30s"),

    ENDUR_CORE(30, StatType.PHYSICAL_ENDURANCE, PerkTier.CORE, "Sturdy", "+2 absorption hearts"),
    ENDUR_ACTIVE(31, StatType.PHYSICAL_ENDURANCE, PerkTier.ACTIVE, "Second Wind", "Regain 4 absorption on kill"),
    ENDUR_SYNERGY(32, StatType.PHYSICAL_ENDURANCE, PerkTier.SYNERGY, "Unstoppable", "Synergy: no knockback when shielded", StatType.PHYSICAL_RESISTANCE),
    ENDUR_SITUATIONAL(33, StatType.PHYSICAL_ENDURANCE, PerkTier.SITUATIONAL, "Adrenaline", "Kills restore hunger/saturation"),
    ENDUR_MASTERY(34, StatType.PHYSICAL_ENDURANCE, PerkTier.MASTERY, "Overflowing Vitality", "Heal 1 HP per 5s in combat"),
    ENDUR_TRANSCENDENCE(35, StatType.PHYSICAL_ENDURANCE, PerkTier.TRANSCENDENCE, "Limit Break", "Double max absorption"),

    PRECI_CORE(36, StatType.PRECISION, PerkTier.CORE, "Steady Aim", "+5% ranged damage"),
    PRECI_ACTIVE(37, StatType.PRECISION, PerkTier.ACTIVE, "Piercing Shot", "Arrows pierce 1 extra target"),
    PRECI_SYNERGY(38, StatType.PRECISION, PerkTier.SYNERGY, "Hunter's Mark", "Synergy: marked arrows deal +20%", StatType.TRACKING),
    PRECI_SITUATIONAL(39, StatType.PRECISION, PerkTier.SITUATIONAL, "Critical Eye", "+15% crit chance at max health"),
    PRECI_MASTERY(40, StatType.PRECISION, PerkTier.MASTERY, "Deadshot", "Headshot multiplier +50%"),
    PRECI_TRANSCENDENCE(41, StatType.PRECISION, PerkTier.TRANSCENDENCE, "True Strike", "Always hit, ignore armor"),

    TRACK_CORE(42, StatType.TRACKING, PerkTier.CORE, "Tracker's Eye", "Glow nearby mobs for 2s"),
    TRACK_ACTIVE(43, StatType.TRACKING, PerkTier.ACTIVE, "Pursuit", "Gain speed toward marked targets"),
    TRACK_SYNERGY(44, StatType.TRACKING, PerkTier.SYNERGY, "Pack Hunter", "Synergy: extra damage near allies", StatType.INTIMIDATION),
    TRACK_SITUATIONAL(45, StatType.TRACKING, PerkTier.SITUATIONAL, "Sillage", "Hitting mobs leaves a scent trail"),
    TRACK_MASTERY(46, StatType.TRACKING, PerkTier.MASTERY, "Predator", "See all mobs through walls"),
    TRACK_TRANSCENDENCE(47, StatType.TRACKING, PerkTier.TRANSCENDENCE, "Omnisight", "Reveal all entities in 50-block radius"),

    SENSE_CORE(48, StatType.KEEN_SENSES, PerkTier.CORE, "Sixth Sense", "Dodge 5% of incoming hits"),
    SENSE_ACTIVE(49, StatType.KEEN_SENSES, PerkTier.ACTIVE, "Treasure Hunter", "Doubles XP from mobs"),
    SENSE_SYNERGY(50, StatType.KEEN_SENSES, PerkTier.SYNERGY, "Predator's Instinct", "Synergy: mark hidden mobs", StatType.TRACKING),
    SENSE_SITUATIONAL(51, StatType.KEEN_SENSES, PerkTier.SITUATIONAL, "Intuition", "Mine ores faster near enemies"),
    SENSE_MASTERY(52, StatType.KEEN_SENSES, PerkTier.MASTERY, "Foresight", "Precognition: dodge next hit"),
    SENSE_TRANSCENDENCE(53, StatType.KEEN_SENSES, PerkTier.TRANSCENDENCE, "Omniscience", "See player health at all times"),

    FORGE_CORE(54, StatType.FORGING, PerkTier.CORE, "Hammer Hand", "Repairs cost 1 fewer level"),
    FORGE_ACTIVE(55, StatType.FORGING, PerkTier.ACTIVE, "Master Smith", "Chance to double repair output"),
    FORGE_SYNERGY(56, StatType.FORGING, PerkTier.SYNERGY, "Enchanting Touch", "Synergy: anvil enchants cost less", StatType.ALCHEMY),
    FORGE_SITUATIONAL(57, StatType.FORGING, PerkTier.SITUATIONAL, "Sharpening", "Weapons deal more after anvil use"),
    FORGE_MASTERY(58, StatType.FORGING, PerkTier.MASTERY, "Artificer", "Create unique anvil-only upgrades"),
    FORGE_TRANSCENDENCE(59, StatType.FORGING, PerkTier.TRANSCENDENCE, "Creation", "Repair items to perfect condition"),

    COOK_CORE(60, StatType.COOKING, PerkTier.CORE, "Home Cook", "+1 saturation from all food"),
    COOK_ACTIVE(61, StatType.COOKING, PerkTier.ACTIVE, "Iron Stomach", "Bad food effects halved"),
    COOK_SYNERGY(62, StatType.COOKING, PerkTier.SYNERGY, "Nutritionist", "Synergy: food gives absorption", StatType.PHYSICAL_ENDURANCE),
    COOK_SITUATIONAL(63, StatType.COOKING, PerkTier.SITUATIONAL, "Fast Food", "Eat 2x faster"),
    COOK_MASTERY(64, StatType.COOKING, PerkTier.MASTERY, "Feast", "Share food effects with nearby allies"),
    COOK_TRANSCENDENCE(65, StatType.COOKING, PerkTier.TRANSCENDENCE, "Ambrosia", "Food gives regeneration for 30s"),

    ALCHEM_CORE(66, StatType.ALCHEMY, PerkTier.CORE, "Mixologist", "+10% potion duration"),
    ALCHEM_ACTIVE(67, StatType.ALCHEMY, PerkTier.ACTIVE, "Brewer's Secret", "Chance to duplicate potions"),
    ALCHEM_SYNERGY(68, StatType.ALCHEMY, PerkTier.SYNERGY, "Transmutation", "Synergy: convert materials on craft", StatType.FORGING),
    ALCHEM_SITUATIONAL(69, StatType.ALCHEMY, PerkTier.SITUATIONAL, "Toxicologist", "Poison effects last 50% longer"),
    ALCHEM_MASTERY(70, StatType.ALCHEMY, PerkTier.MASTERY, "Philosopher's Stone", "Potion effects are 50% stronger"),
    ALCHEM_TRANSCENDENCE(71, StatType.ALCHEMY, PerkTier.TRANSCENDENCE, "Eternal Elixir", "Potions never expire in inventory"),

    INTIM_CORE(72, StatType.INTIMIDATION, PerkTier.CORE, "Menace", "+5% damage to same target"),
    INTIM_ACTIVE(73, StatType.INTIMIDATION, PerkTier.ACTIVE, "Intimidating Aura", "Nearby mobs deal -10% damage"),
    INTIM_SYNERGY(74, StatType.INTIMIDATION, PerkTier.SYNERGY, "Feared", "Synergy: mobs flee at low HP", StatType.WILLPOWER),
    INTIM_SITUATIONAL(75, StatType.INTIMIDATION, PerkTier.SITUATIONAL, "Mark of Fear", "Marked mobs take +25% damage"),
    INTIM_MASTERY(76, StatType.INTIMIDATION, PerkTier.MASTERY, "Dread Lord", "Kills cause nearby mobs to flee"),
    INTIM_TRANSCENDENCE(77, StatType.INTIMIDATION, PerkTier.TRANSCENDENCE, "Absolute Dominion", "Control one mob for 10s"),

    WILL_CORE(78, StatType.WILLPOWER, PerkTier.CORE, "Iron Will", "Status effects last 10% less"),
    WILL_ACTIVE(79, StatType.WILLPOWER, PerkTier.ACTIVE, "Focused Mind", "Resist knockback when blocking"),
    WILL_SYNERGY(80, StatType.WILLPOWER, PerkTier.SYNERGY, "Unbreakable", "Synergy: damage resistance with shield", StatType.PHYSICAL_RESISTANCE),
    WILL_SITUATIONAL(81, StatType.WILLPOWER, PerkTier.SITUATIONAL, "Last Breath", "Survive at 1 HP once per 30s"),
    WILL_MASTERY(82, StatType.WILLPOWER, PerkTier.MASTERY, "Indomitable", "Debuffs become buffs at low HP"),
    WILL_TRANSCENDENCE(83, StatType.WILLPOWER, PerkTier.TRANSCENDENCE, "Transcendence", "Immune to all status effects");

    private static final Perk[] BY_ID = new Perk[values().length];
    static { for (Perk p : values()) BY_ID[p.id] = p; }

    public final int id;
    public final StatType stat;
    public final PerkTier tier;
    public final String name;
    public final String description;
    public final StatType synergyStat;

    Perk(int id, StatType stat, PerkTier tier, String name, String desc) {
        this(id, stat, tier, name, desc, null);
    }

    Perk(int id, StatType stat, PerkTier tier, String name, String desc, StatType synergyStat) {
        this.id = id; this.stat = stat; this.tier = tier;
        this.name = name; this.description = desc; this.synergyStat = synergyStat;
    }

    public static Perk byId(int id) { return id >= 0 && id < BY_ID.length ? BY_ID[id] : null; }

    public static Perk byStatAndTier(StatType stat, PerkTier tier) {
        for (Perk p : values()) {
            if (p.stat == stat && p.tier == tier) return p;
        }
        return null;
    }
}
```

- [ ] **Step 3: Create `PerkManager.java`**

```java
package tong.statmod.perks;

import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import java.util.*;

public class PerkManager {
    private static final int PERK_COUNT = 84;
    private final BitSet unlocked = new BitSet(PERK_COUNT);
    private final PlayerStatData statData;

    public PerkManager(PlayerStatData statData) {
        this.statData = statData;
    }

    public boolean isUnlocked(Perk perk) { return unlocked.get(perk.id); }
    public boolean[] getUnlockedArray() {
        boolean[] arr = new boolean[PERK_COUNT];
        for (int i = 0; i < PERK_COUNT; i++) arr[i] = unlocked.get(i);
        return arr;
    }
    public void setFromArray(boolean[] arr) {
        unlocked.clear();
        for (int i = 0; i < Math.min(arr.length, PERK_COUNT); i++) {
            if (arr[i]) unlocked.set(i);
        }
    }
    public int[] getUnlockedIds() {
        return unlocked.stream().toArray();
    }
    public void setFromIds(int[] ids) {
        unlocked.clear();
        for (int id : ids) if (id >= 0 && id < PERK_COUNT) unlocked.set(id);
    }

    public int getPointsForStat(int statIndex) {
        return statData.getPerkPointsForStat(statIndex);
    }

    public boolean canUnlock(Perk perk) {
        if (isUnlocked(perk)) return false;
        int statLevel = statData.getLevel(perk.stat.index);
        if (statLevel < perk.tier.requiredStatLevel) return false;
        if (getPointsForStat(perk.stat.index) < perk.tier.cost) return false;
        if (perk.synergyStat != null) {
            int synergyLevel = statData.getLevel(perk.synergyStat.index);
            if (synergyLevel < PerkTier.SYNERGY.requiredStatLevel) return false;
        }
        return true;
    }

    public boolean unlock(Perk perk) {
        if (!canUnlock(perk)) return false;
        unlocked.set(perk.id);
        statData.addPerkPointsForStat(perk.stat.index, -perk.tier.cost);
        return true;
    }

    public void resetAll() {
        unlocked.clear();
    }
}
```

- [ ] **Step 4: Create `PerkState.java`**

```java
package tong.statmod.perks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PerkState {
    private static final Map<UUID, Map<Integer, Long>> cooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<Integer, Long>> comboCounters = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<Integer, Long>> hitMobTracker = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> parryCounter = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> crouchStart = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> effectProcessing = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> killStreak = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastKillTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<Integer>> trackedTargets = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastDodgeTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> frenzyStacks = new ConcurrentHashMap<>();

    public static boolean isOnCooldown(UUID uuid, int perkId, long cooldownMs) {
        Map<Integer, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return false;
        Long expiry = playerCooldowns.get(perkId);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    public static void setCooldown(UUID uuid, int perkId) {
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(perkId, System.currentTimeMillis());
    }

    public static void setCooldown(UUID uuid, int perkId, long durationMs) {
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(perkId, System.currentTimeMillis() + durationMs);
    }

    public static void recordComboHit(UUID uuid, long now, long windowMs) {
        comboCounters.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .merge(0, now, (old, n) -> (now - old < windowMs) ? n : n);
    }

    public static int getComboCount(UUID uuid, long now, long windowMs) {
        Map<Integer, Long> playerCombos = comboCounters.get(uuid);
        if (playerCombos == null) return 0;
        int count = 0;
        long threshold = now - windowMs;
        for (long time : playerCombos.values()) {
            if (time >= threshold) count++;
        }
        return count;
    }

    public static void noteTrackedHit(UUID uuid, int mobId) {
        hitMobTracker.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(mobId, System.currentTimeMillis());
    }

    public static int getTrackedHitCount(UUID uuid, long windowMs) {
        Map<Integer, Long> tracker = hitMobTracker.get(uuid);
        if (tracker == null) return 0;
        long threshold = System.currentTimeMillis() - windowMs;
        return (int) tracker.values().stream().filter(t -> t >= threshold).count();
    }

    public static void setCrouching(UUID uuid) { crouchStart.put(uuid, System.currentTimeMillis()); }
    public static void clearCrouching(UUID uuid) { crouchStart.remove(uuid); }
    public static long getCrouchStart(UUID uuid) { return crouchStart.getOrDefault(uuid, 0L); }

    public static boolean tryBeginEffectProcessing(UUID uuid) {
        return effectProcessing.putIfAbsent(uuid, Boolean.TRUE) == null;
    }
    public static void endEffectProcessing(UUID uuid) { effectProcessing.remove(uuid); }

    public static void recordKill(UUID uuid) {
        long now = System.currentTimeMillis();
        if (now - lastKillTime.getOrDefault(uuid, 0L) < 5000) {
            killStreak.merge(uuid, 1, Integer::sum);
        } else {
            killStreak.put(uuid, 1);
        }
        lastKillTime.put(uuid, now);
    }

    public static int getKillStreak(UUID uuid) { return killStreak.getOrDefault(uuid, 0); }
    public static void resetKillStreak(UUID uuid) { killStreak.remove(uuid); }

    public static void addTrackedTarget(UUID uuid, int entityId) {
        trackedTargets.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(entityId);
    }
    public static boolean isTrackedTarget(UUID uuid, int entityId) {
        Set<Integer> targets = trackedTargets.get(uuid);
        return targets != null && targets.contains(entityId);
    }

    public static void setLastDodge(UUID uuid) { lastDodgeTime.put(uuid, System.currentTimeMillis()); }
    public static long getLastDodgeTime(UUID uuid) { return lastDodgeTime.getOrDefault(uuid, 0L); }

    public static int getFrenzyStacks(UUID uuid) { return frenzyStacks.getOrDefault(uuid, 0); }
    public static void addFrenzyStack(UUID uuid) { frenzyStacks.merge(uuid, 1, Integer::sum); }
    public static void resetFrenzy(UUID uuid) { frenzyStacks.remove(uuid); }

    public static void clearPlayer(UUID uuid) {
        cooldowns.remove(uuid);
        comboCounters.remove(uuid);
        hitMobTracker.remove(uuid);
        parryCounter.remove(uuid);
        crouchStart.remove(uuid);
        effectProcessing.remove(uuid);
        killStreak.remove(uuid);
        lastKillTime.remove(uuid);
        trackedTargets.remove(uuid);
        lastDodgeTime.remove(uuid);
        frenzyStacks.remove(uuid);
    }
}
```

- [ ] **Step 5: Build**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL

---

### Task 6: Network System (NeoForge Payloads)

**Files:**
- Create: `src/main/java/tong/statmod/network/NetworkHandler.java`
- Create: `src/main/java/tong/statmod/network/SyncPerksPayload.java`
- Create: `src/main/java/tong/statmod/network/UnlockPerkPayload.java`
- Create: `src/main/java/tong/statmod/network/BatchSyncPayload.java`

- [ ] **Step 1: Create `SyncPerksPayload.java`**

```java
package tong.statmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record SyncPerksPayload(int[] perkIds, int[] perStatPoints) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPerksPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "sync_perks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPerksPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.array(Int[]::new)),
                    SyncPerksPayload::perkIds,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.array(Int[]::new)),
                    SyncPerksPayload::perStatPoints,
                    SyncPerksPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```

- [ ] **Step 2: Create `UnlockPerkPayload.java`**

```java
package tong.statmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record UnlockPerkPayload(int perkId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UnlockPerkPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "unlock_perk"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockPerkPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    UnlockPerkPayload::perkId,
                    UnlockPerkPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```

- [ ] **Step 3: Create `BatchSyncPayload.java`**

```java
package tong.statmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.STATMod;

public record BatchSyncPayload(int[] levels, int[] xp, int[] perStatPoints, int[] perkIds) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BatchSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "batch_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BatchSyncPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.array(Int[]::new)),
                    BatchSyncPayload::levels,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.array(Int[]::new)),
                    BatchSyncPayload::xp,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.array(Int[]::new)),
                    BatchSyncPayload::perStatPoints,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.array(Int[]::new)),
                    BatchSyncPayload::perkIds,
                    BatchSyncPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```

- [ ] **Step 4: Create `NetworkHandler.java`**

```java
package tong.statmod.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import tong.statmod.STATMod;

@EventBusSubscriber(modid = STATMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(STATMod.MODID);
        registrar.playToClient(SyncPerksPayload.TYPE, SyncPerksPayload.CODEC,
                (payload, context) -> {}); // Client handler TBD
        registrar.playToServer(UnlockPerkPayload.TYPE, UnlockPerkPayload.CODEC,
                (payload, context) -> {}); // Server handler TBD
        registrar.playToClient(BatchSyncPayload.TYPE, BatchSyncPayload.CODEC,
                (payload, context) -> {}); // Client handler TBD
    }
}
```

- [ ] **Step 5: Build**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL

---

### Task 7: Client Caches

**Files:**
- Create: `src/main/java/tong/statmod/client/ClientStatCache.java`
- Create: `src/main/java/tong/statmod/client/ClientPerkCache.java`

- [ ] **Step 1: Create `ClientStatCache.java`**

```java
package tong.statmod.client;

public class ClientStatCache {
    private static int[] levels = new int[0];
    private static int[] xp = new int[0];

    public static void updateAll(int[] newLevels, int[] newXp) {
        levels = newLevels.clone();
        xp = newXp.clone();
    }

    public static int getLevel(int index) {
        return index >= 0 && index < levels.length ? levels[index] : 0;
    }

    public static int getXp(int index) {
        return index >= 0 && index < xp.length ? xp[index] : 0;
    }

    public static int[] getLevels() { return levels.clone(); }
    public static int[] getXp() { return xp.clone(); }
}
```

- [ ] **Step 2: Create `ClientPerkCache.java`**

```java
package tong.statmod.client;

import tong.statmod.perks.Perk;

import java.util.Arrays;

public class ClientPerkCache {
    private static boolean[] unlocked = new boolean[0];
    private static int[] perStatPoints = new int[0];

    public static void update(int[] perkIds, int[] points) {
        unlocked = new boolean[84];
        for (int id : perkIds) {
            if (id >= 0 && id < 84) unlocked[id] = true;
        }
        perStatPoints = points.clone();
    }

    public static boolean isUnlocked(Perk perk) {
        return perk != null && perk.id < unlocked.length && unlocked[perk.id];
    }

    public static int getPointsForStat(int statIndex) {
        return statIndex >= 0 && statIndex < perStatPoints.length ? perStatPoints[statIndex] : 0;
    }

    public static int[] getPerStatPoints() { return perStatPoints.clone(); }
}
```

- [ ] **Step 3: Build**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL

---

### Task 8: GUI — Perk Screen

**Files:**
- Create: `src/main/java/tong/statmod/client/gui/PerkScreen.java`
- Create: `src/main/java/tong/statmod/client/gui/perks/PerkNodeWidget.java`
- Create: `src/main/java/tong/statmod/client/gui/perks/TalentTreePanel.java`
- Create: `src/main/java/tong/statmod/client/gui/StatsOverviewScreen.java`

- [ ] **Step 1: Create `PerkNodeWidget.java`**

```java
package tong.statmod.client.gui.perks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkTier;

import java.util.function.Consumer;

public class PerkNodeWidget extends AbstractWidget {
    private static final int NODE_SIZE = 24;

    private final Perk perk;
    private final boolean unlocked;
    private final boolean canUnlock;
    private final Consumer<Perk> onClick;

    public PerkNodeWidget(int x, int y, Perk perk, boolean unlocked, boolean canUnlock, Consumer<Perk> onClick) {
        super(x, y, perk.tier == PerkTier.TRANSCENDENCE ? NODE_SIZE + 8 : NODE_SIZE,
                perk.tier == PerkTier.TRANSCENDENCE ? NODE_SIZE + 8 : NODE_SIZE, Component.literal(perk.name));
        this.perk = perk;
        this.unlocked = unlocked;
        this.canUnlock = canUnlock;
        this.onClick = onClick;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int borderColor = switch (perk.tier) {
            case CORE -> 0xFF808080;
            case ACTIVE -> 0xFF00AA00;
            case SYNERGY -> 0xFF5555FF;
            case SITUATIONAL -> 0xFFFFAA00;
            case MASTERY -> 0xFFAA00AA;
            case TRANSCENDENCE -> 0xFFD4FF00;
        };

        int fillColor = unlocked ? 0xFF333333 : (canUnlock ? 0xFF555555 : 0xFF222222);
        int size = getWidth();

        graphics.fill(getX(), getY(), getX() + size, getY() + size, borderColor);
        graphics.fill(getX() + 2, getY() + 2, getX() + size - 2, getY() + size - 2, fillColor);

        Font font = Minecraft.getInstance().font;
        String label = perk.name.substring(0, Math.min(2, perk.name.length()));
        graphics.drawCenteredString(font, label, getX() + size / 2, getY() + size / 2 - 4, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && canUnlock) {
            onClick.accept(perk);
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
```

- [ ] **Step 2: Create `TalentTreePanel.java`**

```java
package tong.statmod.client.gui.perks;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkTier;
import tong.statmod.stats.StatType;
import tong.statmod.client.ClientPerkCache;

import java.util.ArrayList;
import java.util.List;

public class TalentTreePanel extends AbstractWidget {
    private final StatType stat;
    private final List<PerkNodeWidget> nodes = new ArrayList<>();
    private Perk selectedPerk;

    public TalentTreePanel(int x, int y, int width, int height, StatType stat) {
        super(x, y, width, height, Component.literal(stat.displayName));
        this.stat = stat;
        initNodes();
    }

    private void initNodes() {
        nodes.clear();
        PerkTier[] tiers = PerkTier.values();
        int nodeSpacing = 30;
        int startY = getY() + 20;

        for (int i = 0; i < tiers.length; i++) {
            Perk perk = Perk.byStatAndTier(stat, tiers[i]);
            if (perk == null) continue;

            int nx = getX() + 10;
            int ny = startY + i * nodeSpacing;
            boolean unlocked = ClientPerkCache.isUnlocked(perk);
            boolean canUnlock = !unlocked && hasEnoughPoints(perk);

            PerkNodeWidget widget = new PerkNodeWidget(nx, ny, perk, unlocked, canUnlock, p -> selectedPerk = p);
            nodes.add(widget);
        }
    }

    private boolean hasEnoughPoints(Perk perk) {
        return ClientPerkCache.getPointsForStat(stat.index) >= perk.tier.cost;
    }

    public Perk getSelectedPerk() {
        Perk s = selectedPerk;
        selectedPerk = null;
        return s;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF1A1A1A);
        graphics.drawString(Minecraft.getInstance().font, stat.displayName, getX() + 5, getY() + 5, 0xFFFFFFFF);

        for (PerkNodeWidget node : nodes) {
            node.render(graphics, mouseX, mouseY, partialTick);
        }

        int points = ClientPerkCache.getPointsForStat(stat.index);
        String ptsText = points + " pts";
        graphics.drawString(Minecraft.getInstance().font, ptsText,
                getX() + getWidth() - 30, getY() + 5, 0xFFD4FF00);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (PerkNodeWidget node : nodes) {
            if (node.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
```

- [ ] **Step 3: Create `PerkScreen.java`**

```java
package tong.statmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tong.statmod.client.ClientStatCache;
import tong.statmod.client.gui.perks.TalentTreePanel;
import tong.statmod.stats.StatType;

import java.util.ArrayList;
import java.util.List;

public class PerkScreen extends Screen {
    private static final int PANEL_WIDTH = 130;
    private static final int PANEL_HEIGHT = 180;
    private static final int COLS = 4;
    private final List<TalentTreePanel> panels = new ArrayList<>();
    private int scrollOffset = 0;

    public PerkScreen() {
        super(Component.literal("Perks"));
    }

    @Override
    protected void init() {
        panels.clear();
        int startX = (width - COLS * (PANEL_WIDTH + 10)) / 2;
        int startY = 40;
        int col = 0, row = 0;

        for (StatType stat : StatType.values()) {
            if (!stat.hasPerks()) continue;
            int x = startX + col * (PANEL_WIDTH + 10);
            int y = startY + row * (PANEL_HEIGHT + 10);
            panels.add(new TalentTreePanel(x, y, PANEL_WIDTH, PANEL_HEIGHT, stat));

            col++;
            if (col >= COLS) {
                col = 0;
                row++;
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        for (TalentTreePanel panel : panels) {
            panel.render(graphics, mouseX, mouseY, partialTick);
        }

        int global = 0, count = 0;
        for (StatType s : StatType.values()) {
            if (s.hasPerks()) { global += ClientStatCache.getLevel(s.index); count++; }
        }
        String levelText = "Global Level: " + (count > 0 ? global / count : 0);
        graphics.drawString(font, levelText, 10, 10, 0xFFD4FF00);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (TalentTreePanel panel : panels) {
            if (panel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    public static void open() {
        Minecraft.getInstance().setScreen(new PerkScreen());
    }
}
```

- [ ] **Step 4: Create `StatsOverviewScreen.java`**

```java
package tong.statmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tong.statmod.client.ClientStatCache;
import tong.statmod.stats.StatType;

public class StatsOverviewScreen extends Screen {
    private static final int LINE_HEIGHT = 12;

    public StatsOverviewScreen() {
        super(Component.literal("Stats"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int y = 20;
        for (StatType stat : StatType.values()) {
            int level = ClientStatCache.getLevel(stat.index);
            int xp = ClientStatCache.getXp(stat.index);
            int needed = (level + 1) * (level + 1) * 10;
            String text = String.format("%s: Lv.%d (%d/%d)", stat.displayName, level, xp, needed);
            graphics.drawString(font, text, 20, y, stat.hasPerks() ? 0xFFFFFFFF : 0xFF808080);
            y += LINE_HEIGHT;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    public static void open() {
        Minecraft.getInstance().setScreen(new StatsOverviewScreen());
    }
}
```

- [ ] **Step 5: Add keybinding to `STATMod.java`** (on client setup)

```java
// In STATMod constructor add:
if (event.getDist() == Dist.CLIENT) {
    ClientRegistry.registerKeyBinding(new KeyMapping(
            "key.statmod.open_perks", InputConstants.KEY_P, "key.categories.statmod"));
}
```

- [ ] **Step 6: Build**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL (screen classes may have warnings but compile)

---

### Task 9: Perk Effect Handler

**Files:**
- Create: `src/main/java/tong/statmod/perks/PerkEffectHandler.java`

- [ ] **Step 1: Create `PerkEffectHandler.java`** (simplified with vanilla events)

```java
package tong.statmod.perks;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.perks.PerkManager;
import tong.statmod.perks.PerkState;
import tong.statmod.stats.StatType;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.UUID;

@EventBusSubscriber(modid = STATMod.MODID)
public class PerkEffectHandler {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();
        PlayerStatData data = player.getData(ModAttachments.STATS);
        PerkManager perks = new PerkManager(data);
        long now = System.currentTimeMillis();

        // CORE: Heavy Hitter (BRUTE_CORE id=0) — tracked via damage event
        // CORE: Sharp Edge (BLADE_CORE id=6) — tracked via damage event
        // CORE: Quick Hands (RAPID_CORE id=12)
        if (perks.isUnlocked(Perk.byId(12))) {
            var attr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
            if (attr != null) attr.setBaseValue(1.6 + 0.08);
        }
        // CORE: Light Feet (AGIL_CORE id=18)
        if (perks.isUnlocked(Perk.byId(18))) {
            var attr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            if (attr != null) attr.setBaseValue(0.1 + 0.005);
        }
        // CORE: Tough Skin (RESIST_CORE id=24) — tracked via damage event
        // CORE: Sturdy (ENDUR_CORE id=30)
        if (perks.isUnlocked(Perk.byId(30)) && player.getAbsorptionAmount() < 4) {
            player.setAbsorptionAmount(4);
        }
        // CORE: Steady Aim (PRECI_CORE id=36) — tracked via projectile event
        // CORE: Tracker's Eye (TRACK_CORE id=42)
        if (perks.isUnlocked(Perk.byId(42)) && player.tickCount % 40 == 0) {
            player.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                    player.getBoundingBox().inflate(16), e -> e.isAlive())
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false)));
        }
        // CORE: Sixth Sense (SENSE_CORE id=48) — tracked via damage event
        // CORE: Hammer Hand (FORGE_CORE id=54) — tracked via anvil event
        // CORE: Home Cook (COOK_CORE id=60) — tracked via food event
        // CORE: Mixologist (ALCHEM_CORE id=66) — tracked via potion event
        // CORE: Menace (INTIM_CORE id=72) — tracked via damage event
        // CORE: Iron Will (WILL_CORE id=78)
        if (perks.isUnlocked(Perk.byId(78)) && player.tickCount % 100 == 0) {
            player.getActiveEffects().forEach(effect -> {
                int dur = effect.getDuration();
                if (dur > 0 && !effect.getEffect().value().isBeneficial()) {
                    player.removeEffect(effect.getEffect());
                    player.addEffect(new MobEffectInstance(effect.getEffect(), (int)(dur * 0.9), effect.getAmplifier(), effect.isAmbient(), effect.isVisible()));
                }
            });
        }

        // ACTIVE: Flowing Strike (BLADE_ACTIVE id=7) — tick just records combo
        if (perks.isUnlocked(Perk.byId(7))) {
            PerkState.recordComboHit(uuid, now, 5000);
        }
        // SITUATIONAL: Intuition (SENSE_SITUATIONAL id=51)
        if (perks.isUnlocked(Perk.byId(51)) && player.tickCount % 20 == 0) {
            boolean nearEnemy = !player.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                    player.getBoundingBox().inflate(8), LivingEntity::isAlive).isEmpty();
            if (nearEnemy) {
                var attr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_BREAK_SPEED);
                if (attr != null) attr.setBaseValue(attr.getBaseValue() + 0.5);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        UUID uuid = event.getEntity().getUUID();
        PlayerStatData data;
        PerkManager perks;

        // Attacker perks
        if (event.getSource().getEntity() instanceof Player attacker) {
            data = attacker.getData(ModAttachments.STATS);
            perks = new PerkManager(data);
            float dmg = event.getNewDamage();

            if (perks.isUnlocked(Perk.byId(0))) dmg *= 1.05f;
            if (perks.isUnlocked(Perk.byId(6))) dmg *= 1.05f;
            if (perks.isUnlocked(Perk.byId(72)) && PerkState.isTrackedTarget(attacker.getUUID(), event.getEntity().getId())) {
                dmg *= 1.05f;
            }

            if (Math.abs(dmg - event.getNewDamage()) > 0.001f) {
                event.setNewDamage(dmg);
            }
        }

        // Victim perks
        if (event.getEntity() instanceof Player victim) {
            data = victim.getData(ModAttachments.STATS);
            perks = new PerkManager(data);

            if (perks.isUnlocked(Perk.byId(24))) {
                event.setNewDamage(event.getNewDamage() * 0.95f);
            }
            if (perks.isUnlocked(Perk.byId(21)) && victim.getRandom().nextFloat() < 0.2f
                    && event.getSource().is(DamageTypes.MOB_ATTACK)) {
                event.setNewDamage(0);
                return;
            }
            if (perks.isUnlocked(Perk.byId(48)) && victim.getRandom().nextFloat() < 0.05f
                    && event.getSource().is(DamageTypes.MOB_ATTACK)) {
                event.setNewDamage(0);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            UUID uuid = player.getUUID();
            PlayerStatData data = player.getData(ModAttachments.STATS);
            PerkManager perks = new PerkManager(data);
            PerkState.recordKill(uuid);

            // ENDUR_ACTIVE id=31: Second Wind — regain 4 absorption on kill
            if (perks.isUnlocked(Perk.byId(31))) {
                player.setAbsorptionAmount(Math.min(20, player.getAbsorptionAmount() + 4));
            }
            // ENDUR_SITUATIONAL id=33: Adrenaline — restore hunger on kill
            if (perks.isUnlocked(Perk.byId(33))) {
                FoodData food = player.getFoodData();
                food.setFoodLevel(Math.min(20, food.getFoodLevel() + 2));
                food.setSaturation(Math.min(20, food.getSaturationLevel() + 2));
            }
            // BLADE_MASTERY id=10: Blade Storm — AOE on kill
            if (perks.isUnlocked(Perk.byId(10))) {
                player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                        player.getBoundingBox().inflate(4), e -> e != player && e.isAlive())
                        .forEach(e -> e.hurt(player.damageSources().mobAttack(player), 4));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;
        UUID uuid = player.getUUID();
        PlayerStatData data = player.getData(ModAttachments.STATS);
        PerkManager perks = new PerkManager(data);

        // RESIST_SITUATIONAL id=27: Last Stand — +30% resistance below 20% HP
        if (perks.isUnlocked(Perk.byId(27)) && player.getHealth() < player.getMaxHealth() * 0.2f) {
            event.setNewDamage(event.getNewDamage() * 0.7f);
        }
        // WILL_SITUATIONAL id=81: Last Breath — survive at 1 HP
        if (perks.isUnlocked(Perk.byId(81)) && !PerkState.isOnCooldown(uuid, 81, 30000)
                && player.getHealth() - event.getNewDamage() <= 0) {
            event.setNewDamage(player.getHealth() - 1);
            PerkState.setCooldown(uuid, 81, 30000);
        }
        // RESIST_MASTERY id=28: Diamond Skin — brief invulnerability after hit
        if (perks.isUnlocked(Perk.byId(28)) && !PerkState.isOnCooldown(uuid, 28, 5000)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4, false, false));
            PerkState.setCooldown(uuid, 28);
        }
        // RESIST_TRANSCENDENCE id=29: Immortal — survive fatal hit once per 30s
        if (perks.isUnlocked(Perk.byId(29)) && !PerkState.isOnCooldown(uuid, 29, 30000)
                && player.getHealth() - event.getNewDamage() <= 0) {
            event.setNewDamage(player.getHealth() - 1);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2, false, false));
            PerkState.setCooldown(uuid, 29, 30000);
        }
    }
}
```

- [ ] **Step 2: Build**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL

---

### Task 10: Login Sync + Integration

**Files:**
- Create: `src/main/java/tong/statmod/mixin/PlayerListMixin.java`
- Create: `src/main/resources/statmod.mixins.json`

- [ ] **Step 1: Create `statmod.mixins.json`**

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "tong.statmod.mixin",
  "compatibilityLevel": "JAVA_21",
  "refmap": "statmod.refmap.json",
  "client": [],
  "mixins": ["PlayerListMixin"],
  "server": []
}
```

- [ ] **Step 2: Create `PlayerListMixin.java`**

```java
package tong.statmod.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tong.statmod.STATMod;
import tong.statmod.network.BatchSyncPayload;
import tong.statmod.network.NetworkHandler;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkManager;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;
import net.neoforged.neoforge.network.PacketDistributor;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void onPlaceNewPlayer(Connection connection, ServerPlayer player, CallbackInfo ci) {
        PlayerStatData data = player.getData(ModAttachments.STATS);
        PerkManager manager = new PerkManager(data);
        BatchSyncPayload payload = new BatchSyncPayload(
                data.getLevels(), data.getXp(), data.getPerkPoints(), manager.getUnlockedIds());
        PacketDistributor.sendToPlayer(player, payload);
        STATMod.LOGGER.info("Synced stat data to {}", player.getName().getString());
    }
}
```

- [ ] **Step 3: Add Mixin config to `build.gradle`** (add after `dependencies`)

```groovy
minecraft {
    runs {
        configureEach {
            property 'mixin.env.remapRefMap', 'true'
            property 'mixin.env.refMapRemappingFile', "${buildDir}/createSrgToMcp/output.srg"
        }
    }
}
```

- [ ] **Step 4: Enable mixin in `STATMod.java`** (add near annotation)

```java
// Add to class level if not already present:
// No extra annotation needed — NeoForge auto-discovers mixin config from statmod.mixins.json
```

- [ ] **Step 5: Build**

Run: `.\gradlew build`
Expected: BUILD SUCCESSFUL

---

### Task 11: Build Verification

- [ ] **Step 1: Run full build**

Run: `.\gradlew build`

- [ ] **Step 2: Fix any compilation errors**

Common issues:
- Missing imports for `Minecraft`, `Font`, `GuiGraphics`
- `PoseStack` → `GuiGraphics` API changes
- `ResourceLocation` constructor: NeoForge 1.21 uses `ResourceLocation.of()` or `ResourceLocation.fromNamespaceAndPath()`
- `PayloadDistributor.sendToPlayer` → verify exact method name

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat(neoforge): standalone stats system - MVP"
```
