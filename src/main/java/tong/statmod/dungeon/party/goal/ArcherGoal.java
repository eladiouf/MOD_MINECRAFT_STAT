package tong.statmod.dungeon.party.goal;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Archer / rôdeur du groupe : maintient la distance, tire des flèches (en code, anticipées), et
 * <b>marque sa proie</b> (Glowing) pour focaliser le groupe. Recule quand on l'approche.
 */
public class ArcherGoal extends Goal {

    private static final double PREFERRED = 14.0;
    private static final double FLEE = 6.0;
    private static final int SHOOT_INTERVAL = 35;
    private static final int MARK_INTERVAL = 120;

    private final Mob archer;
    private LivingEntity target;
    private int shootCooldown;
    private int markCooldown;
    private int ultCooldown;
    private int strafe;
    private int powerCharge = -1;
    private int powerCooldown;
    private static final int POWER_WINDUP = 22;

    public ArcherGoal(Mob archer) {
        this.archer = archer;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        target = archer.getTarget();
        if (target == null || !target.isAlive() || archer.distanceToSqr(target) > 50 * 50) {
            target = nearestPlayer(); // tire tout seul, sans attendre d'être attaqué
            if (target != null) archer.setTarget(target);
        }
        return target != null && target.isAlive() && archer.distanceToSqr(target) < 50 * 50;
    }

    private LivingEntity nearestPlayer() {
        AABB box = archer.getBoundingBox().inflate(50.0);
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (Player p : archer.level().getEntitiesOfClass(Player.class, box,
                pl -> pl.isAlive() && !pl.isCreative() && !pl.isSpectator())) {
            double d = archer.distanceToSqr(p);
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
        double dist = archer.distanceToSqr(target);
        archer.getLookControl().setLookAt(target, 30.0f, 30.0f);

        PartyTelegraph.maybeEnrage(archer);

        // TIR PUISSANT télégraphié : ligne de particules sur la trajectoire, puis flèche rapide,
        // perçante, à gros dégâts. Dur à esquiver → punit le joueur immobile ou à découvert.
        if (archer.level() instanceof ServerLevel psLevel) {
            if (powerCharge >= 0) {
                archer.getNavigation().stop();
                archer.getLookControl().setLookAt(target, 30.0f, 30.0f);
                Vec3 from = archer.position().add(0, archer.getEyeHeight(), 0);
                Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(from).normalize();
                for (int i = 1; i <= 7; i++) {
                    Vec3 pp = from.add(dir.scale(i * 1.6));
                    psLevel.sendParticles(ParticleTypes.CRIT, pp.x, pp.y, pp.z, 1, 0.0, 0.0, 0.0, 0.0);
                }
                PartyTelegraph.chargeSound(archer, powerCharge, POWER_WINDUP);
                if (++powerCharge >= POWER_WINDUP) {
                    powerShot();
                    powerCharge = -1;
                    powerCooldown = 200;
                }
                return;
            }
            if (powerCooldown > 0) powerCooldown--;
            if (powerCooldown <= 0 && dist > FLEE * FLEE && dist < 40 * 40) {
                powerCharge = 0;
                archer.playSound(SoundEvents.SKELETON_SHOOT, 0.6f, 0.55f);
                return;
            }
        }

        if (dist < FLEE * FLEE) {
            Vec3 away = archer.position().subtract(target.position()).normalize();
            Vec3 dest = archer.position().add(away.scale(8));
            archer.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.25);
        } else if (dist > PREFERRED * PREFERRED) {
            Vec3 toward = target.position().subtract(archer.position()).normalize();
            Vec3 dest = archer.position().add(toward.scale(4));
            archer.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.1);
        } else {
            // À bonne distance : strafe pour esquiver.
            strafe++;
            Vec3 side = new Vec3(target.getZ() - archer.getZ(), 0, archer.getX() - target.getX())
                    .normalize().scale(strafe % 80 < 40 ? 3 : -3);
            Vec3 dest = archer.position().add(side);
            archer.getNavigation().moveTo(dest.x, dest.y, dest.z, 0.9);
        }

        if (markCooldown > 0) markCooldown--;
        else {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, true));
            markCooldown = MARK_INTERVAL;
        }

        if (ultCooldown > 0) ultCooldown--;
        else if (dist < 40 * 40) {
            volley();
            ultCooldown = 300; // 15 s
        }

        if (shootCooldown > 0) shootCooldown--;
        else if (dist < 40 * 40) {
            shootArrow();
            shootCooldown = SHOOT_INTERVAL + archer.getRandom().nextInt(15);
        }
    }

    /** ULTIME — pluie de flèches sur la zone de la cible. */
    private void volley() {
        if (!(archer.level() instanceof ServerLevel level)) return;
        Vec3 tp = target.position();
        for (int i = 0; i < 12; i++) {
            double ox = (archer.getRandom().nextDouble() - 0.5) * 5.0;
            double oz = (archer.getRandom().nextDouble() - 0.5) * 5.0;
            Arrow arrow = new Arrow(level, archer);
            arrow.setPos(archer.getX(), archer.getEyeY() + 2.0, archer.getZ());
            arrow.setBaseDamage(2.5);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            double dx = tp.x + ox - arrow.getX();
            double dy = tp.y - arrow.getY();
            double dz = tp.z + oz - arrow.getZ();
            double horiz = Math.sqrt(dx * dx + dz * dz);
            arrow.shoot(dx, dy + horiz * 0.35, dz, 1.4f, 6.0f);
            level.addFreshEntity(arrow);
        }
        archer.playSound(SoundEvents.SKELETON_SHOOT, 1.3f, 0.8f);
    }

    /** Tir puissant perçant (télégraphié) : flèche rapide, gros dégâts, transperce l'armure légère. */
    private void powerShot() {
        if (!(archer.level() instanceof ServerLevel level) || target == null) return;
        Arrow arrow = new Arrow(level, archer);
        Vec3 tPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
        arrow.setPos(archer.getX(), archer.getEyeY(), archer.getZ());
        double dx = tPos.x - arrow.getX();
        double dy = tPos.y - arrow.getY();
        double dz = tPos.z - arrow.getZ();
        arrow.setBaseDamage(10.0 + archer.getMaxHealth() * 0.05);
        arrow.setPierceLevel((byte) 3);
        arrow.setCritArrow(true);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.shoot(dx, dy, dz, 3.2f, 0.35f); // tendu et rapide
        applyArrowType(arrow);
        archer.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        archer.playSound(SoundEvents.SKELETON_SHOOT, 1.4f, 0.7f);
        level.addFreshEntity(arrow);
    }

    private void shootArrow() {
        if (!(archer.level() instanceof ServerLevel level)) return;
        Arrow arrow = new Arrow(level, archer);
        Vec3 tPos = target.position().add(0, target.getBbHeight() * 0.4, 0);
        Vec3 tVel = target.getDeltaMovement();
        double lead = archer.distanceTo(target) / 3.0;
        Vec3 predicted = tPos.add(tVel.scale(Math.min(lead, 5.0)));
        double dx = predicted.x - archer.getX();
        double dy = predicted.y - archer.getEyeY();
        double dz = predicted.z - archer.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        arrow.setPos(archer.getX(), archer.getEyeY(), archer.getZ());
        arrow.setBaseDamage(3.0 + archer.getMaxHealth() * 0.02);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.shoot(dx, dy + horiz * 0.06, dz, 2.2f, 1.0f);
        applyArrowType(arrow);
        archer.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        archer.playSound(SoundEvents.SKELETON_SHOOT, 1.0f, 1.1f);
        level.addFreshEntity(arrow);
    }

    /** Applique le type de flèche (feu/poison/gel) tiré au sort au spawn. */
    private void applyArrowType(Arrow arrow) {
        switch (archer.getPersistentData().getString("statmod_archer_arrow")) {
            case "FIRE" -> arrow.setSecondsOnFire(100);
            case "POISON" -> arrow.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            case "FROST" -> arrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            default -> { }
        }
    }
}
