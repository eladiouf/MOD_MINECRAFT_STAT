package tong.statmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import tong.statmod.magic.MagicBranch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class ClientMagicCache {
    private static final Set<String> magicNodes = new HashSet<>();
    private static final Set<String> learnedSpells = new HashSet<>();
    private static int magicPoints;
    private static int[] masteryProgress = new int[MagicBranch.values().length];
    private static int raceOrdinal = -1;
    private static int startBranchOrdinal = -1;

    private ClientMagicCache() {}

    public static void update(String[] nodes, String[] spells, int mp, int[] mastery, int race, int branch) {
        magicNodes.clear();
        if (nodes != null) for (String n : nodes) magicNodes.add(n);
        learnedSpells.clear();
        if (spells != null) for (String s : spells) learnedSpells.add(s);
        magicPoints = mp;
        if (mastery != null && mastery.length == masteryProgress.length) masteryProgress = mastery.clone();
        raceOrdinal = race;
        startBranchOrdinal = branch;
    }

    public static boolean hasMagicNode(String id) { return id != null && magicNodes.contains(id); }
    public static boolean hasLearnedSpell(String id) { return id != null && learnedSpells.contains(id); }
    public static List<String> getLearnedSpells() { return List.copyOf(learnedSpells); }
    public static int getMagicNodeCount() { return magicNodes.size(); }
    public static int getLearnedSpellsCount() { return learnedSpells.size(); }

    public static int getMagicPoints() { return magicPoints; }
    public static int getMasteryProgress(MagicBranch b) {
        return b == null ? 0 : masteryProgress[b.ordinal()];
    }
    public static int[] getMasteryProgressArray() { return masteryProgress.clone(); }
    public static int getRaceOrdinal() { return raceOrdinal; }
    public static int getStartBranchOrdinal() { return startBranchOrdinal; }

    // --- Shims compat pour anciens callers ---

    /** @deprecated Préférer {@link #getMagicPoints()}. */
    @Deprecated
    public static int getArcanePoints() { return magicPoints; }

    /** @deprecated Préférer {@link #getMasteryProgress(MagicBranch)}. */
    @Deprecated
    public static int getSchoolPoints(MagicBranch b) { return 0; }

    /** @deprecated Préférer {@link #getMasteryProgressArray()}. */
    @Deprecated
    public static int[] getSchoolPointsArray() { return new int[masteryProgress.length]; }
}
