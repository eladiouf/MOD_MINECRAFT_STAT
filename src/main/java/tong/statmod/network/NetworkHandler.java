package tong.statmod.network;

import commonnetwork.api.Network;
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
        Network.registerCompatPayloads(event);

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

        registrar.playToClient(SyncMagicPayload.TYPE, SyncMagicPayload.CODEC,
                ClientPayloadHandler::handleSyncMagic);

        registrar.playToServer(UnlockMagicNodePayload.TYPE, UnlockMagicNodePayload.CODEC,
                ServerPayloadHandler::handleUnlockMagicNode);

        registrar.playToServer(OpenVirtualInscriptionPayload.TYPE, OpenVirtualInscriptionPayload.CODEC,
                ServerPayloadHandler::handleOpenVirtualInscription);
    }
}
