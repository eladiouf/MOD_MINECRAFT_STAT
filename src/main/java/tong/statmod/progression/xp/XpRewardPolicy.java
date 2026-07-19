package tong.statmod.progression.xp;

import java.util.ArrayList;
import java.util.List;
import tong.statmod.stats.StatType;

public final class XpRewardPolicy {
    private XpRewardPolicy() {
    }

    public static List<StatXpAward> awards(XpAction action) {
        if (action == null || action.kind() == null) {
            return List.of();
        }
        return switch (action.kind()) {
            case MELEE_HEAVY -> damageAward(action, StatType.BRUTE_FORCE);
            case MELEE_BLADE -> damageAward(action, StatType.BLADE_TECHNIQUE);
            case PROJECTILE -> damageAward(action, StatType.PRECISION);
            case PHYSICAL_DAMAGE_RECEIVED -> damageAward(action, StatType.PHYSICAL_RESISTANCE);
            case SHIELD_BLOCKED -> damageAward(action, StatType.PHYSICAL_ENDURANCE);
            case WILLPOWER_SURVIVAL -> damageAward(action, StatType.WILLPOWER);
            case COMBO -> action.quantity() < 3 ? List.of()
                    : single(StatType.RAPIDITE, clamp(2 + action.quantity() - 3, 2, 6), action);
            case CONTROLLED_LANDING -> !positiveFinite(action.magnitude()) || action.magnitude() < 5
                    ? List.of()
                    : single(StatType.AGILITY,
                            clamp((int) Math.floor(action.magnitude() - 3), 1, 15), action);
            case BIOME_DISCOVERY -> single(StatType.KEEN_SENSES, 10, action);
            case HOSTILE_KILL -> killAwards(action);
            case EQUIPMENT_CRAFTED -> forgingAward(action);
            case FOOD_COOKED -> action.quantity() <= 0 ? List.of()
                    : single(StatType.COOKING, clamp(action.quantity() * 2, 1, 20), action);
            case POTION_BREWED -> action.quantity() <= 0 ? List.of()
                    : single(StatType.ALCHEMY,
                            clamp(5 + 2 * action.quantity() + Math.max(0, action.secondary()), 1, 15), action);
            case SPELL_CAST -> spellCastAwards(action);
            case BOOK_STUDIED -> bookStudyAward(action);
            case SPELL_INSCRIBED -> spellInscriptionAward(action);
            case MAGIC_DAMAGE_RECEIVED -> damageAward(action, StatType.MAGIC_RESISTANCE);
        };
    }

    private static List<StatXpAward> damageAward(XpAction action, StatType stat) {
        return positiveFinite(action.magnitude())
                ? single(stat, damageXp(action.magnitude()), action)
                : List.of();
    }

    private static List<StatXpAward> killAwards(XpAction action) {
        if (!positiveFinite(action.magnitude())) {
            return List.of();
        }
        List<StatXpAward> awards = new ArrayList<>();
        awards.add(new StatXpAward(StatType.TRACKING,
                clamp((int) Math.ceil(action.magnitude() / 5), 2, 20), action.kind().name()));
        if (action.dangerousTarget()) {
            awards.add(new StatXpAward(StatType.INTIMIDATION,
                    clamp((int) Math.ceil(action.magnitude() / 4), 1, 50), action.kind().name()));
        }
        return List.copyOf(awards);
    }

    private static List<StatXpAward> forgingAward(XpAction action) {
        if (!Double.isFinite(action.magnitude()) || action.magnitude() < 0 || action.quantity() <= 0) {
            return List.of();
        }
        int durabilityUnits = (int) Math.ceil(Math.max(1, action.magnitude()) / 100.0);
        return single(StatType.FORGING,
                clamp(durabilityUnits * action.quantity(), 1, 20), action);
    }

    private static List<StatXpAward> spellCastAwards(XpAction action) {
        int level = action.quantity();
        double manaCost = action.magnitude();
        if (level <= 0 || !Double.isFinite(manaCost)) {
            return List.of();
        }

        List<StatXpAward> awards = new ArrayList<>(3);
        int arcanePower = (int) Math.min(12L, 2L + level);
        int castingSpeed = (int) Math.min(6L, 1L + ((long) level + 1L) / 2L);
        awards.add(new StatXpAward(
                StatType.ARCANE_POWER, arcanePower, action.kind().name()));
        awards.add(new StatXpAward(
                StatType.CASTING_SPEED, castingSpeed, action.kind().name()));

        if (manaCost > 0) {
            long manaUnits = (long) Math.ceil(manaCost / 10.0);
            int manaPool = (int) Math.min(15L, Math.max(1L, manaUnits));
            awards.add(new StatXpAward(
                    StatType.MANA_POOL, manaPool, action.kind().name()));
        }
        return List.copyOf(awards);
    }

    private static List<StatXpAward> bookStudyAward(XpAction action) {
        if (action.quantity() <= 0) {
            return List.of();
        }
        return single(StatType.ERUDITION,
                clamp(action.quantity(), 1, EnchantmentStudyXp.MAX_PER_BOOK), action);
    }

    private static List<StatXpAward> spellInscriptionAward(XpAction action) {
        if (action.quantity() <= 0 || action.secondary() < 0) {
            return List.of();
        }
        long raw = 5L + 2L * action.quantity() + 3L * action.secondary();
        return single(StatType.ERUDITION,
                clamp((int) Math.min(Integer.MAX_VALUE, raw), 5, 30), action);
    }

    private static List<StatXpAward> single(StatType stat, int amount, XpAction action) {
        return List.of(new StatXpAward(stat, amount, action.kind().name()));
    }

    private static int damageXp(double value) {
        return clamp((int) Math.ceil(value * 2), 1, 20);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean positiveFinite(double value) {
        return Double.isFinite(value) && value > 0;
    }
}
