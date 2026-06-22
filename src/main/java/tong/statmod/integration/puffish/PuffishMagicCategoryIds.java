package tong.statmod.integration.puffish;

import tong.statmod.magic.MagicBranch;

public final class PuffishMagicCategoryIds {
    public static final String UNIFIED_CATEGORY = "statmod:statmod_magic";

    private PuffishMagicCategoryIds() {}

    public static String toSkillId(String nodeId) {
        return nodeId == null ? null : nodeId.replace('/', '.');
    }

    public static String fromSkillId(String skillId) {
        return skillId == null || skillId.isEmpty() ? null : skillId.replace('.', '/');
    }

    public static String categoryFor(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return null;
        }
        int separator = nodeId.indexOf('/');
        String branchId = separator < 0 ? nodeId : nodeId.substring(0, separator);
        return MagicBranch.byId(branchId) != null ? UNIFIED_CATEGORY : null;
    }
}
