package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class StaminaAttributeTargetTest {
    @Test
    void exposesExactOptionalRegistryIds() {
        assertEquals(Set.of(
                        "epicfight:staminar",
                        "epicfight:stamina_regen",
                        "parcool:max_stamina",
                        "parcool:stamina_recovery"),
                Arrays.stream(StaminaAttributeTarget.values())
                        .map(target -> target.id().toString())
                        .collect(Collectors.toSet()));
    }

    @Test
    void givesEveryTargetAUniqueStableUuid() {
        assertEquals(4, Arrays.stream(StaminaAttributeTarget.values())
                .map(StaminaAttributeTarget::modifierId)
                .distinct()
                .count());
    }

    @Test
    void assignsCapacityAndRecoveryBonuses() {
        assertEquals(2, Arrays.stream(StaminaAttributeTarget.values())
                .filter(target -> target.bonusKind()
                        == StaminaAttributeTarget.BonusKind.CAPACITY)
                .count());
        assertEquals(2, Arrays.stream(StaminaAttributeTarget.values())
                .filter(target -> target.bonusKind()
                        == StaminaAttributeTarget.BonusKind.RECOVERY)
                .count());
    }
}
