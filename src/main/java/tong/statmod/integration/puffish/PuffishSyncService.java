package tong.statmod.integration.puffish;

import tong.statmod.perks.Perk;
import tong.statmod.stats.StatFamily;
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
                gateway.setPoints(categoryId, mirroredFamilyPoints(data, perk.stat.family()));
            }
            if (data.isPerkUnlocked(perk.id)) {
                gateway.unlock(categoryId, PuffishPerkIds.skillId(perk));
            } else {
                gateway.lock(categoryId, PuffishPerkIds.skillId(perk));
            }
        }

        PuffishMagicSyncService.sync(data, gateway);
    }

    private static int mirroredFamilyPoints(PlayerStatData data, StatFamily family) {
        return data.getPerkPointsForFamily(family);
    }
}
