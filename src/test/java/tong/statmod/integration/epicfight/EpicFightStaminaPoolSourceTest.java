package tong.statmod.integration.epicfight;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EpicFightStaminaPoolSourceTest {
    private static final Path COMPAT_SOURCE = Path.of(
            "src", "main", "java", "tong", "statmod", "integration", "epicfight", "EpicFightCompat.java");

    @Test
    void epicFightPatchIsSyncedToLargeStatModPoolEveryTick() throws IOException {
        String source = Files.readString(COMPAT_SOURCE);

        assertTrue(source.contains("EpicFightAttributes.MAX_STAMINA"));
        assertTrue(source.contains("ResourceIds.MAX_STAMINA"));
        assertTrue(source.contains("AttributeModifier.Operation.ADD_VALUE"));
        assertTrue(source.indexOf("syncEpicFightStaminaDisplay(player);")
                < source.indexOf("if (player.tickCount % 20 != 0) return;"));
    }

    @Test
    void clientPatchIsAlsoForcedToStatModPoolBeforeClientReturn() throws IOException {
        String source = Files.readString(COMPAT_SOURCE);

        assertTrue(source.indexOf("syncEpicFightStaminaDisplay(player);")
                < source.indexOf("if (player.level().isClientSide) return;"));
    }

    @Test
    void clientMaxStaminaCacheCannotHideServerAttributeSync() throws IOException {
        String source = Files.readString(COMPAT_SOURCE);

        assertTrue(source.contains("lastServerMaxStamina"));
        assertTrue(source.contains("lastClientMaxStamina"));
    }

    @Test
    void clientPredictionDoesNotBlockStatModStaminaSkills() throws IOException {
        String source = Files.readString(COMPAT_SOURCE);

        assertTrue(source.contains("if (event.getResourceType() == Skill.Resource.STAMINA)"));
        assertTrue(source.contains("if (player.level().isClientSide) {"));
        assertTrue(source.contains("event.setAmount(0.0f);"));
        assertTrue(source.indexOf("if (event.getResourceType() == Skill.Resource.STAMINA)")
                < source.indexOf("if (player.level().isClientSide) {"));
    }
}
