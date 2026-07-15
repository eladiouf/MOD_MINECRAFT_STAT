package tong.statmod.effects;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.perks.AutomaticPerkBonuses;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

public final class PlayerAttributeEffects {
    private static final String ENDURANCE_MODIFIER_NAME = "STAT Mod Physical Endurance";
    private static final String RAPIDITE_MODIFIER_NAME = "STAT Mod Rapidité";
    private static final String AGILITY_MODIFIER_NAME = "STAT Mod Agility";

    private PlayerAttributeEffects() {
    }

    public static void refresh(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlayerStats stats = player.getCapability(StatCapabilities.PLAYER_STATS)
                .resolve()
                .orElse(null);
        if (stats == null) {
            return;
        }

        AutomaticPerkBonuses bonuses = AutomaticPerkBonuses.from(stats);
        int level = stats.get(StatType.PHYSICAL_ENDURANCE).level();
        for (StaminaAttributeTarget target : StaminaAttributeTarget.values()) {
            replaceModifier(player, target.id(), target.modifierId(),
                    ENDURANCE_MODIFIER_NAME, target.amount(level, bonuses));
        }
        for (MobilityAttributeTarget target : MobilityAttributeTarget.values()) {
            String name = target.stat() == StatType.RAPIDITE
                    ? RAPIDITE_MODIFIER_NAME : AGILITY_MODIFIER_NAME;
            replaceModifier(player, target.id(), target.modifierId(), name,
                    target.amount(stats, bonuses));
        }
        for (MagicAttributeTarget target : MagicAttributeTarget.values()) {
            replaceModifier(player, target.id(), target.modifierId(),
                    "STAT Mod " + target.stat().id(), target.amount(stats, bonuses));
        }
    }

    private static void replaceModifier(ServerPlayer player, ResourceLocation attributeId,
            UUID modifierId, String name, double amount) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        instance.removeModifier(modifierId);
        if (amount > 0.0) {
            instance.addTransientModifier(new AttributeModifier(
                    modifierId,
                    name,
                    amount,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }
}
