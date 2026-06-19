package tong.statmod.integration.elementals;

import java.util.EnumSet;

public record MageRaceProfile(
        String raceId,
        boolean supported,
        boolean human,
        boolean beastfolk,
        EnumSet<ElementalBranch> fixedStarters
) {}
