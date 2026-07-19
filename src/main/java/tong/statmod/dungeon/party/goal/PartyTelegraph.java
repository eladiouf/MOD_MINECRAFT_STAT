package tong.statmod.dungeon.party.goal;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Boîte à outils « combat épique » (inspiré d'epic_mobs / soulslike) :
 * attaques TÉLÉGRAPHIÉES = un temps de charge lisible (anneau de particules qui grossit + son)
 * que le joueur peut esquiver, puis un impact de zone. C'est ce qui rend les mobs « épiques »
 * plutôt que des sacs à PV qui tapent au contact.
 */
public final class PartyTelegraph {

    private PartyTelegraph() {}

    private static final DustParticleOptions DANGER =
            new DustParticleOptions(new Vector3f(0.95f, 0.12f, 0.12f), 1.6f);

    /**
     * Anneau d'avertissement au sol qui grossit pendant la charge (rouge = danger imminent).
     * @param center centre de l'attaque, {@code radius} rayon final, {@code tick}/{@code total} progression.
     */
    public static void warningRing(ServerLevel level, Vec3 center, double radius, int tick, int total) {
        double frac = Math.min(1.0, (double) tick / Math.max(1, total));
        double r = Math.max(0.6, radius * frac);
        int points = Math.max(10, (int) (r * 5));
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            double x = center.x + Math.cos(a) * r;
            double z = center.z + Math.sin(a) * r;
            level.sendParticles(DANGER, x, center.y + 0.15, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        // remplissage central clignotant → « ça va tomber ici »
        if (tick % 4 == 0) {
            level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.15, center.z,
                    6, r * 0.4, 0.05, r * 0.4, 0.0);
        }
    }

    /** Son de charge montant (pitch croissant selon l'avancement de la charge). */
    public static void chargeSound(Mob m, int tick, int total) {
        if (tick % 6 != 0) return;
        float pitch = 0.5f + 0.9f * Math.min(1f, (float) tick / total);
        m.playSound(SoundEvents.ANVIL_LAND, 0.45f, pitch);
    }

    /**
     * Impact de zone : explosion visuelle + son + dégâts aux joueurs DANS le rayon (les autres
     * ont esquivé). Repousse et applique un ralentissement optionnel.
     */
    public static void aoeImpact(Mob source, Vec3 center, double radius,
                                 float damage, double knockUp, int slowTicks) {
        if (!(source.level() instanceof ServerLevel level)) return;
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.4, center.z,
                8, radius * 0.5, 0.2, radius * 0.5, 0.0);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y + 0.6, center.z,
                12, radius * 0.5, 0.3, radius * 0.5, 0.0);
        source.playSound(SoundEvents.ANVIL_LAND, 1.2f, 0.55f);
        for (Player p : playersIn(level, center, radius)) {
            p.hurt(source.damageSources().mobAttack(source), damage);
            Vec3 push = p.position().subtract(center);
            if (push.lengthSqr() < 1.0E-4) push = new Vec3(0, 0, 1);
            push = push.normalize();
            p.setDeltaMovement(push.x * 1.2, knockUp, push.z * 1.2);
            p.hurtMarked = true;
            if (slowTicks > 0) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 1));
            }
        }
    }

    public static List<Player> playersIn(ServerLevel level, Vec3 center, double radius) {
        List<Player> hit = level.getEntitiesOfClass(Player.class,
                new AABB(center.x - radius, center.y - 3, center.z - radius,
                        center.x + radius, center.y + 3, center.z + radius),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator());
        hit.removeIf(p -> p.position().multiply(1, 0, 1)
                .distanceToSqr(center.multiply(1, 0, 1)) > radius * radius);
        return hit;
    }

    /** Cri d'enrage + halo : joué une seule fois quand un membre passe en phase « berserk ». */
    public static void roar(Mob m) {
        m.playSound(SoundEvents.WITHER_SPAWN, 0.7f, 1.5f);
        if (m.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, m.getX(), m.getY() + m.getBbHeight(), m.getZ(),
                    14, 0.5, 0.5, 0.5, 0.0);
            level.sendParticles(DANGER, m.getX(), m.getY() + 0.2, m.getZ(),
                    20, 0.6, 0.1, 0.6, 0.02);
        }
    }

    /**
     * Phase « berserk » : sous 35% PV, une seule fois → cri, halo lumineux, vitesse + dégâts boostés.
     * À appeler en début de tick de chaque goal de combat.
     */
    public static void maybeEnrage(Mob m) {
        if (m.getHealth() > m.getMaxHealth() * 0.35f) return;
        if (m.getPersistentData().getBoolean("statmod_enraged")) return;
        m.getPersistentData().putBoolean("statmod_enraged", true);
        roar(m);
        m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 6000, 0, false, false));
        m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, 0, false, false));
        m.setGlowingTag(true);
    }
}
