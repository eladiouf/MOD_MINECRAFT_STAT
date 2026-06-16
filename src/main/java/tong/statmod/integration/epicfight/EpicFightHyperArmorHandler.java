package tong.statmod.integration.epicfight;

import net.minecraft.world.entity.player.Player;
import yesman.epicfight.world.damagesource.StunType;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.stats.StatType;

public final class EpicFightHyperArmorHandler {
    private EpicFightHyperArmorHandler() {}

    public static boolean shouldNegateStun(Player player, StunType stunType) {
        int endurance = player == null ? 0 : RaceEffectApplier.getEffectiveLevel(player, StatType.PHYSICAL_ENDURANCE.index);
        int willpower = player == null ? 0 : RaceEffectApplier.getEffectiveLevel(player, StatType.WILLPOWER.index);
        return shouldNegateStun(endurance, willpower, stunType);
    }

    public static boolean shouldNegateStun(int endurance, int willpower, StunType stunType) {
        if (stunType == null || stunType == StunType.NONE) {
            return false;
        }
        int total = endurance + willpower;

        if (stunType == StunType.SHORT && total >= 60) return true;
        if (stunType == StunType.LONG && total >= 100) return true;
        return stunType == StunType.HOLD && total >= 140;
    }
}
