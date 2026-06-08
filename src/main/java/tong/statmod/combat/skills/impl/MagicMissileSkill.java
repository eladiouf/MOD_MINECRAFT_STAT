package tong.statmod.combat.skills.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import tong.statmod.STATMod;
import tong.statmod.capability.MobStats;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.stats.StatType;

public class MagicMissileSkill implements MobSkill {

    public static final ResourceLocation ID = new ResourceLocation(STATMod.MODID, "magic_missile");

    @Override public ResourceLocation id() { return ID; }
    @Override public StatType requiredStat() { return StatType.CASTING_SPEED; }
    @Override public int requiredLevel() { return 30; }
    @Override public int baseCooldownTicks() { return 80; }
    @Override public int manaCost() { return 6; }
    @Override public double maxRange() { return 24; }
    @Override public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) { return mob.hasLineOfSight(target); }

    @Override
    public void execute(Mob mob, LivingEntity target, MobStats stats) {
        float damage = stats.getLevel(StatType.CASTING_SPEED.index) * 0.06f;
        target.hurt(mob.damageSources().magic(), damage);
        mob.level().playSound(null, mob.blockPosition(),
            SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 0.8f, 1.2f);
    }
}
