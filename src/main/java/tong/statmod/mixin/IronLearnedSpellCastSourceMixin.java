package tong.statmod.mixin;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tong.statmod.integration.ironspells.LearnedSpellCastPolicy;

/**
 * Mappe le slot de sélection virtuel des sorts appris ({@link LearnedSpellCastPolicy#SLOT})
 * vers la source de cast {@code SPELLBOOK} d'Iron's Spellbooks (2026-07-13). Les sorts appris
 * via STAT Mod se lancent alors comme des sorts de grimoire — sans staff en main, avec
 * n'importe quel objet — tout en réutilisant la mana, le cooldown et les animations natifs.
 * Les slots natifs (mainhand, offhand…) conservent l'implémentation d'origine.
 */
@Mixin(SpellSelectionManager.SelectionOption.class)
public class IronLearnedSpellCastSourceMixin {
    @Shadow
    public String slot;

    @Inject(method = "getCastSource", at = @At("HEAD"), cancellable = true)
    private void statmod$useSpellbookSourceForLearnedSpell(
            CallbackInfoReturnable<CastSource> cir) {
        if (LearnedSpellCastPolicy.isVirtualSlot(this.slot)) {
            cir.setReturnValue(CastSource.SPELLBOOK);
        }
    }
}
