package tong.statmod.integration.bountiful;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BountifulFdpResourcesTest {
    private static final List<Long> VALUES = List.of(50L, 100L, 200L, 500L, 1000L, 2000L, 5000L, 10000L);

    @Test
    void poolContainsEveryPhysicalFdpDenomination() throws Exception {
        Path pool = Path.of("src/main/resources/data/bountiful/bounty_pools/statmod/fdp_rewards.json");
        String json = Files.readString(pool);
        assertTrue(json.contains("\"currency\": true"));
        for (long value : VALUES) {
            assertTrue(json.contains("statmod:fdp_" + (value < 1000 ? "coin_" : "note_") + value));
            assertTrue(json.contains("\"unitWorth\":" + value + ".0"));
        }
    }

    @Test
    void everyStandardDecreeIncludesFdpRewards() throws Exception {
        Path decrees = Path.of("src/main/resources/data/bountiful/bounty_decrees/bountiful");
        try (var paths = Files.list(decrees)) {
            var files = paths.filter(p -> p.toString().endsWith(".json")).toList();
            assertTrue(files.size() >= 12);
            for (Path file : files) assertTrue(Files.readString(file).contains("fdp_rewards"), file.toString());
        }
    }
}
