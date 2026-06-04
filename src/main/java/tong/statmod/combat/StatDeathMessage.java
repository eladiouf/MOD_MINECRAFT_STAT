package tong.statmod.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.stats.StatType;

public class StatDeathMessage {
    public static Component getCustomDeathMessage(LivingEntity victim, DamageSource source) {
        if (!(source.getEntity() instanceof Player attacker)) return null;
        if (!(victim instanceof Player)) return null;

        int[] bruteLevel = {0};
        CapabilityHelper.withStats(attacker, stats -> bruteLevel[0] = stats.getLevel(StatType.BRUTE_FORCE.index));

        if (bruteLevel[0] >= 80) {
            return Component.translatable("death.statmod.crushed",
                victim.getDisplayName(), attacker.getDisplayName());
        }

        int[] precisionLevel = {0};
        CapabilityHelper.withStats(attacker, stats -> precisionLevel[0] = stats.getLevel(StatType.PRECISION.index));
        if (precisionLevel[0] >= 50 && source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow) {
            return Component.translatable("death.statmod.sniped",
                victim.getDisplayName(), attacker.getDisplayName());
        }

        int[] bladeLevel = {0};
        CapabilityHelper.withStats(attacker, stats -> bladeLevel[0] = stats.getLevel(StatType.BLADE_TECHNIQUE.index));
        if (bladeLevel[0] >= 100) {
            return Component.translatable("death.statmod.dismantled",
                victim.getDisplayName(), attacker.getDisplayName());
        }

        return null;
    }
}
