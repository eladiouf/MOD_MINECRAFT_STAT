package tong.statmod.integration.sdmshop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MagicShopCommandsContractTest {
    @Test
    void exposesPermissionFourRegenerationCommand() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/sdmshop/MagicShopCommands.java"));
        assertTrue(source.contains("Commands.literal(\"statmod\")"));
        assertTrue(source.contains("Commands.literal(\"shop\")"));
        assertTrue(source.contains("Commands.literal(\"regenerate\")"));
        assertTrue(source.contains("source.hasPermission(4)"));
        assertTrue(source.contains("MagicShopGenerator.regenerate(source.getServer())"));
        assertTrue(source.contains("result.scrollEntries()"));
        assertTrue(source.contains("result.saleEntries()"));
    }

    @Test
    void commandIsRegisteredOnTheExistingForgeCommandEvent() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/event/PlayerStatsEvents.java"));
        assertTrue(source.contains("MagicShopCommands.register(event.getDispatcher())"));
    }
}
