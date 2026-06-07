package tong.statmod.skills;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.*;

class StatPassiveSkillTest {

    @Test
    void allStatTypeValuesAreNonNull() {
        for (StatType stat : StatType.values()) {
            assertDoesNotThrow(
                () -> StatPassiveSkill.assertStatIsNonNull(stat),
                "StatType." + stat.name() + " should be a valid non-null enum constant"
            );
        }
    }

    @Test
    void removeEffectCoversAllAttributeModifiers() {
        var removed = StatPassiveSkill.getRemoveEffectAttributes();
        assertTrue(removed.contains("ATTACK_DAMAGE"),        "ATTACK_DAMAGE must be removed");
        assertTrue(removed.contains("ATTACK_SPEED"),         "ATTACK_SPEED must be removed");
        assertTrue(removed.contains("MOVEMENT_SPEED"),       "MOVEMENT_SPEED must be removed");
        assertTrue(removed.contains("ARMOR"),                "ARMOR must be removed");
        assertTrue(removed.contains("MAX_HEALTH"),           "MAX_HEALTH must be removed");
        assertTrue(removed.contains("JUMP_STRENGTH"),        "JUMP_STRENGTH must be removed");
        assertTrue(removed.contains("ARMOR_TOUGHNESS"),      "ARMOR_TOUGHNESS must be removed");
        assertTrue(removed.contains("KNOCKBACK_RESISTANCE"), "KNOCKBACK_RESISTANCE must be removed");
    }
}
