package tong.statmod.integration.puffish;

import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicNode;
import tong.statmod.magic.MagicTreeCatalog;
import tong.statmod.storage.PlayerStatData;

public final class PuffishMagicSyncService {
    private PuffishMagicSyncService() {}

    public static void sync(PlayerStatData data, PuffishMirrorGateway gateway) {
        if (data == null || gateway == null) {
            return;
        }

        String categoryId = PuffishMagicCategoryIds.UNIFIED_CATEGORY;
        gateway.ensureCategoryUnlocked(categoryId);
        gateway.setPoints(categoryId, totalAvailableMagicPoints(data));

        for (MagicNode node : MagicTreeCatalog.all()) {
            String skillId = PuffishMagicCategoryIds.toSkillId(node.id());
            if (categoryId == null || skillId == null) {
                continue;
            }

            if (data.hasMagicNode(node.id())) {
                gateway.unlock(categoryId, skillId);
            } else {
                gateway.lock(categoryId, skillId);
            }
        }
    }

    static int totalAvailableMagicPoints(PlayerStatData data) {
        int total = data == null ? 0 : data.getArcanePoints();
        if (data == null) {
            return total;
        }
        for (MagicBranch branch : MagicBranch.values()) {
            total += data.getSchoolPoints(branch);
        }
        return total;
    }
}
