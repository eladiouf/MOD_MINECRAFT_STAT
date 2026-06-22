package tong.statmod.integration.puffish;

import tong.statmod.perks.Perk;

import java.util.HashMap;
import java.util.Map;

public final class PuffishPerkIds {
    public static final String UNIFIED_CATEGORY = "statmod:statmod_perks";

    private static final Map<String, Perk> BY_PATH = new HashMap<>();

    static {
        for (Perk perk : Perk.values()) {
            BY_PATH.put(categoryId(perk) + "|" + skillId(perk), perk);
        }
    }

    private PuffishPerkIds() {}

    public static String categoryId(Perk perk) {
        return UNIFIED_CATEGORY;
    }

    public static String skillId(Perk perk) {
        return PuffishFamilyTreeBuilder.skillId(perk);
    }

    public static Perk resolve(String categoryId, String skillId) {
        if (categoryId == null || skillId == null) {
            return null;
        }
        return BY_PATH.get(categoryId + "|" + skillId);
    }
}
