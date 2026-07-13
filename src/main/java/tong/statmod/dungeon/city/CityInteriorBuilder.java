package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Dense, function-specific interiors for every permanent floor-0 building. */
final class CityInteriorBuilder {
    private CityInteriorBuilder() {}

    static void build(ServerLevel level) {
        guild(level);
        humanHomes(level);
        elvenBuildings(level);
        dwarvenForge(level);
        beastLodges(level);
        market(level);
        publicBuildings(level);
    }

    private static void guild(ServerLevel lv) {
        BlockPos c = CityPlan.guild().above();
        meetingRoom(lv, c, "dark_oak", Blocks.BLUE_CARPET);
        shelves(lv, c.offset(-13, 0, -8), "dark_oak", 6, true);
        shelves(lv, c.offset(13, 0, -8), "dark_oak", 6, true);
        reception(lv, c.offset(0, 0, 8), "dark_oak");
        staircase(lv, c.offset(13, 0, 7), Blocks.STONE_BRICK_STAIRS, 7);
        office(lv, c.offset(0, 7, 0), "dark_oak", Blocks.BLUE_CARPET);
    }

    private static void humanHomes(ServerLevel lv) {
        BlockPos c = CityPlan.humanQuarter().above();
        int[][] homes = {{-18, -14}, {2, -16}, {21, -12}, {-17, 13}, {9, 14}};
        for (int i = 0; i < homes.length; i++) {
            BlockPos h = c.offset(homes[i][0], 0, homes[i][1]);
            home(lv, h, i % 2 == 0 ? "spruce" : "oak", Blocks.BLUE_CARPET);
        }
    }

    private static void elvenBuildings(ServerLevel lv) {
        BlockPos c = CityPlan.elvenQuarter().above();
        library(lv, c.offset(2, 0, -19), "birch", Blocks.GREEN_CARPET);
        alchemy(lv, c.offset(-17, 0, -9), "birch");
        bedroom(lv, c.offset(16, 0, 12), "birch", Blocks.LIME_BED);
    }

    private static void dwarvenForge(ServerLevel lv) {
        BlockPos c = CityPlan.dwarvenQuarter().above().offset(0, 0, -12);
        for (int x = -12; x <= 12; x += 4) {
            set(lv, c.offset(x, 1, -7), Blocks.BLAST_FURNACE);
            set(lv, c.offset(x, 1, -4), Blocks.ANVIL);
            set(lv, c.offset(x, 1, 5), CityFurniture.counter("dark_oak"));
            set(lv, c.offset(x, 1, 8), CityFurniture.drawer("dark_oak"));
        }
        for (int x = -10; x <= 10; x += 5) set(lv, c.offset(x, 8, 0), CityFurniture.chandelier());
        staircase(lv, c.offset(12, 0, 8), Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, 7);
        bedroom(lv, c.offset(0, 7, 0), "dark_oak", Blocks.ORANGE_BED);
    }

    private static void beastLodges(ServerLevel lv) {
        BlockPos c = CityPlan.beastQuarter().above();
        for (int[] lodge : new int[][]{{-18, -15}, {15, -18}, {-20, 14}, {18, 15}}) {
            BlockPos h = c.offset(lodge[0], 0, lodge[1]);
            set(lv, h.offset(0, 1, 0), CityMaterialPalette.cookingPot());
            for (int i = -3; i <= 3; i += 3) {
                set(lv, h.offset(i, 1, 3), CityFurniture.chair("dark_oak"));
                set(lv, h.offset(i, 1, -3), CityMaterialPalette.basket());
            }
            set(lv, h.offset(0, 5, 0), CityFurniture.chandelier());
        }
    }

    private static void market(ServerLevel lv) {
        BlockPos c = CityPlan.market().above();
        for (int x = -20; x <= 20; x += 10) {
            shopStock(lv, c.offset(x, 0, -9), "spruce");
            shopStock(lv, c.offset(x, 0, 9), "oak");
        }
    }

    private static void publicBuildings(ServerLevel lv) {
        library(lv, CityPlan.hallOfHeroes().above(), "dark_oak", Blocks.BLUE_CARPET);
        alchemy(lv, CityPlan.sanctuary().above(), "birch");
        reception(lv, CityPlan.artisanDistrict().above(), "spruce");
    }

    private static void home(ServerLevel lv, BlockPos c, String wood, Block carpet) {
        kitchen(lv, c.offset(-3, 0, -2), wood);
        bedroom(lv, c.offset(3, 0, 2), wood, Blocks.BLUE_BED);
        set(lv, c.offset(0, 1, 0), carpet);
        set(lv, c.offset(0, 5, 0), CityFurniture.ceilingLight("yellow"));
    }

    private static void meetingRoom(ServerLevel lv, BlockPos c, String wood, Block carpet) {
        for (int x = -5; x <= 5; x++) set(lv, c.offset(x, 1, 0), CityFurniture.table(wood));
        for (int x = -5; x <= 5; x += 2) {
            set(lv, c.offset(x, 1, -2), CityFurniture.chair(wood));
            set(lv, c.offset(x, 1, 2), CityFurniture.chair(wood));
        }
        for (int x = -7; x <= 7; x++) set(lv, c.offset(x, 0, 0), carpet);
        set(lv, c.offset(0, 8, 0), CityFurniture.chandelier());
    }

    private static void reception(ServerLevel lv, BlockPos c, String wood) {
        for (int x = -5; x <= 5; x++) set(lv, c.offset(x, 1, 0), CityFurniture.counter(wood));
        set(lv, c.offset(0, 1, -2), CityFurniture.desk(wood));
        set(lv, c.offset(0, 2, -4), CityFurniture.bookshelf(wood));
    }

    private static void office(ServerLevel lv, BlockPos c, String wood, Block carpet) {
        set(lv, c.offset(0, 0, 0), carpet);
        set(lv, c.offset(0, 1, -3), CityFurniture.desk(wood));
        set(lv, c.offset(0, 1, -1), CityFurniture.chair(wood));
        shelves(lv, c.offset(-6, 0, 2), wood, 5, false);
        shelves(lv, c.offset(6, 0, 2), wood, 5, false);
    }

    private static void library(ServerLevel lv, BlockPos c, String wood, Block carpet) {
        shelves(lv, c.offset(-6, 0, 0), wood, 7, false);
        shelves(lv, c.offset(6, 0, 0), wood, 7, false);
        for (int z = -4; z <= 4; z += 4) {
            set(lv, c.offset(0, 1, z), CityFurniture.table(wood));
            set(lv, c.offset(-2, 1, z), CityFurniture.chair(wood));
            set(lv, c.offset(2, 1, z), CityFurniture.chair(wood));
            set(lv, c.offset(0, 0, z), carpet);
        }
    }

    private static void alchemy(ServerLevel lv, BlockPos c, String wood) {
        for (int x = -4; x <= 4; x += 2) {
            set(lv, c.offset(x, 1, -3), Blocks.BREWING_STAND);
            set(lv, c.offset(x, 1, 3), CityFurniture.counter(wood));
        }
        set(lv, c.offset(0, 1, 0), Blocks.ENCHANTING_TABLE);
        set(lv, c.offset(0, 6, 0), CityFurniture.ceilingLight("green"));
    }

    private static void kitchen(ServerLevel lv, BlockPos c, String wood) {
        set(lv, c, CityFurniture.sink(wood));
        set(lv, c.offset(1, 0, 0), CityFurniture.counter(wood));
        set(lv, c.offset(2, 0, 0), CityMaterialPalette.cookingPot());
        set(lv, c.offset(0, 0, 2), CityFurniture.table(wood));
        set(lv, c.offset(-1, 0, 2), CityFurniture.chair(wood));
    }

    private static void bedroom(ServerLevel lv, BlockPos c, String wood, Block bed) {
        set(lv, c, bed);
        set(lv, c.offset(2, 0, 0), CityFurniture.wardrobe(wood));
        set(lv, c.offset(-2, 0, 0), CityFurniture.drawer(wood));
        set(lv, c.offset(0, 1, 2), CityFurniture.lamp("yellow"));
    }

    private static void shopStock(ServerLevel lv, BlockPos c, String wood) {
        set(lv, c.offset(-2, 1, 0), CityFurniture.counter(wood));
        set(lv, c.offset(2, 1, 0), CityFurniture.drawer(wood));
        set(lv, c.offset(0, 1, 2), CityMaterialPalette.basket());
        set(lv, c.offset(0, 3, 0), CityFurniture.lamp("yellow"));
    }

    private static void shelves(ServerLevel lv, BlockPos c, String wood, int length, boolean ancient) {
        for (int z = -length / 2; z <= length / 2; z++) for (int y = 1; y <= 3; y++) {
            set(lv, c.offset(0, y, z), ancient ? CityMaterialPalette.ancientBookshelf() : CityFurniture.bookshelf(wood));
        }
    }

    private static void staircase(ServerLevel lv, BlockPos c, Block stair, int height) {
        for (int y = 0; y < height; y++) set(lv, c.offset(0, y, -y), stair);
    }

    private static void set(ServerLevel lv, BlockPos pos, Block block) {
        lv.setBlock(pos, block.defaultBlockState(), 3);
    }
}
