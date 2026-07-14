package tong.statmod.economy;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verrou de régression : la devise SDM créditée par le Banquier (conversion des billets FDP)
 * DOIT être la même que celle enregistrée et utilisée par le shop SDM.
 *
 * <p>Historique : le pont lisait {@code Config.getShopCurrencyName()} (valeur persistée,
 * surchargeable), qui pouvait rester bloquée sur l'ancien nom {@code dungeon_coins} tandis que
 * le shop enregistrait {@code FDP_cfa}. Résultat : {@code addCurrencyValue} sur une devise non
 * enregistrée échouait en silence → la conversion billets → argent ne créditait rien.
 */
class FdpCurrencyIdentityTest {

    private static final Path BRIDGE = Path.of("src", "main", "java", "tong", "statmod",
            "integration", "sdm", "SDMEconomyBridge.java");
    private static final Path INITIALIZER = Path.of("src", "main", "java", "tong", "statmod",
            "integration", "sdm", "SDMShopDatabaseInitializer.java");

    @Test
    void currencyIdIsTheCanonicalFdpName() {
        assertEquals("FDP_cfa", FdpDenomination.CURRENCY_ID);
    }

    @Test
    void economyBridgeUsesTheCanonicalCurrencyAndNotAStaleConfigValue() throws IOException {
        String source = Files.readString(BRIDGE);
        assertTrue(source.contains("FdpDenomination.CURRENCY_ID"),
                "le pont SDM doit créditer/lire la devise canonique FdpDenomination.CURRENCY_ID");
        assertFalse(source.contains("getShopCurrencyName"),
                "le pont ne doit plus lire le nom de devise depuis le config persisté (source de désync)");
    }

    @Test
    void shopInitializerRegistersTheCanonicalCurrency() throws IOException {
        String source = Files.readString(INITIALIZER);
        assertTrue(source.contains("FdpDenomination.CURRENCY_ID"),
                "l'initialiseur du shop doit enregistrer la même devise canonique que le pont");
    }
}
