package tong.statmod.item;

import org.junit.jupiter.api.Test;
import tong.statmod.integration.elementals.ElementalBranch;
import tong.statmod.integration.elementals.ElementalsMageData;
import tong.statmod.integration.elementals.ElementalsPerkBindings;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementalGrimoireItemTest {
    @Test
    void successfulRareGrimoireMarksRareBranchAndFreeGrantedPerk() {
        PlayerStatData statData = new PlayerStatData();
        statData.setLevel(7, 20);
        statData.setLevel(13, 20);
        statData.setLevel(15, 18);
        ElementalsMageData mageData = new ElementalsMageData();

        boolean consumed = ElementalGrimoireItem.tryUnlockForTests(ElementalBranch.LIGHTNING, statData, mageData);

        assertTrue(consumed);
        assertTrue(mageData.rewardedRareBranches().contains(ElementalBranch.LIGHTNING));
        assertTrue(statData.isPerkFreeGranted(ElementalsPerkBindings.rareRewardPerk(ElementalBranch.LIGHTNING).id));
    }

    @Test
    void failingRareGrimoireDoesNotMutateState() {
        PlayerStatData statData = new PlayerStatData();
        ElementalsMageData mageData = new ElementalsMageData();

        boolean consumed = ElementalGrimoireItem.tryUnlockForTests(ElementalBranch.BLOOD, statData, mageData);

        assertFalse(consumed);
        assertFalse(mageData.rewardedRareBranches().contains(ElementalBranch.BLOOD));
    }
}
