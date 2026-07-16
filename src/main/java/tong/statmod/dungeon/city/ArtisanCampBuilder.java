package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * Camp des artisans PROVISOIRE (plan A) : regroupe les services utilitaires de l'ancien temple
 * (enchantement niv. 30, forge, dortoir) pour qu'aucun service ne régresse. Le plan B les
 * redistribue dans les vrais quartiers (Elfique / Nain / Humain) et supprime ce camp.
 */
final class ArtisanCampBuilder {

    private ArtisanCampBuilder() {}

    static void build(ServerLevel lv) {
        BlockPos c = CityPlan.artisanCamp().above(); // Y=101

        // Coin enchantement : table + 15 bibliothèques (niveau 30 garanti).
        BlockPos ench = c.offset(0, 0, -12);
        lv.setBlock(ench, Blocks.ENCHANTING_TABLE.defaultBlockState(), 3);
        int placed = 0;
        for (int x = -2; x <= 2 && placed < 15; x++) {
            for (int z = -2; z <= 2 && placed < 15; z++) {
                if (Math.abs(x) != 2 && Math.abs(z) != 2) continue;
                lv.setBlock(ench.offset(x, 0, z), Blocks.BOOKSHELF.defaultBlockState(), 3);
                if (placed + 1 < 15) lv.setBlock(ench.offset(x, 1, z), Blocks.BOOKSHELF.defaultBlockState(), 3);
                placed += 2;
            }
        }

        // Forge : enclume, table de forge, meule, fourneau, table de craft, coffre de l'Ender.
        BlockPos forge = c.offset(0, 0, 12);
        lv.setBlock(forge, Blocks.ANVIL.defaultBlockState(), 3);
        lv.setBlock(forge.offset(-1, 0, 0), Blocks.SMITHING_TABLE.defaultBlockState(), 3);
        lv.setBlock(forge.offset(1, 0, 0), Blocks.GRINDSTONE.defaultBlockState(), 3);
        lv.setBlock(forge.offset(0, 0, 1), Blocks.BLAST_FURNACE.defaultBlockState(), 3);
        lv.setBlock(forge.offset(-1, 0, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        lv.setBlock(forge.offset(1, 0, 1), Blocks.ENDER_CHEST.defaultBlockState(), 3);

        // Dortoir : 2 lits sur tapis.
        BlockPos dorm = c.offset(12, 0, 0);
        placeBed(lv, dorm, Direction.NORTH, Blocks.RED_BED.defaultBlockState());
        placeBed(lv, dorm.offset(3, 0, 0), Direction.NORTH, Blocks.BLUE_BED.defaultBlockState());
        lv.setBlock(dorm.offset(1, 0, 2), Blocks.LANTERN.defaultBlockState(), 3);
    }

    private static void placeBed(ServerLevel lv, BlockPos foot, Direction facing, BlockState bed) {
        lv.setBlock(foot, bed.setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.FOOT), 3);
        lv.setBlock(foot.relative(facing),
                bed.setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.HEAD), 3);
    }
}
