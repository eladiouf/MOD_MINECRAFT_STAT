package tong.statmod.integration;

import net.minecraft.world.entity.player.Player;
import tong.statmod.storage.PlayerStatData;

public final class RaceEffectApplier {
    private RaceEffectApplier() {}

    public static int getEffectiveLevel(Player player, int statIndex) {
        int base = getBaseLevel(player, statIndex);
        return base + getRaceFlatBonus(player, statIndex);
    }

    public static int getBaseLevel(Player player, int statIndex) {
        return player.getData(tong.statmod.storage.ModAttachments.STATS).getLevel(statIndex);
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
        int scaled = (int) Math.round(baseXp * getXpMultiplier(player, statIndex));
        return data.addXp(statIndex, Math.max(1, scaled));
    }
}
