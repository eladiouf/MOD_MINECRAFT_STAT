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
import tong.statmod.integration.elementals.ElementalBranch;
import tong.statmod.integration.elementals.ElementalsMageData;
import tong.statmod.stats.StatFamily;
import tong.statmod.stamina.StaminaData;

import java.util.EnumSet;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, STATMod.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerStatData>> STATS =
            ATTACHMENTS.register("stats", () ->
                    AttachmentType.builder(PlayerStatData::new)
                            .serialize(StatSerializer.INSTANCE)
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<StaminaData>> STAMINA =
            ATTACHMENTS.register("stamina", () ->
                    AttachmentType.builder(StaminaData::new)
                            .serialize(StaminaSerializer.INSTANCE)
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ElementalsMageData>> ELEMENTALS_MAGE =
            ATTACHMENTS.register("elementals_mage", () ->
                    AttachmentType.builder(ElementalsMageData::new)
                            .serialize(ElementalsMageSerializer.INSTANCE)
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
            }
            if (points.length == PlayerStatData.STAT_COUNT) {
                for (int i = 0; i < PlayerStatData.STAT_COUNT; i++) {
                    data.addPerkPointsForStat(i, points[i]);
                }
            } else {
                StatFamily[] families = StatFamily.values();
                for (int i = 0; i < families.length && i < points.length; i++) {
                    data.setPerkPointsForFamily(families[i], points[i]);
                }
            }
            int[] unlocked = tag.getIntArray("UnlockedPerks");
            if (unlocked.length > 0) data.setUnlockedPerks(unlocked);
            int[] freeGranted = tag.getIntArray("FreeGrantedPerks");
            if (freeGranted.length > 0) data.setFreeGrantedPerks(freeGranted);
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
            tag.putIntArray("FreeGrantedPerks", data.getFreeGrantedPerks());
            tag.putInt("SoulLevel", data.getSoulLevel());
            return tag;
        }
    }

    private static final class StaminaSerializer implements IAttachmentSerializer<CompoundTag, StaminaData> {
        static final StaminaSerializer INSTANCE = new StaminaSerializer();

        @Override
        public @NotNull StaminaData read(net.neoforged.neoforge.attachment.IAttachmentHolder holder,
                                         @NotNull CompoundTag tag,
                                         @NotNull HolderLookup.Provider provider) {
            StaminaData data = new StaminaData();
            if (tag.contains("CurrentStamina")) data.setCurrentStamina(tag.getFloat("CurrentStamina"));
            if (tag.contains("FatigueDebt")) data.setFatigueDebt(tag.getFloat("FatigueDebt"));
            if (tag.contains("Meditating")) data.setMeditating(tag.getBoolean("Meditating"));
            return data;
        }

        @Override
        public CompoundTag write(@NotNull StaminaData data, @NotNull HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            tag.putFloat("CurrentStamina", data.currentStamina());
            tag.putFloat("FatigueDebt", data.fatigueDebt());
            tag.putBoolean("Meditating", data.meditating());
            return tag;
        }
    }

    private static final class ElementalsMageSerializer implements IAttachmentSerializer<CompoundTag, ElementalsMageData> {
        static final ElementalsMageSerializer INSTANCE = new ElementalsMageSerializer();

        @Override
        public @NotNull ElementalsMageData read(net.neoforged.neoforge.attachment.IAttachmentHolder holder,
                                                @NotNull CompoundTag tag,
                                                @NotNull HolderLookup.Provider provider) {
            ElementalsMageData data = new ElementalsMageData();
            data.setMageAwakened(tag.getBoolean("MageAwakened"));
            if (tag.contains("StarterRaceId")) data.setStarterRaceId(tag.getString("StarterRaceId"));
            data.setStarterBranches(fromOrdinals(tag.getIntArray("StarterBranches")));
            data.setUnlockedBranches(fromOrdinals(tag.getIntArray("UnlockedBranches")));
            data.setRewardedRareBranches(fromOrdinals(tag.getIntArray("RewardedRareBranches")));
            if (tag.contains("LastSeenChi")) data.setLastSeenChi(tag.getFloat("LastSeenChi"));
            if (tag.contains("LastSeenXp")) data.setLastSeenXp(tag.getFloat("LastSeenXp"));
            if (tag.contains("LastSeenLevel")) data.setLastSeenLevel(tag.getInt("LastSeenLevel"));
            if (tag.contains("LastSeenActiveBranch")) data.setLastSeenActiveBranch(fromOrdinal(tag.getInt("LastSeenActiveBranch")));
            return data;
        }

        @Override
        public CompoundTag write(@NotNull ElementalsMageData data, @NotNull HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("MageAwakened", data.mageAwakened());
            tag.putString("StarterRaceId", data.starterRaceId());
            tag.putIntArray("StarterBranches", ordinals(data.starterBranches()));
            tag.putIntArray("UnlockedBranches", ordinals(data.unlockedBranches()));
            tag.putIntArray("RewardedRareBranches", ordinals(data.rewardedRareBranches()));
            tag.putFloat("LastSeenChi", data.lastSeenChi());
            tag.putFloat("LastSeenXp", data.lastSeenXp());
            tag.putInt("LastSeenLevel", data.lastSeenLevel());
            if (data.lastSeenActiveBranch() != null) {
                tag.putInt("LastSeenActiveBranch", data.lastSeenActiveBranch().ordinal());
            }
            return tag;
        }

        private static int[] ordinals(EnumSet<ElementalBranch> branches) {
            return branches.stream().mapToInt(Enum::ordinal).toArray();
        }

        private static EnumSet<ElementalBranch> fromOrdinals(int[] ordinals) {
            EnumSet<ElementalBranch> branches = EnumSet.noneOf(ElementalBranch.class);
            for (int ordinal : ordinals) {
                ElementalBranch branch = fromOrdinal(ordinal);
                if (branch != null) {
                    branches.add(branch);
                }
            }
            return branches;
        }

        private static ElementalBranch fromOrdinal(int ordinal) {
            if (ordinal < 0 || ordinal >= ElementalBranch.values().length) {
                return null;
            }
            return ElementalBranch.values()[ordinal];
        }
    }
}
