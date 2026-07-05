package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests pour le tracker refondu sous Mission ε :
 *
 * <ul>
 *   <li>Conversion continue : {@code MASTERY_PER_POINT} unités → 1 magic point</li>
 *   <li>Paliers : franchissement de 1000/2500/5000 mastery cumulé verse un bonus +1/+2/+3</li>
 *   <li>Mastery progress stocke désormais le <b>cumul lifetime</b>, pas un remainder</li>
 * </ul>
 */
class SchoolProgressTrackerTest {

    @Test
    void below_threshold_accumulates_no_points() {
        PlayerStatData d = new PlayerStatData();
        int granted = SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 50);
        assertEquals(0, granted);
        assertEquals(50, d.getSchoolMasteryProgress(MagicBranch.FIRE));
        assertEquals(5, d.getMagicPoints());
    }

    @Test
    void crossing_continuous_threshold_grants_one_point_lifetime_stored() {
        PlayerStatData d = new PlayerStatData();
        int granted = SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 125);
        assertEquals(1, granted, "125 / 100 = 1 continuous point");
        // Sous Mission ε, le progress stocke le lifetime cumulé (125), pas un remainder.
        assertEquals(125, d.getSchoolMasteryProgress(MagicBranch.FIRE));
        assertEquals(6, d.getMagicPoints());
    }

    @Test
    void incremental_calls_track_lifetime_cumulatively() {
        PlayerStatData d = new PlayerStatData();
        // Première dose : 80 → 0 point (sous 100)
        SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 80);
        // Deuxième : 80 → cumul 160 → grant 1 (160/100 - 80/100 = 1)
        int granted = SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 80);
        assertEquals(1, granted);
        assertEquals(160, d.getSchoolMasteryProgress(MagicBranch.FIRE));
        assertEquals(6, d.getMagicPoints());
    }

    @Test
    void crossing_first_milestone_adds_bonus() {
        PlayerStatData d = new PlayerStatData();
        // 1000 mastery = 10 continuous points + 1 milestone bonus
        int granted = SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 1000);
        assertEquals(11, granted, "10 continu + 1 palier 1000");
    }

    @Test
    void crossing_second_milestone_adds_bonus() {
        PlayerStatData d = new PlayerStatData();
        // 2500 mastery = 25 continu + 1 (palier 1000) + 2 (palier 2500) = 28
        int granted = SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 2500);
        assertEquals(28, granted);
    }

    @Test
    void crossing_third_milestone_adds_bonus() {
        PlayerStatData d = new PlayerStatData();
        // 5000 mastery = 50 continu + 1 + 2 + 3 = 56
        int granted = SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 5000);
        assertEquals(56, granted);
    }

    @Test
    void milestone_only_claims_once_lifetime() {
        PlayerStatData d = new PlayerStatData();
        // Franchit 1000 une fois
        SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 1100);
        int before = d.getMagicPoints();
        // Continue à grinder mais ne re-franchit pas 1000 (déjà passé)
        SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 200);
        int after = d.getMagicPoints();
        assertEquals(2, after - before, "200 mastery = 2 continu, pas de palier rejoué");
    }

    @Test
    void zero_or_negative_mastery_is_noop() {
        PlayerStatData d = new PlayerStatData();
        assertEquals(0, SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, 0));
        assertEquals(0, SchoolProgressTracker.applyMastery(d, MagicBranch.FIRE, -10));
        assertEquals(0, d.getSchoolMasteryProgress(MagicBranch.FIRE));
    }
}
