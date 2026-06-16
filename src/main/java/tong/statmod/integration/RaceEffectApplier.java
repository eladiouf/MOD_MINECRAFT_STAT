package tong.statmod.integration;

import net.minecraft.world.entity.player.Player;
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

    public static boolean addScaledXp(Player player, int statIndex, int baseXp, PlayerStatData data) {
        int scaled = scaleXpAmount(player, statIndex, baseXp, data);
        int before = data.getLevel(statIndex);
        boolean leveled = data.addXp(statIndex, scaled);
        if (leveled && player != null) {
            int after = data.getLevel(statIndex);
            if (after > before) {
                for (int level = before + 1; level <= after; level++) {
                    StatLevelSkillRewards.grantReward(player, statIndex, level);
                }
            }
        }
        return leveled;
    }

    public static int scaleXpAmount(Player player, int statIndex, int baseXp, PlayerStatData data) {
        double xpMult = player != null ? getXpMultiplier(player, statIndex) : 1.0d;
        double soulMult = 1.0d + Math.max(0, data.getSoulLevel()) / 100.0d;
        if (hasParallelExistence(player)) {
            soulMult *= 2.0d;
        }
        return Math.max(1, (int) Math.round(baseXp * xpMult * soulMult));
    }

    public static boolean hasParallelExistence(Player player) {
        if (player == null) return false;
        return PlayerDataBridge.hasSkill(player, "tensura:parallel_existence")
                || PlayerDataBridge.hasSkill(player, "tensura:parallel_existences");
    }
}
