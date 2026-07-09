package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

import static tong.statmod.dungeon.DungeonArchitect.B;
import static tong.statmod.dungeon.DungeonArchitect.O;
import static tong.statmod.dungeon.DungeonArchitect.S;

/**
 * Mission M6 — Pièges du donjon via <b>Simply Traps</b> (2026-07-05).
 *
 * <p>Dissémine des blocs-pièges (pointes, pieux, barbelés) sur le sol des salles de combat pour
 * rendre le déplacement dangereux. Blocs résolus en douceur : si Simply Traps est absent, aucun
 * piège n'est posé (pas de crash). Placés à l'écart du centre et des portes.
 */
public final class DungeonTraps {

    /** Pièges de sol (dégâts au contact), du plus commun au plus rare. */
    private static final List<String> FLOOR_TRAPS = List.of(
            "simply_traps:spike_trap",
            "simply_traps:stake_wall",
            "simply_traps:stake",
            "simply_traps:circular_barbed_wire_iron");

    private DungeonTraps() {}

    /** Pose quelques pièges au sol dans une salle de combat (déterministe par étage + index). */
    public static void place(ServerLevel lv, BlockPos sp, DungeonLayout.Room r, int floor) {
        List<BlockState> traps = resolveAll(FLOOR_TRAPS);
        if (traps.isEmpty()) return; // Simply Traps absent → aucun piège

        int w = r.maxX() - r.minX(), d = r.maxZ() - r.minZ();
        if (w < 8 || d < 8) return;
        int count = 4 + Math.floorMod(floor + r.index(), 4); // 4..7 pièges
        int cx = r.centerX(), cz = r.centerZ();

        for (int i = 0; i < count; i++) {
            int hx = hash(floor, r.index(), i * 2);
            int hz = hash(floor, r.index(), i * 2 + 1);
            int x = r.minX() + 3 + hx % (w - 6);
            int z = r.minZ() + 3 + hz % (d - 6);
            // Évite le centre immédiat (2×2) et les axes de portes (milieux de bords).
            if (Math.abs(x - cx) <= 1 && Math.abs(z - cz) <= 1) continue;
            if (x == cx || z == cz) continue;
            BlockState trapState = traps.get(i % traps.size());
            if (trapState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE)) {
                trapState = trapState.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);
            }
            if (trapState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
                try {
                    trapState = trapState.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.UP);
                } catch (IllegalArgumentException e) {
                    // Do nothing if UP is not supported for FACING (e.g. horizontal-only blocks)
                }
            }
            S(lv, O(sp, x, 0, z), trapState);
        }

        placeRedstoneTraps(lv, sp, r, floor);
    }

    /** Places wall dispensers filled with arrows/potions activated by floor pressure plates. */
    private static void placeRedstoneTraps(ServerLevel lv, BlockPos sp, DungeonLayout.Room r, int floor) {
        int w = r.maxX() - r.minX(), d = r.maxZ() - r.minZ();
        if (w < 16 || d < 16) return; // Only place in large enough rooms

        // Traps placements on the 4 walls (offset from door centers)
        // 1. North Wall (facing South)
        int xN = r.minX() + 8;
        placeTrapDispenserAndPlate(lv, O(sp, xN, 1, r.minZ()), Direction.SOUTH, O(sp, xN, 0, r.minZ() + 1));

        // 2. South Wall (facing North)
        int xS = r.maxX() - 8;
        placeTrapDispenserAndPlate(lv, O(sp, xS, 1, r.maxZ()), Direction.NORTH, O(sp, xS, 0, r.maxZ() - 1));

        // 3. West Wall (facing East)
        int zW = r.minZ() + 8;
        placeTrapDispenserAndPlate(lv, O(sp, r.minX(), 1, zW), Direction.EAST, O(sp, r.minX() + 1, 0, zW));

        // 4. East Wall (facing West)
        int zE = r.maxZ() - 8;
        placeTrapDispenserAndPlate(lv, O(sp, r.maxX(), 1, zE), Direction.WEST, O(sp, r.maxX() - 1, 0, zE));
    }

    private static void placeTrapDispenserAndPlate(ServerLevel lv, BlockPos dispenserPos, Direction facing, BlockPos platePos) {
        // Place dispenser facing into the room
        BlockState dispenserState = Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, facing);
        S(lv, dispenserPos, dispenserState);

        // Fill dispenser with chests/dispenser_trap loot table (arrows, charges)
        BlockEntity be = lv.getBlockEntity(dispenserPos);
        if (be instanceof DispenserBlockEntity dbe) {
            dbe.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/dispenser_trap")), lv.getRandom().nextLong());
        }

        // Place stone pressure plate in front of the dispenser
        S(lv, platePos, Blocks.STONE_PRESSURE_PLATE.defaultBlockState());
    }

    private static int hash(int a, int b, int c) {
        int h = a * 73856093 ^ b * 19349663 ^ c * 83492791;
        return Math.floorMod(h ^ (h >>> 15), 4096);
    }

    /** Tous les blocs présents parmi les ids candidats (mods installés), dans l'ordre. */
    private static List<BlockState> resolveAll(List<String> ids) {
        List<BlockState> out = new ArrayList<>();
        for (String id : ids) {
            ResourceLocation loc = ResourceLocation.tryParse(id);
            if (loc != null && BuiltInRegistries.BLOCK.containsKey(loc)) {
                out.add(BuiltInRegistries.BLOCK.get(loc).defaultBlockState());
            }
        }
        return out;
    }
}
