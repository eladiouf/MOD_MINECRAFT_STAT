package tong.statmod.integration.elementals;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                runtime,
                branch -> ElementState.AWAKENED
        );

        assertEquals(5, runtime.level());
        assertEquals(1.04f, runtime.xp(), 0.0001f);
        assertEquals(1, runtime.xpSyncCalls);
    }

    @Test
    void chiPenaltyUsesPreviousActiveBranchWhenPlayerSwapsBeforeTick() {
        FakeRuntime runtime = new FakeRuntime();
        runtime.activeBranch = ElementalBranch.FIRE;
        runtime.chi = 80.0f;

        ElementalsMageData data = new ElementalsMageData();
        data.setMageAwakened(true);
        data.setUnlockedBranches(EnumSet.of(ElementalBranch.FIRE, ElementalBranch.LIGHTNING));
        data.setRewardedRareBranches(EnumSet.of(ElementalBranch.LIGHTNING));
        data.setLastSeenChi(100.0f);
        data.setLastSeenActiveBranch(ElementalBranch.LIGHTNING);

        ElementalsCompat.applyRuntimePenalties(
                ElementalsRaceAffinity.resolve("tensura:human"),
                data,
                runtime,
                branch -> branch == ElementalBranch.LIGHTNING ? ElementState.AWAKENED : ElementState.MASTERED
        );

        assertEquals(70.0f, runtime.chi(), 0.0001f);
        assertEquals(ElementalBranch.FIRE, data.lastSeenActiveBranch());
    }

    @Test
    void xpPenaltyUsesPreviousActiveBranchWhenPlayerSwapsBeforeTick() {
        FakeRuntime runtime = new FakeRuntime();
        runtime.activeBranch = ElementalBranch.LIGHTNING;
        runtime.xp = 10.0f;
        runtime.level = 5;

        ElementalsMageData data = new ElementalsMageData();
        data.setMageAwakened(true);
        data.setUnlockedBranches(EnumSet.of(ElementalBranch.FIRE, ElementalBranch.LIGHTNING));
        data.setRewardedRareBranches(EnumSet.of(ElementalBranch.LIGHTNING));
        data.setLastSeenXp(0.0f);
        data.setLastSeenLevel(5);
        data.setLastSeenActiveBranch(ElementalBranch.FIRE);

        ElementalsCompat.applyRuntimePenalties(
                ElementalsRaceAffinity.resolve("tensura:human"),
                data,
                runtime,
                branch -> ElementState.AWAKENED
        );

        assertEquals(8.0f, runtime.xp(), 0.0001f);
        assertEquals(1, runtime.xpSyncCalls);
        assertEquals(ElementalBranch.LIGHTNING, data.lastSeenActiveBranch());
    }

    @Test
    void beastfolkNeedsHigherMagicalTotalForAwakening() {
        FakeRuntime runtime = new FakeRuntime();
        ElementalsMageData data = new ElementalsMageData();

        ElementalsCompat.reconcileMageState(
                ElementalsRaceAffinity.resolve("tensura:beastfolk"),
                UUID.fromString("00000000-0000-0000-0000-000000000020"),
                index -> switch (index) {
                    case 7, 12 -> 13;
                    case 13 -> 9;
                    case 14 -> 6;
                    default -> 0;
                },
                Set.of(),
                data,
                runtime
        );

        assertFalse(data.mageAwakened());
    }

    @Test
    void humanUnlocksThirdBaseAtNewLowerThreshold() {
        FakeRuntime runtime = new FakeRuntime();
        ElementalsMageData data = new ElementalsMageData();
        data.setMageAwakened(true);
        data.setStarterBranches(EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER));
        data.setUnlockedBranches(EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER));

        ElementalsCompat.reconcileMageState(
                ElementalsRaceAffinity.resolve("tensura:human"),
                UUID.fromString("00000000-0000-0000-0000-000000000021"),
                index -> switch (index) {
                    case 7 -> 16;
                    case 10 -> 20;
                    case 11 -> 18;
                    case 13 -> 20;
                    case 15 -> 16;
                    default -> 0;
                },
                Set.of(Perk.ERUDITION_CORE.id, Perk.FIRE_CORE.id, Perk.AIR_MASTERY.id),
                data,
                runtime
        );

        assertTrue(runtime.allowedBranches.contains(ElementalBranch.FIRE));
    }

    @Test
    void runtimeKeepsRewardedMetalWhenReconciled() {
        FakeRuntime runtime = new FakeRuntime();
        ElementalsMageData data = new ElementalsMageData();
        data.setMageAwakened(true);
        data.setStarterBranches(EnumSet.of(ElementalBranch.FIRE, ElementalBranch.EARTH));
        data.setUnlockedBranches(EnumSet.of(ElementalBranch.FIRE, ElementalBranch.EARTH, ElementalBranch.METAL));
        data.setRewardedRareBranches(EnumSet.of(ElementalBranch.METAL));

        ElementalsCompat.reconcileMageState(
                ElementalsRaceAffinity.resolve("tensura:dwarf"),
                UUID.fromString("00000000-0000-0000-0000-000000000022"),
                index -> 30,
                Set.of(),
                data,
                runtime
        );

        assertTrue(runtime.allowedBranches.contains(ElementalBranch.METAL));
    }

    @Test
    void awakenedMageRefreshesStarterBranchesWhenRaceProfileChanges() {
        FakeRuntime runtime = new FakeRuntime();
        ElementalsMageData data = new ElementalsMageData();
        data.setMageAwakened(true);
        data.setStarterBranches(EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER));
        data.setUnlockedBranches(EnumSet.of(ElementalBranch.AIR, ElementalBranch.WATER));

        ElementalsCompat.reconcileMageState(
                ElementalsRaceAffinity.resolve("tensura:dwarf"),
                UUID.fromString("00000000-0000-0000-0000-000000000023"),
                index -> 30,
                Set.of(),
                data,
                runtime
        );

        assertEquals(EnumSet.of(ElementalBranch.FIRE, ElementalBranch.EARTH), data.starterBranches());
        assertTrue(runtime.allowedBranches.contains(ElementalBranch.FIRE));
        assertTrue(runtime.allowedBranches.contains(ElementalBranch.EARTH));
        assertFalse(runtime.allowedBranches.contains(ElementalBranch.AIR));
        assertFalse(runtime.allowedBranches.contains(ElementalBranch.WATER));
    }
}
