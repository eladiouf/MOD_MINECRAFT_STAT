package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DungeonBountyMilestonesTest {
    @Test
    void thresholdsAreTheFourDelvePaliers() {
        assertEquals(List.of(10, 25, 50, 100), DungeonBountyMilestones.thresholds());
    }

    @Test
    void reachedReturnsOnlyPaliersUpToDeepestFloor() {
        assertEquals(List.of(), DungeonBountyMilestones.reached(9));
        assertEquals(List.of(10), DungeonBountyMilestones.reached(24));
        assertEquals(List.of(10, 25), DungeonBountyMilestones.reached(25));
        assertEquals(List.of(10, 25, 50), DungeonBountyMilestones.reached(60));
        assertEquals(List.of(10, 25, 50, 100), DungeonBountyMilestones.reached(100));
    }

    @Test
    void advancementIdMatchesFloorTheme() {
        assertEquals("statmod:dungeon/delve_50", DungeonBountyMilestones.advancementId(50));
    }
}
