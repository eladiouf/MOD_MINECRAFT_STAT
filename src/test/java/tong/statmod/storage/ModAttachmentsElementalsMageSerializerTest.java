package tong.statmod.storage;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import tong.statmod.integration.elementals.ElementalBranch;
import tong.statmod.integration.elementals.ElementalsMageData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModAttachmentsElementalsMageSerializerTest {
    @Test
    void readsFreeGrantedRewardBranchesFromTag() throws Exception {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("RewardedRareBranches", new int[]{
                ElementalBranch.LIGHTNING.ordinal(),
                ElementalBranch.METAL.ordinal()
        });
        tag.putIntArray("FreeGrantedRewardBranches", new int[]{
                ElementalBranch.LIGHTNING.ordinal()
        });

        ElementalsMageData data = read(tag);

        assertEquals(EnumSet.of(ElementalBranch.LIGHTNING, ElementalBranch.METAL), data.rewardedRareBranches());
        assertEquals(EnumSet.of(ElementalBranch.LIGHTNING), data.freeGrantedRewardBranches());
    }

    @Test
    void writesFreeGrantedRewardBranchesToTag() throws Exception {
        ElementalsMageData data = new ElementalsMageData();
        data.setRewardedRareBranches(EnumSet.of(ElementalBranch.LIGHTNING, ElementalBranch.METAL));
        data.setFreeGrantedRewardBranches(EnumSet.of(ElementalBranch.METAL));

        CompoundTag tag = write(data);

        assertTrue(containsOrdinal(tag.getIntArray("RewardedRareBranches"), ElementalBranch.LIGHTNING.ordinal()));
        assertTrue(containsOrdinal(tag.getIntArray("RewardedRareBranches"), ElementalBranch.METAL.ordinal()));
        assertEquals(1, tag.getIntArray("FreeGrantedRewardBranches").length);
        assertTrue(containsOrdinal(tag.getIntArray("FreeGrantedRewardBranches"), ElementalBranch.METAL.ordinal()));
    }

    private static ElementalsMageData read(CompoundTag tag) throws Exception {
        Object serializer = serializerInstance();
        Method read = serializer.getClass().getDeclaredMethod("read",
                Class.forName("net.neoforged.neoforge.attachment.IAttachmentHolder"),
                CompoundTag.class,
                Class.forName("net.minecraft.core.HolderLookup$Provider"));
        read.setAccessible(true);
        return (ElementalsMageData) read.invoke(serializer, null, tag, null);
    }

    private static CompoundTag write(ElementalsMageData data) throws Exception {
        Object serializer = serializerInstance();
        Method write = serializer.getClass().getDeclaredMethod("write",
                ElementalsMageData.class,
                Class.forName("net.minecraft.core.HolderLookup$Provider"));
        write.setAccessible(true);
        return (CompoundTag) write.invoke(serializer, data, null);
    }

    private static Object serializerInstance() throws Exception {
        Class<?> serializerClass = Class.forName("tong.statmod.storage.ModAttachments$ElementalsMageSerializer");
        Field instance = serializerClass.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        return instance.get(null);
    }

    private static boolean containsOrdinal(int[] values, int target) {
        for (int value : values) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }
}
