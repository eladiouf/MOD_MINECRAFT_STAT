package tong.statmod.integration.puffish;

import tong.statmod.perks.Perk;
import tong.statmod.storage.PlayerStatData;

public final class PuffishSyncService {
    private PuffishSyncService() {}

    public static void sync(PlayerStatData data, PuffishMirrorGateway gateway) {
        for (Perk perk : Perk.values()) {
            String categoryId = PuffishPerkIds.categoryId(perk);
            gateway.ensureCategoryUnlocked(categoryId);
            gateway.setPoints(categoryId, data.getPerkPointsForStat(perk.stat.index));
            if (data.isPerkUnlocked(perk.id)) {
                gateway.unlock(categoryId, PuffishPerkIds.skillId(perk));
            } else {
                gateway.lock(categoryId, PuffishPerkIds.skillId(perk));
            }
        }
    }
}
