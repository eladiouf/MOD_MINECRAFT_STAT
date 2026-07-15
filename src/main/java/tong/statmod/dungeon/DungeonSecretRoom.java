package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import static tong.statmod.dungeon.DungeonArchitect.B;
import static tong.statmod.dungeon.DungeonArchitect.O;
import static tong.statmod.dungeon.DungeonArchitect.S;

/**
 * Mission M6 — Salles secrètes à récompenses (2026-07-05).
 *
 * <p>Une salle par étage cache une <b>chambre au trésor</b> creusée SOUS une pièce de combat,
 * accessible par une <b>trappe en bois</b> dissimulée dans le sol (le seul bloc de bois d'une pièce
 * de pierre → l'indice pour l'explorateur attentif) + une échelle. La chambre contient des coffres
 * de trésor riches ({@code dungeon_treasure}), une statue-trophée et de la lumière.
 *
 * <p>Variante Qliphoth (2026-07-10) : arène 15×15 avec spawner boss à la place du labyrinthe.
 */
public final class DungeonSecretRoom {

    private static final int FLOOR_Y = -6;   // sol de la chambre
    private static final int CEIL_Y = -3;    // plafond de la chambre
    private static final int QLIPHOTH_Y = -8; // sol arène Qliphoth (plus profond)
    private static final int QLIPHOTH_CEIL = -2; // plafond arène Qliphoth (plus haut)

    private DungeonSecretRoom() {}

    /** ~1 floor/17 a un combat Qliphoth optionnel. */
    static boolean isQliphothFloor(int floor) {
        return Math.floorMod(floor * 7919 + 11, 17) == 0;
    }

    /** Index d'arc (0-9) pour un étage. */
    private static int arcIndex(int floor) {
        return Math.floorDiv(floor - 1, 10) % 10;
    }

    /** ID du spawner Qliphoth pour l'arc donné. */
    private static String qliphothSpawnerId(int arc) {
        return switch (arc) {
            case 0, 3 -> "fdbosses:geburah_boss_spawner";
            case 1, 6 -> "fdbosses:netzach_boss_spawner";
            case 2, 5, 7 -> "fdbosses:malkuth_boss_spawner";
            default  -> "fdbosses:chesed_boss_spawner";
        };
    }

    /** Place une salle secrète si {@code r} est la pièce élue de l'étage (≈ 1 par étage). */
    public static void maybePlace(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int floor) {
        // Une seule pièce de combat par étage (index déterministe dans la plage combat 1..10).
        if (r.index() != 2 + Math.floorMod(floor, 7)) return;

        // Chambre sous le QUADRANT nord-ouest (pas le centre, occupé par les décors dais/bassin).
        int q = Math.max(6, Math.min(r.maxX() - r.minX(), r.maxZ() - r.minZ()) / 4);
        int cx = r.centerX() - q, cz = r.centerZ() - q;
        BlockState wall = B(t.base());

        // ── Variante Qliphoth : arène 15×15 avec spawner boss ──
        if (isQliphothFloor(floor)) {
            buildQliphothArena(lv, sp, cx, cz, t, floor, wall);
            return;
        }

        // ── Coque de la chambre (9×9), sous la pièce ──
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                S(lv, O(sp, cx + dx, FLOOR_Y, cz + dz), wall);        // sol
                S(lv, O(sp, cx + dx, CEIL_Y, cz + dz), wall);         // plafond
                boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 4;
                for (int y = FLOOR_Y + 1; y < CEIL_Y; y++) {
                    S(lv, O(sp, cx + dx, y, cz + dz), edge ? wall : B(Blocks.AIR));
                }
            }
        }

        // ── Puits d'accès au bord (x=cx, z=cz-2) : trappe + échelle ──
        for (int y = FLOOR_Y + 1; y <= -1; y++) {
            S(lv, O(sp, cx, y, cz - 2), B(Blocks.AIR));
        }
        BlockState ladder = B(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH);
        for (int y = FLOOR_Y + 1; y <= -2; y++) {
            S(lv, O(sp, cx, y, cz - 3), wall);            // support plein
            S(lv, O(sp, cx, y, cz - 2), ladder);          // échelle
        }
        BlockState trap = B(Blocks.SPRUCE_TRAPDOOR)
                .setValue(TrapDoorBlock.HALF, Half.TOP)
                .setValue(TrapDoorBlock.OPEN, false)
                .setValue(TrapDoorBlock.FACING, Direction.SOUTH);
        S(lv, O(sp, cx, -1, cz - 2), trap);

        // ── Grille du Labyrinthe (9×9) ──
        // 1 = mur, 0 = chemin libre
        int[][] maze = {
            {1, 1, 1, 1, 1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 1, 1, 0, 1, 1, 0, 1}, // (4, 2) est 0 (chemin ouvert pour l'échelle !)
            {1, 0, 1, 0, 0, 0, 1, 0, 1},
            {1, 0, 1, 0, 1, 0, 1, 0, 1},
            {1, 0, 1, 0, 1, 0, 1, 0, 1},
            {1, 0, 1, 1, 1, 1, 1, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1, 1, 1, 1, 1}
        };

        // Remplir les murs internes du labyrinthe
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int mx = dx + 4;
                int mz = dz + 4;
                if (maze[mz][mx] == 1) {
                    for (int y = FLOOR_Y + 1; y < CEIL_Y; y++) {
                        S(lv, O(sp, cx + dx, y, cz + dz), wall);
                    }
                }
            }
        }

        // ── Pièges et obstacles Simply Traps / Toiles d'araignée ──
        BlockState spikeTrap = null;
        var loc = net.minecraft.resources.ResourceLocation.tryParse("simply_traps:spike_trap");
        if (loc != null && net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(loc)) {
            spikeTrap = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(loc).defaultBlockState()
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);
        }
        BlockState cobweb = B(Blocks.COBWEB);

        // Placer des obstacles à des endroits clés du chemin
        if (spikeTrap != null) {
            S(lv, O(sp, cx - 3, FLOOR_Y + 1, cz + 3), spikeTrap);
            S(lv, O(sp, cx + 3, FLOOR_Y + 1, cz + 3), spikeTrap);
        } else {
            S(lv, O(sp, cx - 3, FLOOR_Y + 1, cz + 3), B(Blocks.SWEET_BERRY_BUSH));
            S(lv, O(sp, cx + 3, FLOOR_Y + 1, cz + 3), B(Blocks.SWEET_BERRY_BUSH));
        }

        S(lv, O(sp, cx - 3, FLOOR_Y + 1, cz - 3), cobweb);
        S(lv, O(sp, cx + 3, FLOOR_Y + 1, cz - 3), cobweb);

        // ── Récompenses au fond du labyrinthe (cx, FLOOR_Y + 1, cz + 3) ──
        DungeonArchitect.placeChest(lv, O(sp, cx - 1, FLOOR_Y + 1, cz + 3));
        DungeonArchitect.placeChest(lv, O(sp, cx + 1, FLOOR_Y + 1, cz + 3));
        S(lv, O(sp, cx, FLOOR_Y + 1, cz + 3), B(t.decorPrimary()));
        S(lv, O(sp, cx, CEIL_Y, cz), B(t.light())); // fanal central
    }

    /** Arène Qliphoth 15×15 : pas de labyrinthe, juste un spawner boss au centre. */
    private static void buildQliphothArena(ServerLevel lv, BlockPos sp, int cx, int cz,
                                           BlockPalette t, int floor, BlockState wall) {
        BlockState accent = B(t.accent());
        BlockState light = B(t.light());
        int r = 7; // demi-côté

        // ── Coque 15×15 ──
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                S(lv, O(sp, cx + dx, QLIPHOTH_Y, cz + dz), wall);
                S(lv, O(sp, cx + dx, QLIPHOTH_CEIL, cz + dz), wall);
                boolean edge = Math.abs(dx) == r || Math.abs(dz) == r;
                for (int y = QLIPHOTH_Y + 1; y < QLIPHOTH_CEIL; y++) {
                    S(lv, O(sp, cx + dx, y, cz + dz), edge ? wall : B(Blocks.AIR));
                }
            }
        }

        // ── Puits d'accès (trappe + échelle) ──
        int ladderX = cx - r + 1, ladderZ = cz - r + 1;
        for (int y = QLIPHOTH_Y + 1; y <= -1; y++) {
            S(lv, O(sp, ladderX, y, ladderZ), B(Blocks.AIR));
        }
        BlockState ladder = B(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH);
        for (int y = QLIPHOTH_Y + 1; y <= -2; y++) {
            S(lv, O(sp, ladderX, y, ladderZ + 1), wall);
            S(lv, O(sp, ladderX, y, ladderZ), ladder);
        }
        BlockState trap = B(Blocks.SPRUCE_TRAPDOOR)
                .setValue(TrapDoorBlock.HALF, Half.TOP)
                .setValue(TrapDoorBlock.OPEN, false)
                .setValue(TrapDoorBlock.FACING, Direction.NORTH);
        S(lv, O(sp, ladderX, -1, ladderZ), trap);

        // ── 4 piliers d'angle ──
        for (int px : new int[]{-r + 2, r - 2}) {
            for (int pz : new int[]{-r + 2, r - 2}) {
                for (int y = QLIPHOTH_Y + 1; y < QLIPHOTH_CEIL; y++) {
                    S(lv, O(sp, cx + px, y, cz + pz), accent);
                }
                // Lanterne au sommet de chaque pilier
                S(lv, O(sp, cx + px, QLIPHOTH_CEIL - 1, cz + pz), light);
            }
        }

        // ── Piédestal central + spawner boss Qliphoth ──
        int arc = arcIndex(floor);
        String spawnerId = qliphothSpawnerId(arc);
        S(lv, O(sp, cx, QLIPHOTH_Y + 1, cz), accent);
        S(lv, O(sp, cx, QLIPHOTH_Y + 2, cz), B(Blocks.OBSIDIAN));

        EntityType<?> spawnerType = ModdedMobPool.resolve(spawnerId);
        if (spawnerType != null) {
            Entity entity = spawnerType.create(lv);
            if (entity != null) {
                entity.setPos(cx + 0.5, QLIPHOTH_Y + 3, cz + 0.5);
                lv.addFreshEntity(entity);
            }
        }
    }
}
