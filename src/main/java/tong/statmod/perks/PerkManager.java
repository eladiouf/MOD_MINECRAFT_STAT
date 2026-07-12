package tong.statmod.perks;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.integration.SkillPerkGate;
import tong.statmod.integration.tensura.PerkToSkillMapper;
import tong.statmod.integration.tensura.TensuraSpellGate;
import tong.statmod.storage.PlayerStatData;

public class PerkManager {
    public enum UnlockFailure {
        INVALID_PERK,
        ALREADY_UNLOCKED,
        LEVEL_TOO_LOW,
        NOT_ENOUGH_POINTS,
        SYNERGY_TOO_LOW,
        EXTERNAL_REQUIREMENT
    }

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
        return getUnlockFailure(perk, player) == null;
    }

    public UnlockFailure getUnlockFailure(Perk perk, Player player) {
        if (perk == null) return UnlockFailure.INVALID_PERK;
        if (isUnlocked(perk)) return UnlockFailure.ALREADY_UNLOCKED;
        int statLevel = player != null
                ? RaceEffectApplier.getEffectiveLevel(player, perk.stat.index)
                : statData.getLevel(perk.stat.index);
        if (statLevel < perk.tier.requiredStatLevel) return UnlockFailure.LEVEL_TOO_LOW;
        if (getPointsForStat(perk.stat.index) < perk.tier.cost) return UnlockFailure.NOT_ENOUGH_POINTS;
        if (perk.synergyStat != null) {
            int synergyLevel = player != null
                    ? RaceEffectApplier.getEffectiveLevel(player, perk.synergyStat.index)
                    : statData.getLevel(perk.synergyStat.index);
            if (synergyLevel < PerkTier.SYNERGY.requiredStatLevel) return UnlockFailure.SYNERGY_TOO_LOW;
        }

        if (perk.secondRequiredStat != null) {
            int secondLevel = player != null
                    ? RaceEffectApplier.getEffectiveLevel(player, perk.secondRequiredStat.index)
                    : statData.getLevel(perk.secondRequiredStat.index);
            if (secondLevel < perk.secondRequiredStatLevel) return UnlockFailure.LEVEL_TOO_LOW;
        }

        // Cas particulier de AVATAR_OF_ELEMENTS (id 147) qui requiert 40 dans les 4 éléments
        if (perk.id == 147) {
            int fire = player != null ? RaceEffectApplier.getEffectiveLevel(player, tong.statmod.stats.StatType.FIRE_AFFINITY.index) : statData.getLevel(tong.statmod.stats.StatType.FIRE_AFFINITY.index);
            int water = player != null ? RaceEffectApplier.getEffectiveLevel(player, tong.statmod.stats.StatType.WATER_AFFINITY.index) : statData.getLevel(tong.statmod.stats.StatType.WATER_AFFINITY.index);
            int air = player != null ? RaceEffectApplier.getEffectiveLevel(player, tong.statmod.stats.StatType.AIR_AFFINITY.index) : statData.getLevel(tong.statmod.stats.StatType.AIR_AFFINITY.index);
            int earth = player != null ? RaceEffectApplier.getEffectiveLevel(player, tong.statmod.stats.StatType.EARTH_AFFINITY.index) : statData.getLevel(tong.statmod.stats.StatType.EARTH_AFFINITY.index);
            if (fire < 40 || water < 40 || air < 40 || earth < 40) return UnlockFailure.LEVEL_TOO_LOW;
        }

        if (player != null && !SkillPerkGate.canUnlock(player, perk)) return UnlockFailure.EXTERNAL_REQUIREMENT;
        return null;
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
        return revoke(perk, refundPoints, null);
    }

    public boolean revoke(Perk perk, boolean refundPoints, Player player) {
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
            if (ModList.get().isLoaded("tensura")) {
                TensuraSpellGate.grantReward(player, perk);
                PerkToSkillMapper.grantReward(player, perk);
            }
            if (ModList.get().isLoaded("epicfight")) {
                EpicFightPerkGate.grantReward(player, perk);
            }
        }
    }

    public int resetAll(Player player, boolean refundPoints) {
        int refunded = 0;
        for (int perkId : statData.getUnlockedPerks()) {
            Perk perk = Perk.byId(perkId);
            if (perk == null) {
                continue;
            }
            boolean refundable = refundPoints && !statData.isPerkFreeGranted(perk.id);
            if (revoke(perk, refundPoints, player) && refundable) {
                refunded += perk.tier.cost;
            }
        }
        return refunded;
    }

    public void resetAll() {
        statData.clearUnlockedPerks();
    }
}
