package tong.statmod.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ClientBookStudyInputContractTest {
    @Test
    void reportsHeldUseStateWithoutRewardData() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/client/ClientBookStudyInput.java"));
        assertTrue(source.contains("keyUse.isDown()"));
        assertTrue(source.contains("Items.ENCHANTED_BOOK"));
        assertTrue(source.contains("HEARTBEAT_INTERVAL = 5"));
        assertTrue(source.contains("Action.BEGIN"));
        assertTrue(source.contains("Action.HEARTBEAT"));
        assertTrue(source.contains("Action.RELEASE"));
        assertTrue(source.contains("minecraft.getConnection() == null"));
        assertTrue(source.contains("resetWithoutPacket()"));
        assertFalse(source.contains("EnchantmentHelper"));
        assertFalse(source.contains("EnchantedBookItem.getEnchantments"));
        assertFalse(source.contains("awardedXp"));
    }
}
