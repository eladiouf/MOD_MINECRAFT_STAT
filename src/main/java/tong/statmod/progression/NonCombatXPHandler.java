package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.StatUpdatePacket;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class NonCombatXPHandler {

    public static void awardXp(ServerPlayer player, ActionType action) {
        player.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
            stats.addXp(action.primaryStat.index, action.baseXp);
            NetworkHandler.sendToPlayer(
                new StatUpdatePacket(action.primaryStat.index, stats.getLevel(action.primaryStat.index), stats.getXp(action.primaryStat.index)),
                player);
        });
    }
}
