package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;

/**
 * MenuProvider qui ouvre la table d'inscription Iron's Spellbooks sans bloc physique.
 *
 * <p>The {@link InscriptionTableMenu} constructor accepts a {@link ContainerLevelAccess};
 * the screen+menu filtering applied by our Iron mixins (see {@code IronInscriptionTableScreenMixin}
 * and {@code IronInscriptionTableMenuMixin}) is class-level, so it is automatically active
 * for any instance — virtual or block-backed.
 *
 * <p>The {@code ContainerLevelAccess} is anchored at the player's current position
 * rather than {@link ContainerLevelAccess#NULL} to avoid edge cases in
 * {@code InscriptionTableMenu#stillValid} / {@code removed} that may dereference
 * world coordinates (e.g. dropping items on close).
 */
public final class VirtualInscriptionMenuProvider implements MenuProvider {

    @Override
    public Component getDisplayName() {
        return Component.translatable("statmod.menu.virtual_inscription");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        ContainerLevelAccess access = ContainerLevelAccess.create(player.level(), playerPos(player));
        return new InscriptionTableMenu(containerId, inventory, access);
    }

    private static BlockPos playerPos(Player player) {
        return player.blockPosition();
    }
}
