package tong.statmod.network;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import tong.statmod.STATMod;
import tong.statmod.client.*;
import tong.statmod.client.codex.MageCodexScreen;
import tong.statmod.client.gui.PerkFeedbackToast;

@OnlyIn(Dist.CLIENT)
final class ClientPayloadActions {
    private ClientPayloadActions() {}

    static void handleSyncPerks(SyncPerksPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPerkCache.update(payload.perkIds(), payload.perStatPoints()));
    }

    static void handleBatchSync(BatchSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientStatCache.updateAll(payload.levels(), payload.xp(), payload.soulLevel(),
                    ClientStatCache.getDungeonPoints(), ClientStatCache.getDungeonFloorReached());
            ClientPerkCache.update(payload.perkIds(), payload.perStatPoints());
        });
    }

    static void handleStatUpdate(StatUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientStatCache.updateAll(payload.levels(), payload.xp(),
                payload.soulLevel(), payload.dungeonPoints(), payload.dungeonFloorReached()));
    }

    static void handleStaminaSync(StaminaSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientStaminaCache.update(payload.currentStamina(), payload.fatigueDebt(), payload.meditating()));
    }

    static void handlePerkFeedback(PerkFeedbackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PerkFeedbackToast.show(Component.literal(payload.title()), Component.literal(payload.message())));
    }

    static void handleManaSync(ManaSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientManaCache.update(payload.currentMana(), payload.maxMana()));
    }

    static void handleLearnBook(LearnBookPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gameRenderer != null && minecraft.player != null) {
                minecraft.gameRenderer.displayItemActivation(new ItemStack(Items.ENCHANTED_BOOK));
            }
        });
    }

    static void handleSyncMagic(SyncMagicPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientMagicCache.update(payload.magicNodes(), payload.learnedSpells(),
                payload.magicPoints(), payload.masteryProgress(), payload.raceOrdinal(), payload.startBranchOrdinal()));
    }

    static void handleOpenMageCodex(OpenMageCodexPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new MageCodexScreen()));
    }

    static void handleOpenExchange(OpenExchangePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PointExchangeScreen.openOrRefresh(payload.points(), payload.coins(), payload.rate()));
    }

    static void handleBridgeTensuraSkill(BridgeTensuraSkillPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!ModList.get().isLoaded("tensura")) return;
            try {
                ResourceLocation id = ResourceLocation.parse(payload.tensuraSkillId());
                if (payload.release()) SkillAPI.skillReleasePacket(id, 0, 0);
                else SkillAPI.skillActivationPacket(id, 0, 0);
            } catch (Throwable error) {
                STATMod.LOGGER.warn("Bridge Tensura skill {} ({}) failed: {}", payload.tensuraSkillId(),
                        payload.release() ? "release" : "press", error.toString());
            }
        });
    }
}
