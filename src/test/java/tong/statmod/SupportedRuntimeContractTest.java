package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SupportedRuntimeContractTest {
    @Test
    void recordsExactPlatformAndHonestOptionalIntegrationStatus() throws IOException {
        String record = Files.readString(Path.of(
                "docs/compatibility/forge-1.20.1-supported-runtime.md"));

        assertTrue(record.contains("Minecraft | 1.20.1 | verified"));
        assertTrue(record.contains("Forge | 47.4.10 | verified"));
        assertTrue(record.contains("Java | 17 | verified"));
        assertTrue(record.contains("Iron's Spells 'n Spellbooks | 3.16.2 | prepared"));
        assertTrue(record.contains("Epic Fight | unpinned | untested"));
        assertTrue(record.contains("Tensura | excluded | unsupported"));
    }
}
