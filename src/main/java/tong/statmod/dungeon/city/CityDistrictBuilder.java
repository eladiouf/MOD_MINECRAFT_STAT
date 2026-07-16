package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import tong.statmod.dungeon.DungeonMerchant;
import tong.statmod.dungeon.FloorPalette;

/** Monuments and permanent districts of floor 0. */
final class CityDistrictBuilder {
    private static final int FLAG = 3;

    private CityDistrictBuilder() {}

    static void buildGuild(ServerLevel lv) {
        BlockPos c = CityPlan.guild().above();
        platform(lv, c, 24, 18, Blocks.POLISHED_ANDESITE);
        hall(lv, c, 20, 14, 11, Blocks.STONE_BRICKS, Blocks.DARK_OAK_PLANKS, Blocks.BLUE_STAINED_GLASS);
        tower(lv, c.offset(-17, 0, 0), 5, 18, Blocks.STONE_BRICKS, Blocks.BLUE_WOOL);
        tower(lv, c.offset(17, 0, 0), 5, 18, Blocks.STONE_BRICKS, Blocks.BLUE_WOOL);
        for (int x = -8; x <= 8; x += 4) set(lv, c.offset(x, 1, -5), Blocks.LECTERN);
        set(lv, c.offset(0, 1, 4), Blocks.ENDER_CHEST);
    }

    static void buildHumanQuarter(ServerLevel lv) {
        BlockPos c = CityPlan.humanQuarter().above();
        CityMaterialPalette.Style s = CityMaterialPalette.HUMAN;
        platform(lv, c, 36, 30, Blocks.COBBLESTONE);
        hall(lv, c.offset(-18, 0, -14), 9, 7, 9, s.wall(), s.roof(), s.window());
        hall(lv, c.offset(2, 0, -16), 8, 8, 7, s.wall(), s.roof(), s.window());
        hall(lv, c.offset(21, 0, -12), 8, 7, 10, s.wall(), s.roof(), s.window());
        hall(lv, c.offset(-17, 0, 13), 10, 8, 7, s.wall(), s.roof(), s.window());
        hall(lv, c.offset(9, 0, 14), 12, 8, 8, s.wall(), s.roof(), s.window());
        detailStreet(lv, c, s, 30);
    }

    static void buildElvenQuarter(ServerLevel lv) {
        BlockPos c = CityPlan.elvenQuarter().above();
        CityMaterialPalette.Style s = CityMaterialPalette.ELF;
        circularPlatform(lv, c, 38, Blocks.MOSS_BLOCK);
        tower(lv, c.offset(-17, 0, -9), 6, 16, s.pillar(), s.roof());
        tower(lv, c.offset(16, 0, 12), 7, 21, s.pillar(), s.roof());
        hall(lv, c.offset(2, 0, -19), 10, 7, 10, s.wall(), s.roof(), s.window());
        for (int[] p : new int[][]{{-27, -22}, {25, -18}, {-25, 22}, {6, 24}, {28, 4}}) tree(lv, c.offset(p[0], 0, p[1]));
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
            if (x * x + z * z <= 25) set(lv, c.offset(x, 0, z), x * x + z * z >= 16 ? s.wall() : Blocks.WATER);
        }
        detailStreet(lv, c, s, 28);
    }

    static void buildDwarvenQuarter(ServerLevel lv) {
        BlockPos c = CityPlan.dwarvenQuarter().above();
        CityMaterialPalette.Style s = CityMaterialPalette.DWARF;
        platform(lv, c, 38, 31, Blocks.DEEPSLATE_BRICKS);
        hall(lv, c.offset(0, 0, -12), 17, 12, 12, s.wall(), s.roof(), s.window());
        tower(lv, c.offset(-25, 0, 12), 7, 13, s.pillar(), s.roof());
        tower(lv, c.offset(25, 0, 12), 7, 13, s.pillar(), s.roof());
        for (int x = -6; x <= 6; x += 4) {
            set(lv, c.offset(x, 1, 0), Blocks.BLAST_FURNACE);
            set(lv, c.offset(x, 1, 2), Blocks.ANVIL);
            set(lv, c.offset(x, 1, 5), CityMaterialPalette.cuttingBoard());
        }
        for (int z = -8; z <= 8; z++) set(lv, c.offset(-20, 0, z), Blocks.MAGMA_BLOCK);
        detailStreet(lv, c, s, 29);
    }

    static void buildBeastQuarter(ServerLevel lv) {
        BlockPos c = CityPlan.beastQuarter().above();
        CityMaterialPalette.Style s = CityMaterialPalette.BEAST;
        circularPlatform(lv, c, 38, Blocks.COARSE_DIRT);
        int[][] lodges = {{-18, -15}, {15, -18}, {-20, 14}, {18, 15}};
        for (int i = 0; i < lodges.length; i++) {
            BlockPos lodge = c.offset(lodges[i][0], 0, lodges[i][1]);
            hall(lv, lodge, 8 + i % 2, 7, 6 + i % 3, s.wall(), s.roof(), s.window());
            for (int y = 1; y <= 5; y++) set(lv, lodge.offset(-9, y, 0), s.pillar());
        }
        for (int[] p : new int[][]{{-5, -23}, {7, 23}}) {
            BlockPos pen = c.offset(p[0], 0, p[1]);
            for (int x = -4; x <= 4; x++) {
                set(lv, pen.offset(x, 0, -4), Blocks.DARK_OAK_FENCE);
                set(lv, pen.offset(x, 0, 4), Blocks.DARK_OAK_FENCE);
            }
            for (int z = -3; z <= 3; z++) {
                set(lv, pen.offset(-4, 0, z), Blocks.DARK_OAK_FENCE);
                set(lv, pen.offset(4, 0, z), Blocks.DARK_OAK_FENCE);
            }
        }
        set(lv, c.offset(0, 1, 0), CityMaterialPalette.cookingPot());
        detailStreet(lv, c, s, 28);
    }

    static void buildMarket(ServerLevel lv) {
        BlockPos c = CityPlan.market().above();
        platform(lv, c, 27, 19, Blocks.SMOOTH_STONE);
        for (int x = -20; x <= 20; x += 10) {
            stall(lv, c.offset(x, 0, -9), Blocks.RED_WOOL);
            stall(lv, c.offset(x, 0, 9), Blocks.YELLOW_WOOL);
        }
        for (int z = -12; z <= 12; z += 6) {
            set(lv, c.offset(0, 0, z), Blocks.GLOWSTONE);
        }
        set(lv, c.offset(-7, 1, 0), CityMaterialPalette.bountyBoard());
        set(lv, c.offset(7, 1, 0), CityMaterialPalette.bountyBoard());
        BlockPos market = CityPlan.market().above();
        FloorPalette palette = FloorPalette.forFloor(10);
        DungeonMerchant.placeStall(lv, market, -18, -10, palette, 0, 10);
        DungeonMerchant.placeStall(lv, market, 18, -10, palette, 1, 20);
        DungeonMerchant.placeStall(lv, market, -18, 10, palette, 2, 30);
        DungeonMerchant.placeStall(lv, market, 18, 10, palette, 3, 40);
    }

    static void buildArena(ServerLevel lv) {
        BlockPos c = CityPlan.arena().above();
        for (int x = -40; x <= 40; x++) for (int z = -40; z <= 40; z++) {
            int d2 = x * x + z * z;
            if (d2 <= 40 * 40 && d2 >= 27 * 27) {
                int distance = (int) Math.sqrt(d2);
                int tier = Math.min(9, Math.max(1, (distance - 27) / 2 + 1));
                for (int y = 0; y < tier; y++) set(lv, c.offset(x, y, z), Blocks.STONE_BRICKS);
                set(lv, c.offset(x, tier, z), Blocks.SMOOTH_STONE_SLAB);
            } else if (d2 < 27 * 27) {
                set(lv, c.offset(x, 0, z), ((x + z) & 3) == 0 ? Blocks.SMOOTH_STONE : Blocks.POLISHED_ANDESITE);
            }
        }
        for (int y = 1; y <= 6; y++) for (int x = -3; x <= 3; x++) {
            for (int z = 27; z <= 40; z++) set(lv, c.offset(x, y, z), Blocks.AIR);
        }
        for (int x = -34; x <= 34; x += 17) {
            for (int y = 1; y <= 12; y++) set(lv, c.offset(x, y, -35), Blocks.POLISHED_BLACKSTONE_BRICKS);
            set(lv, c.offset(x, 13, -35), Blocks.SOUL_LANTERN);
        }
        for (int x = -29; x <= 29; x++) for (int z = -29; z <= 29; z++) {
            int d2 = x * x + z * z;
            if (d2 < 27 * 27 || d2 > 29 * 29) continue;
            boolean entrance = z >= 27 && Math.abs(x) <= 4;
            if (!entrance) for (int y = 1; y <= 8; y++) set(lv, c.offset(x, y, z), Blocks.BARRIER);
        }
        // Gatehouse between the combat floor and spectator stands.
        for (int z = 25; z <= 31; z++) for (int y = 1; y <= 10; y++) {
            set(lv, c.offset(-6, y, z), Blocks.POLISHED_BLACKSTONE_BRICKS);
            set(lv, c.offset(6, y, z), Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
        for (int z = 26; z <= 30; z++) for (int x = -6; x <= 6; x++) {
            set(lv, c.offset(x, 9, z), Blocks.CHISELED_POLISHED_BLACKSTONE);
            if (Math.abs(x) >= 5) set(lv, c.offset(x, 10, z), Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
        for (int y = 6; y <= 8; y++) {
            set(lv, c.offset(-3, y, 28), Blocks.CHAIN);
            set(lv, c.offset(3, y, 28), Blocks.CHAIN);
        }
        set(lv, c.offset(-6, 11, 28), Blocks.SOUL_LANTERN);
        set(lv, c.offset(6, 11, 28), Blocks.SOUL_LANTERN);
        CityArenaController.closeGate(lv);
    }

    static void buildTrainingGround(ServerLevel lv) {
        BlockPos c = CityPlan.trainingGround().above();
        platform(lv, c, 38, 27, Blocks.SMOOTH_STONE);

        // Archers: long firing lanes with distance markers and elevated targets.
        for (int x = -36; x <= -14; x++) for (int z = -24; z <= 24; z++) {
            set(lv, c.offset(x, 0, z), Blocks.GRAVEL);
        }
        for (int z = -20; z <= 20; z += 10) {
            set(lv, c.offset(-33, 1, z), Blocks.HAY_BLOCK);
            set(lv, c.offset(-33, 2, z), Blocks.TARGET);
            for (int x = -28; x <= -16; x += 4) set(lv, c.offset(x, 0, z), Blocks.WHITE_WOOL);
        }

        // Mages: blast-resistant ritual court with elemental focus crystals.
        for (int x = -10; x <= 10; x++) for (int z = -24; z <= 24; z++) {
            set(lv, c.offset(x, 0, z), ((x + z) & 3) == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.REINFORCED_DEEPSLATE);
        }
        for (int z = -18; z <= 18; z += 9) {
            set(lv, c.offset(0, 1, z), Blocks.AMETHYST_BLOCK);
            set(lv, c.offset(0, 2, z), Blocks.SEA_LANTERN);
        }
        for (int[] p : new int[][]{{-8, -22}, {8, -22}, {-8, 22}, {8, 22}}) {
            for (int y = 1; y <= 4; y++) set(lv, c.offset(p[0], y, p[1]), Blocks.OBSIDIAN);
            set(lv, c.offset(p[0], 5, p[1]), Blocks.AMETHYST_CLUSTER);
        }

        // Warriors: enclosed melee yard with weapon racks and Tensura dummies.
        for (int x = 14; x <= 36; x++) for (int z = -24; z <= 24; z++) {
            set(lv, c.offset(x, 0, z), Blocks.POLISHED_ANDESITE);
        }
        for (int z = -24; z <= 24; z++) {
            set(lv, c.offset(13, 1, z), Blocks.IRON_BARS);
            set(lv, c.offset(37, 1, z), Blocks.IRON_BARS);
        }
        for (int z = -18; z <= 18; z += 9) {
            set(lv, c.offset(34, 1, z), Blocks.ANVIL);
            set(lv, c.offset(16, 1, z), Blocks.SMITHING_TABLE);
        }
        CityTrainingDummies.spawn(lv);
        DungeonMerchant.placeStall(lv, CityPlan.trainingGround().above(), -25, 20,
                FloorPalette.forFloor(10), 3, 10);
    }

    static void buildSanctuary(ServerLevel lv) {
        BlockPos c = CityPlan.sanctuary().above();
        platform(lv, c, 20, 20, Blocks.MOSS_BLOCK);
        for (int y = 0; y <= 20; y++) {
            int r = Math.max(1, 5 - y / 5);
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                if (Math.abs(x) == r || Math.abs(z) == r) set(lv, c.offset(x, y, z), Blocks.PRISMARINE_BRICKS);
            }
        }
        set(lv, c.offset(0, 21, 0), Blocks.SEA_LANTERN);
        for (int[] p : points()) tree(lv, c.offset(p[0], 0, p[1]));
    }

    static void buildHangingGardens(ServerLevel lv) {
        BlockPos c = CityPlan.hangingGardens().above();
        for (int level = 0; level < 3; level++) {
            int y = level * 5;
            int radius = 20 - level * 5;
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) set(lv, c.offset(x, y, z), level == 0 ? Blocks.STONE_BRICKS : Blocks.MOSS_BLOCK);
            }
            for (int x = -radius; x <= radius; x += 8) set(lv, c.offset(x, y + 1, 0), Blocks.FLOWERING_AZALEA);
        }
        for (int y = 1; y <= 10; y++) set(lv, c.offset(0, y, 0), Blocks.WATER);
    }

    static void buildHallOfHeroes(ServerLevel lv) {
        BlockPos c = CityPlan.hallOfHeroes().above();
        platform(lv, c, 23, 14, Blocks.POLISHED_BLACKSTONE);
        hall(lv, c, 20, 11, 9, Blocks.CHISELED_DEEPSLATE, Blocks.DARK_OAK_PLANKS, Blocks.BLUE_STAINED_GLASS);
        for (int x = -15; x <= 15; x += 5) {
            for (int y = 1; y <= 4; y++) set(lv, c.offset(x, y, -8), Blocks.POLISHED_BLACKSTONE_BRICKS);
            set(lv, c.offset(x, 5, -8), Blocks.SOUL_LANTERN);
        }
    }

    private static void hall(ServerLevel lv, BlockPos c, int rx, int rz, int height,
                             Block wall, Block roof, Block window) {
        // Baies seedées par bâtiment : espacement variable + décalage → plus de damier (x+z)%5.
        long seed = ((long) c.getX() * 73856093L) ^ ((long) c.getZ() * 19349663L);
        int spacing = 4 + (int) Math.floorMod(seed, 3); // 4, 5 ou 6 selon le bâtiment
        for (int x = -rx; x <= rx; x++) for (int z = -rz; z <= rz; z++) {
            boolean edge = Math.abs(x) == rx || Math.abs(z) == rz;
            set(lv, c.offset(x, 0, z), Blocks.POLISHED_ANDESITE);
            if (edge) for (int y = 1; y <= height; y++) {
                boolean bay = Math.floorMod(x + z + (int) seed, spacing) == 0;
                boolean glazed = y >= 2 && y <= 4 && bay;
                set(lv, c.offset(x, y, z), glazed ? window : wall);
            }
        }
        CityRoofs.gableRoof(lv, c, rx, rz, height + 1, roofStair(roof), Blocks.EXPOSED_COPPER);
        CityRoofs.archway(lv, c.offset(0, 0, rz), 2, 4, Blocks.STONE_BRICK_STAIRS);
    }

    /** Escalier de toit assorti au thème du bâtiment (fallback pierre). */
    private static Block roofStair(Block roof) {
        if (roof == Blocks.DARK_OAK_PLANKS) return Blocks.DARK_OAK_STAIRS;
        if (roof == Blocks.SPRUCE_PLANKS) return Blocks.SPRUCE_STAIRS;
        if (roof == Blocks.BIRCH_PLANKS) return Blocks.BIRCH_STAIRS;
        return Blocks.STONE_BRICK_STAIRS;
    }

    private static void tower(ServerLevel lv, BlockPos c, int radius, int height, Block wall, Block cap) {
        for (int y = 0; y <= height; y++) for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (Math.abs(x) == radius || Math.abs(z) == radius) set(lv, c.offset(x, y, z), wall);
        }
        // Chapeau hippé (pyramide en escaliers) au lieu de la dalle plate.
        CityRoofs.hipRoof(lv, c.offset(0, height + 1, 0), radius + 1, 0,
                cap == Blocks.BLUE_WOOL ? Blocks.STONE_BRICK_STAIRS : Blocks.DEEPSLATE_BRICK_STAIRS);
    }

    private static void platform(ServerLevel lv, BlockPos c, int rx, int rz, Block block) {
        for (int x = -rx; x <= rx; x++) for (int z = -rz; z <= rz; z++) set(lv, c.offset(x, 0, z), block);
    }

    private static void circularPlatform(ServerLevel lv, BlockPos c, int radius, Block block) {
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (x * x + z * z <= radius * radius) set(lv, c.offset(x, 0, z), block);
        }
    }

    private static void detailStreet(ServerLevel lv, BlockPos c, CityMaterialPalette.Style style, int radius) {
        for (int d = -radius + 5; d <= radius - 5; d += 10) {
            set(lv, c.offset(d, 1, 0), CityMaterialPalette.stool());
            set(lv, c.offset(d, 1, 3), CityMaterialPalette.basket());
        }
        for (int d = -radius + 7; d <= radius - 7; d += 14) {
            for (int y = 1; y <= 4; y++) set(lv, c.offset(d, y, -5), style.pillar());
            set(lv, c.offset(d, 5, -5), style.light());
            set(lv, c.offset(d + 1, 3, -5), style.banner());
        }
        for (int x = -radius + 6; x <= radius - 6; x += 12) {
            set(lv, c.offset(x, 1, 7), Blocks.BARREL);
            set(lv, c.offset(x + 1, 1, 7), Blocks.CHEST);
            set(lv, c.offset(x, 2, 7), CityMaterialPalette.rope());
        }
    }

    private static void stall(ServerLevel lv, BlockPos c, Block canopy) {
        for (int x = -4; x <= 4; x++) for (int z = -3; z <= 3; z++) set(lv, c.offset(x, 0, z), Blocks.OAK_PLANKS);
        for (int[] p : new int[][]{{-4, -3}, {4, -3}, {-4, 3}, {4, 3}}) for (int y = 1; y <= 3; y++) set(lv, c.offset(p[0], y, p[1]), Blocks.OAK_FENCE);
        for (int x = -4; x <= 4; x++) for (int z = -3; z <= 3; z++) set(lv, c.offset(x, 4, z), canopy);
        set(lv, c.offset(0, 1, 0), Blocks.BARREL);
    }

    private static void tree(ServerLevel lv, BlockPos c) {
        for (int y = 1; y <= 6; y++) set(lv, c.offset(0, y, 0), Blocks.DARK_OAK_LOG);
        for (int x = -3; x <= 3; x++) for (int y = 5; y <= 8; y++) for (int z = -3; z <= 3; z++) {
            if (x * x + (y - 6) * (y - 6) + z * z <= 12) set(lv, c.offset(x, y, z), Blocks.AZALEA_LEAVES);
        }
    }

    private static int[][] points() { return new int[][]{{-12, -12}, {12, -12}, {-12, 12}, {12, 12}}; }

    private static void set(ServerLevel lv, BlockPos pos, Block block) {
        BlockState state = block.defaultBlockState();
        lv.setBlock(pos, state, FLAG);
    }
}
