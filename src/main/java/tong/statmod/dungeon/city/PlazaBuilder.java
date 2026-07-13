package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import tong.statmod.dungeon.DungeonBlocks;
import tong.statmod.dungeon.FloorPalette;
import tong.statmod.economy.MagicBanker;
import tong.statmod.integration.waystones.WaystonesBridge;

/**
 * Grande Place : fontaine magique, statue du Premier Aventurier, Cristal Gardien,
 * waystone, balise de retour, banquier magique et les 4 étals de marchands
 * (provisoirement sur la place — le plan B les redistribue dans les quartiers).
 */
final class PlazaBuilder {

    private PlazaBuilder() {}

    static void build(ServerLevel lv) {
        BlockPos c = CityPlan.center().above(); // Y=101 : niveau de marche

        buildFountain(lv, c.offset(0, 0, -18));
        buildFirstAdventurerStatue(lv, c.offset(-18, 0, 0));
        buildGuardianCrystal(lv, c);
        buildLeaderboardWall(lv, c.offset(18, 0, 0));

        // Services fonctionnels (aucune régression vs l'ancien temple).
        WaystonesBridge.placeCheckpoint(lv, c.offset(0, 0, 12), FloorPalette.forFloor(10), 0);
        lv.setBlock(c.offset(-3, 0, 12), DungeonBlocks.RETURN_BEACON.get().defaultBlockState(), 3);
        MagicBanker.spawn(lv, c.offset(6, 0, 12));

    }

    /** Bassin circulaire r=5 à margelle, jet central lumineux. L'eau est contenue par la margelle. */
    private static void buildFountain(ServerLevel lv, BlockPos c) {
        BlockState rim = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                int d2 = x * x + z * z;
                if (d2 > 25) continue;
                if (d2 >= 16) {
                    lv.setBlock(c.offset(x, 0, z), rim, 3);
                } else {
                    lv.setBlock(c.offset(x, -1, z), Blocks.PRISMARINE_BRICKS.defaultBlockState(), 3);
                    lv.setBlock(c.offset(x, 0, z), water, 3);
                }
            }
        }
        for (int y = 0; y <= 3; y++) {
            lv.setBlock(c.offset(0, y, 0), Blocks.PRISMARINE_WALL.defaultBlockState(), 3);
        }
        lv.setBlock(c.offset(0, 4, 0), Blocks.SEA_LANTERN.defaultBlockState(), 3);
    }

    /** Statue : socle 5×5, armor stand paré netherite, plaque. */
    private static void buildFirstAdventurerStatue(ServerLevel lv, BlockPos c) {
        for (int x = -2; x <= 2; x++)
            for (int z = -2; z <= 2; z++)
                lv.setBlock(c.offset(x, 0, z), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 3);
        lv.setBlock(c.offset(0, 1, 0), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), 3);
        ArmorStand stand = new ArmorStand(lv, c.getX() + 0.5, c.getY() + 2, c.getZ() + 0.5);
        stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
        stand.setShowArms(true);
        stand.setInvulnerable(true);
        lv.addFreshEntity(stand);
    }

    /** Cristal Gardien : pilier d'améthyste flottant au-dessus du centre de la place. */
    private static void buildGuardianCrystal(ServerLevel lv, BlockPos c) {
        for (int y = 8; y <= 14; y++) {
            lv.setBlock(c.offset(0, y, 0), Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
        }
        lv.setBlock(c.offset(0, 11, 0), Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
        lv.setBlock(c.offset(1, 11, 0), Blocks.SEA_LANTERN.defaultBlockState(), 3);
        lv.setBlock(c.offset(-1, 11, 0), Blocks.SEA_LANTERN.defaultBlockState(), 3);
        lv.setBlock(c.offset(0, 15, 0), Blocks.AMETHYST_CLUSTER.defaultBlockState(), 3);
        lv.setBlock(c.offset(0, 7, 0), Blocks.AMETHYST_CLUSTER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.AmethystClusterBlock.FACING,
                        net.minecraft.core.Direction.DOWN), 3);
    }

    /** Mur des classements : décor en plan A (panneaux réels branchés au plan C). */
    private static void buildLeaderboardWall(ServerLevel lv, BlockPos c) {
        for (int z = -3; z <= 3; z++) {
            for (int y = 0; y <= 3; y++) {
                lv.setBlock(c.offset(0, y, z), Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 3);
            }
        }
        lv.setBlock(c.offset(0, 4, 0), Blocks.LANTERN.defaultBlockState(), 3);
    }
}
