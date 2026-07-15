package tong.statmod.effects;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.config.StatModServerConfig;
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
    MANA_REGEN(
            "mana_regen", "4192f4b8-0799-4a2c-b32b-9eb5e457b1cb",
            StatType.MANA_POOL, BonusKind.MANA_REGEN),
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
        return LinearStatScaling.bonus(stats.get(stat).level(), bonusKind.maximumAt100());
    }

    public enum BonusKind {
        SPELL_POWER,
        CAST_TIME,
        COOLDOWN,
        MANA_CAPACITY,
        MANA_REGEN,
        SPELL_RESIST;

        double maximumAt100() {
            return switch (this) {
                case SPELL_POWER -> StatModServerConfig.arcanePowerSpellPowerBonusAt100();
                case CAST_TIME -> StatModServerConfig.castingSpeedCastTimeBonusAt100();
                case COOLDOWN -> StatModServerConfig.castingSpeedCooldownBonusAt100();
                case MANA_CAPACITY -> StatModServerConfig.manaPoolCapacityBonusAt100();
                case MANA_REGEN -> StatModServerConfig.manaPoolRegenBonusAt100();
                case SPELL_RESIST -> StatModServerConfig.magicResistanceBonusAt100();
            };
        }
    }
}
