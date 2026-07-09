package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import static tong.statmod.dungeon.DungeonArchitect.B;
import static tong.statmod.dungeon.DungeonArchitect.O;
import static tong.statmod.dungeon.DungeonArchitect.S;

/**
 * Mission M6 — Balisage de progression (2026-07-05).
 *
 * <p>Dans la chaîne de pièces serpentine, chaque pièce du milieu a DEUX portes (l'entrée et la
 * sortie) → le joueur ne sait pas laquelle mène plus loin. On <b>balise la porte de sortie</b> :
 * une traînée lumineuse au sol (du centre vers la porte) + un fanal au-dessus de l'ouverture. Le
 * joueur apprend vite « suis les lumières » et ne se perd/rebrousse jamais. Langage visuel constant
 * (lanternes de mer cyan) quel que soit le thème, pour rester lisible.
 */
public final class DungeonWayfinding {

    /** Bloc de guidage : lumineux, reconnaissable, identique sur tous les thèmes. */
    private static final BlockState GUIDE = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState BEACON = Blocks.END_ROD.defaultBlockState()
            .setValue(BlockStateProperties.FACING, Direction.UP);
    /** Hauteur de l'ouverture de porte (aligné sur DungeonRoomChain.DOOR_H). */
    private static final int DOOR_H = 4;

    private DungeonWayfinding() {}

    /** Balise la porte de SORTIE d'une pièce (rien pour la dernière : le téléporteur est la cible). */
    public static void mark(ServerLevel lv, BlockPos sp, DungeonLayout.Room r, int floor) {
        if (r.exitDoor() == DungeonLayout.Dir.NONE) return;
        int cx = r.centerX(), cz = r.centerZ();
        int dx = cx, dz = cz;
        switch (r.exitDoor()) {
            case EAST -> dx = r.maxX();
            case WEST -> dx = r.minX();
            case SOUTH -> dz = r.maxZ();
            case NORTH -> dz = r.minZ();
            case NONE -> { return; }
        }
        int i = r.index();
        int y0 = DungeonRoomChain.roomYOffset(i, floor);
        int y1 = DungeonRoomChain.roomYOffset(i + 1, floor);
        boolean hasStairs = (y0 != y1);
        int maxY = Math.max(y0, y1);

        trail(lv, sp, cx, cz, dx, dz, hasStairs);   // traînée au sol center → porte
        doorBeacon(lv, sp, dx, dz, maxY - y0);      // fanal au-dessus de l'ouverture de sortie
    }

    /** Traînée de dots lumineux encastrés dans le sol, du centre vers la porte (ligne axiale). */
    private static void trail(ServerLevel lv, BlockPos sp, int x0, int z0, int x1, int z1, boolean hasStairs) {
        int sx = Integer.signum(x1 - x0), sz = Integer.signum(z1 - z0);
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
        int limit = hasStairs ? steps - 2 : steps;
        for (int i = 2; i <= limit; i += 2) {
            int x = x0 + sx * i;
            int z = z0 + sz * i;
            S(lv, O(sp, x, -1, z), GUIDE); // remplace le sol → dot lumineux
        }
    }

    /** Fanal au-dessus de l'ouverture de la porte de sortie (lanterne + tige d'end vers le haut). */
    private static void doorBeacon(ServerLevel lv, BlockPos sp, int dx, int dz, int yOffset) {
        S(lv, O(sp, dx, yOffset + DOOR_H, dz), GUIDE);
        S(lv, O(sp, dx, yOffset + DOOR_H + 1, dz), BEACON);
    }
}
