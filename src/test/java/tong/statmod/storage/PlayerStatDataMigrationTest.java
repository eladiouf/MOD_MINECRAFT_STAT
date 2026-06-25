package tong.statmod.storage;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Vérifie la migration des saves pré-Mission-δ : ancien {@code arcanePoints} +
 * {@code schoolPoints} → nouveau pool unifié {@code magicPoints}, capped à
 * {@link PlayerStatData#MIGRATION_CAP}.
 */
@SuppressWarnings("deprecation")
class PlayerStatDataMigrationTest {

    @Test
    void empty_legacy_pools_migrate_to_zero() {
        PlayerStatData d = new PlayerStatData();
        int migrated = d.migrateLegacyPointsToUnified();
        assertEquals(0, migrated);
        assertEquals(0, d.getMagicPoints());
    }

    @Test
    void only_arcane_pool_migrates_to_unified() {
        PlayerStatData d = new PlayerStatData();
        d.setArcanePoints(15);
        int migrated = d.migrateLegacyPointsToUnified();
        assertEquals(15, migrated);
        assertEquals(15, d.getMagicPoints());
        assertEquals(0, d.getArcanePoints(), "legacy arcane drained to 0");
    }

    @Test
    void only_school_pools_migrate_summed() {
        PlayerStatData d = new PlayerStatData();
        d.setSchoolPoints(MagicBranch.FIRE, 5);
        d.setSchoolPoints(MagicBranch.WATER, 3);
        d.setSchoolPoints(MagicBranch.AIR, 2);
        int migrated = d.migrateLegacyPointsToUnified();
        assertEquals(10, migrated);
        assertEquals(10, d.getMagicPoints());
        assertEquals(0, d.getSchoolPoints(MagicBranch.FIRE));
        assertEquals(0, d.getSchoolPoints(MagicBranch.WATER));
    }

    @Test
    void arcane_plus_schools_combined() {
        PlayerStatData d = new PlayerStatData();
        d.setArcanePoints(20);
        d.setSchoolPoints(MagicBranch.FIRE, 10);
        d.setSchoolPoints(MagicBranch.HOLY, 5);
        int migrated = d.migrateLegacyPointsToUnified();
        assertEquals(35, migrated);
        assertEquals(35, d.getMagicPoints());
    }

    @Test
    void migration_respects_cap_for_legacy_dev_stockpile() {
        PlayerStatData d = new PlayerStatData();
        d.setArcanePoints(500);
        d.setSchoolPoints(MagicBranch.FIRE, 500);
        int migrated = d.migrateLegacyPointsToUnified();
        assertEquals(1000, migrated, "the raw legacy total is reported");
        assertEquals(PlayerStatData.MIGRATION_CAP, d.getMagicPoints(),
                "actual magicPoints must respect the cap");
    }

    @Test
    void migration_is_idempotent() {
        PlayerStatData d = new PlayerStatData();
        d.setArcanePoints(15);
        d.migrateLegacyPointsToUnified();
        int second = d.migrateLegacyPointsToUnified();
        assertEquals(0, second, "second call should be a no-op (legacy pools already drained)");
        assertEquals(15, d.getMagicPoints());
    }

    @Test
    void unified_points_already_set_are_preserved_on_migration() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicPoints(20);
        d.setArcanePoints(10);
        int migrated = d.migrateLegacyPointsToUnified();
        assertEquals(10, migrated);
        assertEquals(30, d.getMagicPoints(),
                "existing unified pool should accumulate with migrated legacy");
    }

    @Test
    void addArcanePoints_writes_to_unified_pool() {
        PlayerStatData d = new PlayerStatData();
        d.addArcanePoints(7);
        assertEquals(7, d.getMagicPoints(),
                "addArcanePoints is a compat shim — must pour into unified pool");
    }
}
