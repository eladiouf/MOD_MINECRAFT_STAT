package tong.statmod.magic;

import tong.statmod.storage.PlayerStatData;

import java.util.ArrayList;
import java.util.List;

/**
 * Évalue si un joueur peut déverrouiller un {@link MagicNode}. Refondu pour la nouvelle
 * économie : monnaie unique {@code magicPoints} + 3 gates de stats (ARCANE_POWER + ERUDITION
 * + tertiaire selon {@link SpellRole}), avec deltas race appliqués via
 * {@link MagicNodeStatRequirements}.
 *
 * <p>L'ancienne logique d'{@code affinityAdjustedCost} (coût modifié par race et branche de
 * départ) est <b>supprimée</b> — la race agit désormais sur les seuils de stats, pas sur le
 * coût en points.
 *
 * @see MagicNodeStatRequirements pour les tables D1/D2/D3.
 */
public final class MagicEligibilityResolver {
    public enum Failure {
        NONE,
        LOCKED,
        MISSING_PREREQ,
        NOT_ENOUGH_POINTS,
        ALREADY_UNLOCKED,
        NO_RACE,
        RUNTIME_GRANT_FAILED,
        STAT_REQUIREMENT_NOT_MET
    }

    /**
     * @param failure failure principale (peut combiner avec missingStats si STAT_REQUIREMENT_NOT_MET)
     * @param adjustedCost coût final en magicPoints (identique au cost du node sous le nouveau
     *                     modèle ; conservé pour compatibilité signature avec les anciens
     *                     appelants)
     * @param missingStats liste des gates non satisfaits — vide si tout passe ou si la failure
     *                     est ailleurs
     */
    public record Result(Failure failure, int adjustedCost,
                          List<MagicNodeStatRequirements.StatGate> missingStats) {
        public Result(Failure failure, int adjustedCost) {
            this(failure, adjustedCost, List.of());
        }
    }

    private MagicEligibilityResolver() {}

    public static Result evaluate(PlayerStatData data, MagicNode node) {
        if (data == null || node == null) {
            return new Result(Failure.MISSING_PREREQ, 0);
        }
        if (data.hasMagicNode(node.id())) {
            return new Result(Failure.ALREADY_UNLOCKED, node.cost());
        }
        if (node.branch() != MagicBranch.COMMON && data.getMagicRace() == null) {
            return new Result(Failure.NO_RACE, 0);
        }
        for (String p : node.prerequisites()) {
            if (MagicTreeCatalog.LOCKED_SENTINEL.equals(p)) {
                return new Result(Failure.LOCKED, node.cost());
            }
            if (!data.hasMagicNode(p)) {
                return new Result(Failure.MISSING_PREREQ, node.cost());
            }
        }

        // 3 gates de stats. Collecte des gates non satisfaits pour feedback UI.
        List<MagicNodeStatRequirements.StatGate> missing = collectMissingGates(node, data);
        if (!missing.isEmpty()) {
            return new Result(Failure.STAT_REQUIREMENT_NOT_MET, node.cost(), missing);
        }

        // Points unifiés : sous le nouveau modèle, magicPoints porte tous les coûts.
        int cost = node.cost();
        int available = data.getMagicPoints();
        if (available < cost) {
            return new Result(Failure.NOT_ENOUGH_POINTS, cost);
        }
        return new Result(Failure.NONE, cost);
    }

    private static List<MagicNodeStatRequirements.StatGate> collectMissingGates(
            MagicNode node, PlayerStatData data) {
        MagicNodeStatRequirements.Requirements req = MagicNodeStatRequirements.forNode(node);
        if (req == null) return List.of();
        List<MagicNodeStatRequirements.StatGate> missing = new ArrayList<>();
        addIfMissing(req.arcane(), data, node, missing);
        addIfMissing(req.erudition(), data, node, missing);
        addIfMissing(req.tertiary(), data, node, missing);
        return missing;
    }

    private static void addIfMissing(MagicNodeStatRequirements.StatGate gate,
                                      PlayerStatData data, MagicNode node,
                                      List<MagicNodeStatRequirements.StatGate> sink) {
        if (gate == null) return;
        int required = MagicNodeStatRequirements.effectiveThreshold(gate, data, node);
        if (required <= 0) return;
        int actual = data.getLevel(gate.stat().index);
        if (actual < required) {
            // Stocke le gate effectif (avec seuil ajusté race) pour que l'UI affiche le vrai chiffre.
            sink.add(new MagicNodeStatRequirements.StatGate(gate.stat(), required));
        }
    }

    /**
     * @deprecated Conservé pour compat avec les anciens tests qui appellent affinityAdjustedCost.
     *             Sous le nouveau modèle la race n'affecte plus le coût en points — retourne
     *             {@code baseCost} tel quel. Sera retiré en Mission δ avec MagicCurrency.
     */
    @Deprecated
    public static int affinityAdjustedCost(PlayerStatData data, MagicBranch branch, int baseCost) {
        return baseCost;
    }
}
