package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TensuraXpMultiplierTest {

    @Test
    void scalesEpGainByTotalStatLevel() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(0, 6);
        data.setLevel(1, 10);
        data.setLevel(10, 8);

        assertEquals(1.12f, TensuraXpMultiplier.getEpMultiplier(data, "melee_axe"), 0.0001f);
        assertEquals(1.12f, TensuraXpMultiplier.getEpMultiplier(data, "melee_sword"), 0.0001f);
        assertEquals(1.12f, TensuraXpMultiplier.getEpMultiplier(data, "magic_fire"), 0.0001f);
    }
}
