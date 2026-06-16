package tong.statmod.integration.epicfight;

import net.minecraft.world.entity.player.Player;
import yesman.epicfight.world.damagesource.StunType;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.stats.StatType;

public final class EpicFightStunResistanceHandler {
    private EpicFightStunResistanceHandler() {}

    public static float stunTimeMultiplier(Player player, StunType stunType) {
        int willpower = player == null ? 0 : RaceEffectApplier.getEffectiveLevel(player, StatType.WILLPOWER.index);
        int endurance = player == null ? 0 : RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index);
        return stunTimeMultiplier(willpower, endurance, stunType);
    }

    public static float stunTimeMultiplier(int willpower, int endurance, StunType stunType) {
        if (stunType == null || stunType == StunType.NONE) {
            return 1.0f;
        }
        float reduction = willpower * 0.01f + endurance * 0.004f;

        if (stunType == StunType.KNOCKDOWN) {
            reduction += 0.05f;
        } else if (stunType == StunType.HOLD) {
            reduction += 0.08f;
        }

        return Math.max(0.2f, 1.0f - reduction);
    }
}
