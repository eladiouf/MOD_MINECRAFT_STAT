package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RequiredShopDependenciesContractTest {
    @Test
    void buildUsesPinnedSdmShopApi() throws Exception {
        String gradle = Files.readString(Path.of("build.gradle"));
        String properties = Files.readString(Path.of("gradle.properties"));
        assertTrue(gradle.contains("maven.modrinth:sdm-shop:${sdmshop_version}"));
        assertTrue(properties.contains("sdmshop_version=1.20.1-7.2.2"));
        assertTrue(properties.contains("sdmeconomy_version=2.2.0"));
    }

    @Test
    void metadataRequiresShopAndEconomyOnBothSides() throws Exception {
        String metadata = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));
        assertRequired(metadata, "sdmshop", "[1.20.1-7.2.2,)");
        assertRequired(metadata, "sdmeconomy", "[2.2.0,)");
    }

    private static void assertRequired(String metadata, String modId, String version) {
        int start = metadata.indexOf("modId=\"" + modId + "\"");
        assertTrue(start >= 0, modId);
        String block = metadata.substring(start, Math.min(metadata.length(), start + 180));
        assertTrue(block.contains("mandatory=true"), modId);
        assertTrue(block.contains("versionRange=\"" + version + "\""), modId);
        assertTrue(block.contains("side=\"BOTH\""), modId);
    }
}
