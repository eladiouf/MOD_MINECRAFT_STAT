package tong.statmod.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientStaminaCacheTest {
    @Test
    void updateClampsNegativeAndNonFiniteValues() {
        ClientStaminaCache.reset();
        ClientStaminaCache.update(-5.0f, Float.NaN, true);

        assertEquals(0.0f, ClientStaminaCache.getCurrentStamina(), 0.0001f);
        assertEquals(0.0f, ClientStaminaCache.getFatigueDebt(), 0.0001f);
        assertTrue(ClientStaminaCache.isMeditating());
    }
}
