package tong.statmod.progression.xp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tong.statmod.stats.PlayerStats;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

class XpNoticeCalculationTest {
    @Test
    void reportsAcceptedXpAndLevelGainFromBeforeAndAfterSnapshots() {
        PlayerStats stats = new PlayerStats();
        stats.setLevel(StatType.AGILITY, 4);
        stats.addXp(StatType.AGILITY, 240);
        Map<StatType, StatValue> before = stats.snapshot();

        stats.addXp(StatType.AGILITY, 15);

        List<XpProgressNotice> notices = XpNoticeCalculation.from(
                before, stats.snapshot(), Map.of(StatType.AGILITY, 15));

        assertEquals(List.of(new XpProgressNotice(StatType.AGILITY, 15, 5, 1)), notices);
    }

    @Test
    void omitsZeroAwardsAndUsesStableStatOrder() {
        PlayerStats stats = new PlayerStats();
        Map<StatType, StatValue> before = stats.snapshot();
        stats.addXp(StatType.COOKING, 4);
        stats.addXp(StatType.AGILITY, 3);
        EnumMap<StatType, Integer> accepted = new EnumMap<>(StatType.class);
        accepted.put(StatType.COOKING, 4);
        accepted.put(StatType.AGILITY, 3);
        accepted.put(StatType.FORGING, 0);

        List<XpProgressNotice> notices = XpNoticeCalculation.from(
                before, stats.snapshot(), accepted);

        assertEquals(List.of(StatType.AGILITY, StatType.COOKING),
                notices.stream().map(XpProgressNotice::stat).toList());
    }
}
