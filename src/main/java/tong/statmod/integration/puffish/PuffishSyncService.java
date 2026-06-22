package tong.statmod.integration.puffish;

import tong.statmod.perks.Perk;
import tong.statmod.stats.StatFamily;
import tong.statmod.storage.PlayerStatData;

public final class PuffishSyncService {
    private PuffishSyncService() {}

    public static void sync(PlayerStatData data, PuffishMirrorGateway gateway) {
        String categoryId = PuffishPerkIds.UNIFIED_CATEGORY;
        gateway.ensureCategoryUnlocked(categoryId);
        gateway.setPoints(categoryId, totalAvailablePerkPoints(data));

        for (Perk perk : Perk.values()) {
            if (data.isPerkUnlocked(perk.id)) {
                gateway.unlock(categoryId, PuffishPerkIds.skillId(perk));
            } else {
                gateway.lock(categoryId, PuffishPerkIds.skillId(perk));
            }
        }

        PuffishMagicSyncService.sync(data, gateway);
    }

    static int totalAvailablePerkPoints(PlayerStatData data) {
        int total = 0;
        for (StatFamily family : StatFamily.values()) {
            total += data.getPerkPointsForFamily(family);
        }
        return total;
    }
}
