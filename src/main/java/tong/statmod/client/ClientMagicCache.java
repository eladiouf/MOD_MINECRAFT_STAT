package tong.statmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;

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
        if (nodes != null) for (String n : nodes) if (isUsableId(n)) magicNodes.add(n);
        learnedSpells.clear();
        if (spells != null) for (String s : spells) if (isUsableId(s)) learnedSpells.add(s);
        magicPoints = Math.max(0, mp);
        masteryProgress = sanitizeMastery(mastery);
        raceOrdinal = sanitizeOrdinal(race, MagicRace.values().length);
        startBranchOrdinal = sanitizeOrdinal(branch, MagicBranch.values().length);
    }

    public static void reset() {
        magicNodes.clear();
        learnedSpells.clear();
        magicPoints = 0;
        masteryProgress = new int[MagicBranch.values().length];
        raceOrdinal = -1;
        startBranchOrdinal = -1;
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

    private static boolean isUsableId(String value) {
        return value != null && !value.isBlank();
    }

    private static int[] sanitizeMastery(int[] source) {
        int length = MagicBranch.values().length;
        int[] sanitized = new int[length];
        if (source == null || source.length != length) {
            return sanitized;
        }
        for (int i = 0; i < source.length; i++) {
            sanitized[i] = Math.max(0, source[i]);
        }
        return sanitized;
    }

    private static int sanitizeOrdinal(int value, int size) {
        return value >= 0 && value < size ? value : -1;
    }
}
