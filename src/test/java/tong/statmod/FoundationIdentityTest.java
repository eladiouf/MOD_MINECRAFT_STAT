package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FoundationIdentityTest {
    @Test
    void exposesStableModIdentity() {
        assertEquals("statmod", StatMod.MOD_ID);
        assertEquals("STAT Mod", StatMod.MOD_NAME);
    }
}
