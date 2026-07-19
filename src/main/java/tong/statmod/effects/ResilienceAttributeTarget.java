package tong.statmod.effects;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.config.StatModServerConfig;
import tong.statmod.perks.AutomaticPerkBonuses;
import tong.statmod.perks.AutomaticPerkEffect;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

public enum ResilienceAttributeTarget {
    WILLPOWER_KNOCKBACK_RESISTANCE(
            "minecraft", "generic.knockback_resistance", "39d73d6e-927b-4ec5-a0ee-602931a7bc8e",
            StatType.WILLPOWER, BonusKind.WILLPOWER_KNOCKBACK_RESISTANCE),
    INTIMIDATION_ARMOR_TOUGHNESS(
            "minecraft", "generic.armor_toughness", "ea9d7fc2-b5e1-4560-aadd-9ebd65fd6e94",
            StatType.INTIMIDATION, BonusKind.INTIMIDATION_ARMOR_TOUGHNESS);

    private final ResourceLocation id;
    private final UUID modifierId;
    private final StatType stat;
    private final BonusKind bonusKind;

    ResilienceAttributeTarget(String namespace, String path, String modifierId,
            StatType stat, BonusKind bonusKind) {
        this.id = ResourceLocation.fromNamespaceAndPath(namespace, path);
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
        return LinearStatScaling.bonus(stats.get(stat).level(), bonusKind.maximumAt100());
    }

    public double amount(PlayerStats stats, AutomaticPerkBonuses bonuses) {
        return amount(stats) + bonuses.amount(perkEffect());
    }

    public AutomaticPerkEffect perkEffect() {
        return stat == StatType.WILLPOWER
                ? AutomaticPerkEffect.WILLPOWER_KNOCKBACK_RESISTANCE
                : AutomaticPerkEffect.INTIMIDATION_ARMOR_TOUGHNESS;
    }

    public enum BonusKind {
        WILLPOWER_KNOCKBACK_RESISTANCE,
        INTIMIDATION_ARMOR_TOUGHNESS;

        double maximumAt100() {
            return switch (this) {
                case WILLPOWER_KNOCKBACK_RESISTANCE -> StatModServerConfig.willpowerKnockbackResistanceBonusAt100();
                case INTIMIDATION_ARMOR_TOUGHNESS -> StatModServerConfig.intimidationArmorToughnessBonusAt100();
            };
        }
    }
}
