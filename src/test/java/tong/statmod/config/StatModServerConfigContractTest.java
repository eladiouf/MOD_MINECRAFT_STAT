package tong.statmod.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StatModServerConfigContractTest {
    @Test
    void registersServerConfigAndDefinesEveryApprovedKey() throws Exception {
        String entrypoint = Files.readString(Path.of("src/main/java/tong/statmod/StatMod.java"));
        String config = Files.readString(Path.of(
                "src/main/java/tong/statmod/config/StatModServerConfig.java"));

        assertTrue(entrypoint.contains("ModConfig.Type.SERVER"));
        assertTrue(entrypoint.contains("StatModServerConfig.SPEC"));
        assertTrue(config.contains("weaponDamageBase"));
        assertTrue(config.contains("weaponDamageScale"));
        assertTrue(config.contains("weaponDamageExponent"));
        assertTrue(config.contains("physicalResistanceCap"));
        assertTrue(config.contains("physicalEnduranceCap"));
        assertTrue(config.contains("new CombatScalingRules("));
    }
}
