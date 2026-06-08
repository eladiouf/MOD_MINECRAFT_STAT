package tong.statmod.combat.skills.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import tong.statmod.STATMod;
import tong.statmod.capability.MobStats;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.stats.StatType;

public class WhirlwindSkill implements MobSkill {

    public static final ResourceLocation ID = new ResourceLocation(STATMod.MODID, "whirlwind");

    @Override public ResourceLocation id() { return ID; }
    @Override public StatType requiredStat() { return StatType.BLADE_TECHNIQUE; }
    @Override public int requiredLevel() { return 40; }
    @Override public int baseCooldownTicks() { return 240; }
    @Override public int manaCost() { return 12; }
    @Override public double maxRange() { return 4; }

    @Override public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) { return true; }

    @Override
    public void execute(Mob mob, LivingEntity target, MobStats stats) {
        double base = mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
        AABB box = mob.getBoundingBox().inflate(4.0);
        for (Player p : mob.level().getEntitiesOfClass(Player.class, box)) {
            p.hurt(mob.damageSources().mobAttack(mob), (float) (base * 1.5));
        }
        mob.level().playSound(null, mob.blockPosition(),
            SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.HOSTILE, 1.2f, 0.8f);
    }
}
