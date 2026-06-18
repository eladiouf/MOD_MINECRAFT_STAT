package tong.statmod.integration.tensura;

import tong.statmod.stats.StatType;

import java.util.List;

public record TensuraSpellProfile(
        String skillId,
        String discipline,
        StatType primaryStat,
        List<StatType> secondaryStats,
        boolean elemental
) {}
