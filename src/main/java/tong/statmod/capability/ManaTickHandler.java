package tong.statmod.capability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.api.PluginManager;
import tong.statmod.network.ManaSyncPacket;
import tong.statmod.network.NetworkHandler;
import tong.statmod.stats.StatCalculator;
import tong.statmod.stats.StatType;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class ManaTickHandler {
    private static final UUID MANA_SPEED_UUID = UUID.fromString("b1c2d3e4-f5a6-7890-bcde-f12345678901");

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;

        CapabilityHelper.withStats(player, stats -> {
            // Sync mana to client every 40 ticks (2s)
            if (player.tickCount % 40 == 0) {
                NetworkHandler.sendToPlayer(new ManaSyncPacket(stats.getMana()), player);
            }
            stats.tickMana(player);
            float baseRegen = 1.0f + StatCalculator.getManaBonus(stats.getLevel(StatType.MANA_POOL.index)) * 0.01f;
            float pluginRegen = PluginManager.fireManaRegen(player);
            stats.regenMana(baseRegen + pluginRegen);

            // Dynamic mana sprint boost
            AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (moveSpeed != null) {
                float manaPct = stats.getMaxMana() > 0 ? stats.getMana() / stats.getMaxMana() : 0;
                boolean hasBoost = moveSpeed.getModifier(MANA_SPEED_UUID) != null;
                if (manaPct > 0.5f && !hasBoost) {
                    moveSpeed.addPermanentModifier(new AttributeModifier(
                        MANA_SPEED_UUID, "Mana Sprint Boost", 0.1,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
                } else if (manaPct <= 0.5f && hasBoost) {
                    moveSpeed.removeModifier(MANA_SPEED_UUID);
                }
            }
        });
    }
}
