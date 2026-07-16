package tong.statmod.dungeon;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * « Dungeon Rush » (2026-07-09) — la couche d'addiction du Trial Dungeon.
 *
 * <p>Trois mécaniques de renforcement, toutes en pur état mémoire (rien à persister — un combo
 * ne survit pas à une session, c'est le principe) :
 * <ul>
 *   <li><b>Combo de kills</b> : enchaîner les kills dans une fenêtre glissante fait monter un
 *       multiplicateur de points. Encaisser un coup brise le combo — jouer vite ET propre paie.</li>
 *   <li><b>Jackpot</b> : chaque kill a une petite chance de multiplier ses points — renforcement
 *       à ratio variable, le plus puissant des schémas de récompense.</li>
 *   <li><b>Sans-faute</b> : conquérir un étage sans un seul coup reçu double la récompense de
 *       conquête. L'état est armé à l'entrée d'étage ({@link #beginFloor}).</li>
 * </ul>
 *
 * <p>Temps injecté en <b>ticks de jeu</b> (pas d'horloge murale) → logique pure et testable.
 * Le câblage évènementiel vit dans {@link DungeonRushHandler} et {@link DungeonPoints}.
 */
public final class DungeonRush {

    /** Fenêtre (ticks) entre deux kills pour maintenir le combo (8 s). */
    public static final long COMBO_WINDOW_TICKS = 160L;
    /** Plafond du compteur de combo. */
    public static final int COMBO_CAP = 30;
    /** Gain de multiplicateur par kill de combo au-delà du premier. */
    public static final double MULTIPLIER_PER_KILL = 0.05;
    /** Plafond du multiplicateur de combo (atteint à 30+ kills). */
    public static final double MULTIPLIER_CAP = 2.5;
    /** Probabilité de jackpot par kill. */
    public static final double JACKPOT_CHANCE = 0.04;
    /** Multiplicateur de points d'un jackpot. */
    public static final int JACKPOT_MULTIPLIER = 4;
    /** Multiplicateur de la récompense de conquête d'un étage sans-faute. */
    public static final int FLAWLESS_MULTIPLIER = 2;

    private record ComboState(int combo, long lastKillTick) {}

    private static final Map<UUID, ComboState> COMBOS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> FLAWLESS = new ConcurrentHashMap<>();

    private DungeonRush() {}

    // ── Fonctions pures ──────────────────────────────────────────────────────

    /** Prochain compteur de combo : chaîne si dans la fenêtre, sinon repart à 1. */
    static int nextCombo(int current, long lastKillTick, long nowTick) {
        if (current <= 0 || nowTick - lastKillTick > COMBO_WINDOW_TICKS) return 1;
        return Math.min(COMBO_CAP, current + 1);
    }

    /** Multiplicateur de points pour un compteur de combo donné (1.0 sans combo, plafonné). */
    public static double comboMultiplier(int combo) {
        if (combo <= 1) return 1.0;
        return Math.min(MULTIPLIER_CAP, 1.0 + (combo - 1) * MULTIPLIER_PER_KILL);
    }

    /** {@code true} si ce compteur mérite une fanfare (tous les 5 kills à partir de 5). */
    public static boolean isComboMilestone(int combo) {
        return combo >= 5 && combo % 5 == 0;
    }

    /** {@code true} si ce tirage (uniforme [0,1[) déclenche un jackpot. */
    public static boolean isJackpot(double roll) {
        return roll < JACKPOT_CHANCE;
    }

    // ── État par joueur ──────────────────────────────────────────────────────

    /** Enregistre un kill et retourne le nouveau compteur de combo. */
    public static int onKill(UUID playerId, long nowTick) {
        ComboState prev = COMBOS.get(playerId);
        int next = prev == null ? 1 : nextCombo(prev.combo(), prev.lastKillTick(), nowTick);
        COMBOS.put(playerId, new ComboState(next, nowTick));
        return next;
    }

    /** Compteur de combo courant AVANT le prochain kill (0 si aucun, fenêtre non vérifiée). */
    public static int currentCombo(UUID playerId) {
        ComboState s = COMBOS.get(playerId);
        return s == null ? 0 : s.combo();
    }

    /** Le joueur encaisse un coup : combo brisé + sans-faute perdu pour cet étage. */
    public static void onHit(UUID playerId) {
        COMBOS.remove(playerId);
        FLAWLESS.computeIfPresent(playerId, (k, v) -> false);
    }

    /** Arme le sans-faute à l'entrée d'un étage (et à sa conquête, pour l'étage suivant). */
    public static void beginFloor(UUID playerId) {
        FLAWLESS.put(playerId, true);
    }

    /** {@code true} si le joueur n'a encaissé aucun coup depuis {@link #beginFloor}. */
    public static boolean isFlawless(UUID playerId) {
        return FLAWLESS.getOrDefault(playerId, false);
    }

    /** Oublie tout l'état du joueur (logout). */
    public static void clear(UUID playerId) {
        COMBOS.remove(playerId);
        FLAWLESS.remove(playerId);
    }
}
