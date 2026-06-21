package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.storage.PlayerStatData;
import static org.junit.jupiter.api.Assertions.*;

class SchoolProgressTrackerTest {
    @Test
    void below_threshold_accumulates_no_school_points() {
        PlayerStatData d = new PlayerStatData();
        int granted = SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 50);
        assertEquals(0, granted);
        assertEquals(50, d.getSchoolMasteryProgress(MagicBranch.FIRE));
        assertEquals(0, d.getSchoolPoints(MagicBranch.FIRE));
    }

    @Test
    void crossing_threshold_grants_one_school_point_and_carries_remainder() {
        PlayerStatData d = new PlayerStatData();
        int granted = SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 125);
        assertEquals(1, granted);
        assertEquals(25, d.getSchoolMasteryProgress(MagicBranch.FIRE));
        assertEquals(1, d.getSchoolPoints(MagicBranch.FIRE));
    }

    @Test
    void multi_threshold_batch_in_single_call() {
        PlayerStatData d = new PlayerStatData();
        int granted = SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 250);
        assertEquals(2, granted);
        assertEquals(50, d.getSchoolMasteryProgress(MagicBranch.FIRE));
    }

    @Test
    void zero_or_negative_mastery_is_a_noop() {
        PlayerStatData d = new PlayerStatData();
        assertEquals(0, SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 0));
        assertEquals(0, SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, -10));
        assertEquals(0, d.getSchoolMasteryProgress(MagicBranch.FIRE));
    }
}
