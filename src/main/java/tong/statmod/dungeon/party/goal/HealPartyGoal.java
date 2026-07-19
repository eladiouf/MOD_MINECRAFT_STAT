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

    private static final double SUPPORT_RANGE = 18.0;
    private static final double CAST_RANGE = 9.0;
    private static final double FLEE_RANGE = 6.0;
    private static final int HEAL_COOLDOWN = 200;  // 10 s entre deux soins
    private static final int BUFF_INTERVAL = 300;  // buff de groupe toutes les 15 s

    private final Mob healer;
    private LivingEntity patient;
    private int healCooldown;
    private int buffTimer;
    private int ultCooldown;
    private int shieldTimer;
    private static final int SHIELD_INTERVAL = 300; // bouclier + purge toutes les 15 s

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
        if (ultCooldown > 0) ultCooldown--;
        if (shieldTimer > 0) shieldTimer--;

        PartyTelegraph.maybeEnrage(healer);

        // BOUCLIER DE GROUPE + PURGE : absorption (cœurs jaunes) sur tout le monde et retrait des
        // debuffs du joueur (slow/poison/wither/faiblesse/cécité). Rend le groupe bien plus dur à
        // abattre et neutralise ton contrôle.
        if (shieldTimer <= 0) {
            List<Mob> squad = allies(SUPPORT_RANGE, true);
            if (!squad.isEmpty()) {
                shieldAndCleanse(squad);
                shieldTimer = SHIELD_INTERVAL;
            }
        }

        // ULTIME — Sanctuaire : si au moins 2 alliés sont critiques (<40%), gros soin de groupe.
        if (ultCooldown <= 0 && countCritical() >= 2) {
            massHeal();
            ultCooldown = 600; // 30 s
        }

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
        float amount = 5.0f + healer.getMaxHealth() * 0.05f;
        target.heal(amount);
        // Éclaboussure de soin aux camarades proches du patient (soigne vraiment le groupe).
        for (Mob ally : allies(SUPPORT_RANGE, false)) {
            if (ally != target && ally.distanceToSqr(target) < 25.0 && ally.getHealth() < ally.getMaxHealth()) {
                ally.heal(amount * 0.25f);
            }
        }
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
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0, false, true));
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0, false, true));
        }
        healer.swing(net.minecraft.world.InteractionHand.OFF_HAND);
        if (healer.level() instanceof ServerLevel lv) {
            lv.sendParticles(ParticleTypes.GLOW, healer.getX(), healer.getY() + 1.5, healer.getZ(),
                    30, 2.0, 0.8, 2.0, 0.02);
            lv.playSound(null, healer.getX(), healer.getY(), healer.getZ(),
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.0f, 1.2f);
        }
    }

    /** Bouclier d'absorption sur tout le groupe + purge des debuffs infligés par le joueur. */
    private void shieldAndCleanse(List<Mob> squad) {
        for (Mob ally : squad) {
            MobEffectInstance abs = ally.getEffect(MobEffects.ABSORPTION);
            if (abs == null || abs.getDuration() < 40) {
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0, false, true));
            }
            ally.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            ally.removeEffect(MobEffects.WEAKNESS);
            ally.removeEffect(MobEffects.POISON);
            ally.removeEffect(MobEffects.WITHER);
            ally.removeEffect(MobEffects.BLINDNESS);
            ally.removeEffect(MobEffects.DIG_SLOWDOWN);
        }
        healer.swing(net.minecraft.world.InteractionHand.OFF_HAND);
        if (healer.level() instanceof ServerLevel lv) {
            lv.sendParticles(ParticleTypes.END_ROD, healer.getX(), healer.getY() + 1.2, healer.getZ(),
                    26, 2.2, 0.8, 2.2, 0.01);
            lv.playSound(null, healer.getX(), healer.getY(), healer.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.9f, 1.5f);
        }
    }

    private LivingEntity pickPatient() {
        if (healer.getHealth() < healer.getMaxHealth() * 0.60) return healer;
        LivingEntity best = null;
        float bestFrac = 0.75f;
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

    private int countCritical() {
        int n = 0;
        if (healer.getHealth() < healer.getMaxHealth() * 0.30) n++;
        for (Mob ally : allies(SUPPORT_RANGE, false)) {
            if (ally.getHealth() < ally.getMaxHealth() * 0.30) n++;
        }
        return n;
    }

    /** ULTIME — gros soin instantané + régén/résistance sur tout le groupe. */
    private void massHeal() {
        for (Mob ally : allies(SUPPORT_RANGE, true)) {
            ally.heal(ally.getMaxHealth() * 0.2f);
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, true));
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0, false, true));
        }
        if (healer.level() instanceof ServerLevel lv) {
            lv.sendParticles(ParticleTypes.HEART, healer.getX(), healer.getY() + 1.5, healer.getZ(),
                    40, 3.0, 1.0, 3.0, 0.0);
            lv.playSound(null, healer.getX(), healer.getY(), healer.getZ(),
                    SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.2f, 1.4f);
        }
    }

    /** Membres du groupe autour du soigneur ({@code includeSelf} pour les buffs). */
    private List<Mob> allies(double range, boolean includeSelf) {
        List<Mob> out = healer.level().getEntitiesOfClass(Mob.class,
                healer.getBoundingBox().inflate(range),
                m -> m.isAlive() && (m == healer ? includeSelf : m.getPersistentData().contains(PartyRole.TAG)));
        return out;
    }
}
