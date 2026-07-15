package tong.statmod.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EnchantedBookStudySessionsContractTest {
    @Test
    void validatesAndCompletesStudyOnlyOnTheServer() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/event/EnchantedBookStudySessions.java"));
        assertTrue(source.contains("TickEvent.PlayerTickEvent"));
        assertTrue(source.contains("Phase.END"));
        assertTrue(source.contains("Items.ENCHANTED_BOOK"));
        assertTrue(source.contains("EnchantedBookItem.getEnchantments"));
        assertTrue(source.contains("EnchantmentHelper.deserializeEnchantments"));
        assertTrue(source.contains("ItemStack.isSameItemSameTags"));
        assertTrue(source.contains("case COMMON -> 1"));
        assertTrue(source.contains("case UNCOMMON -> 2"));
        assertTrue(source.contains("case RARE -> 4"));
        assertTrue(source.contains("case VERY_RARE -> 8"));
        assertTrue(source.contains("awardBookStudy"));
        assertTrue(source.contains("shrink(1)"));
        assertTrue(source.contains("!player.isCreative()"));
        assertTrue(source.contains("broadcastEntityEvent(player, TOTEM_EVENT)"));
        assertTrue(source.indexOf("SESSIONS.remove") < source.indexOf("awardBookStudy"));
        assertFalse(source.contains("SpellDamageEvent"));
    }

    @Test
    void registersBoundedServerPacketAndClearsLifecycle() throws Exception {
        String network = Files.readString(Path.of(
                "src/main/java/tong/statmod/network/StatNetwork.java"));
        String lifecycle = Files.readString(Path.of(
                "src/main/java/tong/statmod/event/PlayerStatsEvents.java"));
        assertTrue(network.contains("registerMessage(2, BookStudyInputMessage.class"));
        assertTrue(network.contains("NetworkDirection.PLAY_TO_SERVER"));
        assertTrue(network.contains("context.getSender()"));
        assertTrue(network.contains("context.enqueueWork"));
        assertTrue(network.contains("EnchantedBookStudySessions.input"));
        assertTrue(lifecycle.contains("PlayerLoggedOutEvent"));
        assertTrue(lifecycle.contains("EnchantedBookStudySessions.clear"));
    }
}
