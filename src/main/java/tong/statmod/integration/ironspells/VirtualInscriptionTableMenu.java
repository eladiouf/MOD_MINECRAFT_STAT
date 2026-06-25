package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;

/**
 * Iron's {@link InscriptionTableMenu#stillValid} requires an actual inscription
 * table block at the {@link ContainerLevelAccess} position. Without a real
 * block, the menu closes on the very next tick. This subclass keeps every
 * behavior identical except for the block-presence check, which is bypassed
 * so the virtual menu stays open.
 */
public final class VirtualInscriptionTableMenu extends InscriptionTableMenu {

    public VirtualInscriptionTableMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(containerId, inventory, access);
    }

    @Override
    public boolean stillValid(Player player) {
        return !player.isRemoved();
    }
}
