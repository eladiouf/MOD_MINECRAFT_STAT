package tong.statmod.world.effect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import tong.statmod.fatigue.FatigueProvider;

public class AdrenalineEffect extends MobEffect {
    public AdrenalineEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player) {
            player.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue -> {
                float rate = 1.0f + amplifier;
                fatigue.reduceFatigue(rate);
            });
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
