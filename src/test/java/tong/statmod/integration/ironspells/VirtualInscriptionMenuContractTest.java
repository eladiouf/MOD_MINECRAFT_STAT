package tong.statmod.integration.ironspells;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class VirtualInscriptionMenuContractTest {
    @Test
    void virtualMenuKeepsIronInventoryRulesButNotBlockValidity() throws Exception {
        String menu = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/VirtualInscriptionTableMenu.java"));
        String provider = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/VirtualInscriptionMenuProvider.java"));

        assertTrue(menu.contains("extends InscriptionTableMenu"));
        assertTrue(menu.contains("return !player.isRemoved()"));
        assertTrue(provider.contains("ContainerLevelAccess.NULL"));
        assertTrue(provider.contains("statmod.menu.virtual_inscription"));
    }
}
