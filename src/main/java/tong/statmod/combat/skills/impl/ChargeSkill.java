package tong.statmod.combat.skills.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import tong.statmod.STATMod;
import tong.statmod.capability.MobStats;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.stats.StatType;

public class ChargeSkill implements MobSkill {

    public static final ResourceLocation ID = new ResourceLocation(STATMod.MODID, "charge");

    @Override public ResourceLocation id() { return ID; }
    @Override public StatType requiredStat() { return StatType.BRUTE_FORCE; }
    @Override public int requiredLevel() { return 30; }
    @Override public int baseCooldownTicks() { return 200; }
    @Override public int manaCost() { return 8; }
    @Override public double maxRange() { return 12; }

    @Override
    public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) {
        return mob.hasLineOfSight(target);
    }

    @Override
    public void execute(Mob mob, LivingEntity target, MobStats stats) {
        Vec3 toTarget = target.position().subtract(mob.position()).normalize();
        double speed = mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * 6.0;
        mob.setDeltaMovement(toTarget.x * speed, 0.3, toTarget.z * speed);
        mob.hurtMarked = true;

        mob.level().playSound(null, mob.blockPosition(),
            SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0f, 1.0f);

        double damageBonus = stats.getLevel(StatType.BRUTE_FORCE.index) * 0.05;
        if (mob.distanceTo(target) < 2.5) {
            target.hurt(mob.damageSources().mobAttack(mob), (float) damageBonus);
            target.knockback(2.0, -toTarget.x, -toTarget.z);
        }
    }
}
