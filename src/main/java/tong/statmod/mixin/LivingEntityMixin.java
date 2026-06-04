package tong.statmod.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.stats.StatCalculator;
import tong.statmod.stats.StatType;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void statmod_applyResistance(DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) return;

        CapabilityHelper.withStats(player, stats -> {
            float resistance = StatCalculator.getDamageReduction(
                stats.getLevel(StatType.PHYSICAL_RESISTANCE.index));
            float reduction = 1.0f - resistance;

            if (source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                || source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)
                || source.is(net.minecraft.world.damagesource.DamageTypes.WITHER)
                || source.is(net.minecraft.world.damagesource.DamageTypes.DRAGON_BREATH)) {
                float magicReduction = StatCalculator.getMagicReduction(
                    stats.getLevel(StatType.MAGIC_RESISTANCE.index));
                reduction *= (1.0f - magicReduction);
            }

            if (reduction < 1.0f) {
                float newAmount = amount * reduction;
                if (newAmount <= 0) {
                    ci.cancel();
                    return;
                }
            }
        });
    }
}
