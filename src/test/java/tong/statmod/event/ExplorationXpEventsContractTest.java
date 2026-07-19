package tong.statmod.event;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ExplorationXpEventsContractTest {
    @Test
    void samplesSafeLandingsAndFirstBiomeDiscoveries() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/tong/statmod/event/ExplorationXpEvents.java"));

        assertTrue(source.contains("TickEvent.PlayerTickEvent"));
        assertTrue(source.contains("TickEvent.Phase.END"));
        assertTrue(source.contains("gameTime % 20"));
        assertTrue(source.contains("finishLanding"));
        assertTrue(source.contains("tryAgility"));
        assertTrue(source.contains("hasDiscoveredBiome"));
        assertTrue(source.contains("markBiomeDiscovered"));
        assertTrue(source.contains("isCreative"));
        assertTrue(source.contains("isSpectator"));
        assertTrue(source.contains("isFallFlying"));
        assertTrue(source.contains("PlayerChangedDimensionEvent"));
        assertTrue(source.contains("EntityTeleportEvent"));
        assertTrue(source.contains("clearFall"));
    }
}
