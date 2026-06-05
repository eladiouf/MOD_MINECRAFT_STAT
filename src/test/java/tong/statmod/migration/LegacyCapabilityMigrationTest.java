package tong.statmod.migration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tong.statmod.Config;
import tong.statmod.capability.PlayerStats;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkManager;
import tong.statmod.stats.StatCalculator;
import tong.statmod.stats.StatType;
import tong.statmod.weapon.WeaponMasteryManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyCapabilityMigrationTest {

    @BeforeEach
    void configureDefaults() {
        Config.xpPerLevelMultiplier = 40;
        Config.weaponMasteryMaxLevel = 50;
    }

    @Test
    void playerStatsLegacyFixtureRoundTripsToSanitizedState() throws Exception {
        PlayerStats stats = new PlayerStats();

        stats.deserializeNBT(loadFixture("fixtures/migration/player-stats-legacy.snbt"));

        assertEquals(100, stats.getLevel(0));
        assertEquals(0, stats.getXp(0));
        assertEquals(0, stats.getLevel(1));
        assertEquals(0, stats.getXp(1));
        assertEquals(4, stats.getLevel(2));
        assertEquals(17, stats.getXp(2));
        assertEquals(stats.getMaxMana(), stats.getMana());
        assertEquals(0L, stats.getManaBlockRemainingTicks());

        CompoundTag migrated = stats.serializeNBT();
        int[] migratedLevels = migrated.getIntArray("Levels");
        int[] migratedXp = migrated.getIntArray("XP");
        assertEquals(PlayerStats.STAT_COUNT, migratedLevels.length);
        assertEquals(PlayerStats.STAT_COUNT, migratedXp.length);
        assertArrayEquals(new int[] {100, 0, 4, 100}, java.util.Arrays.copyOf(migratedLevels, 4));
        assertArrayEquals(new int[] {0, 0, 17, 0}, java.util.Arrays.copyOf(migratedXp, 4));
        assertEquals(stats.getMaxMana(), migrated.getFloat("Mana"));
        assertEquals(0L, migrated.getLong("ManaBlockTick"));
    }

    @Test
    void perkManagerLegacyFixtureDropsUnknownPerksOnRoundTrip() throws Exception {
        PerkManager manager = new PerkManager();

        manager.deserializeNBT(loadFixture("fixtures/migration/perk-manager-legacy.snbt"));

        assertEquals(0, manager.getAvailablePoints());
        assertTrue(manager.isUnlocked(Perk.BRUTE_DEMOLITION));
        assertTrue(manager.isUnlocked(Perk.BLADE_PARRY));
        assertFalse(manager.isUnlocked(999));

        CompoundTag migrated = manager.serializeNBT();
        assertEquals(0, migrated.getInt("Points"));
        assertEquals(Set.of(Perk.BRUTE_DEMOLITION.id, Perk.BLADE_PARRY.id), readIntSet(migrated.getList("UnlockedPerks", 3)));
    }

    @Test
    void weaponMasteryLegacyFixtureRoundTripsToSanitizedState() throws Exception {
        WeaponMasteryManager manager = new WeaponMasteryManager();

        manager.deserializeNBT(loadFixture("fixtures/migration/weapon-mastery-legacy.snbt"));

        assertEquals(50, manager.getLevel(0));
        assertEquals(0, manager.getXp(0));
        assertEquals(0, manager.getLevel(1));
        assertEquals(0, manager.getXp(1));
        assertEquals(4, manager.getLevel(2));
        assertEquals(StatCalculator.getXpForNextLevel(4) - 1, manager.getXp(2));

        CompoundTag migrated = manager.serializeNBT();
        int[] migratedLevels = migrated.getIntArray("WeaponLevels");
        int[] migratedXp = migrated.getIntArray("WeaponXP");
        assertEquals(WeaponMasteryManager.WEAPON_COUNT, migratedLevels.length);
        assertEquals(WeaponMasteryManager.WEAPON_COUNT, migratedXp.length);
        assertArrayEquals(new int[] {50, 0, 4}, java.util.Arrays.copyOf(migratedLevels, 3));
        assertArrayEquals(new int[] {0, 0, StatCalculator.getXpForNextLevel(4) - 1}, java.util.Arrays.copyOf(migratedXp, 3));
    }

    private static CompoundTag loadFixture(String resourcePath) throws Exception {
        try (InputStream inputStream = LegacyCapabilityMigrationTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Missing test fixture: " + resourcePath);
            }
            String snbt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return TagParser.parseTag(snbt);
        }
    }

    private static Set<Integer> readIntSet(ListTag values) {
        Set<Integer> result = new HashSet<>();
        for (int i = 0; i < values.size(); i++) {
            result.add(values.getInt(i));
        }
        return result;
    }
}
