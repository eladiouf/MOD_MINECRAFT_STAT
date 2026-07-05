package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Mission M6 — Relief organique de l'île (refonte 2026-07-04).
 *
 * <p><b>Rôle</b> : habiller le <i>pourtour</i> de l'île (le sol naturel qui entoure la forteresse
 * {@link DungeonArchitect}) avec du relief et de la végétation thématisés par tier. L'intérieur de
 * la forteresse reste <b>plat et propre</b> : {@code DungeonArchitect} y bâtit ses salles.
 *
 * <p><b>Pourquoi seulement le pourtour ?</b> L'ancienne version sculptait toute la silhouette puis
 * la forteresse effaçait l'intérieur (air-clear {@code y≥0}) et le cône underside recouvrait les
 * creux — tout le relief intérieur était perdu, et les crevasses perçaient des trous. On ne sculpte
 * donc que la couronne extérieure, en relief <b>montant uniquement</b> (jamais de trou), atténué
 * près des murs pour ne pas cogner la forteresse.
 *
 * <p><b>Déterministe</b> : même seed → même relief. Aucun état Minecraft partagé.
 */
public final class IslandTerrainShaper {

    /** Amplitude max d'un monticule de pourtour (blocs). Volontairement doux → sol naturel. */
    private static final int RIM_MAX_HEIGHT = 4;
    /** Largeur (blocs) sur laquelle le relief remonte depuis le mur de la forteresse. */
    private static final int TAPER_WIDTH = 6;

    private final long seed;
    private final IslandShaper shaper;

    /** {@code floor} n'influe plus sur la géométrie du pourtour (relief doux uniforme) mais reste
     * dans la signature pour un futur usage (densité de décor par tier, p. ex.). */
    public IslandTerrainShaper(long seed, int floor, int radius) {
        this.seed = seed;
        this.shaper = new IslandShaper(seed, radius);
    }

    /**
     * Construit le sol organique du pourtour de l'île à {@code sp}. Ne touche <b>jamais</b> à
     * l'emprise de la forteresse ({@code |dx|≤HX && |dz|≤HZ}) : {@link DungeonArchitect} y règne.
     * Le relief est ajouté au-dessus de {@code y=0} (le cône underside fournit déjà le sol
     * {@code y=-1}), donc aucune colonne ne peut créer de vide.
     */
    public void buildIslandGround(ServerLevel lv, BlockPos sp, BlockPalette t, FloorPalette tier) {
        int radius = shaper.radius();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!shaper.isInside(dx, dz)) continue;

                int h = rimHeightAt(dx, dz);
                if (h <= 0) continue;

                // Colonne pleine y=0..h-1 posée sur le sol underside (y=-1). Sommet = surface (thème).
                for (int y = 0; y < h; y++) {
                    lv.setBlock(sp.offset(dx, y, dz), rimBlock(t, y, h), 3);
                }
                // Végétation / cristaux sur les sommets dégagés (par tier).
                if (shouldDecorate(dx, dz)) {
                    addDecoration(lv, sp.offset(dx, h, dz), tier, dx, dz);
                }
            }
        }
    }

    /**
     * Hauteur du monticule de pourtour en (dx,dz) — <b>fonction pure testable</b> (aucun accès
     * monde). Invariants garantis :
     * <ul>
     *   <li><b>0 dans l'emprise de la forteresse</b> ({@code |dx|≤HX && |dz|≤HZ}) → l'intérieur
     *       reste plat, {@link DungeonArchitect} y règne.</li>
     *   <li><b>Toujours ≥ 0</b> → relief montant uniquement, jamais de trou dans le sol.</li>
     *   <li>Atténué sur {@link #TAPER_WIDTH} blocs depuis le mur pour épouser la coque.</li>
     * </ul>
     */
    public int rimHeightAt(int dx, int dz) {
        int fx = DungeonArchitect.HX;
        int fz = DungeonArchitect.HZ;
        // L'intérieur de la forteresse reste plat.
        if (Math.abs(dx) <= fx && Math.abs(dz) <= fz) return 0;

        // Distance de Chebyshev hors de l'emprise → 0 collé au mur, grandit vers le bord.
        int outX = Math.max(0, Math.abs(dx) - fx);
        int outZ = Math.max(0, Math.abs(dz) - fz);
        int out = Math.max(outX, outZ);
        double taper = Math.min(1.0, out / (double) TAPER_WIDTH);

        // Deux harmoniques → collines douces, positif garanti (décalé dans [0,1]).
        double scale = 0.11;
        double n1 = Math.sin(dx * scale + seed * 0.001);
        double n2 = Math.cos(dz * scale - seed * 0.0007);
        double noise = (n1 * n2 + 1.0) / 2.0; // [0,1]

        return (int) Math.round(RIM_MAX_HEIGHT * noise * taper);
    }

    /** Bloc de la colonne de pourtour : cœur en base, sommet en surface d'accent. */
    private BlockState rimBlock(BlockPalette t, int y, int h) {
        if (y == h - 1) return t.accent().defaultBlockState();          // surface foulée
        if (y >= h - 2) return t.decorPrimary().defaultBlockState();    // sous-couche rocheuse
        return t.base().defaultBlockState();                            // masse
    }

    /** ~40 % des sommets reçoivent un décor (bruit indépendant de la hauteur). */
    private boolean shouldDecorate(int dx, int dz) {
        double noise = Math.sin(dx * 0.15 + seed) * Math.cos(dz * 0.15 + seed * 0.7);
        return noise > 0.2;
    }

    /** Décor de surface thématisé par tier (posé sur le sommet du monticule). */
    private void addDecoration(ServerLevel lv, BlockPos pos, FloorPalette palette, int dx, int dz) {
        Random decorRng = new Random(seed + dx * 31L + dz);
        switch (palette) {
            case EARLY -> lv.setBlock(pos, (decorRng.nextDouble() < 0.3
                    ? Blocks.MOSS_CARPET : Blocks.SHORT_GRASS).defaultBlockState(), 3);
            case MID -> lv.setBlock(pos, (decorRng.nextBoolean()
                    ? Blocks.FERN : Blocks.POPPY).defaultBlockState(), 3);
            case LATE -> lv.setBlock(pos, (decorRng.nextBoolean()
                    ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState(), 3);
            case ABYSS -> lv.setBlock(pos, (decorRng.nextDouble() < 0.6
                    ? Blocks.AMETHYST_CLUSTER : palette.light()).defaultBlockState(), 3);
        }
    }
}
