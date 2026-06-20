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
        String familyId = canonicalSupportedRaceId(normalized);
        return switch (familyId) {
            case "tensura:elf" -> new MageRaceProfile(familyId, true, false, false,
                    EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER),
                    EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER));
            case "tensura:human" -> new MageRaceProfile(familyId, true, true, false,
                    EnumSet.noneOf(ElementalBranch.class),
                    EnumSet.noneOf(ElementalBranch.class));
            case "tensura:dwarf" -> new MageRaceProfile(familyId, true, false, false,
                    EnumSet.of(ElementalBranch.FIRE, ElementalBranch.EARTH),
                    EnumSet.of(ElementalBranch.FIRE, ElementalBranch.EARTH));
            case "tensura:beastfolk" -> new MageRaceProfile(familyId, true, false, true,
                    EnumSet.of(ElementalBranch.WATER, ElementalBranch.AIR),
                    EnumSet.noneOf(ElementalBranch.class));
            default -> new MageRaceProfile(normalized, false, false, false,
                    EnumSet.noneOf(ElementalBranch.class),
                    EnumSet.noneOf(ElementalBranch.class));
        };
    }

    private static String canonicalSupportedRaceId(String raceId) {
        return switch (raceId) {
            case "tensura:human", "tensura:enlightened_human", "tensura:human_saint", "tensura:divine_human" ->
                    "tensura:human";
            case "tensura:elf", "tensura:enlightened_elf", "tensura:elf_saint", "tensura:divine_elf" ->
                    "tensura:elf";
            case "tensura:dwarf", "tensura:enlightened_dwarf", "tensura:dwarf_saint", "tensura:divine_dwarf" ->
                    "tensura:dwarf";
            case "tensura:beastfolk", "tensura:beast_lord", "tensura:spirit_beast", "tensura:divine_beast" ->
                    "tensura:beastfolk";
            default -> raceId;
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
