package tong.statmod.storage;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatFamily;
import tong.statmod.stats.StatType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ModAttachmentsStatSerializerTest {
    @Test
    void readsLegacyPerStatPerkPointsIntoCanonicalFamilyPools() throws Exception {
        CompoundTag tag = new CompoundTag();
        int[] legacyPoints = new int[PlayerStatData.STAT_COUNT];
        legacyPoints[StatType.BRUTE_FORCE.index] = 2;
        legacyPoints[StatType.BLADE_TECHNIQUE.index] = 3;
        legacyPoints[StatType.ARCANE_POWER.index] = 1;
        tag.putIntArray("PerkPoints", legacyPoints);

        PlayerStatData data = read(tag);

        assertEquals(5, data.getPerkPointsForStat(StatType.BRUTE_FORCE.index));
        assertEquals(5, data.getPerkPointsForStat(StatType.BLADE_TECHNIQUE.index));
        assertEquals(1, data.getPerkPointsForStat(StatType.ARCANE_POWER.index));
    }

    @Test
    void writesCanonicalFamilyPerkPointsAsFamilySizedArray() throws Exception {
        PlayerStatData data = new PlayerStatData();
        data.setPerkPoints(StatType.BRUTE_FORCE.index, 4);
        data.setPerkPoints(StatType.ARCANE_POWER.index, 2);

        CompoundTag tag = write(data);
        int[] serialized = tag.getIntArray("PerkPoints");
        int[] expected = new int[StatFamily.values().length];
        expected[StatFamily.FRONTLINE_PHYSICAL_COMBAT.ordinal()] = 4;
        expected[StatFamily.MAGICAL_CORE.ordinal()] = 2;

        assertArrayEquals(expected, serialized);
    }

    private static PlayerStatData read(CompoundTag tag) throws Exception {
        Object serializer = serializerInstance();
        Method read = serializer.getClass().getDeclaredMethod("read",
                Class.forName("net.neoforged.neoforge.attachment.IAttachmentHolder"),
                CompoundTag.class,
                Class.forName("net.minecraft.core.HolderLookup$Provider"));
        read.setAccessible(true);
        return (PlayerStatData) read.invoke(serializer, null, tag, null);
    }

    private static CompoundTag write(PlayerStatData data) throws Exception {
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
