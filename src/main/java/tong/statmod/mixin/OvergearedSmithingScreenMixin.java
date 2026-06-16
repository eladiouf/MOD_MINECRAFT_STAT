package tong.statmod.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.stirdrem.overgeared.screen.AbstractSmithingAnvilMenu;
import net.stirdrem.overgeared.screen.AbstractSmithingAnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.integration.overgeared.OvergearedForgingBonus;
import tong.statmod.integration.overgeared.OvergearedRecipeGate;
import tong.statmod.stats.StatType;

@Mixin(AbstractSmithingAnvilScreen.class)
public abstract class OvergearedSmithingScreenMixin extends AbstractContainerScreen<AbstractSmithingAnvilMenu> {
    private OvergearedSmithingScreenMixin(AbstractSmithingAnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "renderGhostResult", at = @At("HEAD"), cancellable = true)
    private void statmod$hideLockedGhostResult(GuiGraphics guiGraphics, int leftPos, int topPos,
                                               int mouseX, int mouseY, CallbackInfo ci) {
        Player player = this.minecraft == null ? null : this.minecraft.player;
        if (player == null) {
            return;
        }

        var ghostResult = this.menu.getGhostResult();
        if (!OvergearedRecipeGate.isForgingRecipeResult(ghostResult)) {
            return;
        }

        int forging = RaceEffectApplier.getEffectiveLevel(player, StatType.FORGING.index);
        if (!OvergearedForgingBonus.canCraftSpecialOutput(ghostResult, forging)) {
            ci.cancel();
        }
    }
}
