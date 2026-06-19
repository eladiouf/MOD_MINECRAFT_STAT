package tong.statmod.integration.elementals;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementalsCompatTest {
    private static final class FakeRuntime implements ElementalsRuntimePort {
        private EnumSet<ElementalBranch> allowedBranches = EnumSet.noneOf(ElementalBranch.class);

        @Override
        public void setAllowedBranches(EnumSet<ElementalBranch> branches) {
            allowedBranches = branches == null
                    ? EnumSet.noneOf(ElementalBranch.class)
                    : EnumSet.copyOf(branches);
        }
    }

    @Test
    void awakeningElfAddsAirAndWaterAsAwakenedBranches() {
        FakeRuntime runtime = new FakeRuntime();
        ElementalsMageData data = new ElementalsMageData();
        MageRaceProfile profile = ElementalsRaceAffinity.resolve("tensura:elf");

        ElementalsCompat.reconcileMageState(
                profile,
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                index -> switch (index) {
                    case 7, 12, 13 -> 12;
                    default -> 0;
                },
                Set.of(),
                data,
                runtime
        );

        assertTrue(data.mageAwakened());
        assertEquals(EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER), runtime.allowedBranches);
    }

    @Test
    void unsupportedRacesLoseManagedBranches() {
        FakeRuntime runtime = new FakeRuntime();
        runtime.allowedBranches = EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER, ElementalBranch.BLOOD);
        ElementalsMageData data = new ElementalsMageData();
        data.setUnlockedBranches(EnumSet.copyOf(runtime.allowedBranches));

        ElementalsCompat.reconcileMageState(
                ElementalsRaceAffinity.resolve("tensura:slime"),
                UUID.fromString("00000000-0000-0000-0000-000000000011"),
                index -> 0,
                Set.of(),
                data,
                runtime
        );

        assertEquals(EnumSet.noneOf(ElementalBranch.class), runtime.allowedBranches);
    }
}
