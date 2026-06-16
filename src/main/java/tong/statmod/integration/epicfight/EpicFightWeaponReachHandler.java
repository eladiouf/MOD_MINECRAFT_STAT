package tong.statmod.integration.epicfight;

import net.minecraft.world.entity.player.Player;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.stats.StatType;

public final class EpicFightWeaponReachHandler {
    private EpicFightWeaponReachHandler() {}

    public static float reachBonus(Player player) {
        int precision = player == null ? 0 : RaceEffectApplier.getEffectiveLevel(player, StatType.PRECISION.index);
        int agility = player == null ? 0 : RaceEffectApplier.getEffectiveLevel(player, StatType.AGILITY.index);
        return reachBonus(precision, agility);
    }

    public static float reachBonus(int precision, int agility) {
        return Math.min(2.0f, precision * 0.01f + agility * 0.005f);
    }
}
