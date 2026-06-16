package tong.statmod.integration.epicfight;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.stats.StatType;

public final class EpicFightExecuteHandler {
    private EpicFightExecuteHandler() {}

    public static boolean shouldExecute(Player attacker, LivingEntity target) {
        int intimidation = attacker == null ? 0 : RaceEffectApplier.getEffectiveLevel(attacker, StatType.INTIMIDATION.index);
        float healthRatio = target == null ? 1.0f : target.getHealth() / Math.max(1.0f, target.getMaxHealth());
        return shouldExecute(intimidation, healthRatio);
    }

    public static boolean shouldExecute(int intimidation, float healthRatio) {
        if (intimidation <= 0) {
            return false;
        }
        return healthRatio < executeThreshold(intimidation);
    }

    public static float executeThreshold(int intimidation) {
        if (intimidation <= 0) {
            return 0.0f;
        }
        return 0.10f + intimidation * 0.002f;
    }
}
