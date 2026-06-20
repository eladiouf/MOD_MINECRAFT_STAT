package tong.statmod.integration.elementals;

import java.util.EnumSet;

public record MageRaceProfile(
        String raceId,
        boolean supported,
        boolean human,
        boolean beastfolk,
        EnumSet<ElementalBranch> fixedStarters,
        EnumSet<ElementalBranch> favoredBaseBranches
) {
    public boolean elf() {
        return "tensura:elf".equals(raceId);
    }

    public boolean dwarf() {
        return "tensura:dwarf".equals(raceId);
    }

    public boolean favors(ElementalBranch branch) {
        return favoredBaseBranches != null && branch != null && favoredBaseBranches.contains(branch);
    }
}
