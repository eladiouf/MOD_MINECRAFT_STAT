package tong.statmod.perks;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.integration.SkillPerkGate;
import tong.statmod.integration.tensura.PerkToSkillMapper;
import tong.statmod.integration.tensura.TensuraSpellGate;
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
        int statLevel = player != null
                ? RaceEffectApplier.getEffectiveLevel(player, perk.stat.index)
                : statData.getLevel(perk.stat.index);
        if (statLevel < perk.tier.requiredStatLevel) return false;
        if (getPointsForStat(perk.stat.index) < perk.tier.cost) return false;
        if (perk.synergyStat != null) {
            int synergyLevel = player != null
                    ? RaceEffectApplier.getEffectiveLevel(player, perk.synergyStat.index)
                    : statData.getLevel(perk.synergyStat.index);
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
        statData.markPerkPaid(perk.id);
        statData.addPerkPointsForStat(perk.stat.index, -perk.tier.cost);
        grantRewards(player, perk);
        return true;
    }

    public boolean grant(Perk perk) {
        return grant(perk, null);
    }

    public boolean grant(Perk perk, Player player) {
        if (perk == null || isUnlocked(perk)) return false;
        statData.markPerkFreeGranted(perk.id);
        grantRewards(player, perk);
        return true;
    }

    public boolean revoke(Perk perk, boolean refundPoints) {
        if (perk == null || !isUnlocked(perk)) return false;
        boolean freeGranted = statData.isPerkFreeGranted(perk.id);
        statData.removeUnlockedPerk(perk.id);
        if (refundPoints && !freeGranted) {
            statData.addPerkPointsForStat(perk.stat.index, perk.tier.cost);
        }
        return true;
    }

    private void grantRewards(Player player, Perk perk) {
        if (player != null) {
            TensuraSpellGate.grantReward(player, perk);
            if (ModList.get().isLoaded("tensura")) {
                PerkToSkillMapper.grantReward(player, perk);
            }
            if (ModList.get().isLoaded("epicfight")) {
                EpicFightPerkGate.grantReward(player, perk);
            }
        }
    }

    public void resetAll() {
        statData.clearUnlockedPerks();
    }
}
