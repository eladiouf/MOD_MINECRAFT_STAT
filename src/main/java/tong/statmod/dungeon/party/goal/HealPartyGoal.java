package tong.statmod.dungeon.party.goal;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import tong.statmod.dungeon.party.PartyCoordinator;
import tong.statmod.dungeon.party.PartyRole;

import java.util.EnumSet;
import java.util.List;

/**
 * Soigneur du groupe — refonte 2026-07-16. Soigne <b>en code</b> (fiable, marche sur les
 * morts-vivants, ne rate jamais), <b>proactivement</b> (dès qu'un allié n'est pas au max), et
 * <b>buffe</b> périodiquement tout le groupe (force / vitesse / résistance / régénération).
 * Se met à l'abri (recule vers l'ancre) quand un joueur le menace.
 */
public class HealPartyGoal extends Goal {

    private static final double SUPPORT_RANGE = 16.0;
    private static final double CAST_RANGE = 7.0;
    private static final double FLEE_RANGE = 6.0;
    private static final int HEAL_COOLDOWN = 30;   // 1,5 s entre deux soins
    private static final int BUFF_INTERVAL = 180;  // buff de groupe toutes les 9 s

    private final Mob healer;
    private LivingEntity patient;
    private int healCooldown;
    private int buffTimer;

    public HealPartyGoal(Mob healer) {
        this.healer = healer;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Support toujours actif tant qu'il y a un groupe (le soigneur ne fait rien d'autre).
        return !allies(SUPPORT_RANGE, false).isEmpty() || healer.getHealth() < healer.getMaxHealth();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        healer.setTarget(null);
    }

    @Override
    public void tick() {
        if (healCooldown > 0) healCooldown--;
        if (buffTimer > 0) buffTimer--;

        // Menace de mêlée → repli vers l'ancre (rester protégé derrière le tank).
        LivingEntity threat = healer.getTarget();
        if (threat != null && threat.isAlive() && healer.distanceToSqr(threat) < FLEE_RANGE * FLEE_RANGE
                && healer.getHealth() > healer.getMaxHealth() * 0.35) {
            fleeToAnchor(threat);
        }

        // Buff de groupe périodique (hymne de guerre).
        if (buffTimer <= 0) {
            List<Mob> squad = allies(SUPPORT_RANGE, true);
            if (!squad.isEmpty()) {
                buffSquad(squad);
                buffTimer = BUFF_INTERVAL;
            }
        }

        // Cible de soin : soi d'abord si bas, sinon l'allié le plus amoché (< 90 %).
        patient = pickPatient();
        if (patient == null) {
            healer.setTarget(null);
            return;
        }

        if (patient == healer) {
            healer.getNavigation().stop();
            castHeal(healer);
            return;
        }

        double dist = healer.distanceToSqr(patient);
        if (dist > CAST_RANGE * CAST_RANGE) {
            healer.getNavigation().moveTo(patient, 1.25); // rejoint vite le blessé
        } else {
            healer.getNavigation().stop();
            healer.getLookControl().setLookAt(patient, 30.0f, 30.0f);
            castHeal(patient);
        }
    }

    /** Soin direct (aucune potion → jamais raté, marche même sur les morts-vivants). */
    private void castHeal(LivingEntity target) {
        if (healCooldown > 0) return;
        float amount = 5.0f + healer.getMaxHealth() * 0.04f;
        target.heal(amount);
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true));
        healer.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (healer.level() instanceof ServerLevel lv) {
            lv.sendParticles(ParticleTypes.HEART, target.getX(), target.getEyeY(), target.getZ(),
                    3, 0.3, 0.3, 0.3, 0.0);
            lv.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getEyeY(), target.getZ(),
                    6, 0.4, 0.5, 0.4, 0.02);
            lv.playSound(null, healer.getX(), healer.getY(), healer.getZ(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 0.7f, 1.4f);
        }
        healCooldown = HEAL_COOLDOWN;
    }

    /** Buffs de soutien sur tout le groupe (et le soigneur). */
    private void buffSquad(List<Mob> squad) {
        for (Mob ally : squad) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 220, 0, false, true));
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 220, 0, false, true));
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 220, 0, false, true));
        }
        healer.swing(net.minecraft.world.InteractionHand.OFF_HAND);
        if (healer.level() instanceof ServerLevel lv) {
            lv.sendParticles(ParticleTypes.GLOW, healer.getX(), healer.getY() + 1.5, healer.getZ(),
                    30, 2.0, 0.8, 2.0, 0.02);
            lv.playSound(null, healer.getX(), healer.getY(), healer.getZ(),
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.0f, 1.2f);
        }
    }

    private LivingEntity pickPatient() {
        if (healer.getHealth() < healer.getMaxHealth() * 0.85) return healer;
        LivingEntity best = null;
        float bestFrac = 0.90f; // seuil proactif : on soigne dès 90 %
        for (Mob ally : allies(SUPPORT_RANGE, false)) {
            float frac = ally.getHealth() / ally.getMaxHealth();
            if (frac < bestFrac) {
                bestFrac = frac;
                best = ally;
            }
        }
        return best;
    }

    private void fleeToAnchor(LivingEntity threat) {
        Vec3 away = healer.position().subtract(threat.position()).normalize();
        Vec3 fleePos = healer.position().add(away.scale(FLEE_RANGE + 2));
        var data = healer.getPersistentData();
        if (data.contains(PartyCoordinator.ANCHOR_X)) {
            double ax = data.getDouble(PartyCoordinator.ANCHOR_X);
            double az = data.getDouble(PartyCoordinator.ANCHOR_Z);
            fleePos = new Vec3(fleePos.x * 0.55 + ax * 0.45, fleePos.y, fleePos.z * 0.55 + az * 0.45);
        }
        healer.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.35);
    }

    /** Membres du groupe autour du soigneur ({@code includeSelf} pour les buffs). */
    private List<Mob> allies(double range, boolean includeSelf) {
        List<Mob> out = healer.level().getEntitiesOfClass(Mob.class,
                healer.getBoundingBox().inflate(range),
                m -> m.isAlive() && (m == healer ? includeSelf : m.getPersistentData().contains(PartyRole.TAG)));
        return out;
    }
}
