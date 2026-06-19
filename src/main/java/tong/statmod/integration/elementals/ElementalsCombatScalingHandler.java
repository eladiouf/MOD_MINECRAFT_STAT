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
        Entity owner = source.getEntity();
        if (!(owner instanceof Player player) || !isElementalsDamage(source)) {
            return;
        }

        ElementalBranch activeBranch = ElementalsCompat.activeBranch(player);
        if (activeBranch == null) {
            return;
        }

        ElementState state = ElementalsCompat.stateFor(player, activeBranch);
        event.setAmount(event.getAmount() * ElementalsPenaltyModel.damageMultiplier(state));
    }

    private static boolean isElementalsDamage(DamageSource source) {
        Entity direct = source.getDirectEntity();
        return direct != null && direct.getClass().getName().startsWith("dev.saperate.elementals.");
    }
}
