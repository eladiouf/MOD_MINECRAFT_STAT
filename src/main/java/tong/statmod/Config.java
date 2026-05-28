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

    public static boolean enableDebug;
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
}
