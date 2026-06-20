package tong.statmod.item;

import org.junit.jupiter.api.Test;
import tong.statmod.integration.elementals.ElementalBranch;
import tong.statmod.integration.elementals.ElementalsMageData;
import tong.statmod.integration.elementals.ElementalsPerkBindings;
import tong.statmod.perks.Perk;
import tong.statmod.storage.PlayerStatData;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementalGrimoireItemTest {
    @Test
    void successfulRareGrimoireMarksRareBranchAndFreeGrantedPerk() {
        PlayerStatData statData = new PlayerStatData();
        statData.setLevel(7, 20);
        statData.setLevel(13, 20);
        statData.setLevel(14, 2);
        statData.setLevel(15, 18);
        ElementalsMageData mageData = new ElementalsMageData();
        mageData.setMageAwakened(true);

        boolean consumed = ElementalGrimoireItem.tryUnlockForTests(
                tong.statmod.integration.elementals.ElementalsRaceAffinity.resolve("tensura:human"),
                ElementalBranch.LIGHTNING,
                statData,
                mageData);

        assertTrue(consumed);
        assertTrue(mageData.rewardedRareBranches().contains(ElementalBranch.LIGHTNING));
        assertTrue(statData.isPerkFreeGranted(ElementalsPerkBindings.rareRewardPerk(ElementalBranch.LIGHTNING).id));
    }

    @Test
    void failingRareGrimoireDoesNotMutateState() {
        PlayerStatData statData = new PlayerStatData();
        ElementalsMageData mageData = new ElementalsMageData();

        boolean consumed = ElementalGrimoireItem.tryUnlockForTests(
                tong.statmod.integration.elementals.ElementalsRaceAffinity.resolve("tensura:human"),
                ElementalBranch.BLOOD,
                statData,
                mageData);

        assertFalse(consumed);
        assertFalse(mageData.rewardedRareBranches().contains(ElementalBranch.BLOOD));
    }

    @Test
    void rareGrimoireRequiresMageAwakening() {
        PlayerStatData statData = new PlayerStatData();
        statData.setLevel(7, 20);
        statData.setLevel(13, 20);
        statData.setLevel(14, 2);
        statData.setLevel(15, 18);
        ElementalsMageData mageData = new ElementalsMageData();

        boolean consumed = ElementalGrimoireItem.tryUnlockForTests(
                tong.statmod.integration.elementals.ElementalsRaceAffinity.resolve("tensura:human"),
                ElementalBranch.LIGHTNING,
                statData,
                mageData);

        assertFalse(consumed);
        assertFalse(mageData.rewardedRareBranches().contains(ElementalBranch.LIGHTNING));
        assertFalse(statData.isPerkFreeGranted(ElementalsPerkBindings.rareRewardPerk(ElementalBranch.LIGHTNING).id));
    }

    @Test
    void dwarfCanUnlockMetalWithLowerThreshold() {
        PlayerStatData statData = new PlayerStatData();
        statData.setLevel(7, 18);
        statData.setLevel(9, 20);
        statData.setLevel(10, 18);
        statData.setLevel(15, 4);
        ElementalsMageData mageData = new ElementalsMageData();
        mageData.setMageAwakened(true);

        boolean consumed = ElementalGrimoireItem.tryUnlockForTests(
                tong.statmod.integration.elementals.ElementalsRaceAffinity.resolve("tensura:dwarf"),
                ElementalBranch.METAL,
                statData,
                mageData
        );

        assertTrue(consumed);
        assertTrue(mageData.rewardedRareBranches().contains(ElementalBranch.METAL));
        assertTrue(statData.isPerkFreeGranted(ElementalsPerkBindings.rareRewardPerk(ElementalBranch.METAL).id));
    }

    @Test
    void humanFailsMetalAtDwarfOnlyThreshold() {
        PlayerStatData statData = new PlayerStatData();
        statData.setLevel(7, 18);
        statData.setLevel(9, 20);
        statData.setLevel(10, 18);
        statData.setLevel(15, 4);
        ElementalsMageData mageData = new ElementalsMageData();
        mageData.setMageAwakened(true);

        boolean consumed = ElementalGrimoireItem.tryUnlockForTests(
                tong.statmod.integration.elementals.ElementalsRaceAffinity.resolve("tensura:human"),
                ElementalBranch.METAL,
                statData,
                mageData
        );

        assertFalse(consumed);
    }

    @Test
    void unsupportedRaceCannotUnlockRareBranchEvenWithStats() {
        PlayerStatData statData = new PlayerStatData();
        statData.setLevel(7, 30);
        statData.setLevel(9, 30);
        statData.setLevel(10, 30);
        statData.setLevel(13, 30);
        statData.setLevel(14, 30);
        statData.setLevel(15, 30);
        statData.setLevel(22, 30);
        ElementalsMageData mageData = new ElementalsMageData();
        mageData.setMageAwakened(true);

        boolean consumed = ElementalGrimoireItem.tryUnlockForTests(
                tong.statmod.integration.elementals.ElementalsRaceAffinity.resolve("tensura:slime"),
                ElementalBranch.METAL,
                statData,
                mageData
        );

        assertFalse(consumed);
        assertFalse(mageData.rewardedRareBranches().contains(ElementalBranch.METAL));
    }

    @Test
    void rareGrimoireDoesNotConvertPaidPerkIntoFreeGrantedPerk() {
        PlayerStatData statData = new PlayerStatData();
        statData.setLevel(7, 20);
        statData.setLevel(13, 20);
        statData.setLevel(14, 2);
        statData.setLevel(15, 18);
        statData.addUnlockedPerk(Perk.AIR_TRANSCENDENCE.id);
        ElementalsMageData mageData = new ElementalsMageData();
        mageData.setMageAwakened(true);

        boolean consumed = ElementalGrimoireItem.tryUnlockForTests(
                tong.statmod.integration.elementals.ElementalsRaceAffinity.resolve("tensura:human"),
                ElementalBranch.LIGHTNING,
                statData,
                mageData);

        assertTrue(consumed);
        assertTrue(mageData.rewardedRareBranches().contains(ElementalBranch.LIGHTNING));
        assertFalse(statData.isPerkFreeGranted(Perk.AIR_TRANSCENDENCE.id));
    }

    @Test
    void successfulRareGrimoireInvokesRewardPerkHook() {
        PlayerStatData statData = new PlayerStatData();
        statData.setLevel(7, 20);
        statData.setLevel(13, 20);
        statData.setLevel(14, 2);
        statData.setLevel(15, 18);
        ElementalsMageData mageData = new ElementalsMageData();
        mageData.setMageAwakened(true);
        AtomicInteger rewardedPerkId = new AtomicInteger(-1);

        boolean consumed = ElementalGrimoireItem.tryUnlockForTests(
                tong.statmod.integration.elementals.ElementalsRaceAffinity.resolve("tensura:human"),
                ElementalBranch.LIGHTNING,
                statData,
                mageData,
                perk -> rewardedPerkId.set(perk.id));

        assertTrue(consumed);
        assertEquals(Perk.AIR_TRANSCENDENCE.id, rewardedPerkId.get());
    }
}
