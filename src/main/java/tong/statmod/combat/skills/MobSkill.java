package tong.statmod.combat.skills;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import tong.statmod.capability.MobStats;
import tong.statmod.stats.StatType;

/**
 * An active or reactive ability that a mob can perform.
 * Stat-gated by {@link #requiredStat()} ≥ {@link #requiredLevel()}.
 * Executed server-side only.
 */
public interface MobSkill {

    ResourceLocation id();

    StatType requiredStat();
    int requiredLevel();

    int baseCooldownTicks();
    int manaCost();

    /** Max distance to target in blocks. */
    double maxRange();

    /** Final guard called by the tick handler after generic checks. */
    boolean canExecute(Mob mob, LivingEntity target, MobStats stats);

    /** Server-side effect. Must check {@code mob.isAlive()} for delayed actions. */
    void execute(Mob mob, LivingEntity target, MobStats stats);

    /** If true, the skill is driven by LivingHurtEvent rather than the tick handler. */
    default boolean isReactive() { return false; }
}
