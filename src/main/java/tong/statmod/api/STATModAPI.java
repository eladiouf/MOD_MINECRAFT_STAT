package tong.statmod.api;

import net.minecraft.world.entity.player.Player;

import java.util.Set;

public interface STATModAPI {
    boolean isPerkUnlocked(Player player, int perkId);
    Set<Integer> getUnlockedPerkIds(Player player);
    int getStatLevel(Player player, int statIndex);
    int getGlobalLevel(Player player);
    int getPerkPoints(Player player, String familySlug);
    void addXpToStat(Player player, int statIndex, int xpAmount);
    void addXpRaw(Player player, int statIndex, int xpAmount);
}
