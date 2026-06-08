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

public class BattleCrySkill implements MobSkill {

    public static final ResourceLocation ID = new ResourceLocation(STATMod.MODID, "battle_cry");

    @Override public ResourceLocation id() { return ID; }
    @Override public StatType requiredStat() { return StatType.INTIMIDATION; }
    @Override public int requiredLevel() { return 40; }
    @Override public int baseCooldownTicks() { return 400; }
    @Override public int manaCost() { return 8; }
    @Override public double maxRange() { return 8; }
    @Override public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) { return true; }

    @Override
    public void execute(Mob mob, LivingEntity target, MobStats stats) {
        AABB box = mob.getBoundingBox().inflate(8.0);
        for (Player p : mob.level().getEntitiesOfClass(Player.class, box)) {
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 0));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
        }
        mob.level().playSound(null, mob.blockPosition(),
            SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5f, 0.7f);
    }
}
