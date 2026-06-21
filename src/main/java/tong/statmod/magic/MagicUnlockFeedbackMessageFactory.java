package tong.statmod.magic;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class MagicUnlockFeedbackMessageFactory {
    private MagicUnlockFeedbackMessageFactory() {}

    public static MagicUnlockFeedbackMessage forFailure(MagicNode node, MagicEligibilityResolver.Failure failure) {
        return new MagicUnlockFeedbackMessage(
                Component.literal(titleFor(node)),
                Component.literal(messageFor(failure))
        );
    }

    private static String titleFor(MagicNode node) {
        if (node == null || node.id() == null || node.id().isBlank()) {
            return "Magic unavailable";
        }
        String[] segments = node.id().split("/");
        String raw = segments.length == 0 ? node.id() : segments[segments.length - 1];
        String[] words = raw.split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
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
        return builder.length() == 0 ? "Magic unavailable" : builder.toString();
    }

    private static String messageFor(MagicEligibilityResolver.Failure failure) {
        if (failure == null) {
            return "Magic requirements not met";
        }
        return switch (failure) {
            case NOT_ENOUGH_POINTS -> "Not enough magic points";
            case MISSING_PREREQ -> "Previous magic node required";
            case NO_RACE -> "Choose a magical race first";
            case LOCKED -> "This branch is not available yet";
            case ALREADY_UNLOCKED -> "Already unlocked";
            case RUNTIME_GRANT_FAILED -> "Spell unlock failed at runtime";
            case NONE -> "Magic requirements not met";
        };
    }

    public record MagicUnlockFeedbackMessage(Component title, Component message) {}
}
