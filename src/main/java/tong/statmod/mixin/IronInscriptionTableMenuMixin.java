package tong.statmod.mixin;

import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.integration.ironspells.IronLearnedSpellBindingService;
import tong.statmod.integration.ironspells.LearnedSpellBindingPolicy;
import tong.statmod.stats.PlayerStats;

@Mixin(value = InscriptionTableMenu.class, remap = false)
public abstract class IronInscriptionTableMenuMixin {
    @Shadow
    private int selectedSpellIndex;

    @Shadow
    public abstract Slot getScrollSlot();

    @Unique
    private String statmod$selectedLearnedSpellId;

    @Inject(method = {"clickMenuButton", "m_6366_"},
            at = @At("HEAD"), cancellable = true)
    private void statmod$handleLearnedSpellButton(
            Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
        int option = LearnedSpellBindingPolicy.optionFromButton(buttonId);
        if (option >= 0) {
            PlayerStats stats = StatCapabilities.get(player);
            if (stats == null) {
                statmod$selectedLearnedSpellId = null;
                cir.setReturnValue(false);
                return;
            }
            List<String> known = IronLearnedSpellBindingService.knownRegisteredSpellIds(
                    stats.learnedSpells().snapshot());
            statmod$selectedLearnedSpellId = option < known.size() ? known.get(option) : null;
            cir.setReturnValue(statmod$selectedLearnedSpellId != null);
            return;
        }
        if (buttonId < 0 && statmod$selectedLearnedSpellId != null
                && !getScrollSlot().hasItem()) {
            PlayerStats stats = StatCapabilities.get(player);
            boolean success = stats != null && IronLearnedSpellBindingService.bind(
                    (InscriptionTableMenu) (Object) this,
                    player,
                    statmod$selectedLearnedSpellId,
                    this.selectedSpellIndex,
                    stats.learnedSpells().snapshot());
            cir.setReturnValue(success);
        }
    }
}
