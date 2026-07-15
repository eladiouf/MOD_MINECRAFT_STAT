package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IronDependencyContractTest {
    @Test
    void pinsDeobfuscatedCompileOnlyIronApiWithoutEmbeddingIt() throws Exception {
        String properties = Files.readString(Path.of("gradle.properties"));
        String build = Files.readString(Path.of("build.gradle"));
        String metadata = Files.readString(
                Path.of("src/main/resources/META-INF/mods.toml"));

        assertTrue(properties.contains(
                "irons_spellbooks_version=1.20.1-3.16.2"));
        assertTrue(build.contains("https://api.modrinth.com/maven"));
        assertTrue(build.contains("includeGroup 'maven.modrinth'"));
        assertTrue(build.contains(
                "compileOnly fg.deobf(\"maven.modrinth:irons-spells-n-spellbooks:${irons_spellbooks_version}\")"));
        assertTrue(metadata.contains("modId=\"irons_spellbooks\""));
        assertTrue(metadata.contains("mandatory=true"));
        assertTrue(metadata.contains("versionRange=\"[1.20.1-3.16.2,)\""));
    }
}
