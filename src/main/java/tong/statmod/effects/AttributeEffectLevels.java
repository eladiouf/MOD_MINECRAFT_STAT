package tong.statmod.effects;

import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;

public record AttributeEffectLevels(
        int rapidite,
        int agility,
        int physicalEndurance,
        int arcanePower,
        int castingSpeed,
        int manaPool,
        int magicResistance) {
    public static AttributeEffectLevels from(PlayerStats stats) {
        return new AttributeEffectLevels(
                stats.get(StatType.RAPIDITE).level(),
                stats.get(StatType.AGILITY).level(),
                stats.get(StatType.PHYSICAL_ENDURANCE).level(),
                stats.get(StatType.ARCANE_POWER).level(),
                stats.get(StatType.CASTING_SPEED).level(),
                stats.get(StatType.MANA_POOL).level(),
                stats.get(StatType.MAGIC_RESISTANCE).level());
    }
}
