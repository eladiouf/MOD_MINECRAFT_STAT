package tong.statmod.dungeon.city;

/**
 * Hauteur du plafond de la caverne-cité (pur, déterministe, sans ServerLevel).
 * Effet cathédrale : haut au centre ({@link CityPlan#CEILING_Y}), plus bas vers le rempart,
 * ondulé par une pseudo-onde trigonométrique (déterministe, jamais Random/Math.random).
 */
public final class CityCeiling {

    private static final int FLOOR_MIN = 148;

    private CityCeiling() {}

    public static int heightAt(int x, int z) {
        double dx = x - CityPlan.CENTER_X, dz = z - CityPlan.CENTER_Z;
        double t = Math.min(1.0, Math.hypot(dx, dz) / CityPlan.WALL_INNER); // 0 centre → 1 rempart
        double base = CityPlan.CEILING_Y + t * (FLOOR_MIN - CityPlan.CEILING_Y); // lerp haut→bas
        double wave = Math.sin(x * 0.08) * Math.cos(z * 0.07) + 0.5 * Math.sin((x + z) * 0.05);
        int h = (int) Math.round(base + wave * 4.0);
        return Math.max(FLOOR_MIN, Math.min(CityPlan.CEILING_Y, h));
    }
}
