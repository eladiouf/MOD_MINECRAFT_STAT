package tong.statmod.magic;

public final class ScrollLearningService {
    private ScrollLearningService() {
    }

    public enum Status {
        LEARNED,
        UPGRADED,
        ALREADY_KNOWN,
        FULL,
        INVALID
    }

    public record Outcome(
            Status status, String spellId, int oldLevel, int newLevel, boolean consume) {
        public static Outcome invalid() {
            return new Outcome(Status.INVALID, "", 0, 0, false);
        }
    }

    public static Outcome apply(
            LearnedSpellState state, String spellId, int rawLevel, int minLevel, int maxLevel) {
        if (state == null || spellId == null || minLevel <= 0 || maxLevel < minLevel) {
            return Outcome.invalid();
        }
        int level = Math.max(minLevel, Math.min(maxLevel, rawLevel));
        int oldLevel = state.level(spellId);
        LearnedSpellState.LearnResult result = state.learn(spellId, level);
        return switch (result) {
            case NEW -> new Outcome(Status.LEARNED, spellId, oldLevel, level, true);
            case UPGRADED -> new Outcome(Status.UPGRADED, spellId, oldLevel, level, true);
            case DUPLICATE -> new Outcome(
                    Status.ALREADY_KNOWN, spellId, oldLevel, oldLevel, false);
            case FULL -> new Outcome(Status.FULL, spellId, oldLevel, oldLevel, false);
            case INVALID -> Outcome.invalid();
        };
    }
}
