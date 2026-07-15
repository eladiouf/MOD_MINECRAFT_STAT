package tong.statmod.dungeon;

/**
 * Mission M6 — « Vraie aventure » (2026-07-04).
 *
 * <p>Objectif à accomplir sur un étage pour en débloquer la sortie. Transforme le donjon d'un
 * couloir (où entrer débloquait déjà l'étage suivant) en une suite de <b>défis à conquérir</b>.
 *
 * <p>Le type d'objectif découle du rôle de l'étage :
 * <ul>
 *   <li>{@link #CLEAR_WAVE} — étages de combat : éliminer toute la vague de mobs.</li>
 *   <li>{@link #LOOT_VAULT} — étages trésor (×5) : ouvrir le coffre de la chambre forte.</li>
 *   <li>{@link #SLAY_BOSS} — étages boss (×10) : vaincre le(s) boss du roster.</li>
 * </ul>
 */
public enum DungeonObjective {

    CLEAR_WAVE("dungeon.objective.clear_wave"),
    LOOT_VAULT("dungeon.objective.loot_vault"),
    SLAY_BOSS("dungeon.objective.slay_boss");

    private final String translationKey;

    DungeonObjective(String translationKey) {
        this.translationKey = translationKey;
    }

    /** Clé de traduction pour le HUD / les messages (« Éliminez tous les ennemis », etc.). */
    public String translationKey() {
        return translationKey;
    }

    /** Objectif de l'étage {@code floor} selon son rôle (boss ×10 > trésor ×5 > combat). */
    public static DungeonObjective forFloor(int floor) {
        if (floor <= 0) return CLEAR_WAVE;
        if (floor % 10 == 0) return SLAY_BOSS;
        if (floor % 5 == 0) return LOOT_VAULT;
        return CLEAR_WAVE;
    }
}
