package tong.statmod.magic;

import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public enum MagicRace {
    HUMAN("human", "tensura:human", "Human", "Versatile and open to every early elemental path.",
            List.of(MagicBranch.FIRE, MagicBranch.WATER, MagicBranch.AIR, MagicBranch.EARTH), true, false),
    ELF("elf", "tensura:elf", "Elf", "Natural spellcasters with graceful control over air and water.",
            List.of(MagicBranch.AIR, MagicBranch.WATER), false, false),
    DWARF("dwarf", "tensura:dwarf", "Dwarf", "Grounded battlemages who stabilize fire and earth.",
            List.of(MagicBranch.EARTH, MagicBranch.FIRE), false, false),
    BEAST("beast", "tensura:beastfolk", "Beastfolk", "Instinctive adepts with fluid water and wind instincts.",
            List.of(MagicBranch.WATER, MagicBranch.AIR), false, true);

    private final String id;
    private final String tensuraStartingRaceId;
    private final String displayName;
    private final String summary;
    private final List<MagicBranch> orderedAffinities;
    private final Set<MagicBranch> naturalAffinities;
    public final boolean flexible;
    public final boolean purityPenalty;

    MagicRace(String id, String tensuraStartingRaceId, String displayName, String summary,
              List<MagicBranch> orderedAffinities, boolean flexible, boolean purityPenalty) {
        this.id = id;
        this.tensuraStartingRaceId = tensuraStartingRaceId;
        this.displayName = displayName;
        this.summary = summary;
        this.orderedAffinities = List.copyOf(orderedAffinities);
        this.naturalAffinities = Set.copyOf(new LinkedHashSet<>(orderedAffinities));
        this.flexible = flexible;
        this.purityPenalty = purityPenalty;
    }

    public String id() { return id; }

    public String tensuraStartingRaceId() { return tensuraStartingRaceId; }

    public ResourceLocation tensuraStartingRaceKey() {
        return ResourceLocation.parse(tensuraStartingRaceId);
    }

    public String displayName() { return displayName; }

    public String summary() { return summary; }

    public List<MagicBranch> orderedAffinities() { return orderedAffinities; }

    public Set<MagicBranch> naturalAffinities() { return naturalAffinities; }

    public boolean hasAffinity(MagicBranch branch) {
        return naturalAffinities.contains(branch);
    }

    public MagicBranch defaultStartBranch() {
        if (flexible) {
            return MagicBranch.FIRE;
        }
        return orderedAffinities.isEmpty() ? MagicBranch.FIRE : orderedAffinities.getFirst();
    }

    public boolean canChooseStartBranch(MagicBranch branch) {
        return branch != null && !branch.lateGame && branch != MagicBranch.COMMON && hasAffinity(branch);
    }

    public static MagicRace byId(String id) {
        if (id == null || id.isBlank()) return null;
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(race -> race.id.equals(normalized) || race.tensuraStartingRaceId.equals(normalized)
                        || ("tensura:" + race.id).equals(normalized)
                        || (race == BEAST && "beastfolk".equals(normalized)))
                .findFirst()
                .orElse(null);
    }

    public static MagicRace byOrdinalOrDefault(int ordinal) {
        MagicRace[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : HUMAN;
    }

    public static Set<ResourceLocation> allowedStartingRaceIds() {
        return Arrays.stream(values())
                .map(MagicRace::tensuraStartingRaceKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public static boolean isAllowedStartingRace(ResourceLocation raceId) {
        if (raceId == null) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(race -> race.tensuraStartingRaceKey().equals(raceId));
    }

    public static MagicRace fromStartingRaceId(String raceId) {
        return byId(raceId);
    }
}
