package tong.statmod.dungeon.party.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * L'assassin contourne sa cible pour attaquer de côté/derrière,
 * se déplace rapidement et esquive les attaques frontales.
 */
public class AssassinAttackGoal extends Goal {

    private static final double FLANK_DIST = 4.0;
    private static final int FLANK_INTERVAL = 30;

    private final Mob assassin;
    private LivingEntity target;
    private int flankTimer;

    public AssassinAttackGoal(Mob assassin) {
        this.assassin = assassin;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        target = assassin.getTarget();
        return target != null && target.isAlive()
                && assassin.distanceToSqr(target) < 20 * 20;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (target == null) return;
        double dist = assassin.distanceToSqr(target);
        assassin.getLookControl().setLookAt(target, 30.0f, 30.0f);

        flankTimer++;

        // Contourne : toutes les 1.5s, change de direction de contournement
        if (flankTimer % FLANK_INTERVAL == 0) {
            // Détermine une position sur le côté/derrière de la cible
            Vec3 toTarget = target.position().subtract(assassin.position()).normalize();
            boolean goRight = assassin.getRandom().nextBoolean();

            // Vecteur perpendiculaire (sideways)
            Vec3 flankDir = goRight
                    ? new Vec3(-toTarget.z, 0, toTarget.x)
                    : new Vec3(toTarget.z, 0, -toTarget.x);

            // Position : sur le côté de la cible, à FLANK_DIST
            Vec3 flankPos = target.position().add(flankDir.scale(FLANK_DIST));
            // Ajoute un petit décalage vers l'avant/arrière de la cible
            flankPos = flankPos.add(toTarget.scale(goRight ? 1 : -1));

            assassin.getNavigation().moveTo(flankPos.x, flankPos.y, flankPos.z, 1.4);
        }

        // Si assez proche, attaque
        if (dist < 2.5 * 2.5) {
            // Attaque boostée (comme un crit)
            assassin.doHurtTarget(target);
            // Recul immédiat après l'attaque (hit-and-run)
            Vec3 back = assassin.position().subtract(target.position()).normalize().scale(3);
            assassin.setDeltaMovement(back.x, 0.2, back.z);
            assassin.hurtMarked = true;
            flankTimer = FLANK_INTERVAL - 5;
        }
    }
}
