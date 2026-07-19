package tong.statmod.effects;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.network.StatNetwork;
import tong.statmod.perks.AutomaticPerkBonuses;
import tong.statmod.perks.AutomaticPerkEffect;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

public final class HunterTrackingService {
    private HunterTrackingService() {
    }

    public static boolean mark(ServerPlayer player, LivingEntity target) {
        if (player == null || !(target instanceof Enemy) || !target.isAlive()) {
            return false;
        }
        PlayerStats stats = player.getCapability(StatCapabilities.PLAYER_STATS)
                .resolve().orElse(null);
        if (stats == null) {
            return false;
        }
        int level = stats.get(StatType.TRACKING).level();
        double score = AutomaticPerkBonuses.from(stats)
                .amount(AutomaticPerkEffect.TRACKING_FOCUS);
        int duration = HunterPerceptionRules.trackingDurationTicks(level, score);
        if (duration == 0) {
            return false;
        }
        StatNetwork.sendTrackedPrey(player, target.getId(), duration);
        return true;
    }
}
