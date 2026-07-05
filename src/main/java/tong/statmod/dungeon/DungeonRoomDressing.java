package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mission M6 — Aménagement des salles de récompense & de boss (2026-07-04).
 *
 * <p>Enrichit les salles trésor (×5) et boss (×10) avec des éléments demandés : point de soin
 * (fontaine + régénération de zone via {@link DungeonHealHandler}), waypoint (waystone via
 * {@link tong.statmod.integration.waystones.WaystonesBridge}), armor stands équipés, et piédestaux
 * présentoirs. Purement décoratif/serveur — aucune dépendance dure.
 *
 * <p>Repère : {@code sp} = spawn de l'étage (Y=100). {@code hz = -HZ+11} est le centre de l'estrade
 * du « cœur » ({@link DungeonArchitect#theHeart}), au nord.
 */
public final class DungeonRoomDressing {

    private DungeonRoomDressing() {}

    static BlockState B(net.minecraft.world.level.block.Block b) { return b.defaultBlockState(); }
    static void S(ServerLevel lv, BlockPos p, BlockState s) { lv.setBlock(p, s, 3); }
    static BlockPos O(BlockPos sp, int x, int y, int z) { return sp.offset(x, y, z); }

    /**
     * Aménage une salle trésor : fontaine de soin au centre, waystone (waypoint), 2 armor stands
     * équipés d'armures de tier, et 2 piédestaux présentoirs (blocs de valeur du tier).
     */
    public static void dressTreasureRoom(ServerLevel lv, BlockPos sp, BlockPalette t, int floor) {
        int hz = -HZ() + 11;
        FloorPalette tier = FloorPalette.forFloor(floor); // armures/présentoirs selon la difficulté

        // Fontaine de soin — bassin 3×3 d'eau bordé, avec une source lumineuse centrale.
        healFountain(lv, sp, O(sp, 0, 0, hz + 4), t);
        DungeonHealHandler.registerHealSpot(lv.dimension(), O(sp, 0, 1, hz + 4));

        // Waypoint : waystone assortie au thème, à l'est de l'estrade (activable pour retour rapide).
        tong.statmod.integration.waystones.WaystonesBridge.placeCheckpoint(
                lv, O(sp, 6, 2, hz), tier, floor);

        // 2 armor stands équipés, encadrant le trésor.
        armorStand(lv, O(sp, -4, 2, hz), tier, false);
        armorStand(lv, O(sp, 4, 2, hz), tier, true);

        // 2 piédestaux présentoirs (bloc de valeur du tier posé sur une colonne).
        pedestal(lv, O(sp, -2, 2, hz - 3), t, showcaseBlock(tier));
        pedestal(lv, O(sp, 2, 2, hz - 3), t, showcaseBlock(tier));
    }

    /**
     * Aménage une salle de boss : fontaine de soin (pour souffler après le combat) et 2 armor
     * stands « trophées ». Le loot du boss reste géré par {@link DungeonBossHandler} / l'autel.
     */
    public static void dressBossRoom(ServerLevel lv, BlockPos sp, BlockPalette t, int floor) {
        int hz = -HZ() + 11;
        FloorPalette tier = FloorPalette.forFloor(floor);

        // Fontaine de soin en retrait (sud de l'estrade), refuge après le combat.
        healFountain(lv, sp, O(sp, 0, 0, hz + 8), t);
        DungeonHealHandler.registerHealSpot(lv.dimension(), O(sp, 0, 1, hz + 8));

        // 2 armor stands « gardiens » aux angles avant de l'estrade.
        armorStand(lv, O(sp, -6, 2, hz + 5), tier, true);
        armorStand(lv, O(sp, 6, 2, hz + 5), tier, false);
    }

    // ═══════════════ éléments ═══════════════

    /** Bassin de soin 3×3 : bordure de blocs du thème, eau au centre, lanterne d'âme lumineuse. */
    private static void healFountain(ServerLevel lv, BlockPos sp, BlockPos c, BlockPalette t) {
        BlockState rim = B(t.decorPrimary());
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos p = c.offset(dx, 0, dz);
            if (dx == 0 && dz == 0) {
                S(lv, p, B(Blocks.WATER));                 // source centrale
                S(lv, p.below(), B(Blocks.SEA_LANTERN));   // lueur sous l'eau
            } else {
                S(lv, p, rim);
            }
        }
        // Colonne lumineuse au-dessus pour repérer le point de soin de loin.
        S(lv, c.above(2), B(Blocks.SOUL_LANTERN));
    }

    /** Piédestal : colonne + dalle + bloc présentoir au sommet. */
    private static void pedestal(ServerLevel lv, BlockPos base, BlockPalette t, BlockState showcase) {
        S(lv, base, B(t.decorPrimary()));
        S(lv, base.above(), B(t.slab()));
        S(lv, base.above(2), showcase);
    }

    /** Armor stand orienté, équipé d'un set d'armure selon le tier. */
    private static void armorStand(ServerLevel lv, BlockPos pos, FloorPalette t, boolean facingEast) {
        ArmorStand stand = new ArmorStand(lv, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        stand.setYRot(facingEast ? -90f : 90f);
        stand.setYHeadRot(facingEast ? -90f : 90f);
        stand.setInvulnerable(true);
        stand.setNoGravity(true);
        stand.setShowArms(true);
        equip(stand, t);
        DungeonSpawnGuard.spawnAuthorized(() -> { lv.addFreshEntity(stand); return stand; });
    }

    /** Équipe l'armor stand d'un set d'armure vanilla selon le tier. */
    private static void equip(ArmorStand stand, FloorPalette t) {
        switch (t) {
            case EARLY -> set(stand, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE,
                    Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS, Items.IRON_SWORD);
            case MID -> set(stand, Items.IRON_HELMET, Items.IRON_CHESTPLATE,
                    Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.DIAMOND_SWORD);
            case LATE -> set(stand, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
                    Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.DIAMOND_AXE);
            case ABYSS -> set(stand, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE,
                    Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS, Items.NETHERITE_SWORD);
        }
    }

    private static void set(ArmorStand s, net.minecraft.world.item.Item head,
                            net.minecraft.world.item.Item chest, net.minecraft.world.item.Item legs,
                            net.minecraft.world.item.Item feet, net.minecraft.world.item.Item hand) {
        s.setItemSlot(EquipmentSlot.HEAD, new ItemStack(head));
        s.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest));
        s.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
        s.setItemSlot(EquipmentSlot.FEET, new ItemStack(feet));
        s.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(hand));
    }

    /** Bloc présentoir de valeur selon le tier. */
    private static BlockState showcaseBlock(FloorPalette t) {
        return switch (t) {
            case EARLY -> B(Blocks.IRON_BLOCK);
            case MID -> B(Blocks.GOLD_BLOCK);
            case LATE -> B(Blocks.DIAMOND_BLOCK);
            case ABYSS -> B(Blocks.NETHERITE_BLOCK);
        };
    }

    private static int HZ() { return DungeonArchitect.HZ; }
}
