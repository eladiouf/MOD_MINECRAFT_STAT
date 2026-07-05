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

    /** Ancre forteresse : centre de l'estrade nord (hz = -HZ+11), y au niveau du dais (+2). */
    public static BlockPos fortressAnchor(BlockPos sp) { return O(sp, 0, 2, -HZ() + 11); }

    /**
     * Aménage une salle trésor autour d'un <b>point d'ancrage monde</b> {@code a} (centre de la
     * pièce, au niveau walkable) : fontaine de soin, waystone (waypoint), 2 armor stands équipés,
     * 2 piédestaux présentoirs. Réutilisable en forteresse (anchor = {@link #fortressAnchor}) comme
     * en chaîne de pièces (anchor = centre de la dernière pièce).
     */
    public static void dressTreasureRoom(ServerLevel lv, BlockPos a, BlockPalette t, int floor) {
        FloorPalette tier = FloorPalette.forFloor(floor); // armures/présentoirs selon la difficulté

        // Fontaine de soin — bassin 3×3 d'eau bordé, avec une source lumineuse centrale.
        healFountain(lv, a, O(a, 0, -2, 4), t);
        DungeonHealHandler.registerHealSpot(lv.dimension(), O(a, 0, -1, 4));

        // Waypoint : waystone assortie au thème, à l'est (activable pour retour rapide).
        tong.statmod.integration.waystones.WaystonesBridge.placeCheckpoint(
                lv, O(a, 6, 0, 0), tier, floor);

        // 2 armor stands équipés, encadrant le trésor.
        armorStand(lv, O(a, -4, 0, 0), floor, false);
        armorStand(lv, O(a, 4, 0, 0), floor, true);

        // 2 piédestaux présentoirs (bloc de valeur du tier posé sur une colonne).
        pedestal(lv, O(a, -2, 0, -3), t, showcaseBlock(tier));
        pedestal(lv, O(a, 2, 0, -3), t, showcaseBlock(tier));
    }

    /**
     * Aménage une salle de boss autour d'un <b>point d'ancrage monde</b> {@code a} : fontaine de
     * soin (refuge) et 2 armor stands « trophées ». Le loot reste géré par {@link DungeonBossHandler}.
     */
    public static void dressBossRoom(ServerLevel lv, BlockPos a, BlockPalette t, int floor) {
        // Fontaine de soin en retrait (sud), refuge après le combat.
        healFountain(lv, a, O(a, 0, -2, 8), t);
        DungeonHealHandler.registerHealSpot(lv.dimension(), O(a, 0, -1, 8));

        // 2 armor stands « gardiens » aux angles avant.
        armorStand(lv, O(a, -6, 0, 5), floor, true);
        armorStand(lv, O(a, 6, 0, 5), floor, false);
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

    /** Armor stand orienté, équipé d'un set d'armure selon le tier déduit de l'étage. */
    private static void armorStand(ServerLevel lv, BlockPos pos, int floor, boolean facingEast) {
        ArmorStand stand = new ArmorStand(lv, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        stand.setYRot(facingEast ? -90f : 90f);
        stand.setYHeadRot(facingEast ? -90f : 90f);
        stand.setInvulnerable(true);
        stand.setNoGravity(true);
        stand.setShowArms(true);
        equip(stand, floor);
        DungeonSpawnGuard.spawnAuthorized(() -> { lv.addFreshEntity(stand); return stand; });
    }

    /**
     * Équipe l'armor stand selon l'ÉTAGE. Les paliers dépassent la netherite via les sets end-game
     * de {@code l2complements} (eternium → poseidite → sculkium → shulkerate), à partir de l'étage
     * 40+. Fallback netherite si le mod est absent (résolution douce par ID).
     */
    private static void equip(ArmorStand stand, int floor) {
        if (floor <= 10) {
            setVanilla(stand, "chainmail", Items.IRON_SWORD);
        } else if (floor <= 25) {
            setVanilla(stand, "iron", Items.DIAMOND_SWORD);
        } else if (floor <= 40) {
            setVanilla(stand, "diamond", Items.DIAMOND_AXE);
        } else if (floor <= 55) {
            setVanilla(stand, "netherite", Items.NETHERITE_SWORD);
        } else if (floor <= 70) {
            setModdedOr(stand, "l2complements", "eternium", "netherite", Items.NETHERITE_SWORD);
        } else if (floor <= 85) {
            setModdedOr(stand, "l2complements", "poseidite", "netherite", Items.NETHERITE_SWORD);
        } else if (floor <= 100) {
            setModdedOr(stand, "l2complements", "sculkium", "netherite", Items.NETHERITE_SWORD);
        } else {
            setModdedOr(stand, "l2complements", "shulkerate", "netherite", Items.NETHERITE_SWORD);
        }
    }

    /** Set d'armure vanilla (préfixe "chainmail"/"iron"/"diamond"/"netherite"). */
    private static void setVanilla(ArmorStand s, String prefix, net.minecraft.world.item.Item hand) {
        s.setItemSlot(EquipmentSlot.HEAD, stack("minecraft:" + prefix + "_helmet"));
        s.setItemSlot(EquipmentSlot.CHEST, stack("minecraft:" + prefix + "_chestplate"));
        s.setItemSlot(EquipmentSlot.LEGS, stack("minecraft:" + prefix + "_leggings"));
        s.setItemSlot(EquipmentSlot.FEET, stack("minecraft:" + prefix + "_boots"));
        s.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(hand));
    }

    /** Set d'armure moddé ({@code ns:set_*}) avec fallback vanilla si le mod/set est absent. */
    private static void setModdedOr(ArmorStand s, String ns, String set, String vanillaFallback,
                                    net.minecraft.world.item.Item hand) {
        ItemStack chest = stack(ns + ":" + set + "_chestplate");
        if (chest.isEmpty()) { setVanilla(s, vanillaFallback, hand); return; }
        s.setItemSlot(EquipmentSlot.HEAD, stack(ns + ":" + set + "_helmet"));
        s.setItemSlot(EquipmentSlot.CHEST, chest);
        s.setItemSlot(EquipmentSlot.LEGS, stack(ns + ":" + set + "_leggings"));
        s.setItemSlot(EquipmentSlot.FEET, stack(ns + ":" + set + "_boots"));
        ItemStack sword = stack(ns + ":" + set + "_sword");
        s.setItemSlot(EquipmentSlot.MAINHAND, sword.isEmpty() ? new ItemStack(hand) : sword);
    }

    /** Résout un ItemStack par ID, ou vide si l'item est absent (mod non installé). */
    private static ItemStack stack(String id) {
        var loc = net.minecraft.resources.ResourceLocation.tryParse(id);
        if (loc == null || !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(loc)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(loc));
    }

    /** Bloc présentoir de valeur selon le tier (dépasse le netherite en profondeur). */
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
