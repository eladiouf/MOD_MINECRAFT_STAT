package tong.statmod.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

class MobilityAttributeTargetTest {
    @Test
    void exposesExactRegistryAndStatMappings() {
        Map<String, StatType> expected = Map.of(
                "minecraft:generic.attack_speed", StatType.RAPIDITE,
                "epicfight:offhand_attack_speed", StatType.RAPIDITE,
                "minecraft:generic.movement_speed", StatType.AGILITY,
                "puffish_attributes:sprinting_speed", StatType.AGILITY);

        assertEquals(expected, Arrays.stream(MobilityAttributeTarget.values())
                .collect(java.util.stream.Collectors.toMap(
                        target -> target.id().toString(), MobilityAttributeTarget::stat)));
    }

    @Test
    void givesEveryTargetAUniqueStableUuid() {
        assertEquals(4, Arrays.stream(MobilityAttributeTarget.values())
                .map(MobilityAttributeTarget::modifierId)
                .distinct()
                .count());
    }

    @Test
    void mapsTargetsToApprovedBonusKinds() {
        assertEquals(2, Arrays.stream(MobilityAttributeTarget.values())
                .filter(target -> target.bonusKind()
                        == MobilityAttributeTarget.BonusKind.RAPIDITE_ATTACK_SPEED)
                .count());
        assertEquals(1, Arrays.stream(MobilityAttributeTarget.values())
                .filter(target -> target.bonusKind()
                        == MobilityAttributeTarget.BonusKind.AGILITY_MOVEMENT_SPEED)
                .count());
        assertEquals(1, Arrays.stream(MobilityAttributeTarget.values())
                .filter(target -> target.bonusKind()
                        == MobilityAttributeTarget.BonusKind.AGILITY_SPRINTING_SPEED)
                .count());
    }
}
