package tong.statmod.integration.overgeared;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.stirdrem.overgeared.item.ModItems;

public final class OvergearedMaterialGate {
    private OvergearedMaterialGate() {}

    public static boolean isOvergearedItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) != null
                && "overgeared".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }

    public static boolean isOvergearedBlock(Block block) {
        if (block == null) {
            return false;
        }
        var id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null && "overgeared".equals(id.getNamespace());
    }

    public static boolean isForgingMaterial(ItemStack stack) {
        if (!isOvergearedItem(stack)) {
            return false;
        }
        return stack.is(ModItems.STEEL_INGOT.get())
                || stack.is(ModItems.COPPER_PLATE.get())
                || stack.is(ModItems.IRON_PLATE.get())
                || stack.is(ModItems.STEEL_PLATE.get())
                || stack.is(ModItems.STEEL_NUGGET.get())
                || stack.is(ModItems.COPPER_NUGGET.get());
    }
}
