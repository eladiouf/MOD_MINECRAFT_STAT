package tong.statmod.effects;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.config.StatModServerConfig;
import tong.statmod.perks.AutomaticPerkBonuses;
import tong.statmod.perks.AutomaticPerkEffect;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

public enum MagicAttributeTarget {
    SPELL_POWER(
            "spell_power", "c4073a9e-3932-4dbd-a03e-067b34a7a47f",
            StatType.ARCANE_POWER, BonusKind.SPELL_POWER),
    CAST_TIME(
            "cast_time_reduction", "5414ef15-24e9-47cb-a6ce-fc060aa04788",
            StatType.CASTING_SPEED, BonusKind.CAST_TIME),
    COOLDOWN(
            "cooldown_reduction", "f7ea3251-9737-4105-8c95-9ec84ed1c3ba",
            StatType.CASTING_SPEED, BonusKind.COOLDOWN),
    MAX_MANA(
            "max_mana", "8c82cb39-5ca4-4f14-b0e1-34cc2c9d98ba",
            StatType.MANA_POOL, BonusKind.MANA_CAPACITY),
    SPELL_RESIST(
            "spell_resist", "e57b31fc-563c-4191-a680-02301fd82869",
            StatType.MAGIC_RESISTANCE, BonusKind.SPELL_RESIST);

    private final ResourceLocation id;
    private final UUID modifierId;
    private final StatType stat;
    private final BonusKind bonusKind;

    MagicAttributeTarget(String path, String modifierId, StatType stat, BonusKind bonusKind) {
        this.id = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", path);
        this.modifierId = UUID.fromString(modifierId);
        this.stat = stat;
        this.bonusKind = bonusKind;
    }

    public ResourceLocation id() {
        return id;
    }

    public UUID modifierId() {
        return modifierId;
    }

    public StatType stat() {
        return stat;
    }

    public BonusKind bonusKind() {
        return bonusKind;
    }

    public double amount(PlayerStats stats) {
        if (this == MAX_MANA) {
            int level = stats.get(StatType.MANA_POOL).level();
            double target = PlayerBaseBalanceRules.maxMana(
                    level, PlayerBaseBalanceRules.manaMilestones(level));
            return target / PlayerBaseBalanceRules.BASE_MAX_MANA - 1.0D;
        }
        return LinearStatScaling.bonus(stats.get(stat).level(), bonusKind.maximumAt100());
    }

    public double amount(PlayerStats stats, AutomaticPerkBonuses bonuses) {
        if (this == MAX_MANA) {
            return amount(stats);
        }
        return amount(stats) + bonuses.amount(perkEffect());
    }

    public AutomaticPerkEffect perkEffect() {
        return switch (stat) {
            case ARCANE_POWER -> AutomaticPerkEffect.ARCANE_SPELL_POWER;
            case CASTING_SPEED -> AutomaticPerkEffect.CASTING_SPEED_REDUCTIONS;
            case MANA_POOL -> AutomaticPerkEffect.MANA_CAPACITY_REGEN;
            case MAGIC_RESISTANCE -> AutomaticPerkEffect.MAGIC_RESISTANCE;
            default -> throw new IllegalStateException("unsupported magic perk stat: " + stat);
        };
    }

    public enum BonusKind {
        SPELL_POWER,
        CAST_TIME,
        COOLDOWN,
        MANA_CAPACITY,
        SPELL_RESIST;

        double maximumAt100() {
            return switch (this) {
                case SPELL_POWER -> StatModServerConfig.arcanePowerSpellPowerBonusAt100();
                case CAST_TIME -> StatModServerConfig.castingSpeedCastTimeBonusAt100();
                case COOLDOWN -> StatModServerConfig.castingSpeedCooldownBonusAt100();
                case MANA_CAPACITY -> 1.91D;
                case SPELL_RESIST -> StatModServerConfig.magicResistanceBonusAt100();
            };
        }
    }
}
