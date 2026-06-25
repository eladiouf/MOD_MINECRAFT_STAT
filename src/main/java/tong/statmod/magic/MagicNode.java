package tong.statmod.magic;

import java.util.List;
import java.util.Set;

public record MagicNode(
        String id,
        MagicBranch branch,
        MagicNodeKind kind,
        MagicTier tier,
        MagicCurrency currency,
        int cost,
        List<String> prerequisites,
        Set<String> learnedSpells,
        SpellRole role
) {
    public MagicNode {
        if (id == null || branch == null || kind == null || tier == null || currency == null) {
            throw new IllegalArgumentException("node fields must not be null");
        }
        if (cost < 0) {
            throw new IllegalArgumentException("cost must be >= 0");
        }
        String expectedPrefix = branch.id + "/";
        if (!id.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException(
                    "node id '" + id + "' does not match branch prefix '" + expectedPrefix + "'");
        }
        prerequisites = List.copyOf(prerequisites);
        learnedSpells = Set.copyOf(learnedSpells);
        // role can be null during Mission α — every node gets tagged in Mission β.
    }

    /**
     * Constructeur backward-compatible — appelé par tout le code existant qui n'a pas encore
     * été migré pour passer le {@link SpellRole}. À retirer après que Mission β ait tagué
     * tous les nœuds du catalog.
     */
    public MagicNode(String id,
                     MagicBranch branch,
                     MagicNodeKind kind,
                     MagicTier tier,
                     MagicCurrency currency,
                     int cost,
                     List<String> prerequisites,
                     Set<String> learnedSpells) {
        this(id, branch, kind, tier, currency, cost, prerequisites, learnedSpells, null);
    }
}
