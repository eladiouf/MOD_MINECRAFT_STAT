package tong.statmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import tong.statmod.integration.tensura.RacePhysicalEffects;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

@Mixin(value = AbstractClientPlayerPatch.class, remap = false)
public abstract class EpicFightPlayerScaleMixin {

    @ModifyConstant(method = "getModelMatrix", constant = @Constant(floatValue = 0.9375f), require = 0)
    private float statmod$applyRaceScale(float original) {
        AbstractClientPlayerPatch<?> self = (AbstractClientPlayerPatch<?>) (Object) this;
        var player = self.getOriginal();
        if (player == null) return original;
        float raceScale = RacePhysicalEffects.getScaleFactor(player);
        if (Math.abs(raceScale - 1.0f) < 0.001f) return original;
        return original * raceScale;
    }
}
