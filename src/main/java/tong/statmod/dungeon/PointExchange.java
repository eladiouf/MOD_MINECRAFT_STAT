package tong.statmod.dungeon;

/**
 * Mission M6 — Calcul pur de la conversion points → coins du shop (2026-07-05).
 * Sans dépendance monde → testable.
 */
public final class PointExchange {

    /** Résultat d'une conversion : points convertis, coins gagnés, points restants. */
    public record Result(int converted, long coins, int remainingPoints) {}

    private PointExchange() {}

    /**
     * Convertit {@code requested} points parmi {@code points} disponibles au taux {@code rate}.
     * Montant borné à [0, points] ; coins = floor(converted * rate).
     */
    public static Result compute(int requested, int points, double rate) {
        int converted = Math.max(0, Math.min(requested, points));
        long coins = (long) Math.floor(converted * rate);
        return new Result(converted, coins, points - converted);
    }
}
