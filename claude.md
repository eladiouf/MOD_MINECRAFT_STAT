```markdown
# Epic Fight Combat Leveling Addon – Implementation Guide for Claude

This document provides a **step-by-step, executable plan** to create a Minecraft Forge mod (1.20.1) that adds a deep leveling system with custom stats to the **Epic Fight** mod. Follow each step precisely.

**Target:** Claude (AI coder) – you will generate code, place files, and run gradle tasks.

---

## 1. Prerequisites & Setup

### 1.1 Environment
- JDK 17
- IntelliJ IDEA (or any IDE with Gradle support)
- Git

### 1.2 Create Project from Epic Fight Skill Tree Template

The official template already contains the correct dependencies and build scripts.

```bash
# Clone the template
git clone https://github.com/Antikythera-Studios/epicskills.git YourModName
cd YourModName

# Remove the original git history
rm -rf .git
git init
```

### 1.3 Configure Mod Metadata

Open `src/main/resources/META-INF/mods.toml`. Change:
- `modId` → `"yourmodid"`
- `displayName` → `"Your Combat Leveling"`
- `description` → `"Adds custom combat leveling and stats to Epic Fight."`

Also update the `[[dependencies.YourModId]]` section to require Epic Fight:

```toml
[[dependencies.yourmodid]]
    modId="epicfight"
    mandatory=true
    versionRange="[20.9.5,)"
    ordering="NONE"
    side="BOTH"
```

### 1.4 Update `gradle.properties`

Set your mod’s base package and version:

```properties
mod_id=yourmodid
mod_version=1.0.0
mod_group=com.yourname.yourmodid
minecraft_version=1.20.1
forge_version=47.3.0
epicfight_version=20.9.5   # stable version for 1.20.1
```

### 1.5 Main Mod Class

Create `src/main/java/com/yourname/yourmodid/YourMod.java`:

```java
package com.yourname.yourmodid;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(YourMod.MODID)
public class YourMod {
    public static final String MODID = "yourmodid";
    public static final Logger LOGGER = LogManager.getLogger();

    public YourMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // Registration of stats will happen here (Step 2)
        MinecraftForge.EVENT_BUS.register(this);
    }
}
```

---

## 2. Register Custom Stats with Epic Fight

Epic Fight uses `AttributeStat` objects that are registered via a `DeferredRegister`. We will create a class that holds all your stats.

### 2.1 Create `ModStats.java`

Path: `src/main/java/com/yourname/yourmodid/ModStats.java`

```java
package com.yourname.yourmodid;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import yesman.epicfight.api.utils.math.ValueCorrector;
import yesman.epicfight.world.entity.ai.attribute.AttributeStat;
import yesman.epicfight.main.EpicFightMod;

public class ModStats {
    // Use Epic Fight's own attribute registry
    public static final DeferredRegister<AttributeStat> STATS = DeferredRegister.create(
            new ResourceLocation(EpicFightMod.MODID, "attributes"), YourMod.MODID);

    // ---------- Combat (8) ----------
    public static final RegistryObject<AttributeStat> BRUTE_FORCE = register("brute_force", 0, 100);
    public static final RegistryObject<AttributeStat> BLADE_TECHNIQUE = register("blade_technique", 0, 100);
    public static final RegistryObject<AttributeStat> RAPIDITE = register("rapidite", 0, 100);
    public static final RegistryObject<AttributeStat> AGILITY = register("agility", 0, 100);
    public static final RegistryObject<AttributeStat> PHYSICAL_RESISTANCE = register("physical_resistance", 0, 100);
    public static final RegistryObject<AttributeStat> PHYSICAL_ENDURANCE = register("physical_endurance", 0, 100);
    public static final RegistryObject<AttributeStat> PRECISION = register("precision", 0, 100);

    // ---------- Magic (7) ----------
    public static final RegistryObject<AttributeStat> ARCANE_POWER = register("arcane_power", 0, 100);
    public static final RegistryObject<AttributeStat> WATER_AFFINITY = register("water_affinity", 0, 100);
    public static final RegistryObject<AttributeStat> EARTH_AFFINITY = register("earth_affinity", 0, 100);
    public static final RegistryObject<AttributeStat> FIRE_AFFINITY = register("fire_affinity", 0, 100);
    public static final RegistryObject<AttributeStat> AIR_AFFINITY = register("air_affinity", 0, 100);
    public static final RegistryObject<AttributeStat> MAGIC_RESISTANCE = register("magic_resistance", 0, 100);
    public static final RegistryObject<AttributeStat> CASTING_SPEED = register("casting_speed", 0, 100);
    public static final RegistryObject<AttributeStat> MANA_POOL = register("mana_pool", 0, 100);
    public static final RegistryObject<AttributeStat> ERUDITION = register("erudition", 0, 100);

    // ---------- Survival (2) ----------
    public static final RegistryObject<AttributeStat> TRACKING = register("tracking", 0, 100);
    public static final RegistryObject<AttributeStat> KEEN_SENSES = register("keen_senses", 0, 100);

    // ---------- Crafting (3) ----------
    public static final RegistryObject<AttributeStat> FORGING = register("forging", 0, 100);
    public static final RegistryObject<AttributeStat> COOKING = register("cooking", 0, 100);
    public static final RegistryObject<AttributeStat> ALCHEMY = register("alchemy", 0, 100);

    // ---------- Mental (2) ----------
    public static final RegistryObject<AttributeStat> INTIMIDATION = register("intimidation", 0, 100);
    public static final RegistryObject<AttributeStat> WILLPOWER = register("willpower", 0, 100);

    private static RegistryObject<AttributeStat> register(String name, int min, int max) {
        // Initial ValueCorrector is null – we will add effects later in Step 4
        return STATS.register(name, () -> new AttributeStat(
                new ResourceLocation(YourMod.MODID, name),
                0.0, min, max, null));
    }

    public static void register(IEventBus modEventBus) {
        STATS.register(modEventBus);
        YourMod.LOGGER.info("Registered {} custom stats for Epic Fight", STATS.getEntries().size());
    }
}
```

### 2.2 Call Registration in Main Mod Class

In `YourMod.java` constructor:

```java
public YourMod() {
    IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
    ModStats.register(modEventBus);   // <-- ADD THIS LINE
    MinecraftForge.EVENT_BUS.register(this);
}
```

**At this point, the stats exist but do nothing yet.** They appear in player data but have no effects.

---

## 3. Player Data: XP and Level Tracking

We need a **capability** to store per‑player XP and levels for each skill.

### 3.1 Create the Capability Interface and Implementation

Create `src/main/java/com/yourname/yourmodid/capability/ICombatLevels.java`:

```java
package com.yourname.yourmodid.capability;

public interface ICombatLevels {
    int getSkillLevel(int skillIndex);
    int getSkillXP(int skillIndex);
    void addXP(int skillIndex, int amount);
    void setSkillLevel(int skillIndex, int level);
    void setSkillXP(int skillIndex, int xp);
    void copyFrom(ICombatLevels source);
}
```

Create `src/main/java/com/yourname/yourmodid/capability/CombatLevels.java`:

```java
package com.yourname.yourmodid.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class CombatLevels implements ICombatLevels, INBTSerializable<CompoundTag> {
    private static final int SKILL_COUNT = 22; // 8+7+2+3+2 = 22 stats
    private int[] levels = new int[SKILL_COUNT];
    private int[] xp = new int[SKILL_COUNT];

    @Override
    public int getSkillLevel(int index) { return levels[index]; }
    @Override
    public int getSkillXP(int index) { return xp[index]; }

    @Override
    public void addXP(int index, int amount) {
        this.xp[index] += amount;
        int required = getXPForNextLevel(levels[index]);
        while (this.xp[index] >= required && levels[index] < 100) {
            levels[index]++;
            this.xp[index] -= required;
            required = getXPForNextLevel(levels[index]);
            // TODO: Fire an event or call a method to apply stat effect
        }
    }

    private int getXPForNextLevel(int level) {
        // Quadratic XP curve: level 1 requires 10 XP, level 2 requires 40 XP, etc.
        return (level + 1) * (level + 1) * 10;
    }

    @Override
    public void setSkillLevel(int index, int level) { this.levels[index] = level; }
    @Override
    public void setSkillXP(int index, int xp) { this.xp[index] = xp; }

    @Override
    public void copyFrom(ICombatLevels source) {
        for (int i = 0; i < SKILL_COUNT; i++) {
            this.levels[i] = source.getSkillLevel(i);
            this.xp[i] = source.getSkillXP(i);
        }
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
        this.levels = tag.getIntArray("Levels");
        if (this.levels.length < SKILL_COUNT) this.levels = new int[SKILL_COUNT];
        this.xp = tag.getIntArray("XP");
        if (this.xp.length < SKILL_COUNT) this.xp = new int[SKILL_COUNT];
    }
}
```

### 3.2 Attach Capability to Player

Create `src/main/java/com/yourname/yourmodid/capability/CombatLevelsProvider.java`:

```java
package com.yourname.yourmodid.capability;

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

public class CombatLevelsProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<ICombatLevels> COMBAT_LEVELS = CapabilityManager.get(new CapabilityToken<>() {});

    private CombatLevels levels = null;
    private final LazyOptional<ICombatLevels> lazyOptional = LazyOptional.of(this::getOrCreate);

    private CombatLevels getOrCreate() {
        if (this.levels == null) this.levels = new CombatLevels();
        return this.levels;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == COMBAT_LEVELS ? lazyOptional.cast() : LazyOptional.empty();
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

### 3.3 Register the Capability

In `YourMod.java` constructor, add:

```java
public YourMod() {
    // ... existing code ...
    CapabilityManager.INSTANCE.register(ICombatLevels.class, new Capability.IStorage<ICombatLevels>() {
        @Override
        public CompoundTag writeNBT(Capability<ICombatLevels> capability, ICombatLevels instance, Direction side) {
            return ((CombatLevels)instance).serializeNBT();
        }
        @Override
        public void readNBT(Capability<ICombatLevels> capability, ICombatLevels instance, Direction side, CompoundTag nbt) {
            ((CombatLevels)instance).deserializeNBT(nbt);
        }
    }, () -> new CombatLevels());
}
```

### 3.4 Attach to Player via Event

Create `src/main/java/com/yourname/yourmodid/AttachCapabilityHandler.java`:

```java
package com.yourname.yourmodid;

import com.yourname.yourmodid.capability.CombatLevelsProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = YourMod.MODID)
public class AttachCapabilityHandler {
    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(YourMod.MODID, "combat_levels"), new CombatLevelsProvider());
        }
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(CombatLevelsProvider.COBAT_LEVELS);
    }
}
```

---

## 4. Grant XP from Combat

We will listen to Epic Fight’s attack event and award XP based on weapon type.

Create `src/main/java/com/yourname/yourmodid/CombatXPHandler.java`:

```java
package com.yourname.yourmodid;

import com.yourname.yourmodid.capability.CombatLevelsProvider;
import com.yourname.yourmodid.capability.ICombatLevels;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.forgeevent.PlayerEvent;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

@Mod.EventBusSubscriber(modid = YourMod.MODID)
public class CombatXPHandler {
    @SubscribeEvent
    public static void onAttack(PlayerEvent.ServerPlayerOnAttackEvent event) {
        Player player = event.getPlayerPatch().getOriginal();
        player.getCapability(CombatLevelsProvider.COMBAT_LEVELS).ifPresent(cap -> {
            CapabilityItem itemCap = event.getPlayerPatch().getHoldingItemCapability();
            WeaponCategory category = itemCap.getWeaponCategory();

            // Map weapon categories to skill indices
            int skillIndex = -1;
            if (category == WeaponCategory.SWORD) skillIndex = 1; // Blade Technique
            else if (category == WeaponCategory.GREATSWORD) skillIndex = 1;
            else if (category == WeaponCategory.AXE) skillIndex = 0; // Brute Force
            else if (category == WeaponCategory.DAGGER) skillIndex = 2; // Rapidité
            else if (category == WeaponCategory.BOW) skillIndex = 6; // Precision
            else if (category == WeaponCategory.FIST) skillIndex = 3; // Agility

            if (skillIndex != -1) {
                // Base XP: 5 + random up to 5
                int xp = 5 + player.getRandom().nextInt(6);
                cap.addXP(skillIndex, xp);
                YourMod.LOGGER.debug("Awarded {} XP to skill {}", xp, skillIndex);
            }
        });
    }
}
```

---

## 5. Apply Stat Effects

Now we need to make the stats affect gameplay. Epic Fight allows **ValueCorrector** to modify attributes like damage, speed, and resistance.

### 5.1 Modify Damage based on Brute Force / Blade Technique

Go back to `ModStats.java` and replace the `register` helper to attach a `ValueCorrector` for each stat. However, because `ValueCorrector` needs access to the player’s stat value, the correct approach is to **listen to an event** and read the stat level directly.

**Better approach:** Use `PlayerEvent.ModifyDamageEvent` (or similar from Epic Fight) to scale damage.

Create `src/main/java/com/yourname/yourmodid/StatEffectApplier.java`:

```java
package com.yourname.yourmodid;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.api.client.model.ClientModel;

@Mod.EventBusSubscriber(modid = YourMod.MODID)
public class StatEffectApplier {
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        player.getCapability(CombatLevelsProvider.COMBAT_LEVELS).ifPresent(cap -> {
            int bruteForce = cap.getSkillLevel(0);   // index 0 = Brute Force
            int bladeTech = cap.getSkillLevel(1);    // index 1 = Blade Technique

            float multiplier = 1.0f + (bruteForce + bladeTech) / 200.0f;
            event.setAmount(event.getAmount() * multiplier);
        });
    }
}
```

### 5.2 Modify Attack Speed (Rapidité, Agility)

Epic Fight handles attack speed through its own animation system. You can modify the player’s `AttackSpeed` attribute via Minecraft’s attributes.

```java
// Inside StatEffectApplier, listen to PlayerEvent.PlayerLoggedInEvent
@SubscribeEvent
public static void onPlayerJoin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
    event.getEntity().getCapability(CombatLevelsProvider.COMBAT_LEVELS).ifPresent(cap -> {
        int rapidite = cap.getSkillLevel(2);
        int agility = cap.getSkillLevel(3);
        double speedBonus = (rapidite + agility) / 100.0; // up to +200% at level 100 each
        // Apply attribute modifier
        var attribute = event.getEntity().getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
        if (attribute != null) {
            attribute.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                new ResourceLocation(YourMod.MODID, "speed_bonus"), speedBonus,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    });
}
```

### 5.3 Physical Resistance

Modify incoming damage to the player:

```java
@SubscribeEvent
public static void onPlayerHurt(LivingHurtEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    player.getCapability(CombatLevelsProvider.COMBAT_LEVELS).ifPresent(cap -> {
        int physRes = cap.getSkillLevel(4); // Physical Resistance
        float reduction = physRes / 200.0f; // max 50% reduction
        event.setAmount(event.getAmount() * (1 - reduction));
    });
}
```

**Note:** For stats like `Mana Pool`, you must implement a separate mana system (not covered here but you can reference `AuraSkills` for inspiration).

---

## 6. UI: Display Stats and Levels (Optional but Recommended)

Create a simple GUI screen that shows each skill name, level, and XP progress.

Create `src/main/java/com/yourname/yourmodid/client/StatsScreen.java`:

```java
package com.yourname.yourmodid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yourname.yourmodid.YourMod;
import com.yourname.yourmodid.capability.CombatLevelsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class StatsScreen extends Screen {
    private static final int START_X = 20;
    private static final int START_Y = 20;
    private static final int LINE_HEIGHT = 12;

    protected StatsScreen() {
        super(Component.translatable("screen.yourmod.stats"));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);

        var player = Minecraft.getInstance().player;
        if (player == null) return;

        player.getCapability(CombatLevelsProvider.COMBAT_LEVELS).ifPresent(cap -> {
            int y = START_Y;
            String[] skillNames = {
                "Brute Force", "Blade Technique", "Rapidité", "Agility",
                "Physical Resistance", "Physical Endurance", "Precision",
                "Arcane Power", "Water Affinity", "Earth Affinity", "Fire Affinity", "Air Affinity",
                "Magic Resistance", "Casting Speed", "Mana Pool", "Erudition",
                "Tracking", "Keen Senses",
                "Forging", "Cooking", "Alchemy",
                "Intimidation", "Willpower"
            };
            for (int i = 0; i < skillNames.length; i++) {
                int level = cap.getSkillLevel(i);
                int xp = cap.getSkillXP(i);
                int needed = (level + 1) * (level + 1) * 10;
                String text = String.format("%s: Lv.%d (%d/%d XP)", skillNames[i], level, xp, needed);
                this.font.draw(poseStack, text, START_X, y, 0xFFFFFF);
                y += LINE_HEIGHT;
                if (y > this.height - 20) break;
            }
        });
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
```

Register a keybinding to open the screen. In `YourMod.java`:

```java
@SubscribeEvent
public static void onClientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> {
        ClientRegistry.registerKeyBinding(new KeyMapping(
            "key.yourmod.open_stats", InputConstants.KEY_P, "key.categories.gameplay"));
    });
}

// In a client event handler class
@SubscribeEvent
public static void onKeyInput(InputEvent.KeyInputEvent event) {
    if (Minecraft.getInstance().screen == null && 
        KeyBindingRegistry.OPEN_STATS.consumeClick()) {
        Minecraft.getInstance().setScreen(new StatsScreen());
    }
}
```

---

## 7. Testing & Debugging

Run the mod with:

```bash
./gradlew runClient
```

Create a new world, give yourself a sword, and attack mobs. Check the console for XP messages (debug level). Then press `P` to open the stats screen.

**If stats are not increasing:**
- Ensure the capability is attached (put a log in `AttachCapabilityHandler.attachCapabilities`).
- Ensure the event `ServerPlayerOnAttackEvent` is being fired (Epic Fight must be loaded).

**If damage is not scaling:**
- Verify that `LivingHurtEvent` is being called and the player is the attacker.
- Check that `bruteForce` and `bladeTech` values are >0.

---

## 8. Reference: AuraSkills for Advanced Features

The repository `https://github.com/Archy-X/AuraSkills.git` is a complete skill system. Use it as a reference for:

- **Skill trees** with multiple branches
- **Configuration** (defining skills in JSON)
- **Party sharing** (XP distribution)
- **PlaceholderAPI integration** (if you later support Spigot)

You can examine its `SkillManager` and `SkillRegistry` classes to see how to manage a large number of skills dynamically.

---

## 9. Final Deliverables

After completing the steps above, you will have:

1. A Forge mod that adds 22 custom stats to Epic Fight.
2. A leveling system where each stat can be leveled from 0 to 100 via combat XP.
3. Functional effects for **damage increase** (Brute Force + Blade Technique) and **damage reduction** (Physical Resistance).
4. A basic UI screen to view stats.
5. Extensible code to implement the remaining stat effects (magic, survival, crafting, mental).

**Next steps for you (Claude):**
- Copy the code blocks exactly into the correct file paths.
- Run `./gradlew build` to compile.
- Fix any import errors (Epic Fight internal classes may have changed – adjust imports accordingly).
- If you get stuck, search the Epic Fight source code for the exact names of events like `ServerPlayerOnAttackEvent`.

Happy coding!
```