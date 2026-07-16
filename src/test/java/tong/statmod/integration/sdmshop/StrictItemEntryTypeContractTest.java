package tong.statmod.integration.sdmshop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StrictItemEntryTypeContractTest {
    @Test
    void requiresExactNbtAndRejectsDamagedVariants() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/sdmshop/StrictItemEntryType.java"));
        assertTrue(source.contains("extends ItemEntryType"));
        assertTrue(source.contains("this.strictNbt = true"));
        assertTrue(source.contains("this.ignoreDamage = false"));
    }
}
