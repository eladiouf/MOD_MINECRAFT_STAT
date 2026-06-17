package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishUnlockServiceTest {
    @Test
    void unlockConsumesCanonicalPointsWhenValid() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(Perk.BRUTE_CORE.stat.index, 10);
        data.setPerkPoints(Perk.BRUTE_CORE.stat.index, 1);

        assertTrue(PuffishUnlockService.tryUnlock(data, Perk.BRUTE_CORE, null));
        assertTrue(data.isPerkUnlocked(Perk.BRUTE_CORE.id));
        assertEquals(0, data.getPerkPointsForStat(Perk.BRUTE_CORE.stat.index));
    }

    @Test
    void unlockDoesNotMutateCanonicalStateWhenInvalid() {
        PlayerStatData data = new PlayerStatData();

        assertFalse(PuffishUnlockService.tryUnlock(data, Perk.BRUTE_CORE, null));
        assertFalse(data.isPerkUnlocked(Perk.BRUTE_CORE.id));
    }

    @Test
    void resolvesCategoryAndSkillIdsBeforeCanonicalUnlock() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(Perk.BRUTE_CORE.stat.index, 10);
        data.setPerkPoints(Perk.BRUTE_CORE.stat.index, 1);

        assertTrue(PuffishUnlockService.tryUnlock(data, "statmod:brute_force", "brute_core", null));
        assertTrue(data.isPerkUnlocked(Perk.BRUTE_CORE.id));
    }

    @Test
    void rejectsUnknownPuffishSkillIds() {
        PlayerStatData data = new PlayerStatData();

        assertFalse(PuffishUnlockService.tryUnlock(data, "statmod:brute_force", "missing_skill", null));
        assertFalse(data.isPerkUnlocked(Perk.BRUTE_CORE.id));
    }
}
