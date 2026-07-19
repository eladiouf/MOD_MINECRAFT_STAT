package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;

public final class VirtualInscriptionTableMenu extends InscriptionTableMenu {
    private final boolean openedWithCurio;

    public VirtualInscriptionTableMenu(
            int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(containerId, inventory, access);
        openedWithCurio = Utils.getPlayerSpellbookStack(inventory.player) != null;
    }

    @Override
    public boolean stillValid(Player player) {
        return !player.isRemoved();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        clearContainer(player, scrollContainer);
        if (openedWithCurio) {
            Utils.setPlayerSpellbookStack(player, getSpellBookSlot().remove(1));
        } else {
            clearContainer(player, spellbookContainer);
        }
    }
}
