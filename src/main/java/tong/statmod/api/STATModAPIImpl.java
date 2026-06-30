package tong.statmod.api;

import net.minecraft.world.entity.player.Player;
import tong.statmod.integration.RaceEffectApplier;
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
        RaceEffectApplier.addScaledXp(player, statIndex, xpAmount, stats(player), false);
    }

    @Override
    public void addXpRaw(Player player, int statIndex, int xpAmount) {
        stats(player).addXp(statIndex, xpAmount);
    }
}
