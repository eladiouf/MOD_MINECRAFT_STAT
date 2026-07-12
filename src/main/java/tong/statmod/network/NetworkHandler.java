package tong.statmod.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import tong.statmod.STATMod;

public class NetworkHandler {

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");

        registrar.playToClient(SyncPerksPayload.TYPE, SyncPerksPayload.CODEC,
                (payload, context) -> ClientPayloadHandler.handleSyncPerks(payload, context));

        registrar.playToClient(BatchSyncPayload.TYPE, BatchSyncPayload.CODEC,
                (payload, context) -> ClientPayloadHandler.handleBatchSync(payload, context));

        registrar.playToServer(UnlockPerkPayload.TYPE, UnlockPerkPayload.CODEC,
                ServerPayloadHandler::handleUnlockPerk);

        registrar.playToServer(OpenPerkTreePayload.TYPE, OpenPerkTreePayload.CODEC,
                ServerPayloadHandler::handleOpenPerkTree);

        registrar.playToClient(StatUpdatePayload.TYPE, StatUpdatePayload.CODEC,
                (payload, context) -> ClientPayloadHandler.handleStatUpdate(payload, context));

        registrar.playToClient(StaminaSyncPayload.TYPE, StaminaSyncPayload.CODEC,
                (payload, context) -> ClientPayloadHandler.handleStaminaSync(payload, context));

        registrar.playToClient(PerkFeedbackPayload.TYPE, PerkFeedbackPayload.CODEC,
                (payload, context) -> ClientPayloadHandler.handlePerkFeedback(payload, context));

        registrar.playToClient(LearnBookPayload.TYPE, LearnBookPayload.CODEC,
                (payload, context) -> ClientPayloadHandler.handleLearnBook(payload, context));

        registrar.playToClient(SyncMagicPayload.TYPE, SyncMagicPayload.CODEC,
                (payload, context) -> ClientPayloadHandler.handleSyncMagic(payload, context));

        registrar.playToServer(UnlockMagicNodePayload.TYPE, UnlockMagicNodePayload.CODEC,
                ServerPayloadHandler::handleUnlockMagicNode);

        registrar.playToServer(OpenVirtualInscriptionPayload.TYPE, OpenVirtualInscriptionPayload.CODEC,
                ServerPayloadHandler::handleOpenVirtualInscription);

        registrar.playToClient(BridgeTensuraSkillPayload.TYPE, BridgeTensuraSkillPayload.CODEC,
                (payload, context) -> ClientPayloadHandler.handleBridgeTensuraSkill(payload, context));

        registrar.playToClient(OpenMageCodexPayload.TYPE, OpenMageCodexPayload.CODEC,
                (payload, context) -> ClientPayloadHandler.handleOpenMageCodex(payload, context));

        registrar.playToServer(ChangeStartBranchPayload.TYPE, ChangeStartBranchPayload.CODEC,
                ServerPayloadHandler::handleChangeStartBranch);

        registrar.playToClient(OpenExchangePayload.TYPE, OpenExchangePayload.CODEC,
                (payload, context) -> ClientPayloadHandler.handleOpenExchange(payload, context));
        registrar.playToServer(ConvertPointsPayload.TYPE, ConvertPointsPayload.CODEC,
                ServerPayloadHandler::handleConvertPoints);
        registrar.playToClient(OpenMagicBankPayload.TYPE, OpenMagicBankPayload.CODEC,
                (payload, context) -> ClientPayloadHandler.handleOpenMagicBank(payload, context));
        registrar.playToServer(MagicBankActionPayload.TYPE, MagicBankActionPayload.CODEC,
                ServerPayloadHandler::handleMagicBankAction);
    }
}
