package tong.statmod.stats;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.PlayerStatsProvider;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class StatEffectApplier {
    private static final UUID ENDURANCE_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                float multiplier = 1.0f;
                multiplier += StatCalculator.getDamageBonus(stats.getLevel(StatType.BRUTE_FORCE.index));
                multiplier += StatCalculator.getDamageBonus(stats.getLevel(StatType.BLADE_TECHNIQUE.index));
                event.setAmount(event.getAmount() * multiplier);
            });
        }

        if (event.getEntity() instanceof Player player) {
            player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                float reduction = StatCalculator.getDamageReduction(stats.getLevel(StatType.PHYSICAL_RESISTANCE.index));
                event.setAmount(event.getAmount() * (1.0f - reduction));
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.removeModifier(ENDURANCE_UUID);
                    float bonusHearts = StatCalculator.getEnduranceHearts(stats.getLevel(StatType.PHYSICAL_ENDURANCE.index));
                    if (bonusHearts > 0) {
                        maxHealth.addPermanentModifier(new AttributeModifier(
                            ENDURANCE_UUID, "Endurance Bonus", bonusHearts, AttributeModifier.Operation.ADDITION));
                    }
                }
            });
        }
    }
}
