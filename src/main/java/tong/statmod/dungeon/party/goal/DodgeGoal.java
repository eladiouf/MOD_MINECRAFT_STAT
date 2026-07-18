package tong.statmod.dungeon.party.goal;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tong.statmod.dungeon.party.PartyRole;

import java.util.EnumSet;

/**
 * Esquive réactive : détecte un projectile qui arrive droit sur le mob et le fait <b>rouler sur le
 * côté</b> (dash perpendiculaire + petit saut). Haute priorité et bref → préempte juste le temps de
 * l'esquive. Ignore les projectiles alliés. Donne aux membres fragiles un vrai skill d'évitement.
 */
public class DodgeGoal extends Goal {

    private static final int COOLDOWN = 22;
    private final Mob mob;
    private int cooldown;
    private Vec3 dodgeDir;
    private int dodgeTicks;

    public DodgeGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        Projectile threat = incomingProjectile();
        if (threat != null) {
            Vec3 v = threat.getDeltaMovement();
            Vec3 perp = new Vec3(-v.z, 0, v.x).normalize();
            Vec3 toMob = mob.position().subtract(threat.position());
            if (perp.dot(toMob) < 0) perp = perp.scale(-1); // esquive du côté où on est déjà
            dodgeDir = perp;
            return true;
        }
        // Esquive au corps-à-corps : un joueur tout proche en train de frapper → roulade latérale
        // (perpendiculaire à son regard) → très dur à toucher au contact.
        Player swinger = closeSwingingFoe();
        if (swinger != null) {
            Vec3 look = swinger.getLookAngle();
            Vec3 perp = new Vec3(-look.z, 0, look.x).normalize();
            Vec3 toMob = mob.position().subtract(swinger.position());
            if (perp.dot(toMob) < 0) perp = perp.scale(-1);
            dodgeDir = perp;
            return true;
        }
        return false;
    }

    private Player closeSwingingFoe() {
        AABB box = mob.getBoundingBox().inflate(3.2);
        Player best = null;
        double bd = Double.MAX_VALUE;
        for (Player p : mob.level().getEntitiesOfClass(Player.class, box,
                pl -> pl.isAlive() && !pl.isCreative() && !pl.isSpectator() && pl.swinging)) {
            double d = mob.distanceToSqr(p);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }

    @Override
    public boolean canContinueToUse() {
        return dodgeTicks > 0;
    }

    @Override
    public void start() {
        dodgeTicks = 6;
        mob.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f, 1.6f);
    }

    @Override
    public void stop() {
        cooldown = COOLDOWN;
        dodgeDir = null;
    }

    @Override
    public void tick() {
        if (dodgeDir == null) return;
        dodgeTicks--;
        Vec3 dest = mob.position().add(dodgeDir.scale(3));
        mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.8);
        if (dodgeTicks == 5 && mob.onGround()) {
            mob.setDeltaMovement(dodgeDir.x * 0.45, 0.32, dodgeDir.z * 0.45);
            mob.hasImpulse = true;
        }
    }

    private Projectile incomingProjectile() {
        AABB box = mob.getBoundingBox().inflate(7.0);
        for (Projectile p : mob.level().getEntitiesOfClass(Projectile.class, box)) {
            if (p.getOwner() == mob) continue;
            if (p.getOwner() instanceof Mob owner
                    && owner.getPersistentData().contains(PartyRole.TAG)) continue; // projectile allié
            Vec3 v = p.getDeltaMovement();
            if (v.lengthSqr() < 0.02) continue;
            Vec3 toMob = mob.position().add(0, mob.getBbHeight() * 0.5, 0).subtract(p.position());
            double dist = toMob.length();
            if (dist > 7.0 || dist < 0.6) continue;
            if (v.normalize().dot(toMob.normalize()) > 0.9) return p; // vient droit sur nous
        }
        return null;
    }
}
