package tong.statmod.client.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tong.statmod.client.ClientStatsState;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

class StatsScreenModelTest {
    @Test
    void containsEveryStatOnceInFamilyAndEnumOrder() {
        StatsScreenModel model = StatsScreenModel.from(new ClientStatsState(7, Map.of()));

        List<StatType> flattened = model.families().stream()
                .flatMap(family -> family.cards().stream())
                .map(StatsScreenModel.StatCard::type)
                .toList();

        assertEquals(List.of(StatType.values()), flattened);
        assertEquals(5, model.families().size());
        assertEquals(7, model.revision());
    }

    @Test
    void computesBoundedProgressAndMaxState() {
        Map<StatType, StatValue> values = Map.of(
                StatType.AGILITY, new StatValue(0, 5),
                StatType.MANA_POOL, new StatValue(100, 999),
                StatType.COOKING, new StatValue(2, -50));

        StatsScreenModel model = StatsScreenModel.from(new ClientStatsState(3, values));

        assertEquals(0.5, model.card(StatType.AGILITY).progress());
        assertEquals(10, model.card(StatType.AGILITY).requiredXp());
        assertTrue(model.card(StatType.MANA_POOL).maxLevel());
        assertEquals(0, model.card(StatType.MANA_POOL).requiredXp());
        assertEquals(0.0, model.card(StatType.COOKING).progress());
    }

    @Test
    void implementedMagicStatsAreActiveAndEruditionRemainsFoundation() {
        assertEquals(StatDisplayState.ACTIVE,
                StatPresentation.of(StatType.BRUTE_FORCE).state());
        assertEquals(StatDisplayState.ACTIVE,
                StatPresentation.of(StatType.ARCANE_POWER).state());
        assertEquals(StatDisplayState.ACTIVE,
                StatPresentation.of(StatType.CASTING_SPEED).state());
        assertEquals(StatDisplayState.ACTIVE,
                StatPresentation.of(StatType.MANA_POOL).state());
        assertEquals(StatDisplayState.ACTIVE,
                StatPresentation.of(StatType.MAGIC_RESISTANCE).state());
        assertEquals(StatDisplayState.FOUNDATION,
                StatPresentation.of(StatType.ERUDITION).state());
    }
}
