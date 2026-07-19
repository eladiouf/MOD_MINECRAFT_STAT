package tong.statmod.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CraftingXpEventsContractTest {
    @Test
    void usesExactForgeProvisioningEventsAndPublicTags() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/tong/statmod/event/CraftingXpEvents.java"));

        assertTrue(source.contains("PlayerEvent.ItemCraftedEvent"));
        assertTrue(source.contains("PlayerEvent.ItemSmeltedEvent"));
        assertTrue(source.contains(
                "net.minecraftforge.event.brewing.PlayerBrewedPotionEvent"));
        assertTrue(source.contains("StatItemTags.FORGEABLE_EQUIPMENT"));
        assertTrue(source.contains("isEdible"));
        assertTrue(source.contains("PotionUtils.getMobEffects"));
        assertTrue(source.contains("XpAwardService.award"));
        assertFalse(source.contains("StatType."));
    }
}
