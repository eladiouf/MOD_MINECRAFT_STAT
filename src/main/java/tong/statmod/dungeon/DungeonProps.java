package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import tong.statmod.integration.lootr.LootrBridge;

import java.util.List;

import static tong.statmod.dungeon.DungeonArchitect.B;
import static tong.statmod.dungeon.DungeonArchitect.O;
import static tong.statmod.dungeon.DungeonArchitect.S;

/**
 * Mission M6 — Accessoires d'ambiance des pièces (2026-07-05).
 *
 * <p>Enrichit l'architecture avec des <b>props</b> disséminés partout (pas seulement dans les salles
 * trésor) : coffres de butin épars, toiles d'araignée, statues moddées sur piédestal, braseros,
 * tas de crânes. Tous les blocs moddés sont résolus en douceur (mod absent → fallback vanilla ou
 * skip). Placés aux quadrants des pièces → jamais sur les portes (aux milieux de bords) ni au centre.
 */
public final class DungeonProps {

    /** Statues moddées candidates (première présente utilisée) → fallback crâne vanilla sur socle. */
    private static final List<String> STATUES = List.of(
            "cataclysm:goddess_statue", "irons_spellbooks:tyros_statue", "irons_lib:player_statue",
            "quark:myalite_crystal");
    /** Braseros / bougies moddés → fallback lanterne. */
    private static final List<String> BRAZIERS = List.of(
            "irons_spellbooks:brazier", "block_factorys_bosses:underworld_tall_candles",
            "block_factorys_bosses:tall_candles", "quark:paper_lantern", "quark:stone_lamp");
    /** Tas d'os/crânes moddés → fallback bloc d'os vanilla. */
    private static final List<String> SKULL_PILES = List.of(
            "born_in_chaos_v1:pile_of_skulls");

    /** Loot table vanilla légère pour les coffres épars (moins riche que le trésor de fin d'étage). */
    private static final ResourceKey<LootTable> MINOR_LOOT = ResourceKey.create(
            Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/simple_dungeon"));

    private DungeonProps() {}

    /** Dissémine des props dans une pièce (déterministe par étage + index). */
    public static void decorate(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int floor, int ceilH) {
        int q = quarter(r);
        int hx = r.centerX(), hz = r.centerZ();
        int hash = Math.floorMod(floor * 7 + r.index() * 13, 100);

        // Toiles d'araignée aux angles (toujours un peu) — bas et haut.
        cobwebs(lv, sp, r, ceilH);

        // Coffre de butin épars (~35 % des pièces), contre un mur au quadrant nord-ouest.
        if (hash < 35) minorChest(lv, sp, hx - q, hz - q);

        // Statue sur piédestal (~50 %), quadrant nord-est.
        if (hash % 2 == 0) statue(lv, sp, t, hx + q, hz - q);

        // Brasero/bougies au quadrant sud-ouest (soft-resolve → fallback lanterne).
        brazier(lv, sp, t, hx - q, hz + q);

        // Tas de crânes (~25 %), quadrant sud-est.
        if (hash < 25) skullPile(lv, sp, hx + q, hz + q);
    }

    /** Décalage de quadrant : ~1/4 de la plus petite dimension (reste bien à l'intérieur). */
    private static int quarter(DungeonLayout.Room r) {
        int w = r.maxX() - r.minX(), d = r.maxZ() - r.minZ();
        return Math.max(4, Math.min(w, d) / 4);
    }

    private static void cobwebs(ServerLevel lv, BlockPos sp, DungeonLayout.Room r, int ceilH) {
        int[][] corners = {
                {r.minX() + 1, r.minZ() + 1}, {r.maxX() - 1, r.minZ() + 1},
                {r.minX() + 1, r.maxZ() - 1}, {r.maxX() - 1, r.maxZ() - 1}};
        for (int[] c : corners) {
            S(lv, worldAir(sp, c[0], 1, c[1]), B(Blocks.COBWEB));
            S(lv, worldAir(sp, c[0], ceilH - 1, c[1]), B(Blocks.COBWEB));
        }
    }

    /** Coffre épars (loot simple_dungeon) posé au sol, contre le décor (30% de chance d'être piégé). */
    private static void minorChest(ServerLevel lv, BlockPos sp, int x, int z) {
        BlockPos chestPos = O(sp, x, 0, z);
        boolean trapped = lv.random.nextFloat() < 0.3f;
        if (trapped) {
            LootrBridge.placeIndividualTrappedChest(lv, chestPos, MINOR_LOOT);
            BlockPos wallPos = chestPos.north().above();
            net.minecraft.world.level.block.state.BlockState dispenserState = Blocks.DISPENSER.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.DispenserBlock.FACING, net.minecraft.core.Direction.SOUTH);
            S(lv, wallPos, dispenserState);

            net.minecraft.world.level.block.entity.BlockEntity be = lv.getBlockEntity(wallPos);
            if (be instanceof net.minecraft.world.level.block.entity.DispenserBlockEntity dbe) {
                dbe.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                        ResourceLocation.withDefaultNamespace("chests/dispenser_trap")), lv.getRandom().nextLong());
            }

            S(lv, chestPos.below(), Blocks.REDSTONE_WIRE.defaultBlockState());
            S(lv, wallPos.below(), Blocks.REDSTONE_WIRE.defaultBlockState());
        } else {
            LootrBridge.placeIndividualChest(lv, chestPos, MINOR_LOOT);
        }
    }

    /** Statue moddée sur socle (dalle) — fallback : crâne vanilla sur colonne. */
    private static void statue(ServerLevel lv, BlockPos sp, BlockPalette t, int x, int z) {
        S(lv, O(sp, x, 0, z), B(t.slab())); // socle
        BlockState statue = resolveFirst(STATUES);
        if (statue != null) {
            S(lv, O(sp, x, 1, z), statue);
        } else {
            S(lv, O(sp, x, 1, z), B(t.decorPrimary()));
            S(lv, O(sp, x, 2, z), B(Blocks.SKELETON_SKULL)); // fallback « gargouille »
        }
    }

    /** Brasero/bougies moddé sur socle — fallback : lanterne. */
    private static void brazier(ServerLevel lv, BlockPos sp, BlockPalette t, int x, int z) {
        S(lv, O(sp, x, 0, z), B(t.decorPrimary()));
        BlockState br = resolveFirst(BRAZIERS);
        S(lv, O(sp, x, 1, z), br != null ? br : B(Blocks.LANTERN));
    }

    /** Tas de crânes moddé — fallback : bloc d'os. */
    private static void skullPile(ServerLevel lv, BlockPos sp, int x, int z) {
        BlockState pile = resolveFirst(SKULL_PILES);
        S(lv, O(sp, x, 0, z), pile != null ? pile : B(Blocks.BONE_BLOCK));
    }

    /** Premier bloc présent parmi les ids candidats, ou {@code null} si aucun (mods absents). */
    private static BlockState resolveFirst(List<String> ids) {
        for (String id : ids) {
            ResourceLocation loc = ResourceLocation.tryParse(id);
            if (loc != null && BuiltInRegistries.BLOCK.containsKey(loc)) {
                return BuiltInRegistries.BLOCK.get(loc).defaultBlockState();
            }
        }
        return null;
    }

    private static BlockPos worldAir(BlockPos sp, int x, int y, int z) {
        return sp.offset(x, y, z);
    }
}
