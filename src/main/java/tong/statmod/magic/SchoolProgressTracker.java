package tong.statmod.magic;

import tong.statmod.storage.PlayerStatData;

/**
 * Tracker de mastery par école. Sous Mission ε, deux comportements :
 *
 * <ol>
 *   <li><b>Continu</b> — chaque {@link #MASTERY_PER_POINT} unités accumulées génère 1 magic
 *       point dans le pool unifié (au lieu de l'ancien school point dédié).</li>
 *   <li><b>Paliers</b> — quand le mastery cumulé d'une école franchit un seuil (10/25/50),
 *       un bonus discret est versé : +1 / +2 / +3 magic points. Récompense la
 *       spécialisation.</li>
 * </ol>
 *
 * <p>Les paliers se mémorisent via {@link PlayerStatData#getSchoolMasteryProgress(MagicBranch)}
 * — non, en fait le progress est consommé par la conversion continue. Pour tracker les
 * paliers indépendamment, on calcule le mastery TOTAL accumulé (progress courant + multiples
 * de MASTERY_PER_POINT déjà convertis) à partir d'un compteur séparé. Pour rester compatible
 * avec la sérialisation existante sans ajouter de champ, on encode la dernière marche atteinte
 * dans les bits hauts du mastery progress (≤ MASTERY_PER_POINT pour la partie basse, marches
 * dans la partie haute). Trop fragile — on opte plutôt pour : <b>le bonus de palier est versé
 * pile au moment où la conversion continue franchit le seuil cumulé exprimé en points convertis</b>.
 *
 * <p>Simplification finale : paliers exprimés en <b>points convertis cumulés</b> (10/25/50
 * points). À chaque conversion continue, on vérifie si on franchit un palier et on verse le
 * bonus correspondant. Le compteur "total points convertis" est lui-même tracké via la mastery
 * encodée — mais comme on n'a pas de champ persistant pour ça, on conserve un comportement
 * non-cumulatif : le bonus de palier <b>ne se réclame qu'une fois</b> par session, ce qui est
 * acceptable car le palier n'est jamais re-franchi (mastery progress accumule, ne décroît pas
 * en pratique).
 */
public final class SchoolProgressTracker {
    public static final int MASTERY_PER_POINT = 100;

    /** Seuils mastery (en unités brutes accumulées) qui versent un bonus en magic points. */
    public static final int[] MILESTONE_THRESHOLDS = { 1000, 2500, 5000 };
    /** Bonus magic points correspondants à chaque seuil (cumulatif total +1/+3/+6). */
    public static final int[] MILESTONE_BONUSES = { 1, 2, 3 };

    private SchoolProgressTracker() {}

    /**
     * Verse {@code amount} d'unités mastery dans la branche. Conversion continue :
     * {@code amount / MASTERY_PER_POINT} magic points crédités. Paliers : si la mastery
     * cumulée totale (calculée comme {@code totalConverted * MASTERY_PER_POINT + progress})
     * franchit un seuil pour la 1ère fois, bonus versé.
     *
     * @return nombre de magic points versés (continu + bonus paliers)
     */
    public static int applyMastery(PlayerStatData data, MagicBranch branch, int amount) {
        if (data == null || branch == null || amount <= 0) return 0;

        int prevProgress = data.getSchoolMasteryProgress(branch);
        int prevTotal = prevProgress; // l'unique source de mastery cumulé persistant
        int newTotal = prevTotal + amount;

        int granted = (newTotal / MASTERY_PER_POINT) - (prevTotal / MASTERY_PER_POINT);
        int milestoneBonus = milestoneCrossingBonus(prevTotal, newTotal);
        int totalGain = granted + milestoneBonus;

        data.setSchoolMasteryProgress(branch, newTotal);
        if (totalGain > 0) data.addMagicPoints(totalGain);
        return totalGain;
    }

    /**
     * Enregistre de la maîtrise de pratique séparément de la maîtrise bancaire. Cette voie ne
     * participe jamais aux conversions continues ni aux paliers et ne verse donc aucun magic
     * point.
     *
     * @return toujours zéro
     */
    public static int applyPracticeMastery(PlayerStatData data, MagicBranch branch, int amount) {
        if (data == null || branch == null || amount <= 0) return 0;

        data.addSchoolPracticeMasteryProgress(branch, amount);
        return 0;
    }

    /**
     * Calcule le bonus de palier si le mastery cumulé franchit un ou plusieurs seuils en une
     * seule application. Multi-seuil possible si {@code amount} est très grand.
     */
    static int milestoneCrossingBonus(int prevTotal, int newTotal) {
        int bonus = 0;
        for (int i = 0; i < MILESTONE_THRESHOLDS.length; i++) {
            int threshold = MILESTONE_THRESHOLDS[i];
            if (prevTotal < threshold && newTotal >= threshold) {
                bonus += MILESTONE_BONUSES[i];
            }
        }
        return bonus;
    }
}
