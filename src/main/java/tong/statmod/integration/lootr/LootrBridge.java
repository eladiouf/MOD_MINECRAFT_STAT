package tong.statmod.integration.lootr;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

public final class LootrBridge {
    private LootrBridge() {}

    public static boolean loaded() {
        return true;
    }

    public static void placeIndividualChest(ServerLevel lv, BlockPos pos, ResourceLocation lootTable) {
        lv.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        if (lv.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity c) {
            c.setLootTable(lootTable, lv.random.nextLong());
            
            // Mark as Lootr individual chest via NBT
            var nbt = c.getPersistentData();
            nbt.putBoolean("lootr:individual", true);
            nbt.putString("lootr:table", lootTable.toString());
            c.setChanged();
        }
    }

    public static void placeIndividualTrappedChest(ServerLevel lv, BlockPos pos, ResourceLocation lootTable) {
        lv.setBlock(pos, Blocks.TRAPPED_CHEST.defaultBlockState(), 3);
        if (lv.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity c) {
            c.setLootTable(lootTable, lv.random.nextLong());
            
            // Mark as Lootr individual trapped chest via NBT
            var nbt = c.getPersistentData();
            nbt.putBoolean("lootr:individual", true);
            nbt.putString("lootr:table", lootTable.toString());
            c.setChanged();
        }
    }
}
