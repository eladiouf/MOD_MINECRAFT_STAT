package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tong.statmod.dungeon.DungeonArchitect.O;
import static tong.statmod.dungeon.DungeonArchitect.S;

/**
 * Pièges du donjon — refonte 2026-07-16 : <b>100 % Java, zéro command block</b>.
 *
 * <p>Deux familles :
 * <ul>
 *   <li><b>Pièges de sol Simply Traps</b> (pointes, pieux, barbelés) — dégâts au contact,
 *       résolus en douceur (mod absent → rien).</li>
 *   <li><b>Pièges à effet codés</b> — une tuile repérable (plaque de pression = tell visuel,
 *       esquivable) enregistrée dans un registre par étage ; {@link DungeonTrapHandler} déclenche
 *       l'effet en code (crocs, fléchettes, gaz, filet, flammes, embuscade, givre, confusion, et
 *       en profondeur wither/malédiction/vexes). Fonctionne sur serveur dédié <b>sans</b>
 *       {@code enable-command-block}, touche le groupe (co-op), cooldown par tuile.</li>
 * </ul>
 * Ancienne implémentation (plaque + command block caché exécutant des commandes vanilla)
 * supprimée : fragile, exigeait enable-command-block, non intégrée au mod.
 */
public final class DungeonTraps {

    /** Types de piège à effet, déclenchés par {@link DungeonTrapHandler}. */
    public enum TrapKind {
        FANG_BURST, DART_VOLLEY, POISON_GAS, SNARE, FLAME, AMBUSH, FROST, CONFUSION,
        // Profondeurs (étage >= 40) :
        WITHER_GAS, HEX, VEX_AMBUSH
    }

    /** Mode de déclenchement. */
    public enum Trigger { STEP, CHEST }

    /** Un piège armé : position de déclenchement + effet + mode. */
    public record Trap(BlockPos pos, TrapKind kind, Trigger trigger) {}

    private static final TrapKind[] BASE_KINDS = {
            TrapKind.FANG_BURST, TrapKind.DART_VOLLEY, TrapKind.POISON_GAS, TrapKind.SNARE,
            TrapKind.FLAME, TrapKind.AMBUSH, TrapKind.FROST, TrapKind.CONFUSION
    };
    private static final TrapKind[] DEEP_KINDS = {
            TrapKind.WITHER_GAS, TrapKind.HEX, TrapKind.VEX_AMBUSH
    };

    /** Registre en mémoire des pièges armés par étage (reconstruit à chaque génération d'étage). */
    private static final Map<Integer, List<Trap>> REGISTRY = new HashMap<>();

    /** Pièges de sol (dégâts au contact), du plus commun au plus rare. */
    private static final List<String> FLOOR_TRAPS = List.of(
            "simply_traps:spike_trap",
            "simply_traps:stake_wall",
            "simply_traps:stake",
            "simply_traps:circular_barbed_wire_iron");

    private DungeonTraps() {}

    // ── Registre ────────────────────────────────────────────────────────────────────────────
    /** Vide les pièges d'un étage (appelé au début de sa (re)génération). */
    public static void clearFloor(int floor) {
        REGISTRY.remove(floor);
    }

    /** Liste immuable des pièges d'un étage (jamais null). */
    public static List<Trap> traps(int floor) {
        return REGISTRY.getOrDefault(floor, List.of());
    }

    /** Arme un piège de sol : pose la tuile-tell (plaque) et l'enregistre. */
    public static void armStep(ServerLevel lv, int floor, BlockPos pos, TrapKind kind) {
        S(lv, pos, Blocks.STONE_PRESSURE_PLATE.defaultBlockState());
        REGISTRY.computeIfAbsent(floor, k -> new ArrayList<>())
                .add(new Trap(pos.immutable(), kind, Trigger.STEP));
    }

    /** Arme un coffre piégé : déclenché à l'ouverture (right-click) de ce bloc. Étage déduit du pos. */
    public static void armChest(BlockPos chestPos, TrapKind kind) {
        int floor = DungeonTeleportHandler.floorAtPos(chestPos.getX(), chestPos.getZ());
        if (floor <= 0) return;
        REGISTRY.computeIfAbsent(floor, k -> new ArrayList<>())
                .add(new Trap(chestPos.immutable(), kind, Trigger.CHEST));
    }

    // ── Placement ───────────────────────────────────────────────────────────────────────────
    /** Pose quelques pièges dans une salle de combat (déterministe par étage + index). */
    public static void place(ServerLevel lv, BlockPos sp, DungeonLayout.Room r, int floor) {
        List<BlockState> traps = resolveAll(FLOOR_TRAPS);
        int w = r.maxX() - r.minX(), d = r.maxZ() - r.minZ();
        if (w >= 8 && d >= 8 && !traps.isEmpty()) {
            int count = 4 + Math.floorMod(floor + r.index(), 4); // 4..7 pièges de sol
            int cx = r.centerX(), cz = r.centerZ();
            for (int i = 0; i < count; i++) {
                int hx = hash(floor, r.index(), i * 2);
                int hz = hash(floor, r.index(), i * 2 + 1);
                int x = r.minX() + 3 + hx % (w - 6);
                int z = r.minZ() + 3 + hz % (d - 6);
                if (Math.abs(x - cx) <= 1 && Math.abs(z - cz) <= 1) continue;
                if (x == cx || z == cz) continue;
                BlockState trapState = traps.get(i % traps.size());
                if (trapState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE)) {
                    trapState = trapState.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);
                }
                if (trapState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
                    try {
                        trapState = trapState.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.UP);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                S(lv, O(sp, x, 0, z), trapState);
            }
        }

        placeEffectTraps(lv, sp, r, floor);
    }

    /** Pièges à effet codés : tuile repérable + enregistrement (déclenchement par le handler). */
    private static void placeEffectTraps(ServerLevel lv, BlockPos sp, DungeonLayout.Room r, int floor) {
        int w = r.maxX() - r.minX(), d = r.maxZ() - r.minZ();
        if (w < 16 || d < 16) return;

        int count = 3 + Math.floorMod(floor + r.index() * 7, 3); // 3..5 pièges
        int cx = r.centerX(), cz = r.centerZ();
        boolean deep = floor >= 40;
        for (int i = 0; i < count; i++) {
            int hx = hash(floor, r.index(), 100 + i * 2);
            int hz = hash(floor, r.index(), 101 + i * 2);
            int x = r.minX() + 4 + hx % (w - 8);
            int z = r.minZ() + 4 + hz % (d - 8);
            if (Math.abs(x - cx) <= 2 && Math.abs(z - cz) <= 2) continue;
            if (x == cx || z == cz) continue;

            TrapKind kind = pickKind(floor, r.index(), i, deep);
            armStep(lv, floor, O(sp, x, 0, z), kind);
        }
    }

    private static TrapKind pickKind(int floor, int roomIndex, int i, boolean deep) {
        int pool = BASE_KINDS.length + (deep ? DEEP_KINDS.length : 0);
        int pick = hash(floor, roomIndex, 200 + i) % pool;
        return pick < BASE_KINDS.length ? BASE_KINDS[pick] : DEEP_KINDS[pick - BASE_KINDS.length];
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
