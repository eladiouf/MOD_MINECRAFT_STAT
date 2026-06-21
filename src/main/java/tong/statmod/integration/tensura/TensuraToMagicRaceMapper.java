package tong.statmod.integration.tensura;

import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;

public final class TensuraToMagicRaceMapper {
    private TensuraToMagicRaceMapper() {}

    public static MagicRace fromTensuraRaceId(String raceId) {
        if (raceId == null) return MagicRace.HUMAN;
        String normalized = raceId.toLowerCase();
        int colon = normalized.indexOf(':');
        String family = colon >= 0 ? normalized.substring(colon + 1) : normalized;
        family = stripEvolutionPrefix(family);

        if (family.startsWith("human")) return MagicRace.HUMAN;
        if (family.startsWith("elf")) return MagicRace.ELF;
        if (family.startsWith("dwarf")) return MagicRace.DWARF;
        if (family.startsWith("beastfolk") || family.startsWith("beast")) return MagicRace.BEAST;

        if (family.startsWith("goblin") || family.startsWith("hobgoblin")
                || family.startsWith("ogre") || family.startsWith("orc")
                || family.startsWith("lizardman")) {
            return MagicRace.BEAST;
        }
        if (family.startsWith("slime") || family.startsWith("merfolk") || family.startsWith("harpy")) {
            return MagicRace.ELF;
        }
        if (family.startsWith("daemon") || family.startsWith("vampire") || family.startsWith("wight")) {
            return MagicRace.HUMAN;
        }
        if (family.startsWith("giant")) {
            return MagicRace.DWARF;
        }
        return MagicRace.HUMAN;
    }

    public static MagicBranch defaultStartBranch(MagicRace race) {
        if (race == null) return MagicBranch.FIRE;
        if (race.flexible) return MagicBranch.FIRE;
        for (MagicBranch b : MagicBranch.values()) {
            if (race.canChooseStartBranch(b)) return b;
        }
        return MagicBranch.FIRE;
    }

    private static String stripEvolutionPrefix(String family) {
        String[] prefixes = {
                "divine_", "enlightened_", "ancient_",
                "arch_", "greater_", "lesser_",
                "spirit_"
        };
        for (String p : prefixes) {
            if (family.startsWith(p)) return family.substring(p.length());
        }
        String[] suffixes = {"_saint", "_lord"};
        for (String s : suffixes) {
            if (family.endsWith(s)) return family.substring(0, family.length() - s.length());
        }
        return family;
    }
}
