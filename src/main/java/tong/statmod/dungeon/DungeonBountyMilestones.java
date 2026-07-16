package tong.statmod.dungeon;

import java.util.ArrayList;
import java.util.List;

/**
 * Paliers de profondeur qui accordent un advancement « Plongée » (ciblé par les bounties de type
 * criteria). Cœur pur, testable sans serveur.
 */
public final class DungeonBountyMilestones {

    private static final List<Integer> THRESHOLDS = List.of(10, 25, 50, 100);

    private DungeonBountyMilestones() {}

    /** Paliers ordonnés. */
    public static List<Integer> thresholds() { return THRESHOLDS; }

    /** Paliers franchis quand l'étage le plus profond atteint vaut {@code deepestFloor}. */
    public static List<Integer> reached(int deepestFloor) {
        List<Integer> out = new ArrayList<>();
        for (int t : THRESHOLDS) if (deepestFloor >= t) out.add(t);
        return out;
    }

    /** Id de l'advancement du palier (ex. 50 → {@code statmod:dungeon/delve_50}). */
    public static String advancementId(int threshold) {
        return "statmod:dungeon/delve_" + threshold;
    }
}
