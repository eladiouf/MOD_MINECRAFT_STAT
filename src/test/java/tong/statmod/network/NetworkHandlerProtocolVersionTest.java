package tong.statmod.network;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkHandlerProtocolVersionTest {
    @Test
    void magicPayloadSchemaUsesProtocolVersionTwo() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java", "tong", "statmod", "network",
                "NetworkHandler.java"));

        assertTrue(source.contains("event.registrar(\"2\")"),
                "the practice-mastery payload schema requires an incompatible protocol version bump");
    }
}
