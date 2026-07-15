package tong.statmod.perks;

import java.util.Objects;
import tong.statmod.stats.StatProgress;
import tong.statmod.stats.StatType;

public record AutomaticPerkRequirement(StatType stat, int minimumLevel) {
    public AutomaticPerkRequirement {
        Objects.requireNonNull(stat, "stat");
        if (minimumLevel < 1 || minimumLevel > StatProgress.MAX_LEVEL) {
            throw new IllegalArgumentException("minimumLevel must be between 1 and 100");
        }
    }
}
