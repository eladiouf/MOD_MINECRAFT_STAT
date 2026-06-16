package tong.statmod.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemKeywordReloadListenerMixinTest {
    @Test
    void epicFightPatternInjectionUsesMutableBackingMap() throws IOException {
        Path mixinSource = Path.of("src", "main", "java", "tong", "statmod", "mixin", "ItemKeywordReloadListenerMixin.java");
        String source = Files.readString(mixinSource);

        assertFalse(source.contains("ItemKeywordReloadListener.getRegexes()"));
        assertTrue(source.contains("getDeclaredField(\"REGEXES\")"));
    }
}
