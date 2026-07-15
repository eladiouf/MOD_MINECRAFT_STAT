package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RequiredProviderMetadataTest {
    @Test
    void requiresAuditedEpicFightPuffishAndIronVersions() throws Exception {
        String metadata = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));

        assertRequired(metadata, "epicfight", "[20.14.17,)");
        assertRequired(metadata, "puffish_attributes", "[0.8.2,)");
        assertRequired(metadata, "irons_spellbooks", "[3.16.2,)");
    }

    private static void assertRequired(String metadata, String modId, String versionRange) {
        int start = metadata.indexOf("modId=\"" + modId + "\"");
        assertTrue(start >= 0, "missing dependency " + modId);
        int next = metadata.indexOf("[[dependencies.", start);
        String block = next < 0 ? metadata.substring(start) : metadata.substring(start, next);
        assertTrue(block.contains("mandatory=true"));
        assertTrue(block.contains("versionRange=\"" + versionRange + "\""));
        assertTrue(block.contains("ordering=\"AFTER\""));
        assertTrue(block.contains("side=\"BOTH\""));
    }
}
