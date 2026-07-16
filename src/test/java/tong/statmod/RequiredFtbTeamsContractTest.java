package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RequiredFtbTeamsContractTest {
    @Test
    void requiresForgeFtbTeamsAndUsesItsServerApiDirectly() throws Exception {
        String properties = Files.readString(Path.of("gradle.properties"));
        String build = Files.readString(Path.of("build.gradle"));
        String metadata = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));
        String bridge = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ftbteams/FTBTeamsBridge.java"));

        assertTrue(properties.contains("ftb_teams_version=2001.3.2"));
        assertTrue(properties.contains("ftb_library_version=2001.2.13"));
        assertTrue(build.contains("https://maven.ftb.dev/releases"));
        assertTrue(build.contains("dev.ftb.mods:ftb-teams-forge:${ftb_teams_version}"));
        assertTrue(build.contains("dev.ftb.mods:ftb-library-forge:${ftb_library_version}"));
        assertRequired(metadata, "ftbteams", "[2001.3.2,)");
        assertTrue(bridge.contains("import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;"));
        assertTrue(bridge.contains("arePlayersInSameTeam"));
        assertTrue(bridge.contains("return false;"), "manager failure must fail closed");
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
