package tong.statmod.storage;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;
import tong.statmod.stats.StatFamily;
import tong.statmod.stats.StatType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
        assertEquals(5, d.getMagicPoints());
    }

    @Test
    void only_arcane_pool_migrates_to_unified() {
        PlayerStatData d = new PlayerStatData();
        d.setArcanePoints(15);
        int migrated = d.migrateLegacyPointsToUnified();
        assertEquals(15, migrated);
        assertEquals(20, d.getMagicPoints());
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
        assertEquals(15, d.getMagicPoints());
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
        assertEquals(40, d.getMagicPoints());
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
        assertEquals(20, d.getMagicPoints());
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
        assertEquals(12, d.getMagicPoints(),
                "addArcanePoints is a compat shim — must pour into unified pool");
    }

    @Test
    void defaultValuesWhenLoadingOldSaveWithoutNewFields() throws Exception {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Levels", new int[PlayerStatData.STAT_COUNT]);
        tag.putIntArray("Xp", new int[PlayerStatData.STAT_COUNT]);
        // Old saves had per-stat perk point arrays (length 23)
        tag.putIntArray("PerkPoints", new int[StatType.values().length]);
        tag.putIntArray("UnlockedPerks", new int[0]);
        tag.putIntArray("FreeGrantedPerks", new int[0]);

        PlayerStatData data = readViaSerializer(tag);

        assertEquals(5, data.getMagicPoints(), "magicPoints defaults to 5 when missing from tag");
        assertEquals(0, data.getLastPerkGrantTier(), "lastPerkGrantTier defaults to 0 when missing");
        assertEquals(0, data.getMagicNodes().length, "magicNodes defaults to empty array");
        assertEquals(0, data.getLearnedSpells().length, "learnedSpells defaults to empty array");
        assertEquals(null, data.getMagicRace(), "magicRace defaults to null when missing");
        assertEquals(null, data.getChosenStartBranch(), "chosenStartBranch defaults to null when missing");
        assertEquals(0, data.getSchoolPracticeMasteryProgress(MagicBranch.FIRE),
                "practice mastery defaults to zero when missing from old saves");
    }

    @Test
    void roundTripPreservesAllNewFields() throws Exception {
        PlayerStatData data = new PlayerStatData();
        data.setMagicPoints(42);
        data.setLastPerkGrantTier(3);
        data.addMagicNode("node_fire_1");
        data.addMagicNode("node_water_2");
        data.learnSpell("spell_fireball");
        data.learnSpell("spell_heal");
        data.setMagicRace(MagicRace.DWARF);
        data.setChosenStartBranch(MagicBranch.EARTH);
        data.setSchoolMasteryProgress(MagicBranch.FIRE, 5);
        data.setSchoolMasteryProgress(MagicBranch.WATER, 3);
        data.setSchoolPracticeMasteryProgress(MagicBranch.FIRE, 7);
        data.setSchoolPracticeMasteryProgress(MagicBranch.WATER, 2);
        data.setSoulLevel(10);
        data.setLevel(StatType.BRUTE_FORCE.index, 5);
        data.setPerkPointsForFamily(StatFamily.FRONTLINE_PHYSICAL_COMBAT, 3);
        data.setLevel(StatType.ARCANE_POWER.index, 7);
        data.setPerkPointsForFamily(StatFamily.MAGICAL_CORE, 2);

        CompoundTag tag = writeViaSerializer(data);
        PlayerStatData restored = readViaSerializer(tag);

        assertEquals(data.getMagicPoints(), restored.getMagicPoints(), "magicPoints round-trip");
        assertEquals(data.getLastPerkGrantTier(), restored.getLastPerkGrantTier(), "lastPerkGrantTier round-trip");
        assertArrayEquals(data.getMagicNodes(), restored.getMagicNodes(), "magicNodes round-trip");
        assertArrayEquals(data.getLearnedSpells(), restored.getLearnedSpells(), "learnedSpells round-trip");
        assertEquals(data.getMagicRace(), restored.getMagicRace(), "magicRace round-trip");
        assertEquals(data.getChosenStartBranch(), restored.getChosenStartBranch(), "chosenStartBranch round-trip");
        assertEquals(data.getSchoolMasteryProgress(MagicBranch.FIRE),
                restored.getSchoolMasteryProgress(MagicBranch.FIRE), "schoolMastery FIRE round-trip");
        assertEquals(data.getSchoolMasteryProgress(MagicBranch.WATER),
                restored.getSchoolMasteryProgress(MagicBranch.WATER), "schoolMastery WATER round-trip");
        assertEquals(data.getSchoolPracticeMasteryProgress(MagicBranch.FIRE),
                restored.getSchoolPracticeMasteryProgress(MagicBranch.FIRE), "schoolPracticeMastery FIRE round-trip");
        assertEquals(data.getSchoolPracticeMasteryProgress(MagicBranch.WATER),
                restored.getSchoolPracticeMasteryProgress(MagicBranch.WATER), "schoolPracticeMastery WATER round-trip");
        assertEquals(data.getSoulLevel(), restored.getSoulLevel(), "soulLevel round-trip");
        assertEquals(data.getLevel(StatType.BRUTE_FORCE.index),
                restored.getLevel(StatType.BRUTE_FORCE.index), "level round-trip");
        assertEquals(data.getLevel(StatType.ARCANE_POWER.index),
                restored.getLevel(StatType.ARCANE_POWER.index), "level round-trip");
        assertArrayEquals(data.getPerkPoints(), restored.getPerkPoints(), "perkPoints round-trip");
    }

    private static PlayerStatData readViaSerializer(CompoundTag tag) throws Exception {
        Object serializer = serializerInstance();
        Method read = serializer.getClass().getDeclaredMethod("read",
                Class.forName("net.neoforged.neoforge.attachment.IAttachmentHolder"),
                CompoundTag.class,
                Class.forName("net.minecraft.core.HolderLookup$Provider"));
        read.setAccessible(true);
        return (PlayerStatData) read.invoke(serializer, null, tag, null);
    }

    private static CompoundTag writeViaSerializer(PlayerStatData data) throws Exception {
        Object serializer = serializerInstance();
        Method write = serializer.getClass().getDeclaredMethod("write",
                PlayerStatData.class,
                Class.forName("net.minecraft.core.HolderLookup$Provider"));
        write.setAccessible(true);
        return (CompoundTag) write.invoke(serializer, data, null);
    }

    private static Object serializerInstance() throws Exception {
        Class<?> serializerClass = Class.forName("tong.statmod.storage.ModAttachments$StatSerializer");
        Field instance = serializerClass.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        return instance.get(null);
    }
}
