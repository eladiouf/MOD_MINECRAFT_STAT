package tong.statmod.integration.elementals;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public final class ElementalsRaceAffinity {
    private static final List<EnumSet<ElementalBranch>> HUMAN_PAIRS = List.of(
            EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER),
            EnumSet.of(ElementalBranch.AIR, ElementalBranch.EARTH),
            EnumSet.of(ElementalBranch.AIR, ElementalBranch.FIRE),
            EnumSet.of(ElementalBranch.WATER, ElementalBranch.EARTH),
            EnumSet.of(ElementalBranch.WATER, ElementalBranch.FIRE),
            EnumSet.of(ElementalBranch.EARTH, ElementalBranch.FIRE)
    );

    private ElementalsRaceAffinity() {}

    public static MageRaceProfile resolve(String raceId) {
        String normalized = raceId == null ? "" : raceId.trim().toLowerCase();
        return switch (normalized) {
            case "tensura:elf" -> new MageRaceProfile(normalized, true, false, false,
                    EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER));
            case "tensura:human" -> new MageRaceProfile(normalized, true, true, false,
                    EnumSet.noneOf(ElementalBranch.class));
            case "tensura:dwarf" -> new MageRaceProfile(normalized, true, false, false,
                    EnumSet.of(ElementalBranch.FIRE, ElementalBranch.EARTH));
            case "tensura:beastfolk" -> new MageRaceProfile(normalized, true, false, true,
                    EnumSet.of(ElementalBranch.WATER, ElementalBranch.AIR));
            default -> new MageRaceProfile(normalized, false, false, false,
                    EnumSet.noneOf(ElementalBranch.class));
        };
    }

    public static EnumSet<ElementalBranch> starterBranches(MageRaceProfile profile, UUID playerId) {
        if (profile == null || !profile.supported()) {
            return EnumSet.noneOf(ElementalBranch.class);
        }
        if (!profile.human()) {
            return profile.fixedStarters().isEmpty()
                    ? EnumSet.noneOf(ElementalBranch.class)
                    : EnumSet.copyOf(profile.fixedStarters());
        }
        int index = Math.floorMod(playerId.hashCode(), HUMAN_PAIRS.size());
        return EnumSet.copyOf(HUMAN_PAIRS.get(index));
    }
}
