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
import tong.statmod.network.SyncHelper;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkManager;
import tong.statmod.sound.SoundHelper;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

public final class TensuraEventSubscriber {
    private TensuraEventSubscriber() {}

    public static void register() {
        TensuraSkillEvents.SKILL_LEARNING.register(TensuraEventSubscriber::onSkillLearned);
        TensuraEntityEvents.AWAKENING_EVENT.register(TensuraEventSubscriber::onAwakening);
    }

    private static EventResult onSkillLearned(ManasSkillInstance instance, LivingEntity entity, int slot, double mastery, Changeable<Double> cost) {
        if (!(entity instanceof Player player)) return EventResult.pass();
        if (player.level().isClientSide) return EventResult.pass();

        ResourceLocation skillId = instance.getSkillId();
        PlayerStatData data = player.getData(ModAttachments.STATS);
        PerkManager perks = new PerkManager(data);

        for (Perk perk : Perk.values()) {
            if (perks.isUnlocked(perk)) continue;
            String required = SkillPerkGate.skillForPerk(perk.id);
            if (required != null && required.equals(skillId.toString())) {
                if (perks.unlock(perk, player)) {
                    STATMod.LOGGER.info("Auto-unlocked perk {} for {} via skill {}",
                            perk.name, player.getName().getString(), skillId);
                }
            }
        }

        SyncHelper.syncStats((ServerPlayer) player);
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
            SyncHelper.syncStats(player);
        }
        return EventResult.pass();
    }
}
