package tong.statmod.integration.ironspells;

import tong.statmod.magic.MagicBranch;

import java.util.Map;

public final class IronSchoolMapping {
    private static final String IRONS_NS = "irons_spellbooks";

    private static final Map<String, MagicBranch> BY_PATH = Map.ofEntries(
            Map.entry("fire", MagicBranch.FIRE),
            Map.entry("ice", MagicBranch.WATER),
            Map.entry("lightning", MagicBranch.AIR),
            Map.entry("nature", MagicBranch.EARTH),
            Map.entry("holy", MagicBranch.HOLY),
            Map.entry("blood", MagicBranch.BLOOD),
            Map.entry("ender", MagicBranch.ENDER),
            Map.entry("evocation", MagicBranch.EVOCATION),
            Map.entry("eldritch", MagicBranch.ELDRITCH)
    );

    private IronSchoolMapping() {}

    public static MagicBranch fromIronsSchoolId(String namespace, String path) {
        if (!IRONS_NS.equals(namespace) || path == null) return null;
        return BY_PATH.get(path);
    }
}
