package tong.statmod.dungeon;

/**
 * Mission M6 — Island Redesign (spec 2026-07-03), Phase A.
 *
 * <p>Géométrie pure d'une île volante : silhouette organique (rayon modulé par bruit harmonique)
 * et underside conique (profondeur décroissante vers le bord). Entièrement déterministe à partir
 * d'un seed — un même étage se régénère toujours identique.
 *
 * <p><b>Aucune dépendance Minecraft world/block</b> — la classe est instanciable en JUnit sans
 * Bootstrap (pattern {@code RuneShardIds} : la géométrie est testable, le placement de blocs
 * reste dans {@code IslandGenerator}).
 */
public final class IslandShaper {

    /** Facteur minimal de modulation du rayon — la silhouette ne descend jamais sous 0.82·R. */
    static final double MIN_FACTOR = 0.82;
    /** Amplitude de la modulation — rayon max = (MIN_FACTOR + AMPLITUDE)·R = 1.0·R. */
    static final double AMPLITUDE = 0.18;

    private final long seed;
    private final int radius;
    private final double phase1;
    private final double phase2;

    public IslandShaper(long seed, int radius) {
        this.seed = seed;
        this.radius = Math.max(1, radius);
        // Deux phases indépendantes dérivées du seed pour les deux harmoniques.
        this.phase1 = (mix(seed) >>> 11) * 0x1.0p-53 * 2.0 * Math.PI;
        this.phase2 = (mix(seed ^ 0x5DEECE66DL) >>> 11) * 0x1.0p-53 * 2.0 * Math.PI;
    }

    /** Seed stable par étage (splitmix64) — indépendant du world seed pour rester régénérable. */
    public static long seedFor(int floor) {
        return mix(floor * 0x9E3779B97F4A7C15L);
    }

    /** Étape de mixage splitmix64. */
    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public int radius() {
        return radius;
    }

    /**
     * Rayon effectif de la silhouette dans la direction {@code theta} (radians).
     * Borné dans {@code [0.82·R, 1.0·R]}.
     */
    public double radiusAt(double theta) {
        double n1 = Math.sin(3.0 * theta + phase1);
        double n2 = 0.5 * Math.sin(7.0 * theta + phase2);
        // (n1+n2) ∈ [-1.5, 1.5] → normalisé dans [0, 1].
        double noise = ((n1 + n2) / 1.5 + 1.0) / 2.0;
        return radius * (MIN_FACTOR + AMPLITUDE * noise);
    }

    /** {@code true} si la cellule (dx,dz) relative au centre est dans la silhouette. */
    public boolean isInside(int dx, int dz) {
        double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
        if (dist == 0) return true;
        double theta = Math.atan2(dz, dx);
        return dist <= radiusAt(theta);
    }

    /** {@code true} si la cellule est inside avec au moins un voisin cardinal outside. */
    public boolean isBoundary(int dx, int dz) {
        if (!isInside(dx, dz)) return false;
        return !isInside(dx + 1, dz) || !isInside(dx - 1, dz)
                || !isInside(dx, dz + 1) || !isInside(dx, dz - 1);
    }

    /** Profondeur maximale du cône underside. */
    public int maxDepth() {
        return radius / 2 + 2;
    }

    /**
     * Profondeur de l'underside sous la cellule (dx,dz) : 0 si outside, sinon 1..maxDepth.
     * Conique — profond au centre, 1 bloc au bord — avec un léger jitter par colonne pour
     * casser la régularité.
     */
    public int depthAt(int dx, int dz) {
        if (!isInside(dx, dz)) return 0;
        double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
        double theta = dist == 0 ? 0 : Math.atan2(dz, dx);
        double rEff = radiusAt(theta);
        double t = Math.max(0.0, 1.0 - dist / rEff);
        int base = (int) Math.round(maxDepth() * Math.pow(t, 1.3));
        int jitter = (int) (Math.abs(mix(seed ^ ((long) dx << 32) ^ (dz & 0xFFFFFFFFL))) % 2);
        return Math.max(1, Math.min(maxDepth(), base + jitter));
    }
}
