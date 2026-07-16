package tong.statmod.perks;

import java.util.List;
import java.util.Objects;

public record AutomaticPerkDefinition(
        String id,
        int order,
        List<AutomaticPerkRequirement> requirements,
        AutomaticPerkEffect effect,
        double defaultAmount) {
    public AutomaticPerkDefinition {
        if (id == null || !id.matches("statmod:[a-z0-9_]+")) {
            throw new IllegalArgumentException("invalid perk id");
        }
        requirements = List.copyOf(requirements == null ? List.of() : requirements);
        if (requirements.isEmpty()
                || requirements.stream().map(AutomaticPerkRequirement::stat)
                        .distinct().count() != requirements.size()) {
            throw new IllegalArgumentException("requirements must be non-empty and unique");
        }
        Objects.requireNonNull(effect, "effect");
        if (!Double.isFinite(defaultAmount) || defaultAmount < 0.0 || defaultAmount > 10.0) {
            throw new IllegalArgumentException("invalid default amount");
        }
    }
}
