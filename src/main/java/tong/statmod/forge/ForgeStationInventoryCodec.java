package tong.statmod.forge;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;

public final class ForgeStationInventoryCodec {

    private ForgeStationInventoryCodec() {}

    public static void save(SimpleContainer container, CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.saveAllItems(tag, container.getItems(), registries);
    }

    public static void load(SimpleContainer container, CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.loadAllItems(tag, container.getItems(), registries);
    }
}
