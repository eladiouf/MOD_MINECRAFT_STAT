package tong.statmod.effects;

import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

public record AttributeEffectLevels(int rapidite, int agility, int physicalEndurance) {
    public static AttributeEffectLevels from(PlayerStats stats) {
        return new AttributeEffectLevels(
                stats.get(StatType.RAPIDITE).level(),
                stats.get(StatType.AGILITY).level(),
                stats.get(StatType.PHYSICAL_ENDURANCE).level());
    }
}
