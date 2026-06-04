package tong.statmod.capability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.api.PluginManager;
import tong.statmod.stats.StatCalculator;
import tong.statmod.stats.StatType;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class ManaTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;

        CapabilityHelper.withStats(player, stats -> {
            stats.tickMana(player);
            float baseRegen = 1.0f + StatCalculator.getManaBonus(stats.getLevel(StatType.MANA_POOL.index)) * 0.01f;
            float pluginRegen = PluginManager.fireManaRegen(player);
            stats.regenMana(baseRegen + pluginRegen);
        });
    }
}
