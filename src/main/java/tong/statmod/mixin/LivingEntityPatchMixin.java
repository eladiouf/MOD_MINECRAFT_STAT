package tong.statmod.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(LivingEntityPatch.class)
public class LivingEntityPatchMixin {

    @Shadow(remap = false)
    private LivingEntity original;

    @Inject(method = "isAirborneState", at = @At("HEAD"), cancellable = true, remap = false)
    private void onIsAirborneState(CallbackInfoReturnable<Boolean> cir) {
        try {
            SynchedEntityData data = original.getEntityData();
            if (data == null) {
                cir.setReturnValue(false);
            }
        } catch (Exception e) {
            cir.setReturnValue(false);
        }
    }
}
