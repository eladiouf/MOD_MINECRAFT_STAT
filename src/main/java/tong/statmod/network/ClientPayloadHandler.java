package tong.statmod.network;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import tong.statmod.STATMod;
import tong.statmod.client.ClientMagicCache;
import tong.statmod.client.ClientPerkCache;
import tong.statmod.client.ClientStatCache;
import tong.statmod.client.ClientStaminaCache;
import tong.statmod.client.gui.PerkFeedbackToast;

@OnlyIn(Dist.CLIENT)
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    public static void handleSyncPerks(SyncPerksPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPerkCache.update(payload.perkIds(), payload.perStatPoints()));
    }

    public static void handleBatchSync(BatchSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientStatCache.updateAll(payload.levels(), payload.xp(), payload.soulLevel());
            ClientPerkCache.update(payload.perkIds(), payload.perStatPoints());
        });
    }

    public static void handleStatUpdate(StatUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientStatCache.updateAll(payload.levels(), payload.xp(), payload.soulLevel()));
    }

    public static void handleStaminaSync(StaminaSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientStaminaCache.update(
                payload.currentStamina(),
                payload.fatigueDebt(),
                payload.meditating()));
    }

    public static void handlePerkFeedback(PerkFeedbackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PerkFeedbackToast.show(
                Component.literal(payload.title()),
                Component.literal(payload.message())));
    }

    public static void handleSyncMagic(SyncMagicPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientMagicCache.update(
                payload.magicNodes(), payload.learnedSpells(),
                payload.magicPoints(), payload.masteryProgress(),
                payload.raceOrdinal(), payload.startBranchOrdinal()));
    }

    /** Ouvre le Codex du Mage sur le client (Mission J). */
    public static void handleOpenMageCodex(OpenMageCodexPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> net.minecraft.client.Minecraft.getInstance()
                .setScreen(new tong.statmod.client.codex.MageCodexScreen()));
    }

    /**
     * Door for the reverse bridge — emulate a Tensura keybind press/release on the
     * client so Tensura's full native flow runs (magic circle, charge, projectile).
     */
    public static void handleBridgeTensuraSkill(BridgeTensuraSkillPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!ModList.get().isLoaded("tensura")) return;
            try {
                ResourceLocation rl = ResourceLocation.parse(payload.tensuraSkillId());
                if (payload.release()) {
                    SkillAPI.skillReleasePacket(rl, 0, 0);
                } else {
                    SkillAPI.skillActivationPacket(rl, 0, 0);
                }
            } catch (Throwable t) {
                STATMod.LOGGER.warn("Bridge Tensura skill {} ({}) failed: {}",
                        payload.tensuraSkillId(), payload.release() ? "release" : "press", t.toString());
            }
        });
    }
}
