package tong.statmod.skills;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.*;

class StatPassiveSkillTest {

    @Test
    void allStatTypesHaveACaseInApplyEffect() {
        for (StatType stat : StatType.values()) {
            assertDoesNotThrow(
                () -> StatPassiveSkill.validateStatHasCase(stat),
                "Stat " + stat.name() + " doit avoir un case dans applyEffect()"
            );
        }
    }

    @Test
    void removeEffectCoversAllAttributeModifiers() {
        var removed = StatPassiveSkill.getRemoveEffectAttributes();
        assertTrue(removed.contains("MAX_HEALTH"),           "MAX_HEALTH must be removed");
        assertTrue(removed.contains("JUMP_STRENGTH"),        "JUMP_STRENGTH must be removed");
        assertTrue(removed.contains("ARMOR_TOUGHNESS"),      "ARMOR_TOUGHNESS must be removed");
        assertTrue(removed.contains("KNOCKBACK_RESISTANCE"), "KNOCKBACK_RESISTANCE must be removed");
    }
}
