package tong.statmod.progression.xp;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void neverEmitsDeferredMagicalStats() {
        Set<StatType> deferred = EnumSet.of(
                StatType.ARCANE_POWER, StatType.CASTING_SPEED, StatType.MANA_POOL,
                StatType.ERUDITION, StatType.MAGIC_RESISTANCE, StatType.FIRE_AFFINITY,
                StatType.WATER_AFFINITY, StatType.EARTH_AFFINITY, StatType.AIR_AFFINITY);
        for (XpActionKind kind : XpActionKind.values()) {
            XpAction action = new XpAction(kind, 120, 4, 2, true, null);
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
