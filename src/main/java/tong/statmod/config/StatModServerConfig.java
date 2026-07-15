package tong.statmod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import tong.statmod.effects.CombatScalingRules;

public final class StatModServerConfig {
    private static final ForgeConfigSpec.DoubleValue WEAPON_DAMAGE_BASE;
    private static final ForgeConfigSpec.DoubleValue WEAPON_DAMAGE_SCALE;
    private static final ForgeConfigSpec.DoubleValue WEAPON_DAMAGE_EXPONENT;
    private static final ForgeConfigSpec.DoubleValue PHYSICAL_RESISTANCE_CAP;
    private static final ForgeConfigSpec.DoubleValue PHYSICAL_ENDURANCE_CAP;
    private static final ForgeConfigSpec.DoubleValue STAMINA_CAPACITY_BONUS_AT_100;
    private static final ForgeConfigSpec.DoubleValue STAMINA_RECOVERY_BONUS_AT_100;
    private static final ForgeConfigSpec.DoubleValue RAPIDITE_ATTACK_SPEED_BONUS_AT_100;
    private static final ForgeConfigSpec.DoubleValue AGILITY_MOVEMENT_SPEED_BONUS_AT_100;
    private static final ForgeConfigSpec.DoubleValue AGILITY_SPRINTING_SPEED_BONUS_AT_100;

    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("combat");
        WEAPON_DAMAGE_BASE = builder.defineInRange("weaponDamageBase", 1.0, 1.0, 10.0);
        WEAPON_DAMAGE_SCALE = builder.defineInRange("weaponDamageScale", 9.0, 0.0, 99.0);
        WEAPON_DAMAGE_EXPONENT = builder.defineInRange(
                "weaponDamageExponent", 1.5, 0.1, 5.0);
        PHYSICAL_RESISTANCE_CAP = builder.defineInRange(
                "physicalResistanceCap", 0.65, 0.0, 0.95);
        PHYSICAL_ENDURANCE_CAP = builder.defineInRange(
                "physicalEnduranceCap", 0.35, 0.0, 0.95);
        builder.pop();
        builder.push("endurance");
        STAMINA_CAPACITY_BONUS_AT_100 = builder.defineInRange(
                "staminaCapacityBonusAt100", 1.0, 0.0, 5.0);
        STAMINA_RECOVERY_BONUS_AT_100 = builder.defineInRange(
                "staminaRecoveryBonusAt100", 0.5, 0.0, 5.0);
        builder.pop();
        builder.push("mobility");
        RAPIDITE_ATTACK_SPEED_BONUS_AT_100 = builder.defineInRange(
                "rapiditeAttackSpeedBonusAt100", 0.30, 0.0, 2.0);
        AGILITY_MOVEMENT_SPEED_BONUS_AT_100 = builder.defineInRange(
                "agilityMovementSpeedBonusAt100", 0.20, 0.0, 2.0);
        AGILITY_SPRINTING_SPEED_BONUS_AT_100 = builder.defineInRange(
                "agilitySprintingSpeedBonusAt100", 0.10, 0.0, 2.0);
        builder.pop();
        SPEC = builder.build();
    }

    private StatModServerConfig() {
    }

    public static CombatScalingRules snapshot() {
        return new CombatScalingRules(
                WEAPON_DAMAGE_BASE.get(),
                WEAPON_DAMAGE_SCALE.get(),
                WEAPON_DAMAGE_EXPONENT.get(),
                PHYSICAL_RESISTANCE_CAP.get(),
                PHYSICAL_ENDURANCE_CAP.get());
    }

    public static double staminaCapacityBonusAt100() {
        return STAMINA_CAPACITY_BONUS_AT_100.get();
    }

    public static double staminaRecoveryBonusAt100() {
        return STAMINA_RECOVERY_BONUS_AT_100.get();
    }

    public static double rapiditeAttackSpeedBonusAt100() {
        return RAPIDITE_ATTACK_SPEED_BONUS_AT_100.get();
    }

    public static double agilityMovementSpeedBonusAt100() {
        return AGILITY_MOVEMENT_SPEED_BONUS_AT_100.get();
    }

    public static double agilitySprintingSpeedBonusAt100() {
        return AGILITY_SPRINTING_SPEED_BONUS_AT_100.get();
    }
}
