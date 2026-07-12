package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;

/**
 * Cité des Aventuriers (Étage 0) — géométrie PURE de la ville (aucun ServerLevel).
 *
 * <p>La cité est un disque de rayon {@value #RADIUS} centré en ({@value #CENTER_X},
 * {@value #CENTER_Z}), entièrement dans la zone étage 0 du donjon (z &lt; -150 →
 * {@code floorAtPos} = 0). Sol plein à Y={@value #GROUND_Y}, pieds du joueur à +1.
 * Toutes les positions sont déterministes : mêmes constantes → même ville.
 */
public final class CityPlan {

    public static final int CENTER_X = 0;
    public static final int CENTER_Z = -500;
    /** Y du bloc de sol : le joueur marche à GROUND_Y+1. */
    public static final int GROUND_Y = 100;
    /** Rayon extérieur de la caverne-cité. */
    public static final int RADIUS = 300;
    /** Rayon intérieur du rempart périmétral (épaisseur RADIUS-WALL_INNER). */
    public static final int WALL_INNER = 292;
    /** Sommet du rempart. */
    public static final int WALL_TOP_Y = 140;
    /** Plafond de la caverne (dalle + cristaux suspendus dessous). */
    public static final int CEILING_Y = 170;
    /** Rayon de la Grande Place. */
    public static final int PLAZA_RADIUS = 45;
    /** Demi-largeur des avenues radiales. */
    public static final double AVENUE_HALF_WIDTH = 3.5;

    private CityPlan() {}

    public static BlockPos center() { return new BlockPos(CENTER_X, GROUND_Y, CENTER_Z); }

    /** Spawn joueur : bord sud de la Grande Place, face au cœur de la cité. */
    public static BlockPos playerSpawn() {
        return new BlockPos(CENTER_X, GROUND_Y + 1, CENTER_Z + PLAZA_RADIUS - 6);
    }

    /** Porte du Donjon : perce le rempart SUD (côté grille des étages 1+). */
    public static BlockPos gateCenter() {
        return new BlockPos(CENTER_X, GROUND_Y, CENTER_Z + WALL_INNER - 12);
    }

    /** Cour des Portails : à l'ouest de la place. */
    public static BlockPos portalCourt() {
        return new BlockPos(CENTER_X - 100, GROUND_Y, CENTER_Z);
    }

    /** Camp des artisans (provisoire, plan A) : à l'est de la place. */
    public static BlockPos artisanCamp() {
        return new BlockPos(CENTER_X + 100, GROUND_Y, CENTER_Z);
    }

    public static boolean inCity(int x, int z) {
        long dx = x - CENTER_X, dz = z - CENTER_Z;
        return dx * dx + dz * dz <= (long) RADIUS * RADIUS;
    }

    public static boolean inPlaza(int x, int z) {
        long dx = x - CENTER_X, dz = z - CENTER_Z;
        return dx * dx + dz * dz <= (long) PLAZA_RADIUS * PLAZA_RADIUS;
    }

    /** Anneau du rempart périmétral (plein, sauf ouverture de la porte). */
    public static boolean inWallRing(int x, int z) {
        long dx = x - CENTER_X, dz = z - CENTER_Z;
        long d2 = dx * dx + dz * dz;
        return d2 <= (long) RADIUS * RADIUS && d2 >= (long) WALL_INNER * WALL_INNER;
    }

    /** Couloir de la Porte du Donjon dans le rempart sud (largeur 13). */
    public static boolean inGateOpening(int x, int z) {
        return Math.abs(x - CENTER_X) <= 6 && z >= CENTER_Z + WALL_INNER - 24;
    }

    /**
     * 6 avenues radiales de la place au rempart. Angle 90° = sud (vers la porte),
     * puis une avenue tous les 60°.
     */
    public static boolean onAvenue(int x, int z) {
        double dx = x - CENTER_X, dz = z - CENTER_Z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist <= PLAZA_RADIUS || dist >= WALL_INNER) return false;
        double angle = Math.toDegrees(Math.atan2(dz, dx)); // sud (+z) = +90°
        for (int k = 0; k < 6; k++) {
            double a = 90.0 + k * 60.0;
            double diff = Math.abs(normalizeDeg(angle - a));
            if (diff < 90.0 && dist * Math.sin(Math.toRadians(diff)) <= AVENUE_HALF_WIDTH) {
                return true;
            }
        }
        return false;
    }

    private static double normalizeDeg(double a) {
        a %= 360.0;
        if (a > 180.0) a -= 360.0;
        if (a < -180.0) a += 360.0;
        return a;
    }
}
