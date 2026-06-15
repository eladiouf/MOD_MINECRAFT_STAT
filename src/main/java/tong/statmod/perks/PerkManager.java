package tong.statmod.perks;

import net.minecraft.world.entity.player.Player;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.integration.SkillPerkGate;
import tong.statmod.storage.PlayerStatData;

public class PerkManager {
    private final PlayerStatData statData;

    public PerkManager(PlayerStatData statData) {
        this.statData = statData;
    }

    public boolean isUnlocked(Perk perk) {
        return perk != null && statData.isPerkUnlocked(perk.id);
    }

    public int[] getUnlockedIds() {
        return statData.getUnlockedPerks();
    }

    public int getPointsForStat(int statIndex) {
        return statData.getPerkPointsForStat(statIndex);
    }

    public boolean canUnlock(Perk perk) {
        return canUnlock(perk, null);
    }

    public boolean canUnlock(Perk perk, Player player) {
        if (perk == null || isUnlocked(perk)) return false;
        int statLevel = statData.getLevel(perk.stat.index);
        if (statLevel < perk.tier.requiredStatLevel) return false;
        if (getPointsForStat(perk.stat.index) < perk.tier.cost) return false;
        if (perk.synergyStat != null) {
            int synergyLevel = statData.getLevel(perk.synergyStat.index);
            if (synergyLevel < PerkTier.SYNERGY.requiredStatLevel) return false;
        }
        if (player != null && !SkillPerkGate.canUnlock(player, perk)) return false;
        return true;
    }

    public boolean unlock(Perk perk) {
        return unlock(perk, null);
    }

    public boolean unlock(Perk perk, Player player) {
        if (!canUnlock(perk, player)) return false;
        statData.addUnlockedPerk(perk.id);
        statData.addPerkPointsForStat(perk.stat.index, -perk.tier.cost);
        return true;
    }

    public void resetAll() {
        statData.clearUnlockedPerks();
    }
}
