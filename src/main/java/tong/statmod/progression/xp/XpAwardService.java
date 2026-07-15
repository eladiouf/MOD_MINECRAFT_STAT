package tong.statmod.progression.xp;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.network.StatNetwork;
import tong.statmod.stats.PlayerStats;

public final class XpAwardService {
    private XpAwardService() {
    }

    public static boolean award(ServerPlayer player, List<XpAction> actions, long tick) {
        if (player == null || player instanceof FakePlayer
                || player.isCreative() || player.isSpectator()) {
            return false;
        }
        PlayerStats stats = player.getCapability(StatCapabilities.PLAYER_STATS)
                .resolve().orElse(null);
        PlayerXpState state = player.getCapability(StatCapabilities.PLAYER_XP_STATE)
                .resolve().orElse(null);
        if (stats == null || state == null) {
            return false;
        }
        XpAwardResult result = XpAwardCoordinator.apply(stats, state, actions, tick);
        if (!result.changed()) {
            return false;
        }
        StatNetwork.sendSnapshot(player);
        return true;
    }
}
