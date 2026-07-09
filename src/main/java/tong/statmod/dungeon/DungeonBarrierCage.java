package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mission M6 — Cage de blocs barrière autour de chaque île (2026-07-05).
 *
 * <p>Entoure entièrement l'île d'une boîte de {@code minecraft:barrier} (invisibles + solides) :
 * 4 murs + plafond + plancher. Empêche de <b>voler d'une île à l'autre</b> (par-dessus, par les
 * côtés ou par-dessous). Posée une fois à la génération de l'étage.
 *
 * <p>Dimensions calées sur {@link IslandGenerator#R} (rayon d'île) + une marge, et sur l'emprise
 * verticale de l'île (underside conique jusqu'à −43, végétation suspendue −44 → sommets ~ +47).
 * Avec {@code FLOOR_SPACING}=300 et R=148, les cages adjacentes (demi-emprise 150) se touchent
 * exactement sans se chevaucher.
 */
public final class DungeonBarrierCage {

    /** Marge horizontale au-delà du rayon d'île. */
    private static final int MARGIN = 2;
    /**
     * Emprise verticale de la cage, relative au sol de l'île (FLOOR_Y). Plafond assez haut pour ne
     * pas trancher les structures/arènes importées les plus hautes (obsidilith ~+47, remparts/piliers
     * d'arène ~+24) ; plancher sous la pointe de l'underside conique (−43) et sa végétation (−44).
     */
    private static final int Y_LOW = -48;
    private static final int Y_HIGH = 50;

    private DungeonBarrierCage() {}

    /** Pose la cage barrière complète autour de l'île centrée sur {@code sp}. */
    public static void enclose(ServerLevel lv, BlockPos sp) {
        int h = IslandGenerator.R + MARGIN; // demi-emprise horizontale (R+2)
        BlockState bar = Blocks.BARRIER.defaultBlockState();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        // ── Murs est/ouest (plans X = ±h) et nord/sud (plans Z = ±h) ──
        for (int y = Y_LOW; y <= Y_HIGH; y++) {
            for (int z = -h; z <= h; z++) {
                set(lv, m, sp, -h, y, z, bar);
                set(lv, m, sp, h, y, z, bar);
            }
            for (int x = -h; x <= h; x++) {
                set(lv, m, sp, x, y, -h, bar);
                set(lv, m, sp, x, y, h, bar);
            }
        }

        // ── Plafond (Y_HIGH) + plancher (Y_LOW) : disques pleins pour bloquer le vol vertical ──
        for (int x = -h; x <= h; x++) {
            for (int z = -h; z <= h; z++) {
                set(lv, m, sp, x, Y_HIGH, z, bar);
                set(lv, m, sp, x, Y_LOW, z, bar);
            }
        }
    }

    private static void set(ServerLevel lv, BlockPos.MutableBlockPos m, BlockPos sp,
                            int dx, int dy, int dz, BlockState s) {
        m.set(sp.getX() + dx, sp.getY() + dy, sp.getZ() + dz);
        lv.setBlock(m, s, 2); // flag 2 : maj client, pas de maj de voisinage (inutile pour une barrière)
    }
}
