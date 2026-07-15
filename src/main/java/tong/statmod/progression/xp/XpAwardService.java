package tong.statmod.progression.xp;

import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.effects.AttributeEffectLevels;
import tong.statmod.effects.PlayerAttributeEffects;
import tong.statmod.network.StatNetwork;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

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
        Map<StatType, StatValue> beforeStats = stats.snapshot();
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
        for (XpProgressNotice notice : XpNoticeCalculation.from(
                beforeStats, stats.snapshot(), result.accepted())) {
            StatNetwork.sendProgressNotice(
                    player, notice.stat(), notice.awardedXp(),
                    notice.newLevel(), notice.levelsGained());
        }
        return true;
    }
}
