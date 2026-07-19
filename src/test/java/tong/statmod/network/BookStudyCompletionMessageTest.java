package tong.statmod.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BookStudyCompletionMessageTest {
    @Test
    void normalizesAndValidatesOnlyEnchantedBooks() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/network/BookStudyCompletionMessage.java"));
        assertTrue(source.contains("book.copyWithCount(1)"));
        assertTrue(source.contains("book.is(Items.ENCHANTED_BOOK)"));
        assertTrue(source.contains("buffer.writeItem(message.book)"));
        assertTrue(source.contains("buffer.readItem()"));
        assertFalse(source.contains("TOTEM_OF_UNDYING"));

        String network = Files.readString(Path.of(
                "src/main/java/tong/statmod/network/StatNetwork.java"));
        assertTrue(network.contains(
                "registerMessage(3, BookStudyCompletionMessage.class"));
        assertTrue(network.contains("NetworkDirection.PLAY_TO_CLIENT"));
        assertTrue(network.contains("ClientBookStudyEffects.show(message)"));
        assertTrue(network.contains("sendBookStudyCompletion"));
    }
}
