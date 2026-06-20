package tong.statmod.integration.elementals;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class ElementalsCombatScalingHandler {
    private ElementalsCombatScalingHandler() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        ElementalBranch damageBranch = damageBranch(source);
        Entity owner = source.getEntity();
        if (!(owner instanceof Player player) || damageBranch == null) {
            return;
        }

        ElementState state = ElementalsCompat.stateFor(player, damageBranch);
        event.setAmount(event.getAmount() * ElementalsPenaltyModel.damageMultiplier(state));
    }

    static ElementalBranch damageBranch(DamageSource source) {
        Entity direct = source.getDirectEntity();
        return direct == null ? null : branchFromEntityClassName(direct.getClass().getName());
    }

    static ElementalBranch branchFromEntityClassName(String className) {
        if (className == null || !className.startsWith("dev.saperate.elementals.entities.")) {
            return null;
        }

        if (className.contains(".entities.air.")) {
            return ElementalBranch.AIR;
        }
        if (className.contains(".entities.water.")) {
            return ElementalBranch.WATER;
        }
        if (className.contains(".entities.earth.")) {
            return ElementalBranch.EARTH;
        }
        if (className.contains(".entities.fire.")) {
            return ElementalBranch.FIRE;
        }
        if (className.contains(".entities.lightning.")) {
            return ElementalBranch.LIGHTNING;
        }
        if (className.contains(".entities.blood.")) {
            return ElementalBranch.BLOOD;
        }
        if (className.contains(".entities.metal.")) {
            return ElementalBranch.METAL;
        }
        return null;
    }
}
