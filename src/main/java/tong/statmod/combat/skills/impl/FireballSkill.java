package tong.statmod.combat.skills.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.Vec3;
import tong.statmod.STATMod;
import tong.statmod.capability.MobStats;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.stats.StatType;

public class FireballSkill implements MobSkill {

    public static final ResourceLocation ID = new ResourceLocation(STATMod.MODID, "fireball");

    @Override public ResourceLocation id() { return ID; }
    @Override public StatType requiredStat() { return StatType.FIRE_AFFINITY; }
    @Override public int requiredLevel() { return 40; }
    @Override public int baseCooldownTicks() { return 150; }
    @Override public int manaCost() { return 10; }
    @Override public double maxRange() { return 20; }
    @Override public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) { return mob.hasLineOfSight(target); }

    @Override
    public void execute(Mob mob, LivingEntity target, MobStats stats) {
        Vec3 dir = target.position().add(0, target.getBbHeight() / 2, 0)
            .subtract(mob.position().add(0, mob.getBbHeight() / 2, 0)).normalize();
        SmallFireball fb = new SmallFireball(mob.level(), mob, dir.x, dir.y, dir.z);
        fb.setPos(mob.getX(), mob.getY() + mob.getBbHeight() * 0.7, mob.getZ());
        mob.level().addFreshEntity(fb);
        mob.level().playSound(null, mob.blockPosition(),
            SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0f, 1.0f);
    }
}
