package tong.statmod.dungeon.party.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
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
    private int hexCooldown;

    public MageRangedGoal(Mob mage) {
        this.mage = mage;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        target = mage.getTarget();
        if (target == null || !target.isAlive() || mage.distanceToSqr(target) > 48 * 48) {
            target = nearestPlayer(); // acquiert une proie TOUT SEUL — n'attend pas d'être attaqué
            if (target != null) mage.setTarget(target);
        }
        return target != null && target.isAlive() && mage.distanceToSqr(target) < 48 * 48;
    }

    private LivingEntity nearestPlayer() {
        AABB box = mage.getBoundingBox().inflate(48.0);
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (Player p : mage.level().getEntitiesOfClass(Player.class, box,
                pl -> pl.isAlive() && !pl.isCreative() && !pl.isSpectator())) {
            double d = mage.distanceToSqr(p);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
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
        } else if (dist > PREFERRED_DIST * PREFERRED_DIST) {
            // Trop loin pour bien lancer → se rapproche à portée de sort (ne reste pas neutre).
            Vec3 toward = target.position().subtract(mage.position()).normalize();
            Vec3 dest = mage.position().add(toward.scale(4));
            mage.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.1);
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

        // Entrave : ralentit un ennemi qui kite/fuit → le tank et l'assassin le rattrapent (combo CC).
        if (hexCooldown <= 0 && dist > FLEE_DIST * FLEE_DIST && dist < 24 * 24) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1, false, true));
            mage.swing(net.minecraft.world.InteractionHand.OFF_HAND);
            hexCooldown = 140 + mage.getRandom().nextInt(40);
        } else if (hexCooldown > 0) {
            hexCooldown--;
        }

        if (castCooldown <= 0) {
            if (dist < POTION_DIST * POTION_DIST) {
                throwHarmPotion();
            } else if (nearbyFoes(target.position(), 5.0) >= 2) {
                // Joueurs regroupés → nappe de crocs balayante (AoE) vers le centre du groupe.
                castFangLine(clusterCentroid(target.position(), 6.0));
            } else {
                castElemental(dist);
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

    /** Sort selon l'élément du mage (feu/glace/foudre/nécro/arcane) lu sur le mob. */
    private void castElemental(double dist) {
        switch (mage.getPersistentData().getString("statmod_mage_element")) {
            case "FROST" -> frostBolt();
            case "STORM" -> stormStrike();
            case "NECRO" -> necroCast();
            case "FIRE" -> fireCast(dist);
            default -> castFangs(dist < 20 * 20 ? 3 : 2); // ARCANE
        }
    }

    private void fireCast(double dist) {
        castFangs(dist < 20 * 20 ? 3 : 2);
        if (target != null) target.setSecondsOnFire(4);
        if (target != null && mage.level() instanceof ServerLevel lv) {
            lv.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.5, target.getZ(),
                    22, 0.4, 0.5, 0.4, 0.02);
        }
    }

    private void frostBolt() {
        if (target == null) return;
        target.hurt(mage.damageSources().magic(), 4.0f + mage.getMaxHealth() * 0.03f);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 1));
        mage.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (mage.level() instanceof ServerLevel lv) {
            lv.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 0.6, target.getZ(),
                    26, 0.4, 0.6, 0.4, 0.03);
        }
    }

    private void stormStrike() {
        if (target == null || !(mage.level() instanceof ServerLevel lv)) return;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(lv);
        if (bolt != null) {
            bolt.moveTo(target.getX(), target.getY(), target.getZ());
            bolt.setVisualOnly(true); // pas de feu sur le décor ; dégâts appliqués à la main
            lv.addFreshEntity(bolt);
        }
        target.hurt(mage.damageSources().lightningBolt(), 5.0f + mage.getMaxHealth() * 0.03f);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
        mage.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    private void necroCast() {
        castFangs(2);
        if (target != null) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            if (mage.level() instanceof ServerLevel lv) {
                lv.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.5, target.getZ(),
                        20, 0.4, 0.5, 0.4, 0.02);
            }
        }
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
        fleePos = blendTowardAnchor(fleePos);
        mage.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.3);
    }

    /** Mélange la destination de repli avec l'ancre de formation (rester derrière le tank). */
    private Vec3 blendTowardAnchor(Vec3 base) {
        var data = mage.getPersistentData();
        if (!data.contains(tong.statmod.dungeon.party.PartyCoordinator.ANCHOR_X)) return base;
        double ax = data.getDouble(tong.statmod.dungeon.party.PartyCoordinator.ANCHOR_X);
        double az = data.getDouble(tong.statmod.dungeon.party.PartyCoordinator.ANCHOR_Z);
        return new Vec3(base.x * 0.6 + ax * 0.4, base.y, base.z * 0.6 + az * 0.4);
    }
}
