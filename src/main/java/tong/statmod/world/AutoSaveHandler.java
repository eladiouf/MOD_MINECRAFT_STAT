package tong.statmod.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class AutoSaveHandler {
    private static int tickCounter = 0;
    private static final int SAVE_INTERVAL = 20 * 60 * 5;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;

        tickCounter++;
        if (tickCounter >= SAVE_INTERVAL) {
            tickCounter = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                CapabilityHelper.withStats(player, stats -> {
                    stats.serializeNBT();
                });
                CapabilityHelper.withPerks(player, perks -> perks.serializeNBT());
            }
            STATMod.LOGGER.debug("Auto-saved stats for {} players", server.getPlayerList().getPlayerCount());
        }
    }
}
