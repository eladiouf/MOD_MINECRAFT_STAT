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
        assertTrue(config.contains("builder.push(\"endurance\")"));
        assertTrue(config.contains("staminaCapacityBonusAt100\", 1.0, 0.0, 5.0"));
        assertTrue(config.contains("staminaRecoveryBonusAt100\", 0.5, 0.0, 5.0"));
        assertTrue(config.contains("staminaCapacityBonusAt100()"));
        assertTrue(config.contains("staminaRecoveryBonusAt100()"));
        assertTrue(config.contains("builder.push(\"mobility\")"));
        assertTrue(config.contains("rapiditeAttackSpeedBonusAt100\", 0.30, 0.0, 2.0"));
        assertTrue(config.contains("agilityMovementSpeedBonusAt100\", 0.20, 0.0, 2.0"));
        assertTrue(config.contains("agilitySprintingSpeedBonusAt100\", 0.10, 0.0, 2.0"));
        assertTrue(config.contains("builder.push(\"magic\")"));
        assertTrue(config.contains("arcanePowerSpellPowerBonusAt100\", 1.00, 0.0, 10.0"));
        assertTrue(config.contains("castingSpeedCastTimeBonusAt100\", 0.30, 0.0, 0.90"));
        assertTrue(config.contains("castingSpeedCooldownBonusAt100\", 0.20, 0.0, 0.90"));
        assertTrue(config.contains("manaPoolCapacityBonusAt100\", 2.00, 0.0, 20.0"));
        assertTrue(config.contains("manaPoolRegenBonusAt100\", 0.50, 0.0, 10.0"));
        assertTrue(config.contains("magicResistanceBonusAt100\", 0.50, 0.0, 0.90"));
        assertTrue(config.contains("arcanePowerSpellPowerBonusAt100()"));
        assertTrue(config.contains("castingSpeedCastTimeBonusAt100()"));
        assertTrue(config.contains("castingSpeedCooldownBonusAt100()"));
        assertTrue(config.contains("manaPoolCapacityBonusAt100()"));
        assertTrue(config.contains("manaPoolRegenBonusAt100()"));
        assertTrue(config.contains("magicResistanceBonusAt100()"));
        assertTrue(config.contains("new CombatScalingRules("));
    }
}
