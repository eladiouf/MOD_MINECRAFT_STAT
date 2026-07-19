package tong.statmod.progression.xp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

class XpAwardCoordinatorTest {
    @Test
    void appliesAllAwardsFromOneActionAsOneResult() {
        PlayerStats stats = new PlayerStats();
        PlayerXpState state = new PlayerXpState();

        XpAwardResult result = XpAwardCoordinator.apply(
                stats, state, List.of(XpAction.kill(120, true)), 0);

        assertTrue(result.changed());
        assertEquals(20, result.accepted().get(StatType.TRACKING));
        assertEquals(30, result.accepted().get(StatType.INTIMIDATION));
        assertEquals(new StatValue(1, 10), stats.get(StatType.TRACKING));
        assertEquals(new StatValue(1, 20), stats.get(StatType.INTIMIDATION));
    }

    @Test
    void ignoresInvalidActions() {
        PlayerStats stats = new PlayerStats();
        XpAwardResult result = XpAwardCoordinator.apply(stats, new PlayerXpState(),
                List.of(XpAction.damage(XpActionKind.MELEE_HEAVY, Double.NaN)), 0);

        assertFalse(result.changed());
        assertTrue(result.accepted().isEmpty());
        assertEquals(new StatValue(0, 0), stats.get(StatType.BRUTE_FORCE));
    }

    @Test
    void reportsOnlyThePartAcceptedByTheLimiter() {
        PlayerStats stats = new PlayerStats();
        PlayerXpState state = new PlayerXpState();
        assertEquals(195, state.acceptXp(StatType.BRUTE_FORCE, 195, 0, null));

        XpAwardResult result = XpAwardCoordinator.apply(stats, state,
                List.of(XpAction.damage(XpActionKind.MELEE_HEAVY, 10)), 1);

        assertTrue(result.changed());
        assertEquals(5, result.accepted().get(StatType.BRUTE_FORCE));
        assertEquals(new StatValue(0, 5), stats.get(StatType.BRUTE_FORCE));
    }

    @Test
    void doesNotReportMaxLevelStatsAsChanged() {
        PlayerStats stats = new PlayerStats();
        stats.setLevel(StatType.BRUTE_FORCE, 100);

        XpAwardResult result = XpAwardCoordinator.apply(stats, new PlayerXpState(),
                List.of(XpAction.damage(XpActionKind.MELEE_HEAVY, 10)), 0);

        assertFalse(result.changed());
        assertTrue(result.accepted().isEmpty());
        assertEquals(new StatValue(100, 0), stats.get(StatType.BRUTE_FORCE));
    }
}
