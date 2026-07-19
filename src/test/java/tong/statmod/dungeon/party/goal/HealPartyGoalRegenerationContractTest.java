package tong.statmod.dungeon.party.goal;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealPartyGoalRegenerationContractTest {
    @Test
    void completeHealerKitUsesBeatableBalance() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/tong/statmod/dungeon/party/goal/HealPartyGoal.java"));

        assertTrue(source.contains("HEAL_COOLDOWN = 200"));
        assertTrue(source.contains("BUFF_INTERVAL = 300"));
        assertTrue(source.contains("SHIELD_INTERVAL = 300"));
        assertTrue(source.contains("5.0f + healer.getMaxHealth() * 0.05f"));
        assertTrue(source.contains("ally.heal(amount * 0.25f)"));
        assertTrue(source.contains("ultCooldown = 600"));
        assertTrue(source.contains("healer.getMaxHealth() * 0.30"));
        assertTrue(source.contains("ally.getMaxHealth() * 0.30"));
        assertTrue(source.contains("ally.heal(ally.getMaxHealth() * 0.2f)"));
        assertTrue(source.contains("MobEffects.REGENERATION, 40, 0, false, true"));
        assertEquals(1, source.split("MobEffects.REGENERATION", -1).length - 1);
        assertTrue(source.contains("MobEffects.ABSORPTION, 100, 0, false, true"));
        assertTrue(source.contains("MobEffects.DAMAGE_BOOST, 160, 0, false, true"));
        assertTrue(source.contains("MobEffects.MOVEMENT_SPEED, 160, 0, false, true"));
        assertTrue(source.contains("MobEffects.DAMAGE_RESISTANCE, 120, 0, false, true"));
        assertEquals(1, source.split("MobEffects.DAMAGE_RESISTANCE", -1).length - 1);
        assertTrue(source.contains("healer.getMaxHealth() * 0.60"));
        assertTrue(source.contains("float bestFrac = 0.75f"));
    }
}
