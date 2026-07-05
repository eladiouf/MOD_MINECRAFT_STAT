package tong.statmod.integration;

import net.minecraft.world.entity.player.Player;
import tong.statmod.client.ClientStatCache;
import tong.statmod.config.Config;
import tong.statmod.network.SyncHelper;
import tong.statmod.progression.LevelUpHandler;
import tong.statmod.integration.tensura.TempBuffManager;
import tong.statmod.integration.tensura.StatLevelSkillRewards;
import tong.statmod.storage.PlayerStatData;

public final class RaceEffectApplier {
    private RaceEffectApplier() {}

    public static int getEffectiveLevel(Player player, int statIndex) {
        int base = getBaseLevel(player, statIndex);
        return base + getRaceFlatBonus(player, statIndex);
    }

    public static int getBaseLevel(Player player, int statIndex) {
        int base = player.getData(tong.statmod.storage.ModAttachments.STATS).getLevel(statIndex);
        if (player.level().isClientSide && ClientStatCache.hasLevel(statIndex)) {
            // Le client n'utilise pas l'attachment serveur pour l'UI; il reçoit les vraies stats
            // via StatUpdatePayload -> ClientStatCache. Une fois le snapshot client reçu, il
            // reste l'autorité pour éviter qu'un attachment local plus élevé masque une vraie
            // baisse de stat synchronisée.
            base = ClientStatCache.getLevel(statIndex);
        }
        return base + TempBuffManager.getAwakeningBonus(player, player.level().getGameTime());
    }

    public static int getRaceFlatBonus(Player player, int statIndex) {
        String raceId = PlayerDataBridge.getRaceId(player);
        RaceData data = RaceModifierRegistry.get(raceId);
        for (RaceModifier m : data.modifiers()) {
            if (m.statIndex() == statIndex) return m.flatBonus();
        }
        return 0;
    }

    public static double getXpMultiplier(Player player, int statIndex) {
        String raceId = PlayerDataBridge.getRaceId(player);
        RaceData data = RaceModifierRegistry.get(raceId);
        for (RaceModifier m : data.modifiers()) {
            if (m.statIndex() == statIndex) return m.xpMultiplier();
        }
        return 1.0;
    }

    public static boolean hasExclusivePerk(Player player, int perkId) {
        String raceId = PlayerDataBridge.getRaceId(player);
        RaceData data = RaceModifierRegistry.get(raceId);
        return data.exclusivePerks().contains(perkId);
    }

    public static boolean addScaledXp(Player player, int statIndex, int baseXp, PlayerStatData data, boolean isCombat) {
        int scaled = scaleXpAmount(player, statIndex, baseXp, data, isCombat);
        return addResolvedXp(player, statIndex, scaled, data);
    }

    public static boolean addRawXp(Player player, int statIndex, int rawXp, PlayerStatData data) {
        return addResolvedXp(player, statIndex, rawXp, data);
    }

    public static boolean addLevels(Player player, int statIndex, int amount, PlayerStatData data, boolean showFeedback) {
        return addResolvedLevels(player, statIndex, amount, data, showFeedback);
    }

    private static boolean addResolvedXp(Player player, int statIndex, int amount, PlayerStatData data) {
        if (data == null) {
            return false;
        }
        int before = data.getLevel(statIndex);
        int raceFlatBonus = player != null ? getRaceFlatBonus(player, statIndex) : 0;
        boolean leveled = data.addXpWithEffectiveStartLevel(statIndex, amount, raceFlatBonus);
        int after = data.getLevel(statIndex);
        return finalizeLevelProgression(player, statIndex, before, after, data, true, raceFlatBonus) || leveled;
    }

    private static boolean addResolvedLevels(Player player, int statIndex, int amount, PlayerStatData data, boolean showFeedback) {
        if (data == null || amount == 0) {
            return false;
        }

        int before = data.getLevel(statIndex);
        data.addLevels(statIndex, amount);
        int after = data.getLevel(statIndex);
        return finalizeLevelProgression(player, statIndex, before, after, data, showFeedback,
                player != null ? getRaceFlatBonus(player, statIndex) : 0);
    }

    private static boolean finalizeLevelProgression(Player player, int statIndex, int before, int after,
                                                    PlayerStatData data, boolean showFeedback, int raceFlatBonus) {
        if (after <= before) {
            return false;
        }

        if (player != null) {
            for (int level = before + 1; level <= after; level++) {
                StatLevelSkillRewards.grantReward(player, statIndex, level);
            }
            if (showFeedback) {
                // Feedback joueur — actionbar avec stat name + effective level (Mission M).
                tong.statmod.progression.LevelUpFeedback.notify(player, statIndex, after + raceFlatBonus);
            }
        }
        int granted = LevelUpHandler.grantPendingPerkTiers(data);
        if (granted > 0 && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            SyncHelper.syncPerks(serverPlayer);
        }
        return true;
    }

    public static int scaleXpAmount(Player player, int statIndex, int baseXp, PlayerStatData data, boolean isCombat) {
        double xpMult = player != null ? getXpMultiplier(player, statIndex) : 1.0d;
        double configMult = isCombat ? Config.getCombatXpMultiplier() : Config.getNonCombatXpMultiplier();
        double soulMult = 1.0d + Math.max(0, data.getSoulLevel()) / 100.0d;
        if (hasParallelExistence(player)) {
            soulMult *= 2.0d;
        }
        return Math.max(1, (int) Math.round(baseXp * xpMult * configMult * soulMult));
    }

    public static boolean hasParallelExistence(Player player) {
        if (player == null) return false;
        return PlayerDataBridge.hasSkill(player, "tensura:parallel_existence")
                || PlayerDataBridge.hasSkill(player, "tensura:parallel_existences");
    }
}
