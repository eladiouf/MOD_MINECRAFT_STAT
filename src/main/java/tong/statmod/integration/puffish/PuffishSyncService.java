package tong.statmod.integration.puffish;

import tong.statmod.perks.Perk;
import tong.statmod.storage.PlayerStatData;

import java.util.HashSet;
import java.util.Set;

public final class PuffishSyncService {
    private PuffishSyncService() {}

    public static void sync(PlayerStatData data, PuffishMirrorGateway gateway) {
        Set<String> initializedCategories = new HashSet<>();
        for (Perk perk : Perk.values()) {
            String categoryId = PuffishPerkIds.categoryId(perk);
            if (initializedCategories.add(categoryId)) {
                gateway.ensureCategoryUnlocked(categoryId);
                gateway.setPoints(categoryId, mirroredCategoryTotal(data, perk));
            }
            if (data.isPerkUnlocked(perk.id)) {
                gateway.unlock(categoryId, PuffishPerkIds.skillId(perk));
            } else {
                gateway.lock(categoryId, PuffishPerkIds.skillId(perk));
            }
        }
    }

    private static int mirroredCategoryTotal(PlayerStatData data, Perk categoryPerk) {
        int total = data.getPerkPointsForStat(categoryPerk.stat.index);
        for (Perk perk : Perk.values()) {
            if (perk.stat == categoryPerk.stat && data.isPerkUnlocked(perk.id)) {
                total += perk.tier.cost;
            }
        }
        return total;
    }
}
