package tong.statmod.dungeon.ai.living.goal;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import tong.statmod.dungeon.ai.DungeonAiActor;
import tong.statmod.dungeon.ai.DungeonFaction;

public final class EngineerRepairGoal extends Goal {
    private static final int REPAIR_COOLDOWN = 100;
    private final Mob engineer;
    private int cooldown;

    public EngineerRepairGoal(Mob engineer) {
        this.engineer = engineer;
    }

    @Override public boolean canUse() { return engineer.isAlive(); }
    @Override public boolean canContinueToUse() { return canUse(); }

    @Override
    public void tick() {
        if (cooldown-- > 0) return;
        String squadId = engineer.getPersistentData().getString(DungeonAiActor.SQUAD_TAG);
        Mob construct = engineer.level().getEntitiesOfClass(Mob.class,
                engineer.getBoundingBox().inflate(12.0), mob -> mob != engineer && mob.isAlive()
                        && mob.getHealth() < mob.getMaxHealth()
                        && squadId.equals(mob.getPersistentData().getString(DungeonAiActor.SQUAD_TAG))
                        && DungeonAiActor.faction(mob) == DungeonFaction.DUNGEON_CONSTRUCTS)
                .stream().min(java.util.Comparator.comparingDouble(engineer::distanceToSqr))
                .orElse(null);
        if (construct != null) construct.heal(Math.min(4.0f, construct.getMaxHealth() * 0.08f));
        cooldown = REPAIR_COOLDOWN;
    }
}
