package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;

import java.util.List;

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
    /** Rayon extérieur de la caverne-cité (resserré : refonte visuelle 2026-07-16). */
    public static final int RADIUS = 160;
    /** Rayon intérieur du rempart périmétral (épaisseur RADIUS-WALL_INNER). */
    public static final int WALL_INNER = 152;
    /** Sommet du rempart. */
    public static final int WALL_TOP_Y = 128;
    /** Plafond max de la caverne (peak au centre ; ondulé par CityCeiling). */
    public static final int CEILING_Y = 170;
    /** Rayon de la Grande Place. */
    public static final int PLAZA_RADIUS = 28;
    /** Demi-largeur des avenues radiales. */
    public static final double AVENUE_HALF_WIDTH = 3.5;
    public static final int INNER_RING_RADIUS = 62;
    public static final int OUTER_RING_RADIUS = 118;
    private static final double RING_HALF_WIDTH = 4.0;

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

    /** Cour des Portails : créneau libre à l'est de la place (le sud est saturé). */
    public static BlockPos portalCourt() {
        return new BlockPos(CENTER_X + 62, GROUND_Y, CENTER_Z);
    }

    /** Camp des artisans (provisoire, plan A) : à l'est de la place. */
    public static BlockPos artisanCamp() {
        return artisanDistrict();
    }

    // Layout 2 anneaux (refonte 2026-07-16, +z = sud vers la porte).
    // Extérieur R=118 : guilde, 4 quartiers, marché, arène, entraînement.
    public static BlockPos guild() { return new BlockPos(CENTER_X + 45, GROUND_Y, CENTER_Z - 109); }
    public static BlockPos humanQuarter() { return new BlockPos(CENTER_X - 109, GROUND_Y, CENTER_Z + 45); }
    public static BlockPos elvenQuarter() { return new BlockPos(CENTER_X - 109, GROUND_Y, CENTER_Z - 45); }
    public static BlockPos dwarvenQuarter() { return new BlockPos(CENTER_X - 45, GROUND_Y, CENTER_Z + 109); }
    public static BlockPos beastQuarter() { return new BlockPos(CENTER_X + 45, GROUND_Y, CENTER_Z + 109); }
    public static BlockPos market() { return new BlockPos(CENTER_X + 109, GROUND_Y, CENTER_Z + 45); }
    public static BlockPos arena() { return new BlockPos(CENTER_X + 109, GROUND_Y, CENTER_Z - 45); }
    public static BlockPos trainingGround() { return new BlockPos(CENTER_X - 45, GROUND_Y, CENTER_Z - 109); }
    // Intérieur R=62 (tourné 45°) : artisans, sanctuaire, jardins, hall des héros.
    public static BlockPos artisanDistrict() { return new BlockPos(CENTER_X + 44, GROUND_Y, CENTER_Z + 44); }
    public static BlockPos sanctuary() { return new BlockPos(CENTER_X - 44, GROUND_Y, CENTER_Z + 44); }
    public static BlockPos hangingGardens() { return new BlockPos(CENTER_X - 44, GROUND_Y, CENTER_Z - 44); }
    public static BlockPos hallOfHeroes() { return new BlockPos(CENTER_X + 44, GROUND_Y, CENTER_Z - 44); }

    public record CitySite(String id, BlockPos center, int radius) {}

    public static List<CitySite> sites() {
        return List.of(
                new CitySite("plaza", center(), PLAZA_RADIUS),
                new CitySite("guild", guild(), 30),
                new CitySite("human", humanQuarter(), 30),
                new CitySite("elven", elvenQuarter(), 30),
                new CitySite("dwarven", dwarvenQuarter(), 30),
                new CitySite("beast", beastQuarter(), 30),
                new CitySite("market", market(), 30),
                new CitySite("artisans", artisanDistrict(), 22),
                new CitySite("arena", arena(), 30),
                new CitySite("training", trainingGround(), 30),
                new CitySite("sanctuary", sanctuary(), 20),
                new CitySite("gardens", hangingGardens(), 20),
                new CitySite("heroes", hallOfHeroes(), 20),
                new CitySite("portals", portalCourt(), 16),
                new CitySite("gate", gateCenter(), 0));
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

    public static boolean onRingRoad(int x, int z) {
        double dx = x - CENTER_X, dz = z - CENTER_Z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        return Math.abs(dist - INNER_RING_RADIUS) <= RING_HALF_WIDTH
                || Math.abs(dist - OUTER_RING_RADIUS) <= RING_HALF_WIDTH;
    }

    public static boolean onDistrictConnector(int x, int z) {
        for (CitySite site : sites()) {
            if (site.id().equals("plaza") || site.id().equals("gate")) continue;
            double ax = CENTER_X, az = CENTER_Z;
            double bx = site.center().getX(), bz = site.center().getZ();
            double vx = bx - ax, vz = bz - az;
            double length2 = vx * vx + vz * vz;
            double t = ((x - ax) * vx + (z - az) * vz) / length2;
            if (t < 0.18 || t > 0.88) continue;
            double px = ax + t * vx, pz = az + t * vz;
            if (Math.hypot(x - px, z - pz) <= AVENUE_HALF_WIDTH) return true;
        }
        return false;
    }

    public static boolean inArenaCombat(int x, int z) {
        long dx = x - arena().getX();
        long dz = z - arena().getZ();
        return dx * dx + dz * dz <= 26L * 26L;
    }

    private static double normalizeDeg(double a) {
        a %= 360.0;
        if (a > 180.0) a -= 360.0;
        if (a < -180.0) a += 360.0;
        return a;
    }
}
