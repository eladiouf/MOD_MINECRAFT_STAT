package tong.statmod.effects;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.config.StatModServerConfig;
import tong.statmod.perks.AutomaticPerkBonuses;
import tong.statmod.perks.AutomaticPerkEffect;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

public enum MobilityAttributeTarget {
    MAIN_HAND_ATTACK_SPEED(
            "minecraft", "generic.attack_speed", "9b0b9734-46f8-4c25-921f-a37fba3480a6",
            StatType.RAPIDITE, BonusKind.RAPIDITE_ATTACK_SPEED),
    EPIC_FIGHT_OFFHAND_ATTACK_SPEED(
            "epicfight", "offhand_attack_speed", "8beb72a7-b10c-4714-836c-60854fd6f53c",
            StatType.RAPIDITE, BonusKind.RAPIDITE_ATTACK_SPEED),
    MOVEMENT_SPEED(
            "minecraft", "generic.movement_speed", "1251b5b9-ed60-42b1-85be-c0ad550b7c5b",
            StatType.AGILITY, BonusKind.AGILITY_MOVEMENT_SPEED),
    PUFFISH_SPRINTING_SPEED(
            "puffish_attributes", "sprinting_speed", "40222eb0-eb4d-449b-b147-aaddfef7e54e",
            StatType.AGILITY, BonusKind.AGILITY_SPRINTING_SPEED);

    private final ResourceLocation id;
    private final UUID modifierId;
    private final StatType stat;
    private final BonusKind bonusKind;

    MobilityAttributeTarget(String namespace, String path, String modifierId,
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
        return stat == StatType.RAPIDITE
                ? AutomaticPerkEffect.RAPIDITE_ATTACK_SPEED
                : AutomaticPerkEffect.AGILITY_MOVEMENT;
    }

    public enum BonusKind {
        RAPIDITE_ATTACK_SPEED,
        AGILITY_MOVEMENT_SPEED,
        AGILITY_SPRINTING_SPEED;

        double maximumAt100() {
            return switch (this) {
                case RAPIDITE_ATTACK_SPEED -> StatModServerConfig.rapiditeAttackSpeedBonusAt100();
                case AGILITY_MOVEMENT_SPEED -> StatModServerConfig.agilityMovementSpeedBonusAt100();
                case AGILITY_SPRINTING_SPEED -> StatModServerConfig.agilitySprintingSpeedBonusAt100();
            };
        }
    }
}
