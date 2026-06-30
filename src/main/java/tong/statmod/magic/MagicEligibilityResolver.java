package tong.statmod.magic;

import tong.statmod.storage.PlayerStatData;

import java.util.List;

/**
 * Évalue si un joueur peut déverrouiller un {@link MagicNode}. Utilise le système
 * {@link Condition}/{@link ConditionEvaluator} pour les vérifications de prérequis
 * stats/race/spell au lieu de l'ancienne {@code MagicNodeStatRequirements}.
 *
 * <p>Sous l'économie unifiée, le coût est un nombre unique de {@code magicPoints}
 * (pas de modificateur race — la race agit sur les seuils de conditions via
 * {@link ConditionContext}).</p>
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
     * @param failure      failure principale
     * @param adjustedCost coût final en magicPoints (identique au {@code cost} du node)
     * @param missingStats descriptions des conditions non satisfaites — vide si tout passe
     *                     ou si la failure est ailleurs
     */
    public record Result(Failure failure, int adjustedCost,
                          List<String> missingStats) {
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

        // Condition tree (stats, race, spells, tiers, global level)
        if (!ConditionEvaluator.evaluate(node, data)) {
            List<String> missing = ConditionEvaluator.describeMissing(node, data);
            return new Result(Failure.STAT_REQUIREMENT_NOT_MET, node.cost(), missing);
        }

        // Points unifiés
        int cost = node.cost();
        int available = data.getMagicPoints();
        if (available < cost) {
            return new Result(Failure.NOT_ENOUGH_POINTS, cost);
        }
        return new Result(Failure.NONE, cost);
    }

    /**
     * @deprecated Conservé pour compat avec les anciens appelants.
     *             Sous le nouveau modèle la race n'affecte plus le coût en points — retourne
     *             {@code baseCost} tel quel. Sera retiré en Mission δ avec MagicCurrency.
     */
    @Deprecated
    public static int affinityAdjustedCost(PlayerStatData data, MagicBranch branch, int baseCost) {
        return baseCost;
    }
}
