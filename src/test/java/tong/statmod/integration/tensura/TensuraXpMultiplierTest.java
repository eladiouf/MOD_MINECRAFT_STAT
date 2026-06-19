package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TensuraXpMultiplierTest {

    @Test
    void scalesMartialEpGainByRelevantPrimaryStat() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(0, 6);
        data.setLevel(1, 10);
        data.setLevel(10, 8);
        data.setLevel(7, 4);
        data.setLevel(13, 2);

        assertEquals(1.30f, TensuraXpMultiplier.getEpMultiplier(data, "melee_axe"), 0.0001f);
        assertEquals(1.50f, TensuraXpMultiplier.getEpMultiplier(data, "melee_sword"), 0.0001f);
    }

    @Test
    void scalesMagicEpGainByPrimaryAndSecondarySpellStats() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(10, 8);
        data.setLevel(7, 4);
        data.setLevel(22, 4);
        data.setLevel(13, 2);
        data.setLevel(8, 6);
        data.setLevel(11, 2);
        data.setLevel(14, 10);
        data.setLevel(15, 12);

        assertEquals(1.50f, TensuraXpMultiplier.getEpMultiplier(data, "magic_fire"), 0.0001f);
        assertEquals(1.85f, TensuraXpMultiplier.getEpMultiplier(data, "tensura:healing_rain"), 0.0001f);
        assertEquals(1.70f, TensuraXpMultiplier.getEpMultiplier(data, "tensura:analyze"), 0.0001f);
        assertEquals(1.90f, TensuraXpMultiplier.getEpMultiplier(data, "tensura:maximum_magic_bullet"), 0.0001f);
    }

    @Test
    void unknownActionsDoNotInventGlobalBonus() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(0, 40);
        data.setLevel(1, 40);

        assertEquals(1.0f, TensuraXpMultiplier.getEpMultiplier(data, "unknown_action"), 0.0001f);
    }
}
