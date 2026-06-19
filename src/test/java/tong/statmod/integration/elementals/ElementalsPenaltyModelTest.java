package tong.statmod.integration.elementals;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElementalsPenaltyModelTest {
    @Test
    void awakenedBranchesUseTheApprovedBasePenalty() {
        assertEquals(0.80f, ElementalsPenaltyModel.damageMultiplier(ElementState.AWAKENED), 0.0001f);
        assertEquals(1.25f, ElementalsPenaltyModel.chiCostMultiplier(ElementState.AWAKENED, false, 0), 0.0001f);
        assertEquals(75.0f, ElementalsPenaltyModel.adjustedChiAfterSpend(100.0f, 80.0f, ElementState.AWAKENED, false, 0), 0.0001f);
    }

    @Test
    void rareBranchesIncreaseCostsAndSlowOtherBranchProgression() {
        assertEquals(1.50f, ElementalsPenaltyModel.chiCostMultiplier(ElementState.AWAKENED, true, 1), 0.0001f);
        assertEquals(0.68f, ElementalsPenaltyModel.progressionMultiplier(true, ElementalBranch.FIRE, EnumSet.of(ElementalBranch.LIGHTNING, ElementalBranch.BLOOD)), 0.0001f);
        assertEquals(16.8f, ElementalsPenaltyModel.adjustedXpAfterGain(10.0f, 20.0f, true, ElementalBranch.FIRE, EnumSet.of(ElementalBranch.LIGHTNING, ElementalBranch.BLOOD)), 0.0001f);
    }
}
