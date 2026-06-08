package tong.statmod.combat.skills.impl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import tong.statmod.STATMod;
import tong.statmod.capability.MobStats;
import tong.statmod.combat.skills.MobSkill;
import tong.statmod.stats.StatType;

public class ReflectSkill implements MobSkill {

    public static final ResourceLocation ID = new ResourceLocation(STATMod.MODID, "reflect");
    public static final float REFLECT_FRACTION = 0.30f;

    @Override public ResourceLocation id() { return ID; }
    @Override public StatType requiredStat() { return StatType.PHYSICAL_RESISTANCE; }
    @Override public int requiredLevel() { return 50; }
    @Override public int baseCooldownTicks() { return 40; }
    @Override public int manaCost() { return 0; }
    @Override public double maxRange() { return 0; }
    @Override public boolean isReactive() { return true; }

    @Override public boolean canExecute(Mob mob, LivingEntity target, MobStats stats) { return false; }
    @Override public void execute(Mob mob, LivingEntity target, MobStats stats) { /* triggered by ReflectSkillHandler */ }
}
