package tong.statmod;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EventBusRegistrationTest {
    private static final Path MAIN_SOURCES = Path.of("src", "main", "java");
    private static final Pattern MANUAL_REGISTRATION = Pattern.compile(
            "NeoForge\\.EVENT_BUS\\.register\\(([A-Za-z0-9_]+)\\.class\\)");

    @Test
    void gameEventHandlersAreNotRegisteredBothManuallyAndByAnnotation() throws IOException {
        Set<String> manuallyRegisteredClasses = manuallyRegisteredClasses();
        Set<String> duplicateRegistrations = new HashSet<>();

        try (Stream<Path> stream = Files.walk(MAIN_SOURCES)) {
            for (Path sourcePath : stream.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(sourcePath);
                if (!source.contains("@EventBusSubscriber")) {
                    continue;
                }

                String className = sourcePath.getFileName().toString().replace(".java", "");
                if (manuallyRegisteredClasses.contains(className)) {
                    duplicateRegistrations.add(className);
                }
            }
        }

        assertTrue(duplicateRegistrations.isEmpty(),
                "A handler must use either @EventBusSubscriber or manual NeoForge registration, not both: "
                        + duplicateRegistrations);
    }

    private static Set<String> manuallyRegisteredClasses() throws IOException {
        Set<String> classes = new HashSet<>();

        try (Stream<Path> stream = Files.walk(MAIN_SOURCES)) {
            for (Path sourcePath : stream.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = MANUAL_REGISTRATION.matcher(Files.readString(sourcePath));
                while (matcher.find()) {
                    classes.add(matcher.group(1));
                }
            }
        }

        return classes;
    }
}
