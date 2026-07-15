package tong.statmod.perks;

import java.util.EnumMap;
import java.util.Map;
import tong.statmod.config.StatModServerConfig;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

public final class AutomaticPerkBonuses {
    private static final double MAX_AGGREGATE = 0.75;
    private final Map<AutomaticPerkEffect, Double> amounts;

    private AutomaticPerkBonuses(Map<AutomaticPerkEffect, Double> amounts) {
        this.amounts = Map.copyOf(amounts);
    }

    public static AutomaticPerkBonuses from(PlayerStats stats) {
        EnumMap<StatType, Integer> levels = new EnumMap<>(StatType.class);
        if (stats != null) {
            stats.snapshot().forEach((type, value) -> levels.put(type, value.level()));
        }
        EnumMap<AutomaticPerkEffect, Double> amounts =
                new EnumMap<>(AutomaticPerkEffect.class);
        AutomaticPerkResolver.active(levels).forEach(definition -> amounts.merge(
                definition.effect(), configuredAmount(definition.effect()), Double::sum));
        amounts.replaceAll((effect, amount) -> Math.min(MAX_AGGREGATE, amount));
        return new AutomaticPerkBonuses(amounts);
    }

    public double amount(AutomaticPerkEffect effect) {
        return effect == null ? 0.0 : amounts.getOrDefault(effect, 0.0);
    }

    private static double configuredAmount(AutomaticPerkEffect effect) {
        return switch (effect) {
            case RAPIDITE_ATTACK_SPEED -> StatModServerConfig.rapiditeAttackSpeedPerMilestone();
            case AGILITY_MOVEMENT -> StatModServerConfig.agilityMovementPerMilestone();
            case ENDURANCE_STAMINA -> StatModServerConfig.enduranceStaminaPerMilestone();
            case ARCANE_SPELL_POWER -> StatModServerConfig.arcaneSpellPowerPerMilestone();
            case CASTING_SPEED_REDUCTIONS ->
                    StatModServerConfig.castingSpeedReductionsPerMilestone();
            case MANA_CAPACITY_REGEN -> StatModServerConfig.manaCapacityRegenPerMilestone();
            case MAGIC_RESISTANCE -> StatModServerConfig.magicResistancePerMilestone();
            case BRUTE_FORCE_DAMAGE -> StatModServerConfig.bruteForceDamagePerMilestone();
            case BLADE_TECHNIQUE_DAMAGE ->
                    StatModServerConfig.bladeTechniqueDamagePerMilestone();
            case PRECISION_DAMAGE -> StatModServerConfig.precisionDamagePerMilestone();
            case PHYSICAL_RESISTANCE ->
                    StatModServerConfig.physicalResistancePerMilestone();
        };
    }
}
