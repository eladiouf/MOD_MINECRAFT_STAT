package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
 * <p>Repère : sol de pièce à y=-1. La chambre est sous l'underside, en {@code y ∈ [-6,-3]}.
 */
public final class DungeonSecretRoom {

    private static final int FLOOR_Y = -6;   // sol de la chambre
    private static final int CEIL_Y = -3;    // plafond de la chambre

    private DungeonSecretRoom() {}

    /** Place une salle secrète si {@code r} est la pièce élue de l'étage (≈ 1 par étage). */
    public static void maybePlace(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int floor) {
        // Une seule pièce de combat par étage (index déterministe dans la plage combat 1..10).
        if (r.index() != 2 + Math.floorMod(floor, 7)) return;

        // Chambre sous le QUADRANT nord-ouest (pas le centre, occupé par les décors dais/bassin).
        int q = Math.max(6, Math.min(r.maxX() - r.minX(), r.maxZ() - r.minZ()) / 4);
        int cx = r.centerX() - q, cz = r.centerZ() - q;
        BlockState wall = B(t.base());

        // ── Coque de la chambre (5×5), sous la pièce ──
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                S(lv, O(sp, cx + dx, FLOOR_Y, cz + dz), wall);        // sol
                S(lv, O(sp, cx + dx, CEIL_Y, cz + dz), wall);         // plafond
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                for (int y = FLOOR_Y + 1; y < CEIL_Y; y++) {
                    S(lv, O(sp, cx + dx, y, cz + dz), edge ? wall : B(Blocks.AIR));
                }
            }
        }

        // ── Puits d'accès au bord nord (x=cx, z=cz-2) : trappe + échelle ──
        // Colonne d'air du sol de la pièce (y=-1) jusqu'à la chambre (y=FLOOR_Y+1).
        for (int y = FLOOR_Y + 1; y <= -1; y++) {
            S(lv, O(sp, cx, y, cz - 2), B(Blocks.AIR));
        }
        // Échelle (dos au nord, bloc support plein en cz-3), de la chambre au ras du sol.
        // On garantit le support en posant un bloc plein derrière l'échelle (l'underside peut être
        // creux à cet endroit → sinon l'échelle tomberait).
        BlockState ladder = B(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH);
        for (int y = FLOOR_Y + 1; y <= -2; y++) {
            S(lv, O(sp, cx, y, cz - 3), wall);            // support plein
            S(lv, O(sp, cx, y, cz - 2), ladder);          // échelle
        }
        // Trappe en bois au niveau du sol de la pièce (le « tell » discret), fermée.
        BlockState trap = B(Blocks.SPRUCE_TRAPDOOR)
                .setValue(TrapDoorBlock.HALF, Half.TOP)
                .setValue(TrapDoorBlock.OPEN, false)
                .setValue(TrapDoorBlock.FACING, Direction.SOUTH);
        S(lv, O(sp, cx, -1, cz - 2), trap);

        // ── Récompenses : 2 coffres de trésor + statue-trophée + lumière ──
        DungeonArchitect.placeChest(lv, O(sp, cx - 1, FLOOR_Y + 1, cz + 1));
        DungeonArchitect.placeChest(lv, O(sp, cx + 1, FLOOR_Y + 1, cz + 1));
        S(lv, O(sp, cx, FLOOR_Y + 1, cz + 2), B(t.decorPrimary()));
        S(lv, O(sp, cx, CEIL_Y, cz), B(t.light())); // fanal au plafond
    }
}
