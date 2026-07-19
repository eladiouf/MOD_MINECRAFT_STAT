package tong.statmod.dungeon.ai.goal;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public final class BerserkerGoal extends Goal {
    private final Mob mob;

    public BerserkerGoal(Mob mob) { this.mob = mob; }

    @Override public boolean canUse() { return mob.isAlive(); }
    @Override public boolean canContinueToUse() { return mob.isAlive(); }

    @Override public void tick() {
        if (mob.getHealth() <= mob.getMaxHealth() * 0.5F) {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 0, false, false));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, false, false));
        }
    }
}
