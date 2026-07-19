package tong.statmod.dungeon.ai.goal;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tong.statmod.dungeon.ai.DungeonAiActor;
import tong.statmod.dungeon.ai.DungeonTacticalRole;
import tong.statmod.integration.ironspells.IronSpellIntent;
import tong.statmod.integration.ironspells.IronSpellIntentPolicy;
import tong.statmod.integration.ironspells.IronSpellProfile;
import tong.statmod.integration.ironspells.IronSpellTacticalContext;
import tong.statmod.integration.ironspells.IronsCasterSpells;

/** Tactical Iron's Spells behavior scoped to one exact dungeon-room squad. */
public final class DungeonSpellcasterGoal extends Goal {
    public static final String SUMMON_SQUAD_TAG = "statmod_ai_summon_squad";
    private static final int MAX_SUMMONS_PER_SQUAD = 2;
    private static final double TARGET_RANGE = 32.0;
    private static final double SUPPORT_RANGE = 18.0;
    private static final int HIGH_IMPACT_WINDUP = 20;

    private final Mob caster;
    private final EnumMap<IronSpellIntent, Integer> cooldowns =
            new EnumMap<IronSpellIntent, Integer>(IronSpellIntent.class);
    private LivingEntity hostileTarget;
    private IronSpellIntent pendingIntent;
    private int windupTicks;
    private int decisionDelay;

    public DungeonSpellcasterGoal(Mob caster) {
        this.caster = caster;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        for (IronSpellIntent intent : IronSpellIntent.values()) cooldowns.put(intent, 0);
    }

    @Override
    public boolean canUse() {
        if (squadId().isEmpty()) return false;
        hostileTarget = acquireVisibleTarget();
        return hostileTarget != null || caster.getHealth() < caster.getMaxHealth()
                || sameSquadCriticalAlly();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || pendingIntent != null;
    }

    @Override
    public void tick() {
        cooldowns.replaceAll((intent, ticks) -> Math.max(0, ticks - 1));
        if (decisionDelay > 0) decisionDelay--;

        if (hostileTarget != null && hostileTarget.isAlive()) {
            caster.getLookControl().setLookAt(hostileTarget, 30.0f, 30.0f);
            maintainCastingDistance(hostileTarget);
        }

        if (pendingIntent != null) {
            telegraphHighImpact();
            if (--windupTicks <= 0) {
                castNow(pendingIntent);
                pendingIntent = null;
            }
            return;
        }
        if (decisionDelay > 0) return;

        IronSpellIntent intent = IronSpellIntentPolicy.choose(context());
        if (cooldowns.getOrDefault(intent, 0) > 0) {
            decisionDelay = 5;
            return;
        }
        if (intent == IronSpellIntent.AREA_DAMAGE || intent == IronSpellIntent.SUMMON) {
            pendingIntent = intent;
            windupTicks = HIGH_IMPACT_WINDUP;
            decisionDelay = HIGH_IMPACT_WINDUP;
        } else {
            castNow(intent);
        }
    }

    private void castNow(IronSpellIntent intent) {
        Set<UUID> before = intent == IronSpellIntent.SUMMON ? nearbyMobIds() : Set.of();
        int floor = Math.max(1, caster.getPersistentData().getInt(DungeonAiActor.FLOOR_TAG));
        boolean cast = IronsCasterSpells.cast(caster, hostileTarget, profile(), intent, floor);
        if (cast) {
            cooldowns.put(intent, cooldownFor(intent));
            if (intent == IronSpellIntent.SUMMON) tagNewSummons(before);
        }
        decisionDelay = 10;
    }

    private IronSpellTacticalContext context() {
        float health = caster.getHealth() / Math.max(1.0f, caster.getMaxHealth());
        double distance = hostileTarget == null ? Double.MAX_VALUE : caster.distanceTo(hostileTarget);
        return new IronSpellTacticalContext(DungeonAiActor.tacticalRole(caster), health,
                sameSquadCriticalAlly(), clusteredEnemyCount(), distance,
                summonCount() < MAX_SUMMONS_PER_SQUAD, hostileTarget != null);
    }

    private IronSpellProfile profile() {
        DungeonTacticalRole role = DungeonAiActor.tacticalRole(caster);
        return switch (role) {
            case BATTLE_CLERIC -> IronSpellProfile.HOLY_SUPPORT;
            case NECROMANCER -> IronSpellProfile.NECROMANTIC_PRESSURE;
            case HEXER -> IronSpellProfile.ARCANE_DUELIST;
            case ARCANE_ARTILLERY -> IronSpellProfile.FIRE_ARTILLERY;
            default -> IronSpellProfile.fromElement(
                    caster.getPersistentData().getString("statmod_mage_element"));
        };
    }

    private LivingEntity acquireVisibleTarget() {
        if (caster.getTarget() instanceof Player player && validTarget(player)
                && caster.getSensing().hasLineOfSight(player)) return player;
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Player player : caster.level().getEntitiesOfClass(Player.class,
                caster.getBoundingBox().inflate(TARGET_RANGE), this::validTarget)) {
            double distance = caster.distanceToSqr(player);
            if (distance < best && caster.getSensing().hasLineOfSight(player)) {
                nearest = player;
                best = distance;
            }
        }
        if (nearest != null) caster.setTarget(nearest);
        return nearest;
    }

    private boolean validTarget(Player player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private List<Mob> sameSquadAllies() {
        String squadId = squadId();
        return caster.level().getEntitiesOfClass(Mob.class,
                caster.getBoundingBox().inflate(SUPPORT_RANGE), mob -> mob.isAlive()
                        && squadId.equals(mob.getPersistentData().getString(
                                DungeonAiActor.SQUAD_TAG)));
    }

    private boolean sameSquadCriticalAlly() {
        return sameSquadAllies().stream().anyMatch(mob -> mob != caster
                && mob.getHealth() < mob.getMaxHealth() * 0.40f);
    }

    private int clusteredEnemyCount() {
        if (hostileTarget == null) return 0;
        return caster.level().getEntitiesOfClass(Player.class,
                hostileTarget.getBoundingBox().inflate(5.0), this::validTarget).size();
    }

    private int summonCount() {
        String squadId = squadId();
        return (int) caster.level().getEntitiesOfClass(Mob.class,
                caster.getBoundingBox().inflate(TARGET_RANGE), Mob::isAlive).stream()
                .filter(mob -> squadId.equals(mob.getPersistentData().getString(
                        SUMMON_SQUAD_TAG))).count();
    }

    private Set<UUID> nearbyMobIds() {
        Set<UUID> ids = new HashSet<>();
        for (Mob mob : caster.level().getEntitiesOfClass(Mob.class,
                caster.getBoundingBox().inflate(SUPPORT_RANGE), Mob::isAlive)) {
            ids.add(mob.getUUID());
        }
        return ids;
    }

    private void tagNewSummons(Set<UUID> before) {
        String squadId = squadId();
        int remaining = MAX_SUMMONS_PER_SQUAD - summonCount();
        if (remaining <= 0) return;
        for (Mob mob : caster.level().getEntitiesOfClass(Mob.class,
                caster.getBoundingBox().inflate(SUPPORT_RANGE), Mob::isAlive)) {
            if (mob == caster || before.contains(mob.getUUID())) continue;
            if (remaining > 0) {
                mob.getPersistentData().putString(SUMMON_SQUAD_TAG, squadId);
                remaining--;
            } else {
                mob.discard();
            }
        }
    }

    private void maintainCastingDistance(LivingEntity target) {
        double distance = caster.distanceTo(target);
        if (distance < 6.0) {
            Vec3 away = caster.position().subtract(target.position()).normalize();
            Vec3 destination = caster.position().add(away.scale(5.0));
            caster.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.15);
        } else if (distance > 20.0) {
            caster.getNavigation().moveTo(target, 1.0);
        } else {
            caster.getNavigation().stop();
        }
    }

    private void telegraphHighImpact() {
        if (!(caster.level() instanceof ServerLevel level)) return;
        level.sendParticles(pendingIntent == IronSpellIntent.SUMMON
                        ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.ENCHANT,
                caster.getX(), caster.getY() + 1.2, caster.getZ(),
                4, 0.55, 0.7, 0.55, 0.01);
        if (windupTicks == HIGH_IMPACT_WINDUP) {
            level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON,
                    SoundSource.HOSTILE, 1.0f, 0.85f);
        }
    }

    private String squadId() {
        return caster.getPersistentData().getString(DungeonAiActor.SQUAD_TAG);
    }

    private static int cooldownFor(IronSpellIntent intent) {
        return switch (intent) {
            case DIRECT_DAMAGE -> 45;
            case AREA_DAMAGE -> 180;
            case CONTROL -> 120;
            case DEFENSE -> 200;
            case MOBILITY -> 160;
            case ALLY_SUPPORT -> 140;
            case SUMMON -> 320;
        };
    }
}
