package tong.statmod.effects;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.config.StatModServerConfig;

public enum StaminaAttributeTarget {
    EPIC_FIGHT_CAPACITY(
            "epicfight", "staminar", "1a7e5f2c-b850-42fb-9381-ad535697fe72", BonusKind.CAPACITY),
    EPIC_FIGHT_RECOVERY(
            "epicfight", "stamina_regen", "c78f3882-e45e-4208-9345-249c94679b2d", BonusKind.RECOVERY),
    PARCOOL_CAPACITY(
            "parcool", "max_stamina", "26a607d0-5724-48e0-9741-402708c18349", BonusKind.CAPACITY),
    PARCOOL_RECOVERY(
            "parcool", "stamina_recovery", "0b8d0999-79f8-4573-923e-60bd83fa5e02", BonusKind.RECOVERY);

    private final ResourceLocation id;
    private final UUID modifierId;
    private final BonusKind bonusKind;

    StaminaAttributeTarget(String namespace, String path, String modifierId, BonusKind bonusKind) {
        this.id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        this.modifierId = UUID.fromString(modifierId);
        this.bonusKind = bonusKind;
    }

    public ResourceLocation id() {
        return id;
    }

    public UUID modifierId() {
        return modifierId;
    }

    public BonusKind bonusKind() {
        return bonusKind;
    }

    public double amount(int level) {
        double maximum = bonusKind == BonusKind.CAPACITY
                ? StatModServerConfig.staminaCapacityBonusAt100()
                : StatModServerConfig.staminaRecoveryBonusAt100();
        return LinearStatScaling.bonus(level, maximum);
    }

    public enum BonusKind {
        CAPACITY,
        RECOVERY
    }
}
