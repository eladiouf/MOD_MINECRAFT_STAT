package tong.statmod;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;
import tong.statmod.stats.StatFamily;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;
import static org.junit.jupiter.api.Assertions.*;

class PlayerStatDataTest {

    @Test
    void testInitialValues() {
        PlayerStatData data = new PlayerStatData();
        for (int i = 0; i < PlayerStatData.STAT_COUNT; i++) {
            assertEquals(0, data.getLevel(i));
            assertEquals(0, data.getXp(i));
            assertEquals(0, data.getPerkPointsForStat(i));
        }
        assertEquals(0, data.getGlobalLevel());
        assertArrayEquals(new int[0], data.getUnlockedPerks());
    }

    @Test
    void testAddXp() {
        PlayerStatData data = new PlayerStatData();
        assertFalse(data.addXp(0, 0));
        assertFalse(data.addXp(-1, 10));
        assertFalse(data.addXp(PlayerStatData.STAT_COUNT, 10));
        assertTrue(data.addXp(0, 10));
        assertEquals(0, data.getXp(0));
        assertEquals(1, data.getLevel(0));
    }

    @Test
    void testAddXpNoLevelUp() {
        PlayerStatData data = new PlayerStatData();
        data.addXp(0, 5);
        assertEquals(5, data.getXp(0));
        assertEquals(0, data.getLevel(0));
    }

    @Test
    void testAddXpMultipleLevels() {
        PlayerStatData data = new PlayerStatData();
        data.addXp(0, 10000);
        assertTrue(data.getLevel(0) > 10);
        assertTrue(data.getXp(0) >= 0);
    }

    @Test
    void addXpSaturatesInsteadOfOverflowingNegative() {
        PlayerStatData data = new PlayerStatData();
        data.setXp(0, Integer.MAX_VALUE - 5);

        assertTrue(data.addXp(0, 10));

        assertEquals(data.maxStatLevel(), data.getLevel(0));
        assertEquals(0, data.getXp(0));
    }

    @Test
    void testLevelCap() {
        PlayerStatData data = new PlayerStatData();
        for (int i = 0; i < 200; i++) data.addXp(0, 999999);
        assertEquals(100, data.getLevel(0));
    }

    @Test
    void settersClampInvalidLevelAndXpValues() {
        PlayerStatData data = new PlayerStatData();

        data.setLevel(0, -5);
        data.setXp(0, -50);

        assertEquals(0, data.getLevel(0));
        assertEquals(0, data.getXp(0));

        data.setLevel(0, 999);

        assertEquals(data.maxStatLevel(), data.getLevel(0));
    }

    @Test
    void addXpAtLevelCapDoesNotAccumulateDeadXp() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(0, data.maxStatLevel());
        data.setXp(0, 0);

        assertFalse(data.addXp(0, 999999));
        assertEquals(0, data.getXp(0));
    }

    @Test
    void loweringSoulLevelClampsExistingLevelsAndClearsCapXp() {
        PlayerStatData data = new PlayerStatData();
        data.setSoulLevel(80);
        data.setLevel(0, 70);
        data.setXp(0, 123);

        data.setSoulLevel(30);

        assertEquals(30, data.maxStatLevel());
        assertEquals(30, data.getLevel(0));
        assertEquals(0, data.getXp(0));
    }

    @Test
    void testAddLevels() {
        PlayerStatData data = new PlayerStatData();
        data.addLevels(0, 5);
        assertEquals(5, data.getLevel(0));
        data.addLevels(0, -2);
        assertEquals(3, data.getLevel(0));
        data.addLevels(0, 200);
        assertEquals(100, data.getLevel(0));
    }

    @Test
    void addLevelsSaturatesInsteadOfOverflowingNegative() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(0, 90);

        data.addLevels(0, Integer.MAX_VALUE);

        assertEquals(data.maxStatLevel(), data.getLevel(0));
    }

    @Test
    void testPerkPoints() {
        PlayerStatData data = new PlayerStatData();
        data.addPerkPointsForStat(0, 5);
        assertEquals(5, data.getPerkPointsForStat(0));
        data.addPerkPointsForStat(0, -2);
        assertEquals(3, data.getPerkPointsForStat(0));
        data.addPerkPointsForStat(0, -10);
        assertEquals(0, data.getPerkPointsForStat(0));
    }

    @Test
    void addPerkPointsSaturatesInsteadOfOverflowingNegative() {
        PlayerStatData data = new PlayerStatData();
        data.setPerkPointsForFamily(StatFamily.FRONTLINE_PHYSICAL_COMBAT, Integer.MAX_VALUE - 1);

        data.addPerkPointsForFamily(StatFamily.FRONTLINE_PHYSICAL_COMBAT, 10);

        assertEquals(Integer.MAX_VALUE,
                data.getPerkPointsForFamily(StatFamily.FRONTLINE_PHYSICAL_COMBAT));
    }

    @Test
    void testPerkPointsAreSharedAcrossStatsInTheSameFamily() {
        PlayerStatData data = new PlayerStatData();

        data.setPerkPoints(StatType.BRUTE_FORCE.index, 4);

        assertEquals(4, data.getPerkPointsForStat(StatType.BRUTE_FORCE.index));
        assertEquals(4, data.getPerkPointsForStat(StatType.BLADE_TECHNIQUE.index));

        data.addPerkPointsForStat(StatType.BLADE_TECHNIQUE.index, -1);

        assertEquals(3, data.getPerkPointsForStat(StatType.BRUTE_FORCE.index));
        assertEquals(3, data.getPerkPointsForStat(StatType.BLADE_TECHNIQUE.index));
    }

    @Test
    void testGlobalLevel() {
        // Sous Mission S : globalLevel = moyenne des stats ≥ 1 uniquement. Plus de pénalité
        // pour les stats jamais montées. Une seule stat active à 46 → globalLevel = 46.
        PlayerStatData data = new PlayerStatData();
        data.setMagicRace(MagicRace.HUMAN);
        assertEquals(0, data.getGlobalLevel(), "pas de stat ≥ 1 → globalLevel = 0");
        data.setLevel(0, 46);
        assertEquals(46, data.getGlobalLevel(),
                "une seule stat à 46 → globalLevel = 46");
        data.setLevel(1, 10);
        assertEquals(28, data.getGlobalLevel(),
                "deux stats actives (46 + 10) / 2 = 28");
    }

    @Test
    void globalLevelIgnoresMagicalStatsWhileMagicRaceIsUnset() {
        PlayerStatData data = new PlayerStatData();
        data.setLevel(StatType.BRUTE_FORCE.index, 10);
        data.setLevel(StatType.ARCANE_POWER.index, 50);
        data.setLevel(StatType.FIRE_AFFINITY.index, 30);

        assertEquals(10, data.getGlobalLevel(),
                "without a magic race, locked magical stats must not inflate global level");
    }

    @Test
    void testRequiredXp() {
        assertEquals(10, PlayerStatData.requiredXp(0));
        assertEquals(40, PlayerStatData.requiredXp(1));
        assertEquals(90, PlayerStatData.requiredXp(2));
        assertEquals(100000, PlayerStatData.requiredXp(99));
    }

    @Test
    void testUnlockedPerks() {
        PlayerStatData data = new PlayerStatData();
        assertFalse(data.isPerkUnlocked(0));
        assertFalse(data.isPerkUnlocked(83));

        data.addUnlockedPerk(0);
        assertTrue(data.isPerkUnlocked(0));
        assertFalse(data.isPerkUnlocked(1));

        data.addUnlockedPerk(83);
        assertTrue(data.isPerkUnlocked(83));

        data.addUnlockedPerk(0);
        assertTrue(data.isPerkUnlocked(0));
        assertEquals(2, data.getUnlockedPerks().length);
    }

    @Test
    void directPerkMutatorsIgnoreInvalidIds() {
        PlayerStatData data = new PlayerStatData();

        data.addUnlockedPerk(-1);
        data.markPerkFreeGranted(-2);

        assertArrayEquals(new int[0], data.getUnlockedPerks());
        assertArrayEquals(new int[0], data.getFreeGrantedPerks());
    }

    @Test
    void testClearUnlockedPerks() {
        PlayerStatData data = new PlayerStatData();
        data.addUnlockedPerk(0);
        data.addUnlockedPerk(1);
        data.clearUnlockedPerks();
        assertArrayEquals(new int[0], data.getUnlockedPerks());
    }

    @Test
    void testSetUnlockedPerks() {
        PlayerStatData data = new PlayerStatData();
        data.setUnlockedPerks(new int[]{10, 20, 30});
        assertTrue(data.isPerkUnlocked(10));
        assertTrue(data.isPerkUnlocked(20));
        assertTrue(data.isPerkUnlocked(30));
        assertFalse(data.isPerkUnlocked(11));
    }

    @Test
    void setUnlockedPerksDropsDuplicatesAndInvalidIds() {
        PlayerStatData data = new PlayerStatData();

        data.setUnlockedPerks(new int[]{10, 10, -1, 20, 10, 0});

        assertArrayEquals(new int[]{10, 20, 0}, data.getUnlockedPerks());
        assertFalse(data.isPerkUnlocked(-1));
    }

    @Test
    void setFreeGrantedPerksKeepsOnlyUniqueUnlockedPerks() {
        PlayerStatData data = new PlayerStatData();
        data.setUnlockedPerks(new int[]{10, 20});

        data.setFreeGrantedPerks(new int[]{20, 20, 99, -3, 10});

        assertArrayEquals(new int[]{20, 10}, data.getFreeGrantedPerks());
        assertTrue(data.isPerkFreeGranted(20));
        assertTrue(data.isPerkFreeGranted(10));
        assertFalse(data.isPerkFreeGranted(99));
        assertFalse(data.isPerkFreeGranted(-3));
    }

    @Test
    void setUnlockedPerksPrunesFreeGrantedPerksNoLongerUnlocked() {
        PlayerStatData data = new PlayerStatData();
        data.setUnlockedPerks(new int[]{10, 20, 30});
        data.setFreeGrantedPerks(new int[]{10, 30});

        data.setUnlockedPerks(new int[]{20, 30});

        assertArrayEquals(new int[]{30}, data.getFreeGrantedPerks());
    }

    @Test
    void copyFromPreservesAllPersistentRespawnProgress() {
        PlayerStatData source = new PlayerStatData();
        source.setLevel(StatType.BRUTE_FORCE.index, 12);
        source.setXp(StatType.BRUTE_FORCE.index, 34);
        source.setPerkPointsForFamily(StatFamily.FRONTLINE_PHYSICAL_COMBAT, 3);
        source.addUnlockedPerk(10);
        source.markPerkFreeGranted(20);
        source.setSoulLevel(77);
        source.setLastPerkGrantTier(4);
        source.setMagicPoints(42);
        source.addMagicNode("fire/opener/ignition");
        source.learnSpell("irons_spellbooks:firebolt");
        source.setMagicRace(MagicRace.ELF);
        source.setChosenStartBranch(MagicBranch.FIRE);
        source.setSchoolMasteryProgress(MagicBranch.FIRE, 9);
        source.setDungeonFloorReached(6);
        source.setLastOverworldDimensionId("minecraft:overworld");
        source.setLastOverworldPos(123456L);

        PlayerStatData target = new PlayerStatData();
        target.setLevel(StatType.BRUTE_FORCE.index, 1);
        target.setMagicRace(MagicRace.DWARF);

        target.copyFrom(source);

        assertArrayEquals(source.getLevels(), target.getLevels());
        assertArrayEquals(source.getXp(), target.getXp());
        assertArrayEquals(source.getPerkPoints(), target.getPerkPoints());
        assertArrayEquals(source.getUnlockedPerks(), target.getUnlockedPerks());
        assertArrayEquals(source.getFreeGrantedPerks(), target.getFreeGrantedPerks());
        assertEquals(source.getSoulLevel(), target.getSoulLevel());
        assertEquals(source.getLastPerkGrantTier(), target.getLastPerkGrantTier());
        assertEquals(source.getMagicPoints(), target.getMagicPoints());
        assertArrayEquals(source.getMagicNodes(), target.getMagicNodes());
        assertArrayEquals(source.getLearnedSpells(), target.getLearnedSpells());
        assertEquals(source.getMagicRace(), target.getMagicRace());
        assertEquals(source.getChosenStartBranch(), target.getChosenStartBranch());
        assertEquals(source.getSchoolMasteryProgress(MagicBranch.FIRE),
                target.getSchoolMasteryProgress(MagicBranch.FIRE));
        assertEquals(source.getDungeonFloorReached(), target.getDungeonFloorReached());
        assertEquals(source.getLastOverworldDimensionId(), target.getLastOverworldDimensionId());
        assertEquals(source.getLastOverworldPosPacked(), target.getLastOverworldPosPacked());
        assertTrue(target.hasLastOverworldPos());
    }
}
