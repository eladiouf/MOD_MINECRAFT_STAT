package tong.statmod.dungeon.party.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import java.util.EnumSet;

public class MageRangedGoal extends Goal {

    private static final double PREFERRED_DIST = 12.0;
    private static final double FLEE_DIST = 5.0;
    private static final double CORNERED_DIST = 3.0;
    private static final double POTION_DIST = 6.0;
    private static final int CAST_INTERVAL = 60;

    private final Mob mage;
    private LivingEntity target;
    private int castCooldown;
    private int strafeTimer;
    private int stuckTicks;

    public MageRangedGoal(Mob mage) {
        this.mage = mage;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        target = mage.getTarget();
        return target != null && target.isAlive()
                && mage.distanceToSqr(target) < 36 * 36;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (target == null) return;
        double dist = mage.distanceToSqr(target);

        if (dist < CORNERED_DIST * CORNERED_DIST) {
            if (mage.getNavigation().isDone()) {
                if (++stuckTicks > 20) {
                    teleportEscape();
                    return;
                }
            } else {
                stuckTicks = 0;
            }
        } else {
            stuckTicks = 0;
        }

        if (dist < FLEE_DIST * FLEE_DIST) {
            flee(target);
            return;
        }

        if (dist < PREFERRED_DIST * PREFERRED_DIST * 0.6) {
            Vec3 away = mage.position().subtract(target.position()).normalize();
            Vec3 dest = mage.position().add(away.scale(6));
            mage.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.0);
        } else {
            mage.getNavigation().stop();
        }

        mage.getLookControl().setLookAt(target, 30.0f, 30.0f);

        strafeTimer++;
        if (strafeTimer % 60 < 25) {
            Vec3 right = new Vec3(
                    target.getZ() - mage.getZ(), 0,
                    mage.getX() - target.getX()).normalize();
            Vec3 strafePos = mage.position().add(right.scale(3));
            mage.getNavigation().moveTo(strafePos.x, strafePos.y, strafePos.z, 0.8);
        } else if (strafeTimer % 60 >= 40) {
            Vec3 left = new Vec3(
                    mage.getZ() - target.getZ(), 0,
                    target.getX() - mage.getX()).normalize();
            Vec3 strafePos = mage.position().add(left.scale(3));
            mage.getNavigation().moveTo(strafePos.x, strafePos.y, strafePos.z, 0.8);
        }

        if (castCooldown <= 0) {
            if (dist < POTION_DIST * POTION_DIST) {
                throwHarmPotion();
            } else if (nearbyFoes(target.position(), 5.0) >= 2) {
                // Joueurs regroupés → nappe de crocs balayante (AoE) vers le centre du groupe.
                castFangLine(clusterCentroid(target.position(), 6.0));
            } else {
                castFangs(dist < 20 * 20 ? 3 : 2);
            }
            castCooldown = CAST_INTERVAL + mage.getRandom().nextInt(20);
        } else {
            castCooldown--;
        }
    }

    private int nearbyFoes(Vec3 center, double radius) {
        return mage.level().getEntitiesOfClass(Player.class,
                new AABB(center.x - radius, center.y - 3, center.z - radius,
                        center.x + radius, center.y + 3, center.z + radius),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative()).size();
    }

    private Vec3 clusterCentroid(Vec3 center, double radius) {
        List<Player> players = mage.level().getEntitiesOfClass(Player.class,
                new AABB(center.x - radius, center.y - 3, center.z - radius,
                        center.x + radius, center.y + 3, center.z + radius),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
        if (players.isEmpty()) return center;
        double x = 0, z = 0;
        for (Player p : players) {
            x += p.getX();
            z += p.getZ();
        }
        return new Vec3(x / players.size(), center.y, z / players.size());
    }

    /** Ligne d'EvokerFangs qui jaillit du mage vers {@code toward} (balayage, comme l'évocateur). */
    private void castFangLine(Vec3 toward) {
        if (!(mage.level() instanceof ServerLevel level)) return;
        double angle = Math.atan2(toward.z - mage.getZ(), toward.x - mage.getX());
        for (int i = 0; i < 16; i++) {
            double d = 1.25 * (i + 1);
            double fx = mage.getX() + Math.cos(angle) * d;
            double fz = mage.getZ() + Math.sin(angle) * d;
            var fangs = new net.minecraft.world.entity.projectile.EvokerFangs(
                    level, fx, mage.getY(), fz, (float) angle, i, mage);
            level.addFreshEntity(fangs);
        }
        mage.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    private void castFangs(int count) {
        if (!(mage.level() instanceof ServerLevel level)) return;

        Vec3 tPos = target.position();
        Vec3 tVel = target.getDeltaMovement();
        double lead = mage.distanceTo(target) / 2.0;
        Vec3 predicted = tPos.add(tVel.scale(Math.min(lead, 4.0)));

        for (int i = 0; i < count; i++) {
            double ox = (mage.getRandom().nextDouble() - 0.5) * 3;
            double oz = (mage.getRandom().nextDouble() - 0.5) * 3;
            BlockPos fangPos = BlockPos.containing(predicted.x + ox, predicted.y, predicted.z + oz);

            var fangs = new net.minecraft.world.entity.projectile.EvokerFangs(
                    level, fangPos.getX() + 0.5, fangPos.getY(), fangPos.getZ() + 0.5,
                    (float) Math.atan2(oz, ox), 2, mage);
            level.addFreshEntity(fangs);
        }

        mage.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    private void throwHarmPotion() {
        if (!(mage.level() instanceof ServerLevel level)) return;

        ItemStack potionStack = PotionUtils.setPotion(
                new ItemStack(Items.SPLASH_POTION), Potions.STRONG_HARMING);

        ThrownPotion potion = new ThrownPotion(
                net.minecraft.world.entity.EntityType.POTION, level);
        potion.setItem(potionStack);
        potion.setOwner(mage);
        potion.setPos(mage.getX(), mage.getEyeY(), mage.getZ());

        Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(potion.position()).normalize();
        potion.shoot(dir.x, dir.y + 0.15, dir.z, 0.7f, 2.0f);

        level.addFreshEntity(potion);
        mage.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    private void teleportEscape() {
        if (!(mage.level() instanceof ServerLevel level)) return;

        Vec3 away = mage.position().subtract(target.position()).normalize();
        for (int attempt = 0; attempt < 5; attempt++) {
            BlockPos dest = BlockPos.containing(
                    mage.getX() + away.x * (10 + attempt * 3),
                    mage.getY(),
                    mage.getZ() + away.z * (10 + attempt * 3));

            for (int dy = 0; dy <= 1; dy++) {
                BlockPos check = dest.above(dy);
                if (level.getBlockState(check).isAir()
                        && level.getBlockState(check.above()).isAir()
                        && level.getBlockState(check.below()).isSolid()) {
                    mage.teleportTo(check.getX() + 0.5, check.getY(), check.getZ() + 0.5);
                    mage.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    castCooldown = 20;
                    return;
                }
            }
        }

        BlockPos ground = BlockPos.containing(
                mage.getX() + away.x * 12, mage.getY(), mage.getZ() + away.z * 12);
        mage.teleportTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5);
        mage.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);
        castCooldown = 20;
    }

    private void flee(LivingEntity threat) {
        Vec3 away = mage.position().subtract(threat.position()).normalize();
        Vec3 fleePos = mage.position().add(away.scale(12));
        mage.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.3);
    }
}
