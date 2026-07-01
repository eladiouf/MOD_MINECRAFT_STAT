package tong.statmod.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tong.statmod.forge.ForgeStationInventoryCodec;
import tong.statmod.menu.EnchantmentAnvilMenu;

public class EnchantmentAnvilBlockEntity extends BlockEntity implements MenuProvider {

    private final SimpleContainer container = new SimpleContainer(EnchantmentAnvilMenu.INPUT_SLOT_COUNT);

    public EnchantmentAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCHANTMENT_ANVIL.get(), pos, state);
    }

    public SimpleContainer getContainer() {
        return container;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.statmod.enchantment_anvil");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new EnchantmentAnvilMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ForgeStationInventoryCodec.save(container, tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ForgeStationInventoryCodec.load(container, tag, registries);
    }
}
