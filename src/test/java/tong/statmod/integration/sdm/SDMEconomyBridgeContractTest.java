package tong.statmod.integration.sdm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SDMEconomyBridgeContractTest {
    @Test
    void targetsTheInstalledVersionTwoApiAndDefaultCurrency() throws Exception {
        String source = source();
        assertTrue(source.contains("net.sixik.sdmeconomy.api.EconomyAPI"));
        assertTrue(source.contains("getPlayerCurrencyServerData"));
        assertTrue(source.contains("addCurrencyValue"));
        assertTrue(source.contains("setCurrencyValue"));
        assertTrue(source.contains("getBalance"));
        assertTrue(source.contains("sdm_coin"));
        assertFalse(source.contains("FDP_cfa"));
        assertFalse(source.contains("economyData.CurrencyPlayerData"));
    }

    @Test
    void acceptsOnlySuccessfulMutationsThenSavesAndSynchronizes() throws Exception {
        String source = source();
        assertTrue(source.contains("isSuccess"));
        assertTrue(source.contains("savePlayerData"));
        assertTrue(source.contains("syncPlayer"));
        assertTrue(source.contains("ensureStartingBalance"));
        assertTrue(source.contains("markShopStartingBalanceReceived"));
    }

    private static String source() throws Exception {
        return Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/sdm/SDMEconomyBridge.java"));
    }
}
