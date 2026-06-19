package tong.statmod.perks;

import tong.statmod.stats.StatFamily;
import tong.statmod.storage.PlayerStatData;

public final class PerkPointAllocator {
    private PerkPointAllocator() {}

    public static void grantPointsToAllFamilies(PlayerStatData data, int amount) {
        if (data == null || amount <= 0) {
            return;
        }

        for (StatFamily family : StatFamily.values()) {
            data.addPerkPointsForFamily(family, amount);
        }
    }

    public static int refundPaidUnlockedPerks(PlayerStatData data) {
        if (data == null) {
            return 0;
        }

        int totalRefund = 0;
        for (int id : data.getUnlockedPerks()) {
            if (data.isPerkFreeGranted(id)) {
                continue;
            }
            Perk perk = Perk.byId(id);
            if (perk == null) {
                continue;
            }
            data.addPerkPointsForFamily(perk.stat.family(), perk.tier.cost);
            totalRefund += perk.tier.cost;
        }
        return totalRefund;
    }
}
