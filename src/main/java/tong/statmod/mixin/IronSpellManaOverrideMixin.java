package tong.statmod.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tong.statmod.STATMod;

@Mixin(AbstractSpell.class)
public class IronSpellManaOverrideMixin {

    @Inject(method = "canBeCastedBy", at = @At("RETURN"), cancellable = true)
    private void statmod$fixManaCheck(int spellLevel, CastSource castSource, MagicData magicData, Player player,
                                      CallbackInfoReturnable<CastResult> cir) {
        CastResult result = cir.getReturnValue();
        if (result == null || result.isSuccess()) return;

        AbstractSpell self = (AbstractSpell) (Object) this;

        if (magicData != null) {
            var cooldowns = magicData.getPlayerCooldowns();
            if (cooldowns != null && cooldowns.isOnCooldown(self)) {
                return;
            }
        }
        if (self.requiresLearning() && !self.isLearned(player)) {
            return;
        }

        cir.setReturnValue(new CastResult(CastResult.Type.SUCCESS));
    }
}
