package tong.statmod.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import tong.statmod.STATMod;

@EventBusSubscriber(modid = STATMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(SyncPerksPayload.TYPE, SyncPerksPayload.CODEC,
                ClientPayloadHandler::handleSyncPerks);

        registrar.playToClient(BatchSyncPayload.TYPE, BatchSyncPayload.CODEC,
                ClientPayloadHandler::handleBatchSync);

        registrar.playToServer(UnlockPerkPayload.TYPE, UnlockPerkPayload.CODEC,
                ServerPayloadHandler::handleUnlockPerk);

        registrar.playToServer(OpenPerkTreePayload.TYPE, OpenPerkTreePayload.CODEC,
                ServerPayloadHandler::handleOpenPerkTree);

        registrar.playToClient(StatUpdatePayload.TYPE, StatUpdatePayload.CODEC,
                ClientPayloadHandler::handleStatUpdate);

        registrar.playToClient(StaminaSyncPayload.TYPE, StaminaSyncPayload.CODEC,
                ClientPayloadHandler::handleStaminaSync);

        registrar.playToClient(PerkFeedbackPayload.TYPE, PerkFeedbackPayload.CODEC,
                ClientPayloadHandler::handlePerkFeedback);
    }
}
