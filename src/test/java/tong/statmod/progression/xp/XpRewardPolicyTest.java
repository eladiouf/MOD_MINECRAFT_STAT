package tong.statmod.progression.xp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

class XpRewardPolicyTest {
    @Test
    void calculatesConfiguredRewardsAndCaps() {
        assertEquals(20, only(XpRewardPolicy.awards(
                XpAction.damage(XpActionKind.MELEE_HEAVY, 20))).amount());
        assertEquals(11, only(XpRewardPolicy.awards(
                XpAction.damage(XpActionKind.MELEE_BLADE, 5.25))).amount());
        assertEquals(2, only(XpRewardPolicy.awards(XpAction.combo(3))).amount());
        assertEquals(6, only(XpRewardPolicy.awards(XpAction.combo(20))).amount());
        assertEquals(7, only(XpRewardPolicy.awards(XpAction.landing(10.8))).amount());
        assertEquals(10, only(XpRewardPolicy.awards(XpAction.biome())).amount());
        assertEquals(16, only(XpRewardPolicy.awards(XpAction.forging(1561, 1))).amount());
        assertEquals(8, only(XpRewardPolicy.awards(XpAction.cooking(4))).amount());
        assertEquals(10, only(XpRewardPolicy.awards(XpAction.alchemy(2, 1))).amount());
    }

    @Test
    void rejectsInvalidMagnitudes() {
        assertTrue(XpRewardPolicy.awards(
                XpAction.damage(XpActionKind.MELEE_HEAVY, Double.NaN)).isEmpty());
        assertTrue(XpRewardPolicy.awards(
                XpAction.damage(XpActionKind.MELEE_HEAVY, 0)).isEmpty());
        assertTrue(XpRewardPolicy.awards(XpAction.alchemy(0, 0)).isEmpty());
    }

    @Test
    void dangerousKillRewardsTrackingAndIntimidation() {
        List<StatXpAward> awards = XpRewardPolicy.awards(XpAction.kill(120, true));
        assertEquals(2, awards.size());
        assertEquals(20, amountFor(awards, StatType.TRACKING));
        assertEquals(30, amountFor(awards, StatType.INTIMIDATION));
    }

    @Test
    void spellCastRewardsCoreMagicStatsFromOriginalEffort() {
        List<StatXpAward> minimum = XpRewardPolicy.awards(XpAction.spellCast(1, 0));
        assertEquals(2, minimum.size());
        assertEquals(3, amountFor(minimum, StatType.ARCANE_POWER));
        assertEquals(2, amountFor(minimum, StatType.CASTING_SPEED));

        List<StatXpAward> ordinary = XpRewardPolicy.awards(XpAction.spellCast(4, 21));
        assertEquals(3, ordinary.size());
        assertEquals(6, amountFor(ordinary, StatType.ARCANE_POWER));
        assertEquals(3, amountFor(ordinary, StatType.CASTING_SPEED));
        assertEquals(3, amountFor(ordinary, StatType.MANA_POOL));

        List<StatXpAward> capped = XpRewardPolicy.awards(
                XpAction.spellCast(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(12, amountFor(capped, StatType.ARCANE_POWER));
        assertEquals(6, amountFor(capped, StatType.CASTING_SPEED));
        assertEquals(15, amountFor(capped, StatType.MANA_POOL));
    }

    @Test
    void spellCastRejectsInvalidLevelAndNeverRewardsDeferredMagicStats() {
        assertTrue(XpRewardPolicy.awards(XpAction.spellCast(0, 20)).isEmpty());
        assertTrue(XpRewardPolicy.awards(XpAction.spellCast(-1, 20)).isEmpty());

        List<StatXpAward> negativeCost = XpRewardPolicy.awards(
                XpAction.spellCast(3, -10));
        assertEquals(2, negativeCost.size());
        assertFalse(negativeCost.stream().map(StatXpAward::stat)
                .anyMatch(stat -> stat == StatType.MANA_POOL
                        || stat == StatType.ERUDITION
                        || stat == StatType.MAGIC_RESISTANCE));
    }

    @Test
    void onlyExplicitKnowledgeAndMagicDefenseActionsRewardDeferredStats() {
        assertEquals(85, amountFor(
                XpRewardPolicy.awards(XpAction.bookStudied(85)), StatType.ERUDITION));
        assertEquals(160, amountFor(
                XpRewardPolicy.awards(XpAction.bookStudied(999)), StatType.ERUDITION));
        assertEquals(18, amountFor(
                XpRewardPolicy.awards(XpAction.spellInscribed(2, 3)), StatType.ERUDITION));
        assertEquals(15, amountFor(
                XpRewardPolicy.awards(XpAction.magicDamageReceived(7.1)),
                StatType.MAGIC_RESISTANCE));

        Set<StatType> deferred = EnumSet.of(
                StatType.ERUDITION, StatType.MAGIC_RESISTANCE);
        for (XpActionKind kind : XpActionKind.values()) {
            if (kind == XpActionKind.BOOK_STUDIED
                    || kind == XpActionKind.SPELL_INSCRIBED
                    || kind == XpActionKind.MAGIC_DAMAGE_RECEIVED) {
                continue;
            }
            XpAction action = kind == XpActionKind.SPELL_CAST
                    ? XpAction.spellCast(4, 20)
                    : new XpAction(kind, 120, 4, 2, true, null);
            assertTrue(XpRewardPolicy.awards(action).stream()
                    .map(StatXpAward::stat).noneMatch(deferred::contains));
        }
    }

    private static StatXpAward only(List<StatXpAward> awards) {
        assertEquals(1, awards.size());
        return awards.get(0);
    }

    private static int amountFor(List<StatXpAward> awards, StatType stat) {
        return awards.stream().filter(award -> award.stat() == stat)
                .findFirst().orElseThrow().amount();
    }
}
