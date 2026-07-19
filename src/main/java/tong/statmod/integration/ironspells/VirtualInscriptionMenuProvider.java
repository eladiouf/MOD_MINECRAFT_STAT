package tong.statmod.integration.ironspells;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;

public final class VirtualInscriptionMenuProvider implements MenuProvider {
    @Override
    public Component getDisplayName() {
        return Component.translatable("statmod.menu.virtual_inscription");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId, Inventory inventory, Player player) {
        return new VirtualInscriptionTableMenu(
                containerId, inventory, ContainerLevelAccess.NULL);
    }
}
