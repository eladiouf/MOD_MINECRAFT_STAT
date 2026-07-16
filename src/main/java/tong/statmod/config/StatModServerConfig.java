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
    private static final ForgeConfigSpec.DoubleValue ARCANE_POWER_SPELL_POWER_BONUS_AT_100;
    private static final ForgeConfigSpec.DoubleValue CASTING_SPEED_CAST_TIME_BONUS_AT_100;
    private static final ForgeConfigSpec.DoubleValue CASTING_SPEED_COOLDOWN_BONUS_AT_100;
    private static final ForgeConfigSpec.DoubleValue MANA_POOL_CAPACITY_BONUS_AT_100;
    private static final ForgeConfigSpec.DoubleValue MAGIC_RESISTANCE_BONUS_AT_100;
    private static final ForgeConfigSpec.DoubleValue RAPIDITE_ATTACK_SPEED_PER_MILESTONE;
    private static final ForgeConfigSpec.DoubleValue AGILITY_MOVEMENT_PER_MILESTONE;
    private static final ForgeConfigSpec.DoubleValue ENDURANCE_STAMINA_PER_MILESTONE;
    private static final ForgeConfigSpec.DoubleValue ARCANE_SPELL_POWER_PER_MILESTONE;
    private static final ForgeConfigSpec.DoubleValue CASTING_SPEED_REDUCTIONS_PER_MILESTONE;
    private static final ForgeConfigSpec.DoubleValue MANA_CAPACITY_REGEN_PER_MILESTONE;
    private static final ForgeConfigSpec.DoubleValue MAGIC_RESISTANCE_PER_MILESTONE;
    private static final ForgeConfigSpec.DoubleValue BRUTE_FORCE_DAMAGE_PER_MILESTONE;
    private static final ForgeConfigSpec.DoubleValue BLADE_TECHNIQUE_DAMAGE_PER_MILESTONE;
    private static final ForgeConfigSpec.DoubleValue PRECISION_DAMAGE_PER_MILESTONE;
    private static final ForgeConfigSpec.DoubleValue PHYSICAL_RESISTANCE_PER_MILESTONE;

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
        builder.push("magic");
        ARCANE_POWER_SPELL_POWER_BONUS_AT_100 = builder.defineInRange(
                "arcanePowerSpellPowerBonusAt100", 1.00, 0.0, 10.0);
        CASTING_SPEED_CAST_TIME_BONUS_AT_100 = builder.defineInRange(
                "castingSpeedCastTimeBonusAt100", 0.30, 0.0, 0.90);
        CASTING_SPEED_COOLDOWN_BONUS_AT_100 = builder.defineInRange(
                "castingSpeedCooldownBonusAt100", 0.20, 0.0, 0.90);
        MANA_POOL_CAPACITY_BONUS_AT_100 = builder.defineInRange(
                "manaPoolCapacityBonusAt100", 2.00, 0.0, 20.0);
        MAGIC_RESISTANCE_BONUS_AT_100 = builder.defineInRange(
                "magicResistanceBonusAt100", 0.50, 0.0, 0.90);
        builder.pop();
        builder.push("automaticPerks");
        RAPIDITE_ATTACK_SPEED_PER_MILESTONE = builder.defineInRange(
                "rapiditeAttackSpeedPerMilestone", 0.02, 0.0, 0.25);
        AGILITY_MOVEMENT_PER_MILESTONE = builder.defineInRange(
                "agilityMovementPerMilestone", 0.02, 0.0, 0.25);
        ENDURANCE_STAMINA_PER_MILESTONE = builder.defineInRange(
                "enduranceStaminaPerMilestone", 0.04, 0.0, 0.25);
        ARCANE_SPELL_POWER_PER_MILESTONE = builder.defineInRange(
                "arcaneSpellPowerPerMilestone", 0.03, 0.0, 0.25);
        CASTING_SPEED_REDUCTIONS_PER_MILESTONE = builder.defineInRange(
                "castingSpeedReductionsPerMilestone", 0.02, 0.0, 0.25);
        MANA_CAPACITY_REGEN_PER_MILESTONE = builder.defineInRange(
                "manaCapacityRegenPerMilestone", 0.03, 0.0, 0.25);
        MAGIC_RESISTANCE_PER_MILESTONE = builder.defineInRange(
                "magicResistancePerMilestone", 0.02, 0.0, 0.25);
        BRUTE_FORCE_DAMAGE_PER_MILESTONE = builder.defineInRange(
                "bruteForceDamagePerMilestone", 0.05, 0.0, 0.25);
        BLADE_TECHNIQUE_DAMAGE_PER_MILESTONE = builder.defineInRange(
                "bladeTechniqueDamagePerMilestone", 0.05, 0.0, 0.25);
        PRECISION_DAMAGE_PER_MILESTONE = builder.defineInRange(
                "precisionDamagePerMilestone", 0.05, 0.0, 0.25);
        PHYSICAL_RESISTANCE_PER_MILESTONE = builder.defineInRange(
                "physicalResistancePerMilestone", 0.02, 0.0, 0.25);
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

    public static double arcanePowerSpellPowerBonusAt100() {
        return ARCANE_POWER_SPELL_POWER_BONUS_AT_100.get();
    }

    public static double castingSpeedCastTimeBonusAt100() {
        return CASTING_SPEED_CAST_TIME_BONUS_AT_100.get();
    }

    public static double castingSpeedCooldownBonusAt100() {
        return CASTING_SPEED_COOLDOWN_BONUS_AT_100.get();
    }

    public static double manaPoolCapacityBonusAt100() {
        return MANA_POOL_CAPACITY_BONUS_AT_100.get();
    }

    public static double magicResistanceBonusAt100() {
        return MAGIC_RESISTANCE_BONUS_AT_100.get();
    }

    public static double rapiditeAttackSpeedPerMilestone() {
        return RAPIDITE_ATTACK_SPEED_PER_MILESTONE.get();
    }

    public static double agilityMovementPerMilestone() {
        return AGILITY_MOVEMENT_PER_MILESTONE.get();
    }

    public static double enduranceStaminaPerMilestone() {
        return ENDURANCE_STAMINA_PER_MILESTONE.get();
    }

    public static double arcaneSpellPowerPerMilestone() {
        return ARCANE_SPELL_POWER_PER_MILESTONE.get();
    }

    public static double castingSpeedReductionsPerMilestone() {
        return CASTING_SPEED_REDUCTIONS_PER_MILESTONE.get();
    }

    public static double manaCapacityRegenPerMilestone() {
        return MANA_CAPACITY_REGEN_PER_MILESTONE.get();
    }

    public static double magicResistancePerMilestone() {
        return MAGIC_RESISTANCE_PER_MILESTONE.get();
    }

    public static double bruteForceDamagePerMilestone() {
        return BRUTE_FORCE_DAMAGE_PER_MILESTONE.get();
    }

    public static double bladeTechniqueDamagePerMilestone() {
        return BLADE_TECHNIQUE_DAMAGE_PER_MILESTONE.get();
    }

    public static double precisionDamagePerMilestone() {
        return PRECISION_DAMAGE_PER_MILESTONE.get();
    }

    public static double physicalResistancePerMilestone() {
        return PHYSICAL_RESISTANCE_PER_MILESTONE.get();
    }
}
