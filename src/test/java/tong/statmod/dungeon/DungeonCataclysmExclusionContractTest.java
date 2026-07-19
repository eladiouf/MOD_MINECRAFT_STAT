package tong.statmod.dungeon;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DungeonCataclysmExclusionContractTest {
    @Test
    void activeDungeonContentNeverReferencesCataclysmEntities() throws Exception {
        List<Path> roots = List.of(
                Path.of("src/main/java/tong/statmod/config"),
                Path.of("src/main/java/tong/statmod/dungeon"),
                Path.of("src/main/resources/data/statmod"));
        List<String> offenders = new ArrayList<>();
        for (Path root : roots) {
            try (var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    try {
                        if (Files.readString(path).toLowerCase().contains("cataclysm:")) {
                            offenders.add(path.toString());
                        }
                    } catch (java.io.IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                });
            }
        }
        assertTrue(offenders.isEmpty(), "Cataclysm dungeon references: " + offenders);
    }
}
