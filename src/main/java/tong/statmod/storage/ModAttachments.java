package tong.statmod.storage;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.core.HolderLookup;
import org.jetbrains.annotations.NotNull;
import tong.statmod.STATMod;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, STATMod.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerStatData>> STATS =
            ATTACHMENTS.register("stats", () ->
                    AttachmentType.builder(PlayerStatData::new)
                            .serialize(StatSerializer.INSTANCE)
                            .build());

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }

    private static final class StatSerializer implements IAttachmentSerializer<CompoundTag, PlayerStatData> {
        static final StatSerializer INSTANCE = new StatSerializer();

        @Override
        public @NotNull PlayerStatData read(net.neoforged.neoforge.attachment.IAttachmentHolder holder,
                                            @NotNull CompoundTag tag,
                                            @NotNull HolderLookup.Provider provider) {
            PlayerStatData data = new PlayerStatData();
            int[] levels = tag.getIntArray("Levels");
            int[] xp = tag.getIntArray("Xp");
            int[] points = tag.getIntArray("PerkPoints");
            for (int i = 0; i < PlayerStatData.STAT_COUNT; i++) {
                if (i < levels.length) data.setLevel(i, levels[i]);
                if (i < xp.length) data.setXp(i, xp[i]);
                if (i < points.length) data.setPerkPoints(i, points[i]);
            }
            int[] unlocked = tag.getIntArray("UnlockedPerks");
            if (unlocked.length > 0) data.setUnlockedPerks(unlocked);
            if (tag.contains("SoulLevel")) data.setSoulLevel(tag.getInt("SoulLevel"));
            return data;
        }

        @Override
        public CompoundTag write(@NotNull PlayerStatData data, @NotNull HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            tag.putIntArray("Levels", data.getLevels());
            tag.putIntArray("Xp", data.getXp());
            tag.putIntArray("PerkPoints", data.getPerkPoints());
            tag.putIntArray("UnlockedPerks", data.getUnlockedPerks());
            tag.putInt("SoulLevel", data.getSoulLevel());
            return tag;
        }
    }
}
