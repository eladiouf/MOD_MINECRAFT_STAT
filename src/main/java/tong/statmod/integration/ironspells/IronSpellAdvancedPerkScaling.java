package tong.statmod.integration.ironspells;

import tong.statmod.magic.MagicBranch;

final class IronSpellAdvancedPerkScaling {
    private IronSpellAdvancedPerkScaling() {}

    static double advancedPercentAttributeBonus(boolean active,
                                                boolean synergy,
                                                boolean situational,
                                                boolean mastery,
                                                boolean transcendence) {
        double bonus = 0.0d;
        if (active) bonus += 0.03d;
        if (synergy) bonus += 0.05d;
        if (situational) bonus += 0.04d;
        if (mastery) bonus += 0.07d;
        if (transcendence) bonus += 0.10d;
        return bonus;
    }

    static double advancedManaBonus(boolean active,
                                    boolean synergy,
                                    boolean situational,
                                    boolean mastery,
                                    boolean transcendence) {
        double bonus = 0.0d;
        if (active) bonus += 5.0d;
        if (synergy) bonus += 10.0d;
        if (situational) bonus += 5.0d;
        if (mastery) bonus += 15.0d;
        if (transcendence) bonus += 25.0d;
        return bonus;
    }

    static double arcaneDamageMultiplier(boolean activeWindow,
                                         boolean synergyWindow,
                                         boolean situationalTarget,
                                         int cascadeStacks,
                                         boolean masteryUnlocked,
                                         boolean transcendenceUnlocked) {
        double bonus = 0.0d;
        if (activeWindow) bonus += 0.08d;
        if (synergyWindow) bonus += 0.06d;
        if (situationalTarget) bonus += 0.10d;
        if (masteryUnlocked) bonus += Math.min(3, Math.max(0, cascadeStacks)) * 0.04d;
        if (transcendenceUnlocked && cascadeStacks >= 3) bonus += 0.20d;
        return 1.0d + bonus;
    }

    static double elementalDamageMultiplier(MagicBranch perkBranch,
                                            MagicBranch spellBranch,
                                            boolean activeWindow,
                                            boolean synergyCondition,
                                            boolean situationalCondition,
                                            int chainStacks,
                                            boolean masteryUnlocked,
                                            boolean transcendenceUnlocked) {
        if (perkBranch == null || spellBranch == null || perkBranch != spellBranch) {
            return 1.0d;
        }

        double bonus = 0.0d;
        if (activeWindow) bonus += 0.08d;
        if (synergyCondition) bonus += 0.06d;
        if (situationalCondition) bonus += 0.10d;
        if (masteryUnlocked && chainStacks >= 2) bonus += 0.08d;
        if (transcendenceUnlocked && chainStacks >= 3) bonus += 0.15d;
        return 1.0d + bonus;
    }

    static double manaRefund(double manaCost,
                             boolean manaPoolActive,
                             boolean manaPoolMastery,
                             boolean manaPoolTranscendence) {
        if (manaCost <= 0.0d || !manaPoolActive) {
            return 0.0d;
        }
        double ratio = 0.10d;
        if (manaPoolMastery) ratio += 0.05d;
        if (manaPoolTranscendence) ratio += 0.10d;
        return manaCost * ratio;
    }
}
