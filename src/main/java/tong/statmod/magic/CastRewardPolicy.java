package tong.statmod.magic;

/**
 * Policy de récompense au cast d'un sort dans la flow Iron's. Renommé sous Mission ε :
 * {@code arcaneDelta} → {@code magicPointsDelta} pour refléter le pool unifié.
 *
 * <p>La maîtrise de pratique des casts sans impact reste séparée de la maîtrise de progression
 * bancaire. Seule cette dernière peut se convertir en magic points.
 */
public final class CastRewardPolicy {
    /**
     * @param practiceMasteryDelta maîtrise visible gagnée par un cast sans impact
     * @param progressionMasteryDelta maîtrise bancaire gagnée par un cast avec impact
     * @param magicPointsDelta gain de magic points unifiés (peut être 0 si cast trivial)
     */
    public record Reward(int practiceMasteryDelta, int progressionMasteryDelta, int magicPointsDelta) {
        public static final Reward NONE = new Reward(0, 0, 0);

        /**
         * @deprecated Alias de compatibilité pour les callers historiques : seule la maîtrise
         *             bancaire doit être envoyée à {@code applyMastery}.
         */
        @Deprecated
        public int masteryDelta() { return progressionMasteryDelta; }

        /**
         * @deprecated Alias pour compat — préférer {@link #magicPointsDelta()}.
         */
        @Deprecated
        public int arcaneDelta() { return magicPointsDelta; }
    }

    private static final double MIN_MANA_FRACTION = 0.05;
    private static final int PRACTICE_MASTERY = 1;
    private static final int IMPACT_MASTERY = 5;

    private CastRewardPolicy() {}

    public static Reward evaluate(CastContext ctx) {
        if (ctx == null || ctx.branch() == null) return Reward.NONE;
        if (ctx.wasFreeCast()) return Reward.NONE;
        if (ctx.manaFraction() < MIN_MANA_FRACTION) return Reward.NONE;

        if (ctx.hadImpact()) {
            return new Reward(0, IMPACT_MASTERY + Math.max(0, ctx.spellLevel() - 1), 1);
        }
        return new Reward(PRACTICE_MASTERY, 0, 0);
    }
}
