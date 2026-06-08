package tong.statmod.combat.skills.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import tong.statmod.STATMod;
import tong.statmod.capability.MobStats;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.stats.StatType;

public class LungeSkill implements MobSkill {

    public static final ResourceLocation ID = new ResourceLocation(STATMod.MODID, "lunge");

    @Override public ResourceLocation id() { return ID; }
    @Override public StatType requiredStat() { return StatType.RAPIDITE; }
    @Override public int requiredLevel() { return 35; }
    @Override public int baseCooldownTicks() { return 100; }
    @Override public int manaCost() { return 5; }
    @Override public double maxRange() { return 10; }

    @Override public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) { return mob.onGround(); }

    @Override
    public void execute(Mob mob, LivingEntity target, MobStats stats) {
        Vec3 toTarget = target.position().subtract(mob.position()).normalize();
        mob.setDeltaMovement(toTarget.x * 1.2, 0.6, toTarget.z * 1.2);
        mob.hurtMarked = true;
        mob.level().playSound(null, mob.blockPosition(),
            SoundEvents.WOLF_GROWL, SoundSource.HOSTILE, 0.8f, 1.4f);
    }
}
