package tong.statmod.progression;

final class ActivityXpScaling {
    private static final double CLOSE_THREAT_KILL_DISTANCE = 4.0d;

    private ActivityXpScaling() {}

    static int agilityXpForMovement(boolean moving, boolean sprinting, boolean flying, boolean passenger) {
        // Balance 30j : doublé de 1→2 pour rendre lv 10-20 atteignable en ~5h de sprint.
        return moving && sprinting && !flying && !passenger ? 2 : 0;
    }

    static int enduranceXpForMovement(boolean moving,
                                      boolean sprinting,
                                      boolean swimming,
                                      boolean flying,
                                      boolean passenger) {
        return moving && (sprinting || swimming) && !flying && !passenger ? 1 : 0;
    }

    static int enduranceXpForPhysicalDamage(float originalDamage) {
        if (originalDamage <= 0.0f) {
            return 0;
        }
        return Math.min(6, Math.max(1, Math.round(originalDamage / 6.0f)));
    }

    static int intimidationXpForKill(double distanceToTarget, boolean targetWasThreatening, float targetMaxHealth) {
        if (!targetWasThreatening || targetMaxHealth <= 0.0f || distanceToTarget > CLOSE_THREAT_KILL_DISTANCE) {
            return 0;
        }
        return Math.min(6, Math.max(1, Math.round(targetMaxHealth / 20.0f)));
    }

    // ── Balance 30j : nouvelles sources d'XP ────────────────────────────────

    /**
     * RAPIDITÉ — XP pour attaque sprint (sprint + hit dans la même fenêtre).
     * Récompense le gameplay agressif rapide même sans kodachi.
     */
    static int rapiditeXpForSprintAttack(boolean sprinting, float damageDealt) {
        if (!sprinting || damageDealt <= 0.0f) {
            return 0;
        }
        return Math.min(4, Math.max(1, Math.round(damageDealt / 5.0f)));
    }

    /**
     * WILLPOWER — XP pour encaisser un debuff (ajouté quand un debuff néfaste est appliqué).
     * Compense le fait que les debuffs expirent rarement naturellement.
     */
    static int willpowerXpForDebuffReceived(int effectAmplifier) {
        return Math.max(1, Math.min(4, effectAmplifier + 1));
    }

    /**
     * WILLPOWER — XP bonus quand le joueur survit en dessous de 30% HP pendant un tick check.
     * Récompense la persévérance sous pression.
     */
    static int willpowerXpForLowHpSurvival(float currentHealth, float maxHealth) {
        if (maxHealth <= 0.0f || currentHealth <= 0.0f) {
            return 0;
        }
        float ratio = currentHealth / maxHealth;
        if (ratio > 0.3f) {
            return 0;
        }
        // Plus on est bas, plus on gagne : 1 XP à 30%, 2 XP à 15%, 3 XP sous 5%
        if (ratio < 0.05f) return 3;
        if (ratio < 0.15f) return 2;
        return 1;
    }

    /**
     * COOKING — XP pour manger de la nourriture. Proportionnel à la nutrition.
     * Remplace le taux anémique de 1 XP par cuisson.
     */
    static int cookingXpForEating(int nutrition) {
        return Math.max(1, Math.min(8, nutrition / 2));
    }

    /**
     * ALCHEMY — XP pour boire/utiliser une potion. Proportionnel au nombre d'effets.
     */
    static int alchemyXpForDrinking(int effectCount) {
        return Math.max(2, Math.min(8, effectCount * 3));
    }

    /**
     * KEEN_SENSES — XP passif quand le joueur est en sneak à proximité de mobs hostiles.
     * Remplace la dépendance totale à EpicFight.
     */
    static int keenSensesXpForSneakNearHostiles(int nearbyHostileCount) {
        if (nearbyHostileCount <= 0) {
            return 0;
        }
        return Math.min(3, nearbyHostileCount);
    }
}
