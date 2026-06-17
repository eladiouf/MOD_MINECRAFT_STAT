package tong.statmod.integration;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.event.TensuraEntityEvents;
import io.github.manasmods.tensura.event.TensuraSkillEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import tong.statmod.STATMod;
import tong.statmod.integration.tensura.TensuraSkillGate;
import tong.statmod.integration.tensura.TempBuffManager;
import tong.statmod.network.SyncHelper;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkManager;
import tong.statmod.sound.SoundHelper;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public final class TensuraEventSubscriber {
    private static final Map<String, int[]> INTRINSIC_PERK_REWARDS = Map.of(
            "tensura:ogre_berserker", new int[] { Perk.BRUTE_CORE.id, Perk.BRUTE_ACTIVE.id },
            "tensura:giantification", new int[] { Perk.ENDUR_CORE.id },
            "tensura:dragon_skin", new int[] { Perk.RESIST_CORE.id },
            "tensura:dragon_eye", new int[] { Perk.SENSE_CORE.id, Perk.PRECI_CORE.id },
            "tensura:dragon_ear", new int[] { Perk.TRACK_CORE.id },
            "tensura:body_armor", new int[] { Perk.RESIST_CORE.id },
            "tensura:charm", new int[] { Perk.INTIM_CORE.id },
            "tensura:unpredictability", new int[] { Perk.AGIL_CORE.id }
    );

    private TensuraEventSubscriber() {}

    public static void register() {
        TensuraSkillEvents.SKILL_LEARNING.register(TensuraEventSubscriber::onSkillLearned);
        TensuraEntityEvents.AWAKENING_EVENT.register(TensuraEventSubscriber::onAwakening);
        TensuraEntityEvents.NAMING_EVENT.register(TensuraEventSubscriber::onNaming);
    }

    private static EventResult onSkillLearned(ManasSkillInstance instance, LivingEntity entity, int slot, double mastery, Changeable<Double> cost) {
        if (!(entity instanceof Player player)) return EventResult.pass();
        if (player.level().isClientSide) return EventResult.pass();

        ResourceLocation skillId = instance.getSkillId();
        PlayerStatData data = player.getData(ModAttachments.STATS);
        PerkManager perks = new PerkManager(data);

        for (Perk perk : Perk.values()) {
            if (perks.isUnlocked(perk)) continue;
            String required = TensuraSkillGate.skillForPerk(perk.id);
            if (required != null && required.equals(skillId.toString())) {
                if (perks.unlock(perk, player)) {
                    STATMod.LOGGER.info("Auto-unlocked perk {} for {} via skill {}",
                            perk.name, player.getName().getString(), skillId);
                }
            }
        }

        if (isUltimateSkillId(skillId.toString())) {
            unlockTranscendencePerks(player, perks);
        }

        SyncHelper.syncAll((ServerPlayer) player);
        return EventResult.pass();
    }

    private static EventResult onAwakening(LivingEntity entity, Changeable<Boolean> changeable) {
        if (entity instanceof ServerPlayer player) {
            PlayerStatData data = player.getData(ModAttachments.STATS);
            int tensuraSoul = PlayerDataBridge.getSoulLevel(player);
            if (data.getSoulLevel() != tensuraSoul) {
                data.setSoulLevel(tensuraSoul);
                STATMod.LOGGER.debug("Soul level synced on awakening for {}: {}",
                        player.getName().getString(), tensuraSoul);
            }
            TempBuffManager.grantAwakeningBuff(player, player.level().getGameTime());
            SyncHelper.syncStats(player);
        }
        return EventResult.pass();
    }

    private static EventResult onNaming(LivingEntity entity, Player player, Changeable<Double> health, Changeable<Double> energy,
                                        Changeable<io.github.manasmods.tensura.network.c2s.RequestNamingMenuPacket.NamingType> type,
                                        Changeable<String> name) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return EventResult.pass();
        }

        unlockIntrinsicPerks(serverPlayer);
        return EventResult.pass();
    }

    public static boolean isUltimateSkillId(String skillId) {
        if (skillId == null) return false;
        String lower = skillId.toLowerCase();
        return lower.contains("ultimate") || lower.contains("evolution") || lower.contains("transcend");
    }

    private static void unlockTranscendencePerks(Player player, PerkManager perks) {
        for (Perk perk : Perk.values()) {
            if (perk.tier != tong.statmod.perks.PerkTier.TRANSCENDENCE || perks.isUnlocked(perk)) {
                continue;
            }
            if (perks.grant(perk, player)) {
                STATMod.LOGGER.info("Granted transcendence perk {} to {} via ultimate skill",
                        perk.name, player.getName().getString());
            }
        }
    }

    public static void unlockIntrinsicPerks(Player player) {
        unlockIntrinsicPerks(player, PlayerDataBridge.getRaceInstance(player)
                .map(race -> race.getIntrinsicSkills(player).stream()
                        .map(skill -> skill.getRegistryName().toString())
                        .collect(java.util.stream.Collectors.toSet()))
                .orElse(Set.of()), true);
    }

    public static void unlockIntrinsicPerks(Player player, Set<String> intrinsicSkills) {
        unlockIntrinsicPerks(player, intrinsicSkills, true);
    }

    public static void unlockIntrinsicPerks(Player player, Set<String> intrinsicSkills, boolean sync) {
        if (player == null || player.level().isClientSide) {
            return;
        }

        PlayerStatData data = player.getData(ModAttachments.STATS);
        PerkManager perks = new PerkManager(data);

        for (String skillId : intrinsicSkills) {
            int[] perkIds = intrinsicPerkIdsForSkill(skillId);
            if (perkIds == null) continue;
            for (int perkId : perkIds) {
                Perk perk = Perk.byId(perkId);
                if (perk != null && !perks.isUnlocked(perk)) {
                    perks.grant(perk, player);
                    STATMod.LOGGER.info("Granted intrinsic perk {} to {} via {}",
                            perk.name, player.getName().getString(), skillId);
                }
            }
        }

        if (sync && player instanceof ServerPlayer serverPlayer) {
            SyncHelper.syncPerks(serverPlayer);
        }
    }

    static int[] intrinsicPerkIdsForSkill(String skillId) {
        int[] perkIds = INTRINSIC_PERK_REWARDS.get(skillId);
        return perkIds == null ? null : perkIds.clone();
    }

    public static Set<Integer> intrinsicPerkIdsForSkills(Set<String> skillIds) {
        Set<Integer> perkIds = new HashSet<>();
        if (skillIds == null) {
            return perkIds;
        }

        for (String skillId : skillIds) {
            int[] mapped = intrinsicPerkIdsForSkill(skillId);
            if (mapped == null) {
                continue;
            }
            for (int perkId : mapped) {
                perkIds.add(perkId);
            }
        }

        return perkIds;
    }
}
