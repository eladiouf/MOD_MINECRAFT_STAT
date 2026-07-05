package tong.statmod.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.network.SyncHelper;
import tong.statmod.progression.LevelUpHandler;
import tong.statmod.stats.StatFamily;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class STATModAPIImpl implements STATModAPI {
    private static PlayerStatData stats(Player player) {
        return player.getData(ModAttachments.STATS);
    }

    @Override
    public boolean isPerkUnlocked(Player player, int perkId) {
        return stats(player).isPerkUnlocked(perkId);
    }

    @Override
    public Set<Integer> getUnlockedPerkIds(Player player) {
        return Arrays.stream(stats(player).getUnlockedPerks())
                .boxed()
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public int getStatLevel(Player player, int statIndex) {
        return stats(player).getLevel(statIndex);
    }

    @Override
    public int getGlobalLevel(Player player) {
        return stats(player).getGlobalLevel();
    }

    @Override
    public int getPerkPoints(Player player, String familySlug) {
        for (StatFamily family : StatFamily.values()) {
            if (family.slug.equals(familySlug)) {
                return stats(player).getPerkPointsForFamily(family);
            }
        }
        return 0;
    }

    @Override
    public void addXpToStat(Player player, int statIndex, int xpAmount) {
        PlayerStatData data = stats(player);
        RaceEffectApplier.addScaledXp(player, statIndex, xpAmount, data, false);
        syncStatsIfServer(player, data);
    }

    @Override
    public void addXpRaw(Player player, int statIndex, int xpAmount) {
        PlayerStatData data = stats(player);
        RaceEffectApplier.addRawXp(player, statIndex, xpAmount, data);
        syncStatsIfServer(player, data);
    }

    private static void syncStatsIfServer(Player player, PlayerStatData data) {
        if (player instanceof ServerPlayer serverPlayer) {
            int granted = LevelUpHandler.grantPendingPerkTiers(data);
            SyncHelper.syncStats(serverPlayer);
            if (granted > 0) {
                SyncHelper.syncPerks(serverPlayer);
            }
        }
    }
}
