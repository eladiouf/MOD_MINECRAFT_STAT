package tong.statmod.effects;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

public final class PlayerAttributeEffects {
    private static final String ENDURANCE_MODIFIER_NAME = "STAT Mod Physical Endurance";

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

        int level = stats.get(StatType.PHYSICAL_ENDURANCE).level();
        for (StaminaAttributeTarget target : StaminaAttributeTarget.values()) {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(target.id());
            if (attribute == null) {
                continue;
            }
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }

            instance.removeModifier(target.modifierId());
            double amount = target.amount(level);
            if (amount > 0.0) {
                instance.addTransientModifier(new AttributeModifier(
                        target.modifierId(),
                        ENDURANCE_MODIFIER_NAME,
                        amount,
                        AttributeModifier.Operation.MULTIPLY_BASE));
            }
        }
    }
}
