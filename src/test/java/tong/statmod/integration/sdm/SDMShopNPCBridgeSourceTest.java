package tong.statmod.integration.sdm;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SDMShopNPCBridgeSourceTest {

    private static final Path BRIDGE = Path.of("src", "main", "java", "tong", "statmod",
            "integration", "sdm", "SDMShopNPCBridge.java");

    @Test
    void dungeonExchangerIsExcludedFromShopClicksAndAutomaticTagging() throws IOException {
        String source = Files.readString(BRIDGE);
        String guard = "getPersistentData().getBoolean(DungeonExchanger.TAG)";

        assertTrue(source.indexOf(guard) >= 0,
                "le bridge SDM doit reconnaître explicitement le PNJ changeur");
        assertTrue(source.indexOf(guard) != source.lastIndexOf(guard),
                "le changeur doit être exclu au clic et lors du tag automatique");
    }
}
