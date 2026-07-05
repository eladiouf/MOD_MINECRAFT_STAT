package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import static tong.statmod.dungeon.DungeonArchitect.B;
import static tong.statmod.dungeon.DungeonArchitect.HX;
import static tong.statmod.dungeon.DungeonArchitect.HZ;
import static tong.statmod.dungeon.DungeonArchitect.O;
import static tong.statmod.dungeon.DungeonArchitect.S;
import static tong.statmod.dungeon.DungeonArchitect.column;
import static tong.statmod.dungeon.DungeonArchitect.fill;

/**
 * Mission M6 — Étage BOSS = <b>arène géante à ciel ouvert</b> (2026-07-05).
 *
 * <p>À la demande : les salles de boss sont d'<b>énormes arènes</b> occupant toute l'emprise de
 * l'île, ceinturées d'une <b>couronne de grands piliers</b> aux extrémités. Le centre est façonné
 * selon le type du boss ({@link DungeonBossArena}), avec l'autel sur une estrade, l'aménagement
 * ({@link DungeonRoomDressing}), un checkpoint waystone et le téléporteur (scellé jusqu'au kill).
 * Thématisé par {@link BlockPalette}. Pas de toit → combat épique à ciel ouvert.
 */
public final class DungeonBossArenaFloor {

    /** Hauteur des remparts de l'arène (hauts pour l'imposant). */
    private static final int WALL_H = 18;
    /** Hauteur des grands piliers de la couronne. */
    private static final int PILLAR_H = 24;
    /** Retrait des piliers depuis le rempart. */
    private static final int INSET = 8;
    /** Espacement des piliers le long de chaque bord. */
    private static final int SPACING = 20;

    private DungeonBossArenaFloor() {}

    /** Construit l'arène de boss complète autour du centre d'île {@code sp} (y=100). */
    public static void build(ServerLevel lv, BlockPos sp, BlockPalette t, int floor) {
        // Plateforme de base (sol plein + remparts solides, à ciel ouvert).
        buildPlatform(lv, sp, t);

        // ── Couronne de grands piliers autour de l'arène (les « extrémités ») ──
        pillarRing(lv, sp, t);

        // ── Centre : arène façonnée selon le type du boss + estrade + autel ──
        DungeonBossArena.shape(lv, sp, t, floor); // anchor = centre au niveau du sol
        buildDais(lv, sp, t);
        S(lv, O(sp, 0, 2, 0), B(DungeonBlocks.BOSS_ALTAR.get())); // autel sur l'estrade

        // ── Aménagement (fontaine, armor stands) autour de l'estrade (anchor = haut du dais, y=2) ──
        DungeonRoomDressing.dressBossRoom(lv, O(sp, 0, 2, 0), t, floor);

        // ── Checkpoint waystone à côté de l'autel (retour rapide une fois le boss vaincu) ──
        tong.statmod.integration.waystones.WaystonesBridge.placeCheckpoint(
                lv, O(sp, 4, 2, 0), FloorPalette.forFloor(floor), floor);

        // ── Téléporteur de descente, encadré au nord, scellé jusqu'au kill du boss ──
        exitGate(lv, sp, t);
    }

    /** Plateforme de base : sol plein sur toute l'emprise + remparts solides, à ciel ouvert. */
    public static void buildPlatform(ServerLevel lv, BlockPos sp, BlockPalette t) {
        BlockState floorB = B(t.base());
        BlockState wallB = B(t.base()); // bloc PLEIN (pas wallBlock() qui est un muret ajouré)
        fill(lv, sp, -HX, -1, -HZ, HX, -1, HZ, floorB);
        for (int x = -HX; x <= HX; x++) {
            column(lv, sp, x, -HZ, WALL_H, wallB);
            column(lv, sp, x, HZ, WALL_H, wallB);
        }
        for (int z = -HZ; z <= HZ; z++) {
            column(lv, sp, -HX, z, WALL_H, wallB);
            column(lv, sp, HX, z, WALL_H, wallB);
        }
    }

    /**
     * Finalise une arène IMPORTÉE (déjà posée sur la plateforme) : pose l'autel du boss sur le VRAI
     * sol de l'arène (trouvé par scan de la colonne centrale → s'adapte à la hauteur réelle de la
     * structure) ; waystone + téléporteur restent SUR LA PLATEFORME, au sud, hors de la structure.
     */
    public static void finishImportedArena(ServerLevel lv, BlockPos sp, BlockPalette t, int floor) {
        // ── Autel DANS la structure : sur le sol réel de l'arène (scan de la colonne centrale). ──
        int arenaFloorY = surfaceY(lv, sp.getX(), sp.getZ(), sp.getY() + 60, sp.getY() - 6);
        BlockPos altar = new BlockPos(sp.getX(), arenaFloorY + 1, sp.getZ());
        clearColumn(lv, altar); // dégage l'autel + espace au-dessus (pas encastré dans un décor central)
        S(lv, altar, B(DungeonBlocks.BOSS_ALTAR.get()));

        // ── Waystone + téléporteur HORS structure, sur la plateforme (y=platform surface), au sud. ──
        int platformY = sp.getY(); // sol walkable de la plateforme (bloc à y-1)
        BlockPos way = new BlockPos(sp.getX() + 4, platformY, sp.getZ() + HZ - 10);
        tong.statmod.integration.waystones.WaystonesBridge.placeCheckpoint(
                lv, way, FloorPalette.forFloor(floor), floor);
        BlockPos gate = new BlockPos(sp.getX(), platformY, sp.getZ() + HZ - 6);
        clearColumn(lv, gate);
        S(lv, gate, B(DungeonBlocks.NEXT_FLOOR_TELEPORTER.get()));
    }

    /** Y de la 1ʳᵉ surface solide (bloc plein avec de l'air au-dessus), scannée du haut vers le bas. */
    private static int surfaceY(ServerLevel lv, int x, int z, int top, int bottom) {
        for (int y = top; y >= bottom; y--) {
            BlockPos p = new BlockPos(x, y, z);
            if (!lv.getBlockState(p).isAir() && lv.getBlockState(p.above()).isAir()) return y;
        }
        return bottom;
    }

    /** Dégage 3 blocs d'air au-dessus de {@code p} (pour qu'un bloc posé ne soit pas encastré). */
    private static void clearColumn(ServerLevel lv, BlockPos p) {
        for (int dy = 1; dy <= 3; dy++) S(lv, p.above(dy), B(net.minecraft.world.level.block.Blocks.AIR));
    }

    /** Couronne de piliers 3×3 le long des 4 bords, retirés du rempart, avec cap lumineux. */
    private static void pillarRing(ServerLevel lv, BlockPos sp, BlockPalette t) {
        int rx = HX - INSET, rz = HZ - INSET;
        // Bords nord/sud.
        for (int x = -rx; x <= rx; x += SPACING) {
            bigPillar(lv, sp, t, x, -rz);
            bigPillar(lv, sp, t, x, rz);
        }
        // Bords est/ouest (évite de re-poser les coins).
        for (int z = -rz + SPACING; z <= rz - SPACING; z += SPACING) {
            bigPillar(lv, sp, t, -rx, z);
            bigPillar(lv, sp, t, rx, z);
        }
    }

    /** Un grand pilier 3×3 de hauteur {@link #PILLAR_H}, coiffé d'un bloc lumineux du thème. */
    private static void bigPillar(ServerLevel lv, BlockPos sp, BlockPalette t, int cx, int cz) {
        BlockState body = B(t.decorPrimary());
        BlockState cap = B(t.light());
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            column(lv, sp, cx + dx, cz + dz, PILLAR_H, body);
        }
        // Chapiteau + fanal lumineux au sommet.
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            S(lv, O(sp, cx + dx, PILLAR_H, cz + dz), B(t.accent()));
        }
        S(lv, O(sp, cx, PILLAR_H + 1, cz), cap);
    }

    /**
     * Estrade centrale (2 blocs, sommet walkable à y=2) sur laquelle reposent l'autel et les armor
     * stands d'aménagement. Emprise x∈[-7,7], z∈[-6,5] — assez large pour porter les trophées placés
     * par {@link DungeonRoomDressing#dressBossRoom} (aux ±6). Bâtie APRÈS l'arène → restaure un plateau
     * plat au centre même si l'arène creuse une fosse autour.
     */
    private static void buildDais(ServerLevel lv, BlockPos sp, BlockPalette t) {
        BlockState body = B(t.accent());
        BlockState edge = B(t.decorPrimary());
        for (int dx = -7; dx <= 7; dx++) for (int dz = -6; dz <= 5; dz++) {
            boolean rim = Math.abs(dx) == 7 || dz == -6 || dz == 5;
            S(lv, O(sp, dx, 0, dz), rim ? edge : body);
            S(lv, O(sp, dx, 1, dz), rim ? edge : body);
        }
    }

    /** Portail de sortie encadré au nord (téléporteur scellé tant que le boss n'est pas vaincu). */
    private static void exitGate(ServerLevel lv, BlockPos sp, BlockPalette t) {
        BlockState pillar = B(t.decorPrimary());
        BlockState light = B(t.light());
        int gz = -HZ + 4;
        for (int[] c : new int[][]{{-2, 0}, {2, 0}}) {
            for (int y = 0; y <= 4; y++) S(lv, O(sp, c[0], y, gz), pillar);
            S(lv, O(sp, c[0], 5, gz), light);
        }
        S(lv, O(sp, 0, 0, gz), B(DungeonBlocks.NEXT_FLOOR_TELEPORTER.get()));
    }
}
