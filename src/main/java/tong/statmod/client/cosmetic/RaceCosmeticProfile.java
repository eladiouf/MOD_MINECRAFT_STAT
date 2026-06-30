package tong.statmod.client.cosmetic;

import java.util.Locale;

public enum RaceCosmeticProfile {
    NONE,
    ELF,
    DWARF,
    BEASTFOLK;

    public static RaceCosmeticProfile resolve(String raceId) {
        if (raceId == null || raceId.isBlank()) {
            return NONE;
        }

        String normalized = raceId.trim().toLowerCase(Locale.ROOT);
        int colon = normalized.indexOf(':');
        String family = colon >= 0 ? normalized.substring(colon + 1) : normalized;
        family = stripEvolutionPrefix(family);

        return switch (family) {
            case "elf" -> ELF;
            case "dwarf" -> DWARF;
            case "beast", "beastfolk" -> BEASTFOLK;
            default -> NONE;
        };
    }

    private static String stripEvolutionPrefix(String family) {
        String[] prefixes = {
                "divine_", "enlightened_", "ancient_",
                "arch_", "greater_", "lesser_",
                "spirit_"
        };
        for (String prefix : prefixes) {
            if (family.startsWith(prefix)) {
                return family.substring(prefix.length());
            }
        }

        String[] suffixes = {"_saint", "_lord"};
        for (String suffix : suffixes) {
            if (family.endsWith(suffix)) {
                return family.substring(0, family.length() - suffix.length());
            }
        }

        return family;
    }
}
