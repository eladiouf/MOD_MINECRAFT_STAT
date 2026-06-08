package tong.statmod.combat.skills.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import tong.statmod.STATMod;
import tong.statmod.capability.MobStats;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.stats.StatType;

public class CurseSkill implements MobSkill {

    public static final ResourceLocation ID = new ResourceLocation(STATMod.MODID, "curse");

    @Override public ResourceLocation id() { return ID; }
    @Override public StatType requiredStat() { return StatType.ARCANE_POWER; }
    @Override public int requiredLevel() { return 45; }
    @Override public int baseCooldownTicks() { return 300; }
    @Override public int manaCost() { return 15; }
    @Override public double maxRange() { return 16; }
    @Override public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) { return true; }

    @Override
    public void execute(Mob mob, LivingEntity target, MobStats stats) {
        AABB box = new AABB(target.blockPosition()).inflate(3.0);
        for (Player p : mob.level().getEntitiesOfClass(Player.class, box)) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1));
        }
        mob.level().playSound(null, mob.blockPosition(),
            SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 1.0f, 0.9f);
    }
}
