package tong.statmod.network;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class SyncSnapshotGate {
    private final Map<UUID, StatsSnapshot> statsSnapshots = new ConcurrentHashMap<>();
    private final Map<UUID, PerksSnapshot> perksSnapshots = new ConcurrentHashMap<>();
    private final Map<UUID, StaminaSnapshot> staminaSnapshots = new ConcurrentHashMap<>();
    private final Map<UUID, MagicSnapshot> magicSnapshots = new ConcurrentHashMap<>();

    boolean shouldSendStats(UUID playerId, int[] levels, int[] xp, int soulLevel,
                            int dungeonPoints, int dungeonFloorReached) {
        if (playerId == null) {
            return true;
        }

        StatsSnapshot previous = statsSnapshots.get(playerId);
        if (previous != null && previous.matches(levels, xp, soulLevel, dungeonPoints, dungeonFloorReached)) {
            return false;
        }

        statsSnapshots.put(playerId, StatsSnapshot.capture(levels, xp, soulLevel, dungeonPoints, dungeonFloorReached));
        return true;
    }

    boolean shouldSendPerks(UUID playerId, int[] perkIds, int[] perStatPoints) {
        if (playerId == null) {
            return true;
        }

        PerksSnapshot previous = perksSnapshots.get(playerId);
        if (previous != null && previous.matches(perkIds, perStatPoints)) {
            return false;
        }

        perksSnapshots.put(playerId, PerksSnapshot.capture(perkIds, perStatPoints));
        return true;
    }

    boolean shouldSendStamina(UUID playerId, float currentStamina, float fatigueDebt, boolean meditating) {
        if (playerId == null) {
            return true;
        }

        StaminaSnapshot previous = staminaSnapshots.get(playerId);
        if (previous != null && previous.matches(currentStamina, fatigueDebt, meditating)) {
            return false;
        }

        staminaSnapshots.put(playerId, new StaminaSnapshot(currentStamina, fatigueDebt, meditating));
        return true;
    }

    boolean shouldSendMagic(UUID playerId, String[] magicNodes, String[] learnedSpells, int magicPoints,
                             int[] masteryProgress, int raceOrdinal, int startBranchOrdinal) {
        return shouldSendMagic(playerId, magicNodes, learnedSpells, magicPoints, masteryProgress, new int[0],
                raceOrdinal, startBranchOrdinal);
    }

    boolean shouldSendMagic(UUID playerId, String[] magicNodes, String[] learnedSpells, int magicPoints,
                             int[] masteryProgress, int[] practiceMasteryProgress,
                             int raceOrdinal, int startBranchOrdinal) {
        if (playerId == null) {
            return true;
        }

        MagicSnapshot previous = magicSnapshots.get(playerId);
        if (previous != null && previous.matches(
                magicNodes, learnedSpells, magicPoints, masteryProgress, practiceMasteryProgress,
                raceOrdinal, startBranchOrdinal)) {
            return false;
        }

        magicSnapshots.put(playerId, MagicSnapshot.capture(
                magicNodes, learnedSpells, magicPoints, masteryProgress, practiceMasteryProgress,
                raceOrdinal, startBranchOrdinal));
        return true;
    }

    void clear(UUID playerId) {
        if (playerId == null) {
            return;
        }

        statsSnapshots.remove(playerId);
        perksSnapshots.remove(playerId);
        staminaSnapshots.remove(playerId);
        magicSnapshots.remove(playerId);
    }

    private record StatsSnapshot(int[] levels, int[] xp, int soulLevel, int dungeonPoints, int dungeonFloorReached) {
        private static StatsSnapshot capture(int[] levels, int[] xp, int soulLevel,
                                             int dungeonPoints, int dungeonFloorReached) {
            return new StatsSnapshot(copy(levels), copy(xp), soulLevel, dungeonPoints, dungeonFloorReached);
        }

        private boolean matches(int[] candidateLevels, int[] candidateXp, int candidateSoulLevel,
                                int candidateDungeonPoints, int candidateDungeonFloorReached) {
            return Arrays.equals(levels, candidateLevels)
                    && Arrays.equals(xp, candidateXp)
                    && soulLevel == candidateSoulLevel
                    && dungeonPoints == candidateDungeonPoints
                    && dungeonFloorReached == candidateDungeonFloorReached;
        }
    }

    private record PerksSnapshot(int[] perkIds, int[] perStatPoints) {
        private static PerksSnapshot capture(int[] perkIds, int[] perStatPoints) {
            return new PerksSnapshot(copy(perkIds), copy(perStatPoints));
        }

        private boolean matches(int[] candidatePerkIds, int[] candidatePerStatPoints) {
            return Arrays.equals(perkIds, candidatePerkIds)
                    && Arrays.equals(perStatPoints, candidatePerStatPoints);
        }
    }

    private record StaminaSnapshot(float currentStamina, float fatigueDebt, boolean meditating) {
        private boolean matches(float candidateCurrentStamina, float candidateFatigueDebt, boolean candidateMeditating) {
            return Float.compare(currentStamina, candidateCurrentStamina) == 0
                    && Float.compare(fatigueDebt, candidateFatigueDebt) == 0
                    && meditating == candidateMeditating;
        }
    }

    private record MagicSnapshot(String[] magicNodes, String[] learnedSpells, int magicPoints,
                                  int[] masteryProgress, int[] practiceMasteryProgress,
                                  int raceOrdinal, int startBranchOrdinal) {
        private static MagicSnapshot capture(String[] magicNodes, String[] learnedSpells, int magicPoints,
                                              int[] masteryProgress, int[] practiceMasteryProgress,
                                              int raceOrdinal, int startBranchOrdinal) {
            return new MagicSnapshot(
                    copy(magicNodes),
                    copy(learnedSpells),
                    magicPoints,
                    copy(masteryProgress),
                    copy(practiceMasteryProgress),
                    raceOrdinal,
                    startBranchOrdinal
            );
        }

        private boolean matches(String[] candidateMagicNodes, String[] candidateLearnedSpells, int candidateMagicPoints,
                                 int[] candidateMasteryProgress, int[] candidatePracticeMasteryProgress,
                                 int candidateRaceOrdinal,
                                 int candidateStartBranchOrdinal) {
            return Arrays.equals(magicNodes, candidateMagicNodes)
                    && Arrays.equals(learnedSpells, candidateLearnedSpells)
                    && magicPoints == candidateMagicPoints
                    && Arrays.equals(masteryProgress, candidateMasteryProgress)
                    && Arrays.equals(practiceMasteryProgress, candidatePracticeMasteryProgress)
                    && raceOrdinal == candidateRaceOrdinal
                    && startBranchOrdinal == candidateStartBranchOrdinal;
        }
    }

    private static int[] copy(int[] values) {
        return values == null ? new int[0] : values.clone();
    }

    private static String[] copy(String[] values) {
        return values == null ? new String[0] : values.clone();
    }
}
