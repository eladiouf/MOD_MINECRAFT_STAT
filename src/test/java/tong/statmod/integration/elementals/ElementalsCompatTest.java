package tong.statmod.integration.elementals;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementalsCompatTest {
    private static final class FakeRuntime implements ElementalsRuntimePort {
        private EnumSet<ElementalBranch> allowedBranches = EnumSet.noneOf(ElementalBranch.class);
        private float chi;
        private float xp;
        private int level;
        private ElementalBranch activeBranch;
        private int xpSyncCalls;
        private float maxXpPerLevel = 10.0f;

        @Override
        public void setAllowedBranches(EnumSet<ElementalBranch> branches) {
            allowedBranches = branches == null
                    ? EnumSet.noneOf(ElementalBranch.class)
                    : EnumSet.copyOf(branches);
        }

        @Override
        public float chi() {
            return chi;
        }

        @Override
        public float xp() {
            return xp;
        }

        @Override
        public int level() {
            return level;
        }

        @Override
        public void setXp(float value) {
            xp = value;
            xpSyncCalls++;
        }

        @Override
        public void setChi(float value) {
            chi = value;
        }

        @Override
        public ElementalBranch activeBranch() {
            return activeBranch;
        }

        @Override
        public float maxXpForLevel(int level) {
            return maxXpPerLevel;
        }

        @Override
        public void setLevelAndXp(int level, float xp) {
            this.level = level;
            this.xp = xp;
            xpSyncCalls++;
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

    @Test
    void masteredThirdBaseCountsTowardFourthBaseUnlock() {
        FakeRuntime runtime = new FakeRuntime();
        ElementalsMageData data = new ElementalsMageData();
        data.setMageAwakened(true);
        data.setStarterBranches(EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER));
        data.setUnlockedBranches(EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER, ElementalBranch.EARTH));
        MageRaceProfile profile = ElementalsRaceAffinity.resolve("tensura:human");

        Set<Integer> perks = Set.of(
                ElementalsPerkBindings.masteryPerk(ElementalBranch.AIR).id,
                ElementalsPerkBindings.masteryPerk(ElementalBranch.EARTH).id,
                Perk.ERUDITION_MASTERY.id,
                Perk.FIRE_ACTIVE.id
        );

        ElementalsCompat.reconcileMageState(
                profile,
                UUID.fromString("00000000-0000-0000-0000-000000000012"),
                index -> switch (index) {
                    case 7 -> 22;
                    case 9 -> 18;
                    case 10 -> 26;
                    case 11 -> 26;
                    case 13 -> 14;
                    case 14 -> 14;
                    case 15 -> 22;
                    default -> 0;
                },
                perks,
                data,
                runtime
        );

        assertTrue(runtime.allowedBranches.contains(ElementalBranch.FIRE));
    }

    @Test
    void xpPenaltyStillAppliesWhenElementalsLevelChanges() {
        FakeRuntime runtime = new FakeRuntime();
        runtime.activeBranch = ElementalBranch.FIRE;
        runtime.xp = 2.0f;
        runtime.level = 5;

        ElementalsMageData data = new ElementalsMageData();
        data.setMageAwakened(true);
        data.setUnlockedBranches(EnumSet.of(ElementalBranch.FIRE, ElementalBranch.LIGHTNING));
        data.setRewardedRareBranches(EnumSet.of(ElementalBranch.LIGHTNING));
        data.setLastSeenXp(9.0f);
        data.setLastSeenLevel(4);

        ElementalsCompat.applyRuntimePenalties(
                ElementalsRaceAffinity.resolve("tensura:beastfolk"),
                data,
                ElementState.AWAKENED,
                runtime
        );

        assertEquals(5, runtime.level());
        assertEquals(1.04f, runtime.xp(), 0.0001f);
        assertEquals(1, runtime.xpSyncCalls);
    }
}
