package tong.statmod.dungeon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Mission M6 — Suivi des boss vivants par étage.
 *
 * <p>L'autel enregistre ici les UUID des boss qu'il invoque ({@link #register}). Le
 * {@link DungeonBossHandler} ne débloque l'étage suivant que quand <b>tous</b> les boss d'un
 * étage sont morts ({@link #onBossDeath} retourne {@code true}) — au lieu de débloquer au premier
 * kill (ce qui cassait les combats duo/vague).
 *
 * <p>En mémoire uniquement : après un redémarrage serveur en plein combat, le tracking d'un étage
 * disparaît. {@link DungeonBossHandler} bascule alors sur son heuristique de secours (n'importe
 * quel kill débloque) — voir {@link #isTracked}.
 */
public final class DungeonBossTracker {

    private static final Map<Integer, Set<UUID>> ALIVE = new HashMap<>();

    private DungeonBossTracker() {}

    /** Enregistre un boss vivant pour l'étage. */
    public static void register(int floor, UUID bossId) {
        ALIVE.computeIfAbsent(floor, k -> new HashSet<>()).add(bossId);
    }

    /** {@code true} si l'étage a des boss suivis (l'autel a été activé cette session). */
    public static boolean isTracked(int floor) {
        Set<UUID> s = ALIVE.get(floor);
        return s != null && !s.isEmpty();
    }

    /** {@code true} si {@code id} est l'un des boss suivis de l'étage. */
    public static boolean isTrackedBoss(int floor, UUID id) {
        Set<UUID> s = ALIVE.get(floor);
        return s != null && s.contains(id);
    }

    /** Nombre de boss encore vivants sur l'étage. */
    public static int remaining(int floor) {
        Set<UUID> s = ALIVE.get(floor);
        return s == null ? 0 : s.size();
    }

    /**
     * Retire un boss mort. Retourne {@code true} si c'était le dernier (étage clear).
     */
    public static boolean onBossDeath(int floor, UUID deadId) {
        Set<UUID> s = ALIVE.get(floor);
        if (s == null) return false;
        s.remove(deadId);
        if (s.isEmpty()) {
            ALIVE.remove(floor);
            return true;
        }
        return false;
    }

    /** Oublie tout le suivi de l'étage (retry / purge). */
    public static void clear(int floor) {
        ALIVE.remove(floor);
    }
}
