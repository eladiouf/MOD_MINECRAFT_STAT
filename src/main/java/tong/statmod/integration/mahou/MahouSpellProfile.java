package tong.statmod.integration.mahou;

import tong.statmod.stats.StatType;

import java.util.List;

public record MahouSpellProfile(
        String itemId,
        String family,
        StatType primaryStat,
        List<StatType> secondaryStats
) {}
