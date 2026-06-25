package tong.statmod.magic;

import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

import java.util.EnumMap;
import java.util.Map;

/**
 * Calcule les seuils de stats requis pour déverrouiller un {@link MagicNode}. Centralise les
 * trois tables design (D1 mapping role → stat, D2 seuils par tier, D3 deltas race) pour que
 * {@code MagicEligibilityResolver} se contente de "demander au helper" sans connaître les
 * détails.
 *
 * <p>Trois gates par nœud :
 * <ol>
 *   <li>{@code ARCANE_POWER} ≥ seuil(tier, race) — universel</li>
 *   <li>{@code ERUDITION} ≥ seuil(tier, race) — universel</li>
 *   <li>Tertiaire ≥ seuil(tier, race, affinité) — selon {@link SpellRole}</li>
 * </ol>
 *
 * <p>Référence design : {@code docs/superpowers/specs/2026-06-22-magic-tree-stat-gated-economy-design.md}.
 */
public final class MagicNodeStatRequirements {
    /** Gate atomique : "stat X au niveau ≥ Y". */
    public record StatGate(StatType stat, int minLevel) {}

    /** Triple gates d'un nœud. {@code tertiary} peut être {@code null} pour les TRUNK_FOUNDATION. */
    public record Requirements(StatGate arcane, StatGate erudition, StatGate tertiary) {}

    private MagicNodeStatRequirements() {}

    // ------------------------------------------------------------------------
    // D2 — Seuils universels (avant deltas race)
    // ------------------------------------------------------------------------

    private record UniversalBase(int arcane, int erudition) {}

    private static UniversalBase universalBase(MagicNode node) {
        MagicNodeKind kind = node.kind();
        MagicTier tier = node.tier();
        return switch (kind) {
            case TRUNK_FOUNDATION -> switch (tier) {
                case T1 -> new UniversalBase(1, 1);
                case T2 -> new UniversalBase(2, 2);
                case T3, T4 -> new UniversalBase(3, 3);
            };
            case BRANCH_OPENER -> new UniversalBase(2, 1);
            case BRANCH_TIER, SIGNATURE_SPELL -> switch (tier) {
                case T1 -> new UniversalBase(2, 2);
                case T2 -> new UniversalBase(4, 3);
                case T3 -> new UniversalBase(6, 5);
                case T4 -> new UniversalBase(10, 7); // late game gate
            };
            case LATEGAME_GATE -> new UniversalBase(10, 7);
        };
    }

    // ------------------------------------------------------------------------
    // D1 — Mapping rôle/branche → tertiaire
    // ------------------------------------------------------------------------

    /**
     * Pour les BRANCH_OPENER et BRANCH_TIER, la tertiaire dépend de la branche et du tier
     * (cas spécial documenté dans le spec D1). Les SIGNATURE_SPELL passent par
     * {@link SpellRole#tertiaryStat()}.
     */
    private static StatGate tertiaryFor(MagicNode node) {
        if (node.kind() == MagicNodeKind.TRUNK_FOUNDATION) {
            return null;
        }

        SpellRole role = node.role();
        // Cas SIGNATURE_SPELL — role fixe → tertiaire directe via enum.
        if (role != null && !role.isStructural()) {
            int threshold = signatureTertiaryThreshold(node.tier());
            return new StatGate(role.tertiaryStat(), threshold);
        }

        // Cas structurel : opener / tier — table par (branche, kind, tier).
        return structuralTertiary(node);
    }

    private static int signatureTertiaryThreshold(MagicTier tier) {
        return switch (tier) {
            case T1 -> 1;
            case T2 -> 3;
            case T3 -> 5;
            case T4 -> 7;
        };
    }

    private static StatGate structuralTertiary(MagicNode node) {
        BranchTertiarySet set = BRANCH_TERTIARY.get(node.branch());
        if (set == null) {
            return null; // COMMON or unknown — no tertiary
        }
        return switch (node.kind()) {
            case BRANCH_OPENER -> set.opener;
            case BRANCH_TIER -> switch (node.tier()) {
                case T1 -> set.tierT1;
                case T2 -> set.tierT2;
                case T3, T4 -> set.tierT3;
            };
            default -> null;
        };
    }

    private record BranchTertiarySet(StatGate opener, StatGate tierT1, StatGate tierT2, StatGate tierT3) {}

    private static final Map<MagicBranch, BranchTertiarySet> BRANCH_TERTIARY = buildBranchTertiaryTable();

    private static Map<MagicBranch, BranchTertiarySet> buildBranchTertiaryTable() {
        Map<MagicBranch, BranchTertiarySet> map = new EnumMap<>(MagicBranch.class);
        map.put(MagicBranch.FIRE, new BranchTertiarySet(
                new StatGate(StatType.FIRE_AFFINITY, 2),
                new StatGate(StatType.FIRE_AFFINITY, 1),
                new StatGate(StatType.FIRE_AFFINITY, 3),
                new StatGate(StatType.INTIMIDATION, 5)));
        map.put(MagicBranch.WATER, new BranchTertiarySet(
                new StatGate(StatType.WATER_AFFINITY, 2),
                new StatGate(StatType.WATER_AFFINITY, 1),
                new StatGate(StatType.WATER_AFFINITY, 3),
                new StatGate(StatType.MAGIC_RESISTANCE, 5)));
        map.put(MagicBranch.AIR, new BranchTertiarySet(
                new StatGate(StatType.AIR_AFFINITY, 2),
                new StatGate(StatType.AIR_AFFINITY, 1),
                new StatGate(StatType.AIR_AFFINITY, 3),
                new StatGate(StatType.AGILITY, 5)));
        map.put(MagicBranch.EARTH, new BranchTertiarySet(
                new StatGate(StatType.EARTH_AFFINITY, 2),
                new StatGate(StatType.EARTH_AFFINITY, 1),
                new StatGate(StatType.EARTH_AFFINITY, 3),
                new StatGate(StatType.PHYSICAL_ENDURANCE, 5)));
        map.put(MagicBranch.HOLY, new BranchTertiarySet(
                new StatGate(StatType.WILLPOWER, 2),
                new StatGate(StatType.WILLPOWER, 1),
                new StatGate(StatType.ERUDITION, 3),
                new StatGate(StatType.WILLPOWER, 5)));
        map.put(MagicBranch.BLOOD, new BranchTertiarySet(
                new StatGate(StatType.PHYSICAL_ENDURANCE, 2),
                new StatGate(StatType.PHYSICAL_ENDURANCE, 1),
                new StatGate(StatType.INTIMIDATION, 3),
                new StatGate(StatType.WILLPOWER, 5)));
        map.put(MagicBranch.ENDER, new BranchTertiarySet(
                new StatGate(StatType.KEEN_SENSES, 2),
                new StatGate(StatType.KEEN_SENSES, 1),
                new StatGate(StatType.KEEN_SENSES, 3),
                new StatGate(StatType.KEEN_SENSES, 5)));
        map.put(MagicBranch.EVOCATION, new BranchTertiarySet(
                new StatGate(StatType.TRACKING, 2),
                new StatGate(StatType.ERUDITION, 1),
                new StatGate(StatType.TRACKING, 3),
                new StatGate(StatType.ERUDITION, 5)));
        map.put(MagicBranch.ELDRITCH, new BranchTertiarySet(
                new StatGate(StatType.WILLPOWER, 2),
                new StatGate(StatType.MAGIC_RESISTANCE, 1),
                new StatGate(StatType.WILLPOWER, 3),
                new StatGate(StatType.WILLPOWER, 5)));
        // COMMON has no tertiary by design.
        return map;
    }

    // ------------------------------------------------------------------------
    // D3 — Deltas race (sur seuils, jamais sur coûts)
    // ------------------------------------------------------------------------

    /** Delta race sur le seuil ARCANE_POWER (négatif = discount). */
    private static int raceArcaneDelta(MagicRace race) {
        if (race == null) return 0;
        return switch (race) {
            case HUMAN -> -1;
            case ELF -> 0;
            case DWARF -> -1;
            case BEAST -> 0;
        };
    }

    /** Delta race sur le seuil ERUDITION (négatif = discount). */
    private static int raceErudtionDelta(MagicRace race) {
        if (race == null) return 0;
        return switch (race) {
            case HUMAN -> -1;
            case ELF -> -2;
            case DWARF -> 0;
            case BEAST -> 1; // penalty: instinct over study
        };
    }

    /**
     * Delta race sur le seuil tertiaire d'un nœud. Combine :
     * <ul>
     *   <li>−1 si la tertiaire est une affinité élémentale matching la race</li>
     *   <li>−1 si la branche du nœud == branche de départ choisie</li>
     *   <li>+1 (purity penalty Beast) si la branche n'est pas affine pour Beast</li>
     * </ul>
     */
    private static int raceTertiaryDelta(MagicRace race, MagicBranch chosenStart,
                                          StatType tertiaryStat, MagicBranch nodeBranch) {
        int delta = 0;
        if (race != null && tertiaryStat != null
                && isElementalAffinityMatchingRace(race, tertiaryStat)) {
            delta -= 1;
        }
        if (chosenStart != null && chosenStart == nodeBranch) {
            delta -= 1;
        }
        if (race == MagicRace.BEAST && nodeBranch != null && !race.hasAffinity(nodeBranch)
                && nodeBranch != MagicBranch.COMMON) {
            delta += 1;
        }
        return delta;
    }

    private static boolean isElementalAffinityMatchingRace(MagicRace race, StatType tertiaryStat) {
        MagicBranch matching = switch (tertiaryStat) {
            case FIRE_AFFINITY -> MagicBranch.FIRE;
            case WATER_AFFINITY -> MagicBranch.WATER;
            case AIR_AFFINITY -> MagicBranch.AIR;
            case EARTH_AFFINITY -> MagicBranch.EARTH;
            default -> null;
        };
        return matching != null && race.hasAffinity(matching);
    }

    // ------------------------------------------------------------------------
    // API publique
    // ------------------------------------------------------------------------

    /**
     * Retourne les trois gates bruts (sans deltas race appliqués). Pour les seuils effectifs
     * en jeu, appeler {@link #effectiveThreshold(StatGate, PlayerStatData, MagicNode)}.
     */
    public static Requirements forNode(MagicNode node) {
        if (node == null) {
            return null;
        }
        UniversalBase base = universalBase(node);
        StatGate arcane = new StatGate(StatType.ARCANE_POWER, base.arcane);
        StatGate erudition = new StatGate(StatType.ERUDITION, base.erudition);
        StatGate tertiary = tertiaryFor(node);
        return new Requirements(arcane, erudition, tertiary);
    }

    /**
     * Calcule le seuil effectif pour un gate, en appliquant les deltas race. Clamp à 0 (un
     * seuil négatif est traité comme "toujours satisfait").
     */
    public static int effectiveThreshold(StatGate gate, PlayerStatData data, MagicNode node) {
        if (gate == null) return 0;
        if (data == null || node == null) return gate.minLevel();
        MagicRace race = data.getMagicRace();
        MagicBranch chosenStart = data.getChosenStartBranch();
        int delta = switch (gate.stat()) {
            case ARCANE_POWER -> raceArcaneDelta(race);
            case ERUDITION -> raceErudtionDelta(race);
            default -> raceTertiaryDelta(race, chosenStart, gate.stat(), node.branch());
        };
        return Math.max(0, gate.minLevel() + delta);
    }

    /**
     * Helper de test d'éligibilité : retourne {@code true} si toutes les 3 gates du nœud
     * sont satisfaites par le joueur. {@link MagicEligibilityResolver} l'utilise dans
     * Mission γ.
     */
    public static boolean playerMeetsAllGates(MagicNode node, PlayerStatData data) {
        if (node == null || data == null) return false;
        Requirements req = forNode(node);
        if (req == null) return true;
        return playerMeetsGate(req.arcane(), data, node)
                && playerMeetsGate(req.erudition(), data, node)
                && playerMeetsGate(req.tertiary(), data, node);
    }

    public static boolean playerMeetsGate(StatGate gate, PlayerStatData data, MagicNode node) {
        if (gate == null) return true; // pas de gate = OK
        int required = effectiveThreshold(gate, data, node);
        if (required <= 0) return true; // clamp = always satisfied
        int actual = data.getLevel(gate.stat().index);
        return actual >= required;
    }
}
