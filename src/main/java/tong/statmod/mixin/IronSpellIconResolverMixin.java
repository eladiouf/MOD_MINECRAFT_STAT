package tong.statmod.mixin;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tong.statmod.integration.ironspells.bridge.TensuraDelegatingSpell;

/**
 * Redirige {@code AbstractSpell.getSpellIconResource()} pour les wrappers Tensura vers
 * l'icône native Tensura ({@code Magic.getSkillIcon()}), au lieu du PNG auto-dérivé par
 * Iron's depuis {@code getSpellResource()} (qui pointerait sur un placeholder inexistant).
 *
 * <p>Le getter d'Iron's est marqué {@code final} en Java — on l'injecte donc en {@code HEAD}
 * avec {@code cancellable = true} et on court-circuite la valeur de retour. Pas de toucher
 * aux sorts natifs Iron's : l'injection ne s'active que si l'instance est un
 * {@link TensuraDelegatingSpell}.
 */
@Mixin(AbstractSpell.class)
public class IronSpellIconResolverMixin {

    @Inject(method = "getSpellIconResource", at = @At("HEAD"), cancellable = true)
    private void statmod$redirectTensuraIcon(CallbackInfoReturnable<ResourceLocation> cir) {
        if ((Object) this instanceof TensuraDelegatingSpell wrapper) {
            ResourceLocation override = wrapper.getTensuraIconOverride();
            if (override != null) {
                cir.setReturnValue(override);
            }
        }
    }
}
