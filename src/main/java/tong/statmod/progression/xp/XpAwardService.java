package tong.statmod.progression.xp;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.effects.AttributeEffectLevels;
import tong.statmod.effects.PlayerAttributeEffects;
import tong.statmod.network.StatNetwork;
import tong.statmod.stats.PlayerStats;

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
        AttributeEffectLevels beforeEffects = AttributeEffectLevels.from(stats);
        XpAwardResult result = XpAwardCoordinator.apply(stats, state, actions, tick);
        if (!result.changed()) {
            return false;
        }
        AttributeEffectLevels afterEffects = AttributeEffectLevels.from(stats);
        if (!afterEffects.equals(beforeEffects)) {
            PlayerAttributeEffects.refresh(player);
        }
        StatNetwork.sendSnapshot(player);
        return true;
    }
}
