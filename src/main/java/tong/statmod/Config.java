package tong.statmod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = STATMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLE_DEBUG = BUILDER
            .comment("Enable debug logging for stats")
            .define("enableDebug", false);

    // ── XP Progression ──
    public static final ForgeConfigSpec.IntValue XP_PER_LEVEL_MULTIPLIER = BUILDER
            .comment("Multiplier for XP required per level. Formula: (level + 1) * multiplier")
            .defineInRange("xpPerLevelMultiplier", 50, 10, 500);

    public static final ForgeConfigSpec.IntValue XP_TIER_COMMON_MIN = BUILDER
            .comment("Minimum XP awarded for COMMON actions")
            .defineInRange("xpTierCommonMin", 2, 1, 100);

    public static final ForgeConfigSpec.IntValue XP_TIER_COMMON_MAX = BUILDER
            .comment("Maximum XP awarded for COMMON actions")
            .defineInRange("xpTierCommonMax", 3, 1, 100);

    public static final ForgeConfigSpec.IntValue XP_TIER_INTERMEDIATE_MIN = BUILDER
            .comment("Minimum XP awarded for INTERMEDIATE actions")
            .defineInRange("xpTierIntermediateMin", 5, 1, 100);

    public static final ForgeConfigSpec.IntValue XP_TIER_INTERMEDIATE_MAX = BUILDER
            .comment("Maximum XP awarded for INTERMEDIATE actions")
            .defineInRange("xpTierIntermediateMax", 8, 1, 100);

    public static final ForgeConfigSpec.IntValue XP_TIER_RARE_MIN = BUILDER
            .comment("Minimum XP awarded for RARE actions")
            .defineInRange("xpTierRareMin", 12, 1, 100);

    public static final ForgeConfigSpec.IntValue XP_TIER_RARE_MAX = BUILDER
            .comment("Maximum XP awarded for RARE actions")
            .defineInRange("xpTierRareMax", 20, 1, 100);

    public static final ForgeConfigSpec.IntValue WEAPON_XP_MIN = BUILDER
            .comment("Minimum weapon mastery XP per hit")
            .defineInRange("weaponXpMin", 5, 1, 100);

    public static final ForgeConfigSpec.IntValue WEAPON_XP_MAX = BUILDER
            .comment("Maximum weapon mastery XP per hit (random between min and max)")
            .defineInRange("weaponXpMax", 10, 1, 100);

    public static final ForgeConfigSpec.IntValue WEAPON_MASTERY_MAX_LEVEL = BUILDER
            .comment("Maximum level for weapon mastery")
            .defineInRange("weaponMasteryMaxLevel", 50, 10, 200);

    // ── Perk System ──
    public static final ForgeConfigSpec.IntValue PERK_TIER1_LEVEL = BUILDER
            .comment("Level required to unlock tier 1 perks")
            .defineInRange("perkTier1Level", 20, 1, 100);

    public static final ForgeConfigSpec.IntValue PERK_TIER2_LEVEL = BUILDER
            .comment("Level required to unlock tier 2 perks")
            .defineInRange("perkTier2Level", 50, 1, 100);

    public static final ForgeConfigSpec.IntValue PERK_TIER3_LEVEL = BUILDER
            .comment("Level required to unlock tier 3 perks")
            .defineInRange("perkTier3Level", 80, 1, 100);

    public static final ForgeConfigSpec.IntValue SKILL_TIER1_LEVEL = BUILDER
            .comment("Level required to unlock tier 1 passive skills")
            .defineInRange("skillTier1Level", 20, 1, 100);

    public static final ForgeConfigSpec.IntValue SKILL_TIER2_LEVEL = BUILDER
            .comment("Level required to unlock tier 2 passive skills")
            .defineInRange("skillTier2Level", 50, 1, 100);

    public static final ForgeConfigSpec.IntValue SKILL_TIER3_LEVEL = BUILDER
            .comment("Level required to unlock tier 3 passive skills")
            .defineInRange("skillTier3Level", 80, 1, 100);

    public static final ForgeConfigSpec.IntValue SKILL_ACTIVE_LEVEL = BUILDER
            .comment("Level required to unlock active skills")
            .defineInRange("skillActiveLevel", 100, 1, 100);

    // ── Skill Cooldowns (global multipliers) ──
    public static final ForgeConfigSpec.DoubleValue COOLDOWN_MULTIPLIER = BUILDER
            .comment("Global cooldown multiplier for ALL skills (1.0 = default)")
            .defineInRange("cooldownMultiplier", 1.0, 0.1, 10.0);

    // ── Thirst System ──
    public static final ForgeConfigSpec.DoubleValue THIRST_BASE_DECAY = BUILDER
            .comment("Base thirst decay per tick")
            .defineInRange("thirstBaseDecay", 0.003, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue THIRST_SPRINT_COST = BUILDER
            .comment("Additional thirst cost per tick while sprinting")
            .defineInRange("thirstSprintCost", 0.05, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue THIRST_JUMP_COST = BUILDER
            .comment("Thirst cost per jump")
            .defineInRange("thirstJumpCost", 0.1, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue THIRST_BLOCK_BREAK_COST = BUILDER
            .comment("Thirst cost per block broken")
            .defineInRange("thirstBlockBreakCost", 0.5, 0.0, 10.0);

    public static final ForgeConfigSpec.DoubleValue THIRST_ARMOR_COST_PER_PIECE = BUILDER
            .comment("Thirst cost per tick per armor piece worn")
            .defineInRange("thirstArmorCostPerPiece", 0.005, 0.0, 0.1);

    public static final ForgeConfigSpec.DoubleValue THIRST_HOT_BIOME_COST = BUILDER
            .comment("Additional thirst cost per tick in hot biomes")
            .defineInRange("thirstHotBiomeCost", 0.005, 0.0, 0.1);

    // ── Fatigue System ──
    public static final ForgeConfigSpec.DoubleValue FATIGUE_SNEAK_RECOVERY = BUILDER
            .comment("Fatigue recovered per tick while sneaking")
            .defineInRange("fatigueSneakRecovery", 0.5, 0.0, 10.0);

    public static final ForgeConfigSpec.DoubleValue FATIGUE_WATER_BOTTLE_RECOVERY = BUILDER
            .comment("Fatigue reduction per water bottle consumed")
            .defineInRange("fatigueWaterBottleRecovery", 20.0, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue FATIGUE_DAY_RATE = BUILDER
            .comment("Passive fatigue accumulation per tick during day")
            .defineInRange("fatigueDayRate", 0.0003, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue FATIGUE_NIGHT_RATE = BUILDER
            .comment("Passive fatigue accumulation per tick during night")
            .defineInRange("fatigueNightRate", 0.0005, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue FATIGUE_UNDERGROUND_RATE = BUILDER
            .comment("Passive fatigue accumulation per tick underground")
            .defineInRange("fatigueUndergroundRate", 0.0002, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue FATIGUE_DAMAGE_COST = BUILDER
            .comment("Fatigue added when player takes damage")
            .defineInRange("fatigueDamageCost", 1.0, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue FATIGUE_SPRINT_COST = BUILDER
            .comment("Fatigue added per sprint tick")
            .defineInRange("fatigueSprintCost", 0.05, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue FATIGUE_JUMP_COST = BUILDER
            .comment("Fatigue added per jump")
            .defineInRange("fatigueJumpCost", 0.1, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue FATIGUE_BLOCK_BREAK_COST = BUILDER
            .comment("Fatigue added per block broken")
            .defineInRange("fatigueBlockBreakCost", 0.1, 0.0, 100.0);

    public static final ForgeConfigSpec.DoubleValue FATIGUE_SLEEP_PENALTY_BASE = BUILDER
            .comment("Base fatigue penalty at dawn for each sleepless night")
            .defineInRange("fatigueSleepPenaltyBase", 25.0, 0.0, 100.0);

    public static final ForgeConfigSpec.IntValue FATIGUE_MAX_CAPACITY = BUILDER
            .comment("Base max fatigue capacity (endurance adds +5 per level)")
            .defineInRange("fatigueMaxCapacity", 500, 50, 5000);

    public static final ForgeConfigSpec.IntValue FATIGUE_EXHAUSTED_DAMAGE_INTERVAL = BUILDER
            .comment("Ticks between damage ticks when exhausted (100% fatigue)")
            .defineInRange("fatigueExhaustedDamageInterval", 40, 1, 200);

    public static final ForgeConfigSpec.DoubleValue FATIGUE_EXHAUSTED_DAMAGE = BUILDER
            .comment("Damage dealt per tick when exhausted (hearts)")
            .defineInRange("fatigueExhaustedDamage", 1.0, 0.0, 20.0);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // ── Cached runtime values (updated on config load/reload) ──

    public static boolean enableDebug;

    // XP
    public static int xpPerLevelMultiplier;
    public static int xpTierCommonMin;
    public static int xpTierCommonMax;
    public static int xpTierIntermediateMin;
    public static int xpTierIntermediateMax;
    public static int xpTierRareMin;
    public static int xpTierRareMax;
    public static int weaponXpMin;
    public static int weaponXpMax;
    public static int weaponMasteryMaxLevel;

    // Perks & Skills
    public static int perkTier1Level;
    public static int perkTier2Level;
    public static int perkTier3Level;
    public static int skillTier1Level;
    public static int skillTier2Level;
    public static int skillTier3Level;
    public static int skillActiveLevel;
    public static double cooldownMultiplier;

    // Thirst
    public static double thirstBaseDecay;
    public static double thirstSprintCost;
    public static double thirstJumpCost;
    public static double thirstBlockBreakCost;
    public static double thirstArmorCostPerPiece;
    public static double thirstHotBiomeCost;

    // Fatigue
    public static double fatigueSneakRecovery;
    public static double fatigueWaterBottleRecovery;
    public static double fatigueDayRate;
    public static double fatigueNightRate;
    public static double fatigueUndergroundRate;
    public static double fatigueDamageCost;
    public static double fatigueSprintCost;
    public static double fatigueJumpCost;
    public static double fatigueBlockBreakCost;
    public static double fatigueSleepPenaltyBase;
    public static int fatigueExhaustedDamageInterval;
    public static double fatigueExhaustedDamage;
    public static int fatigueMaxCapacity;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        enableDebug = ENABLE_DEBUG.get();

        xpPerLevelMultiplier = XP_PER_LEVEL_MULTIPLIER.get();
        xpTierCommonMin = XP_TIER_COMMON_MIN.get();
        xpTierCommonMax = XP_TIER_COMMON_MAX.get();
        xpTierIntermediateMin = XP_TIER_INTERMEDIATE_MIN.get();
        xpTierIntermediateMax = XP_TIER_INTERMEDIATE_MAX.get();
        xpTierRareMin = XP_TIER_RARE_MIN.get();
        xpTierRareMax = XP_TIER_RARE_MAX.get();
        weaponXpMin = WEAPON_XP_MIN.get();
        weaponXpMax = WEAPON_XP_MAX.get();
        weaponMasteryMaxLevel = WEAPON_MASTERY_MAX_LEVEL.get();

        perkTier1Level = PERK_TIER1_LEVEL.get();
        perkTier2Level = PERK_TIER2_LEVEL.get();
        perkTier3Level = PERK_TIER3_LEVEL.get();
        skillTier1Level = SKILL_TIER1_LEVEL.get();
        skillTier2Level = SKILL_TIER2_LEVEL.get();
        skillTier3Level = SKILL_TIER3_LEVEL.get();
        skillActiveLevel = SKILL_ACTIVE_LEVEL.get();
        cooldownMultiplier = COOLDOWN_MULTIPLIER.get();

        thirstBaseDecay = THIRST_BASE_DECAY.get();
        thirstSprintCost = THIRST_SPRINT_COST.get();
        thirstJumpCost = THIRST_JUMP_COST.get();
        thirstBlockBreakCost = THIRST_BLOCK_BREAK_COST.get();
        thirstArmorCostPerPiece = THIRST_ARMOR_COST_PER_PIECE.get();
        thirstHotBiomeCost = THIRST_HOT_BIOME_COST.get();

        fatigueSneakRecovery = FATIGUE_SNEAK_RECOVERY.get();
        fatigueWaterBottleRecovery = FATIGUE_WATER_BOTTLE_RECOVERY.get();
        fatigueDayRate = FATIGUE_DAY_RATE.get();
        fatigueNightRate = FATIGUE_NIGHT_RATE.get();
        fatigueUndergroundRate = FATIGUE_UNDERGROUND_RATE.get();
        fatigueDamageCost = FATIGUE_DAMAGE_COST.get();
        fatigueSprintCost = FATIGUE_SPRINT_COST.get();
        fatigueJumpCost = FATIGUE_JUMP_COST.get();
        fatigueBlockBreakCost = FATIGUE_BLOCK_BREAK_COST.get();
        fatigueSleepPenaltyBase = FATIGUE_SLEEP_PENALTY_BASE.get();
        fatigueExhaustedDamageInterval = FATIGUE_EXHAUSTED_DAMAGE_INTERVAL.get();
        fatigueExhaustedDamage = FATIGUE_EXHAUSTED_DAMAGE.get();
        fatigueMaxCapacity = FATIGUE_MAX_CAPACITY.get();
    }

    // ── Helper for computing effective cooldown ──
    public static long getEffectiveCooldown(long baseCooldownMs) {
        return (long)(baseCooldownMs * cooldownMultiplier);
    }
}
