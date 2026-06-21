package tong.statmod.magic;

public final class CastRewardPolicy {
    public record Reward(int masteryDelta, int arcaneDelta) {
        public static final Reward NONE = new Reward(0, 0);
    }

    private static final double MIN_MANA_FRACTION = 0.05;
    private static final int BASE_MASTERY = 2;
    private static final int IMPACT_MASTERY = 5;

    private CastRewardPolicy() {}

    public static Reward evaluate(CastContext ctx) {
        if (ctx == null || ctx.branch() == null) return Reward.NONE;
        if (ctx.wasFreeCast()) return Reward.NONE;
        if (ctx.manaFraction() < MIN_MANA_FRACTION) return Reward.NONE;

        int mastery = BASE_MASTERY;
        int arcane = 0;
        if (ctx.hadImpact()) {
            mastery = IMPACT_MASTERY + Math.max(0, ctx.spellLevel() - 1);
            arcane = 1;
        }
        return new Reward(mastery, arcane);
    }
}
