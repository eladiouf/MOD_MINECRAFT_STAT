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

        gateway.ensureCategoryUnlocked(PuffishMagicCategoryIds.COMMON_CATEGORY);
        gateway.ensureCategoryUnlocked(PuffishMagicCategoryIds.FIRE_CATEGORY);
        gateway.ensureCategoryUnlocked(PuffishMagicCategoryIds.LOCKED_CATEGORY);

        gateway.setPoints(PuffishMagicCategoryIds.COMMON_CATEGORY, data.getArcanePoints());
        gateway.setPoints(PuffishMagicCategoryIds.FIRE_CATEGORY, data.getSchoolPoints(MagicBranch.FIRE));
        gateway.setPoints(PuffishMagicCategoryIds.LOCKED_CATEGORY, 0);

        for (MagicNode node : MagicTreeCatalog.all()) {
            String categoryId = PuffishMagicCategoryIds.categoryFor(node.id());
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
}
