package tong.statmod.dungeon.party;

/**
 * Sélection de cibles coordonnée du groupe d'aventuriers — logique PURE (aucun objet Minecraft),
 * donc testable. Décide, face à un ou plusieurs joueurs, qui le groupe doit concentrer, quelle
 * proie l'assassin isole, et quelle menace le tank doit intercepter pour protéger le backline.
 *
 * <p>Conventions : {@code pos[i] = {x, z}} (plan horizontal), {@code hpFrac[i] ∈ [0,1]}
 * (PV restants / PV max, absorption incluse par l'appelant). Renvoie des index dans ces tableaux,
 * ou -1 si aucun joueur.
 */
public final class PartyTargeting {

    private PartyTargeting() {}

    /**
     * Cible de FOCUS (concentration de dégâts) : le joueur le plus « finançable » — plus bas
     * ratio de PV, départage par proximité au centre du groupe (on achève le plus proche/faible
     * pour sécuriser un kill plutôt que d'étaler les dégâts).
     */
    public static int focusIndex(double[] hpFrac, double[][] pos, double partyCx, double partyCz) {
        int best = -1;
        double bestScore = Double.MAX_VALUE;
        for (int i = 0; i < hpFrac.length; i++) {
            double d2 = dist2(pos[i][0], pos[i][1], partyCx, partyCz);
            // Poids : PV d'abord (×1000), distance en départage léger.
            double score = hpFrac[i] * 1000.0 + Math.sqrt(d2);
            if (score < bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    /**
     * Proie de l'ASSASSIN : le joueur le plus ISOLÉ (distance maximale à son plus proche allié) —
     * on pique la cible que ses alliés ne peuvent pas couvrir. Avec un seul joueur, c'est lui.
     */
    public static int isolatedIndex(double[][] pos) {
        int n = pos.length;
        if (n == 0) return -1;
        if (n == 1) return 0;
        int best = -1;
        double bestNearest = -1.0;
        for (int i = 0; i < n; i++) {
            double nearest = Double.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                nearest = Math.min(nearest, dist2(pos[i][0], pos[i][1], pos[j][0], pos[j][1]));
            }
            if (nearest > bestNearest) {
                bestNearest = nearest;
                best = i;
            }
        }
        return best;
    }

    /**
     * Menace prioritaire pour le TANK : le joueur le plus proche du backline (mage/soigneur) à
     * protéger — le tank va l'intercepter. {@code backCx/backCz} = centre du backline.
     */
    public static int nearestToPoint(double[][] pos, double backCx, double backCz) {
        int best = -1;
        double bestD2 = Double.MAX_VALUE;
        for (int i = 0; i < pos.length; i++) {
            double d2 = dist2(pos[i][0], pos[i][1], backCx, backCz);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = i;
            }
        }
        return best;
    }

    /** Nombre de joueurs groupés autour de {@code (cx,cz)} dans un rayon {@code radius}. */
    public static int clusterSize(double[][] pos, double cx, double cz, double radius) {
        double r2 = radius * radius;
        int n = 0;
        for (double[] p : pos) {
            if (dist2(p[0], p[1], cx, cz) <= r2) n++;
        }
        return n;
    }

    private static double dist2(double ax, double az, double bx, double bz) {
        double dx = ax - bx, dz = az - bz;
        return dx * dx + dz * dz;
    }
}
