package tong.statmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tong.statmod.integration.epicfight.EpicFightAnimationCompat;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(LivingEntityPatch.class)
public abstract class EpicFightNullAnimationMixin {
    @Inject(method = "playAnimationSynchronized", at = @At("HEAD"), cancellable = true)
    private void statmod$skipNullSynchronizedAnimations(AssetAccessor<? extends StaticAnimation> animation, float convertTime,
                                                        CallbackInfo ci) {
        if (EpicFightAnimationCompat.shouldSkipPlayback(animation)) {
            ci.cancel();
        }
    }
}
