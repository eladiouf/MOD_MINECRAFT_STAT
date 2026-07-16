package tong.statmod.dungeon.party.goal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.Vec3;
import tong.statmod.dungeon.party.PartyRole;

import java.util.EnumSet;
import java.util.List;

public class HealPartyGoal extends Goal {

    private static final double HEAL_RANGE = 14.0;
    private static final double FLEE_RANGE = 8.0;
    private static final int COOLDOWN = 80;

    private final Mob healer;
    private LivingEntity patient;
    private int cooldown;

    public HealPartyGoal(Mob healer) {
        this.healer = healer;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) return false;
        patient = findPatient();
        return patient != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (patient == null || !patient.isAlive()) return false;
        return healer.distanceToSqr(patient) <= HEAL_RANGE * HEAL_RANGE * 1.5;
    }

    @Override
    public void start() {
        healer.setTarget(null);
    }

    @Override
    public void stop() {
        patient = null;
        cooldown = COOLDOWN;
    }

    @Override
    public void tick() {
        if (patient == null || !patient.isAlive()) return;

        double dist = healer.distanceToSqr(patient);
        LivingEntity threat = healer.getTarget();
        boolean hasThreat = threat != null && healer.distanceToSqr(threat) < FLEE_RANGE * FLEE_RANGE;

        if (hasThreat) {
            flee(threat);
            return;
        }

        if (dist > 6 * 6) {
            healer.getNavigation().moveTo(patient, 1.0);
        } else {
            healer.getNavigation().stop();
            healer.getLookControl().setLookAt(patient, 30.0f, 30.0f);
            throwHeal();
        }
    }

    private void throwHeal() {
        if (!(healer.level() instanceof ServerLevel level)) return;

        ItemStack potionStack = PotionUtils.setPotion(
                new ItemStack(Items.SPLASH_POTION), Potions.STRONG_HEALING);

        ThrownPotion potion = new ThrownPotion(
                net.minecraft.world.entity.EntityType.POTION, level);
        potion.setItem(potionStack);
        potion.setOwner(healer);
        potion.setPos(healer.getX(), healer.getEyeY(), healer.getZ());

        Vec3 target = patient.position().add(0, patient.getBbHeight() * 0.5, 0);
        Vec3 dir = target.subtract(potion.position()).normalize();
        potion.shoot(dir.x, dir.y + 0.15, dir.z, 0.8f, 2.0f);

        healer.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        level.addFreshEntity(potion);
        cooldown = COOLDOWN;
    }

    private void flee(LivingEntity threat) {
        Vec3 away = healer.position().subtract(threat.position()).normalize();
        Vec3 fleePos = healer.position().add(away.scale(FLEE_RANGE));
        healer.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.3);
    }

    private LivingEntity findPatient() {
        List<Mob> nearby = healer.level().getEntitiesOfClass(Mob.class,
                healer.getBoundingBox().inflate(HEAL_RANGE),
                m -> m != healer && m.isAlive()
                        && m.getPersistentData().contains(PartyRole.TAG)
                        && m.getHealth() < m.getMaxHealth() * 0.5);

        if (nearby.isEmpty()) return null;
        nearby.sort((a, b) -> Float.compare(
                a.getHealth() / a.getMaxHealth(),
                b.getHealth() / b.getMaxHealth()));
        return nearby.get(0);
    }
}
