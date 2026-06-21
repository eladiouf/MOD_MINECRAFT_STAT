package tong.statmod.magic;

import java.util.Set;

public enum MagicRace {
    HUMAN(Set.of(MagicBranch.FIRE, MagicBranch.WATER, MagicBranch.AIR, MagicBranch.EARTH), true, false),
    ELF(Set.of(MagicBranch.AIR, MagicBranch.WATER), false, false),
    DWARF(Set.of(MagicBranch.EARTH, MagicBranch.FIRE), false, false),
    BEAST(Set.of(MagicBranch.WATER, MagicBranch.AIR), false, true);

    private final Set<MagicBranch> naturalAffinities;
    public final boolean flexible;
    public final boolean purityPenalty;

    MagicRace(Set<MagicBranch> naturalAffinities, boolean flexible, boolean purityPenalty) {
        this.naturalAffinities = naturalAffinities;
        this.flexible = flexible;
        this.purityPenalty = purityPenalty;
    }

    public Set<MagicBranch> naturalAffinities() { return naturalAffinities; }

    public boolean hasAffinity(MagicBranch branch) {
        return naturalAffinities.contains(branch);
    }

    public boolean canChooseStartBranch(MagicBranch branch) {
        return branch != null && !branch.lateGame && branch != MagicBranch.COMMON && hasAffinity(branch);
    }

    public static MagicRace byId(String id) {
        if (id == null) return null;
        return switch (id.toLowerCase()) {
            case "human" -> HUMAN;
            case "elf" -> ELF;
            case "dwarf" -> DWARF;
            case "beast" -> BEAST;
            default -> null;
        };
    }

    public static MagicRace byOrdinalOrDefault(int ordinal) {
        MagicRace[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : HUMAN;
    }
}
