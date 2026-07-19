package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PlayerAttributeEffectsWiringTest {
    @Test
    void refreshesBeforeLifecycleAndCommandSnapshots() throws Exception {
        String events = source("event/PlayerStatsEvents.java");
        String commands = source("command/StatsCommands.java");

        assertTrue(events.contains("PlayerAttributeEffects.refresh(serverPlayer)"));
        assertTrue(events.indexOf("PlayerAttributeEffects.refresh(serverPlayer)")
                < events.indexOf("StatNetwork.sendSnapshot(serverPlayer)"));
        assertTrue(commands.contains("PlayerAttributeEffects.refresh(target)"));
        assertTrue(commands.indexOf("PlayerAttributeEffects.refresh(target)")
                < commands.indexOf("StatNetwork.sendSnapshot(target)"));
    }

    @Test
    void automaticXpRefreshesOnlyAfterRelevantAttributeLevelsChange() throws Exception {
        String xp = source("progression/xp/XpAwardService.java");

        assertTrue(xp.contains("AttributeEffectLevels beforeEffects"));
        assertTrue(xp.indexOf("AttributeEffectLevels beforeEffects")
                < xp.indexOf("XpAwardCoordinator.apply"));
        assertTrue(xp.contains("AttributeEffectLevels afterEffects"));
        assertTrue(xp.contains("!afterEffects.equals(beforeEffects)"));
        assertTrue(xp.contains("PlayerAttributeEffects.refresh(player)"));
    }

    private static String source(String relativePath) throws Exception {
        return Files.readString(Path.of("src/main/java/tong/statmod", relativePath));
    }
}
