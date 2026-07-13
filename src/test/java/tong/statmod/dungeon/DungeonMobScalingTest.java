package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DungeonMobScalingTest {

    @Test
    void followsConfiguredHealthAnchors() {
        assertMultiplier(3.0, 1, DungeonMobScaling.MobRole.NORMAL);
        assertMultiplier(4.5, 10, DungeonMobScaling.MobRole.NORMAL);
        assertMultiplier(7.5, 25, DungeonMobScaling.MobRole.NORMAL);
        assertMultiplier(12.0, 50, DungeonMobScaling.MobRole.NORMAL);
        assertMultiplier(17.0, 75, DungeonMobScaling.MobRole.NORMAL);
        assertMultiplier(22.0, 100, DungeonMobScaling.MobRole.NORMAL);
    }

    @Test
    void interpolatesAndClampsTheAbyssCurve() {
        assertMultiplier(3.0 + (4.0 / 9.0) * 1.5, 5, DungeonMobScaling.MobRole.NORMAL);
        assertMultiplier(24.5, 125, DungeonMobScaling.MobRole.NORMAL);
        assertMultiplier(32.0, 500, DungeonMobScaling.MobRole.NORMAL);
        assertMultiplier(3.0, 0, DungeonMobScaling.MobRole.NORMAL);
        assertMultiplier(3.0, -50, DungeonMobScaling.MobRole.NORMAL);
    }

    @Test
    void appliesEliteAndBossRoleBonusesAfterTheFloorCurve() {
        assertMultiplier(9.375, 25, DungeonMobScaling.MobRole.ELITE);
        assertMultiplier(11.25, 25, DungeonMobScaling.MobRole.BOSS);
        assertMultiplier(48.0, 500, DungeonMobScaling.MobRole.BOSS);
    }

    @Test
    void convertsFinalMultiplierToAStableAttributeModifierAmount() {
        assertEquals(2.0, DungeonMobScaling.healthModifierAmount(3.0), 1.0e-9);
        assertEquals(0.0, DungeonMobScaling.healthModifierAmount(1.0), 1.0e-9);
    }

    @Test
    void decodesUnknownPersistentRolesAsNormal() {
        assertEquals(DungeonMobScaling.MobRole.NORMAL, DungeonMobScaling.MobRole.fromId(null));
        assertEquals(DungeonMobScaling.MobRole.NORMAL, DungeonMobScaling.MobRole.fromId(""));
        assertEquals(DungeonMobScaling.MobRole.NORMAL, DungeonMobScaling.MobRole.fromId("unknown"));
        assertEquals(DungeonMobScaling.MobRole.ELITE, DungeonMobScaling.MobRole.fromId("elite"));
        assertEquals(DungeonMobScaling.MobRole.BOSS, DungeonMobScaling.MobRole.fromId("boss"));
    }

    private static void assertMultiplier(double expected, int floor, DungeonMobScaling.MobRole role) {
        assertEquals(expected, DungeonMobScaling.healthMultiplier(floor, role), 1.0e-9);
    }
}
