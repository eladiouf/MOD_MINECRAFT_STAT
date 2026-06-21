package tong.statmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import tong.statmod.magic.MagicBranch;

import java.util.HashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class ClientMagicCache {
    private static final Set<String> magicNodes = new HashSet<>();
    private static final Set<String> learnedSpells = new HashSet<>();
    private static int arcanePoints;
    private static int[] schoolPoints = new int[MagicBranch.values().length];
    private static int raceOrdinal = -1;
    private static int startBranchOrdinal = -1;

    private ClientMagicCache() {}

    public static void update(String[] nodes, String[] spells, int ap, int[] sp, int race, int branch) {
        magicNodes.clear();
        if (nodes != null) for (String n : nodes) magicNodes.add(n);
        learnedSpells.clear();
        if (spells != null) for (String s : spells) learnedSpells.add(s);
        arcanePoints = ap;
        if (sp != null) schoolPoints = sp.clone();
        raceOrdinal = race;
        startBranchOrdinal = branch;
    }

    public static boolean hasMagicNode(String id) { return id != null && magicNodes.contains(id); }
    public static boolean hasLearnedSpell(String id) { return id != null && learnedSpells.contains(id); }
    public static int getArcanePoints() { return arcanePoints; }
    public static int getSchoolPoints(MagicBranch b) { return b == null ? 0 : schoolPoints[b.ordinal()]; }
    public static int[] getSchoolPointsArray() { return schoolPoints.clone(); }
    public static int getRaceOrdinal() { return raceOrdinal; }
    public static int getStartBranchOrdinal() { return startBranchOrdinal; }
}
