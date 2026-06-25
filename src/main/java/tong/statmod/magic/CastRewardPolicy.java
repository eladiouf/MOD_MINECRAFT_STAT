package tong.statmod.magic;

/**
 * Policy de récompense au cast d'un sort dans la flow Iron's. Renommé sous Mission ε :
 * {@code arcaneDelta} → {@code magicPointsDelta} pour refléter le pool unifié.
 *
 * <p>L'ancienne signature {@code Reward(masteryDelta, arcaneDelta)} reste accessible via
 * {@link Reward#arcaneDelta()} pour ne pas casser le bridge d'event (compat shim
 * temporaire).
 */
public final class CastRewardPolicy {
    /**
     * @param masteryDelta progression mastery dans l'école du sort cast
     * @param magicPointsDelta gain de magic points unifiés (peut être 0 si cast trivial)
     */
    public record Reward(int masteryDelta, int magicPointsDelta) {
        public static final Reward NONE = new Reward(0, 0);

        /**
         * @deprecated Alias pour compat — préférer {@link #magicPointsDelta()}.
         */
        @Deprecated
        public int arcaneDelta() { return magicPointsDelta; }
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
        int magicPoints = 0;
        if (ctx.hadImpact()) {
            mastery = IMPACT_MASTERY + Math.max(0, ctx.spellLevel() - 1);
            magicPoints = 1;
        }
        return new Reward(mastery, magicPoints);
    }
}
