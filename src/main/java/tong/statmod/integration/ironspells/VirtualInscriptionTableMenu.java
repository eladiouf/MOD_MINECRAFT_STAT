package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;

public final class VirtualInscriptionTableMenu extends InscriptionTableMenu {
    public VirtualInscriptionTableMenu(
            int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(containerId, inventory, access);
    }

    @Override
    public boolean stillValid(Player player) {
        return !player.isRemoved();
    }
}
