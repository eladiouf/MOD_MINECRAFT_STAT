package tong.statmod.progression.xp;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.effects.PlayerAttributeEffects;
import tong.statmod.network.StatNetwork;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

public final class XpAwardService {
    private XpAwardService() {
    }

    public static boolean isEligible(ServerPlayer player) {
        return player != null && !(player instanceof FakePlayer)
                && !player.isCreative() && !player.isSpectator();
    }

    public static boolean award(ServerPlayer player, List<XpAction> actions, long tick) {
        if (!isEligible(player)) {
            return false;
        }
        PlayerStats stats = player.getCapability(StatCapabilities.PLAYER_STATS)
                .resolve().orElse(null);
        PlayerXpState state = player.getCapability(StatCapabilities.PLAYER_XP_STATE)
                .resolve().orElse(null);
        if (stats == null || state == null) {
            return false;
        }
        int beforeEnduranceLevel = stats.get(StatType.PHYSICAL_ENDURANCE).level();
        XpAwardResult result = XpAwardCoordinator.apply(stats, state, actions, tick);
        if (!result.changed()) {
            return false;
        }
        int afterEnduranceLevel = stats.get(StatType.PHYSICAL_ENDURANCE).level();
        if (afterEnduranceLevel != beforeEnduranceLevel) {
            PlayerAttributeEffects.refresh(player);
        }
        StatNetwork.sendSnapshot(player);
        return true;
    }
}
