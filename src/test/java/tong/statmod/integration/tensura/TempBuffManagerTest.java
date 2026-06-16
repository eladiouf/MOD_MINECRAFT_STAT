package tong.statmod.integration.tensura;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TempBuffManagerTest {

    @Test
    void awakeningBuffAppliesAndExpires() {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        TempBuffManager.grantAwakeningBuff(playerId, 100);

        assertEquals(10, TempBuffManager.getAwakeningBonus(playerId, 110));
        assertEquals(0, TempBuffManager.getAwakeningBonus(playerId, 100 + 20 * 30 + 1));
    }
}
