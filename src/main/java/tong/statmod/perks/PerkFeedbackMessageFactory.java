package tong.statmod.perks;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.integration.SkillPerkGate;
import tong.statmod.storage.PlayerStatData;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class PerkFeedbackMessageFactory {
    private PerkFeedbackMessageFactory() {}

    public static PerkFeedbackMessage forFailure(Perk perk, PerkManager.UnlockFailure failure, PlayerStatData data, Player player) {
        if (perk == null || failure == null) {
            return new PerkFeedbackMessage(Component.literal("Perk unavailable"), Component.literal("Invalid perk"));
        }

        return new PerkFeedbackMessage(Component.literal(perk.name), Component.literal(messageFor(perk, failure, data, player)));
    }

    private static String messageFor(Perk perk, PerkManager.UnlockFailure failure, PlayerStatData data, Player player) {
        return switch (failure) {
            case LEVEL_TOO_LOW -> perk.stat.displayName + " " + statLevel(perk.stat.index, data, player) + "/" + perk.tier.requiredStatLevel + " required";
            case NOT_ENOUGH_POINTS -> "Family points " + data.getPerkPointsForStat(perk.stat.index) + "/" + perk.tier.cost + " required";
            case SYNERGY_TOO_LOW -> perk.synergyStat.displayName + " " + statLevel(perk.synergyStat.index, data, player) + "/" + PerkTier.SYNERGY.requiredStatLevel + " required";
            case EXTERNAL_REQUIREMENT -> externalRequirementMessage(perk);
            case ALREADY_UNLOCKED -> "Already unlocked";
            case INVALID_PERK -> "Invalid perk";
        };
    }

    private static int statLevel(int statIndex, PlayerStatData data, Player player) {
        return player != null
                ? RaceEffectApplier.getEffectiveLevel(player, statIndex)
                : data.getLevel(statIndex);
    }

    private static String externalRequirementMessage(Perk perk) {
        String race = SkillPerkGate.requiredRace(perk.id);
        List<String> skills = SkillPerkGate.requiredSkills(perk.id);
        String raceText = race == null ? "" : humanizeRequirement(race) + " race";
        String skillText = skills.isEmpty() ? "" : skills.stream().map(PerkFeedbackMessageFactory::humanizeRequirement).collect(Collectors.joining(", "));

        if (!raceText.isEmpty() && !skillText.isEmpty()) {
            return "Requires " + raceText + " and " + skillText;
        }
        if (!raceText.isEmpty()) {
            return "Requires " + raceText;
        }
        if (!skillText.isEmpty()) {
            return "Requires " + skillText;
        }
        return "Perk requirements not met";
    }

    private static String humanizeRequirement(String raw) {
        String path = raw;
        int separator = path.indexOf(':');
        if (separator >= 0 && separator + 1 < path.length()) {
            path = path.substring(separator + 1);
        }

        String[] words = path.split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.toString();
    }

    public record PerkFeedbackMessage(Component title, Component message) {}
}
