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

    /**
     * Overload qui enrichit le message avec la liste des stats manquantes quand la failure
     * est {@link MagicEligibilityResolver.Failure#STAT_REQUIREMENT_NOT_MET}. Pour les autres
     * failures, équivalent à l'overload simple.
     */
    public static MagicUnlockFeedbackMessage forFailure(MagicNode node,
                                                         MagicEligibilityResolver.Result result) {
        if (result == null) {
            return forFailure(node, MagicEligibilityResolver.Failure.NONE);
        }
        return buildMessage(node, result.failure(), result.missingStats());
    }

    /** Variante consommée par {@link MagicTreeProgressionService} via son {@code UnlockResult}. */
    public static MagicUnlockFeedbackMessage forFailure(MagicNode node,
                                                         MagicEligibilityResolver.Failure failure,
                                                         java.util.List<MagicNodeStatRequirements.StatGate> missingStats) {
        return buildMessage(node, failure, missingStats == null ? java.util.List.of() : missingStats);
    }

    private static MagicUnlockFeedbackMessage buildMessage(MagicNode node,
                                                            MagicEligibilityResolver.Failure failure,
                                                            java.util.List<MagicNodeStatRequirements.StatGate> missingStats) {
        String message = messageFor(failure);
        if (failure == MagicEligibilityResolver.Failure.STAT_REQUIREMENT_NOT_MET
                && missingStats != null && !missingStats.isEmpty()) {
            StringBuilder sb = new StringBuilder(message).append(':');
            for (MagicNodeStatRequirements.StatGate gate : missingStats) {
                sb.append("\n• ").append(gate.stat().displayName).append(" ≥ ").append(gate.minLevel());
            }
            message = sb.toString();
        }
        return new MagicUnlockFeedbackMessage(
                Component.literal(titleFor(node)),
                Component.literal(message)
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
            case STAT_REQUIREMENT_NOT_MET -> "Stat requirements not met";
            case NONE -> "Magic requirements not met";
        };
    }

    public record MagicUnlockFeedbackMessage(Component title, Component message) {}
}
