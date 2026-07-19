package tong.statmod.dungeon.ai.living;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DungeonLivingEventPolicyTest {
    @Test void earlyFloorsHaveOnlyOneReadableInhabitant() {
        assertEquals(java.util.List.of(DungeonLivingRole.WOUNDED_SURVIVOR),
                DungeonLivingEventPolicy.safehouseRoles(8));
    }

    @Test void deepSafehousesStayCappedAtFourActors() {
        var roles = DungeonLivingEventPolicy.safehouseRoles(90);
        assertEquals(4, roles.size());
        assertTrue(roles.contains(DungeonLivingRole.PRISONER));
        assertTrue(roles.contains(DungeonLivingRole.WANDERING_MERCHANT));
        assertTrue(roles.contains(DungeonLivingRole.SCAVENGER));
    }

    @Test void hostileSpecialistsUseFixedRoomSlotsAndThresholds() {
        assertEquals(Optional.empty(), DungeonLivingEventPolicy.combatRole(30, 4, 0));
        assertEquals(Optional.of(DungeonLivingRole.RIVAL_EXPLORER),
                DungeonLivingEventPolicy.combatRole(31, 4, 0));
        assertEquals(Optional.of(DungeonLivingRole.RITUALIST),
                DungeonLivingEventPolicy.combatRole(51, 12, 0));
        assertEquals(Optional.of(DungeonLivingRole.ENGINEER),
                DungeonLivingEventPolicy.combatRole(71, 15, 0));
        assertEquals(Optional.empty(), DungeonLivingEventPolicy.combatRole(90, 12, 1));
        assertEquals(Optional.empty(), DungeonLivingEventPolicy.combatRole(90, 15, 3));
    }

    @Test void everyLivingRoleAppearsByFloorOneHundred() {
        EnumSet<DungeonLivingRole> seen = EnumSet.noneOf(DungeonLivingRole.class);
        for (int floor = 1; floor <= 100; floor++) {
            seen.addAll(DungeonLivingEventPolicy.safehouseRoles(floor));
            for (int room = 0; room < 20; room++) {
                for (int ordinal = 0; ordinal < 6; ordinal++) {
                    DungeonLivingEventPolicy.combatRole(floor, room, ordinal).ifPresent(seen::add);
                }
            }
        }
        assertEquals(EnumSet.allOf(DungeonLivingRole.class), seen);
    }
}
