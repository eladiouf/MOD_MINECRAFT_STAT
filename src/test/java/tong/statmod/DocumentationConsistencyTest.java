package tong.statmod;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;
import tong.statmod.stats.StatType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationConsistencyTest {
    @Test
    void wikiStatsAndPerksDeclareRuntimeCounts() throws IOException {
        String stats = Files.readString(Path.of("docs", "wiki", "stats.md"));
        String perks = Files.readString(Path.of("docs", "wiki", "perks.md"));

        assertTrue(stats.contains(StatType.values().length + " stats"),
                "docs/wiki/stats.md must declare the runtime stat count");
        assertTrue(perks.contains(Perk.values().length + " perks"),
                "docs/wiki/perks.md must declare the runtime perk count");
    }
}
