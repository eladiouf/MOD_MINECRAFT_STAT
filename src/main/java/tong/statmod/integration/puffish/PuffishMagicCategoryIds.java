package tong.statmod.integration.puffish;

import tong.statmod.magic.MagicBranch;

public final class PuffishMagicCategoryIds {
    public static final String COMMON_CATEGORY = "statmod:statmod_magic_common";
    public static final String FIRE_CATEGORY = "statmod:statmod_magic_fire";
    public static final String LOCKED_CATEGORY = "statmod:statmod_magic_locked";

    private PuffishMagicCategoryIds() {}

    public static String toSkillId(String nodeId) {
        return nodeId == null ? null : nodeId.replace('/', '.');
    }

    public static String fromSkillId(String skillId) {
        return skillId == null || skillId.isEmpty() ? null : skillId.replace('.', '/');
    }

    public static String categoryFor(String nodeId) {
        if (nodeId == null) return null;
        if (nodeId.startsWith("common/")) return COMMON_CATEGORY;
        if (nodeId.startsWith("fire/")) return FIRE_CATEGORY;
        for (MagicBranch b : MagicBranch.values()) {
            if (b == MagicBranch.COMMON || b == MagicBranch.FIRE) continue;
            if (nodeId.startsWith(b.id + "/")) return LOCKED_CATEGORY;
        }
        return null;
    }
}
