package tong.statmod.mixin;

import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tong.statmod.integration.ironspells.IronInscriptionKnownSpellIndex;
import tong.statmod.integration.ironspells.IronInscriptionSelectionService;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

@Mixin(InscriptionTableMenu.class)
public abstract class IronInscriptionTableMenuMixin {
    @Shadow private int selectedSpellIndex;

    @Shadow public abstract Slot getSpellBookSlot();

    @Shadow public abstract Slot getScrollSlot();

    @Unique
    private String statmod$selectedKnownSpellId;

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void statmod$selectKnownTreeSpell(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
        int optionIndex = IronInscriptionKnownSpellIndex.optionIndexFromButtonId(buttonId);
        if (optionIndex < 0) {
            return;
        }

        PlayerStatData data = player.getData(ModAttachments.STATS);
        this.statmod$selectedKnownSpellId = IronInscriptionSelectionService.resolveSelectedSpellId(data, optionIndex);
        cir.setReturnValue(this.statmod$selectedKnownSpellId != null);
    }

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void statmod$inscribeKnownTreeSpell(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
        if (buttonId != -1 || this.statmod$selectedKnownSpellId == null || this.getScrollSlot().hasItem()) {
            return;
        }

        boolean success = IronInscriptionSelectionService.inscribeSelectedSpell(
                (InscriptionTableMenu) (Object) this,
                this.selectedSpellIndex,
                this.statmod$selectedKnownSpellId,
                player);
        cir.setReturnValue(success);
    }
}
