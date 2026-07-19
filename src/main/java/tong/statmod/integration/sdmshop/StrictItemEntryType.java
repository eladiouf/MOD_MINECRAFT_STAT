package tong.statmod.integration.sdmshop;

import net.minecraft.world.item.ItemStack;
import net.sixik.sdmshop.shop.ShopEntry;
import net.sixik.sdmshop.shop.entry_types.ItemEntryType;

public final class StrictItemEntryType extends ItemEntryType {
    public StrictItemEntryType(ShopEntry entry, ItemStack stack) {
        super(entry, stack);
        this.strictNbt = true;
        this.ignoreDamage = false;
    }
}
