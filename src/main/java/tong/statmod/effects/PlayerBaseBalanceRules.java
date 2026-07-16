package tong.statmod.effects;

public final class PlayerBaseBalanceRules {
    public static final double BASE_MAX_HEALTH = 100.0D;
    public static final double BASE_ATTACK_DAMAGE = 5.0D;
    public static final double BASE_MAX_MANA = 500.0D;
    public static final double BASE_MANA_REGEN_PER_SECOND = 1.0D;
    public static final double MAX_MANA_REGEN_PER_SECOND = 17.0D;

    private PlayerBaseBalanceRules() {
    }

    public static double maxMana(int level, int milestones) {
        int safeLevel = Math.max(0, Math.min(100, level));
        int safeMilestones = Math.max(0, Math.min(3, milestones));
        return BASE_MAX_MANA * (1.0D + 0.02D * safeLevel + 0.03D * safeMilestones);
    }

    public static double manaRegenPerSecond(int level, int milestones) {
        int safeLevel = Math.max(0, Math.min(100, level));
        int safeMilestones = Math.max(0, Math.min(3, milestones));
        double regen = BASE_MANA_REGEN_PER_SECOND + 0.145D * safeLevel + 0.5D * safeMilestones;
        return Math.min(MAX_MANA_REGEN_PER_SECOND, regen);
    }

    public static int manaMilestones(int level) {
        int safeLevel = Math.max(0, Math.min(100, level));
        if (safeLevel >= 75) {
            return 3;
        }
        if (safeLevel >= 50) {
            return 2;
        }
        return safeLevel >= 25 ? 1 : 0;
    }
}
