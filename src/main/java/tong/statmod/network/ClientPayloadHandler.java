package tong.statmod.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    public static void handleSyncPerks(SyncPerksPayload payload, IPayloadContext context) {
        ClientPayloadActions.handleSyncPerks(payload, context);
    }

    public static void handleBatchSync(BatchSyncPayload payload, IPayloadContext context) {
        ClientPayloadActions.handleBatchSync(payload, context);
    }

    public static void handleStatUpdate(StatUpdatePayload payload, IPayloadContext context) {
        ClientPayloadActions.handleStatUpdate(payload, context);
    }

    public static void handleStaminaSync(StaminaSyncPayload payload, IPayloadContext context) {
        ClientPayloadActions.handleStaminaSync(payload, context);
    }

    public static void handlePerkFeedback(PerkFeedbackPayload payload, IPayloadContext context) {
        ClientPayloadActions.handlePerkFeedback(payload, context);
    }

    
    public static void handleLearnBook(LearnBookPayload payload, IPayloadContext context) {
        ClientPayloadActions.handleLearnBook(payload, context);
    }

    public static void handleSyncMagic(SyncMagicPayload payload, IPayloadContext context) {
        ClientPayloadActions.handleSyncMagic(payload, context);
    }

    public static void handleManaSync(ManaSyncPayload payload, IPayloadContext context) {
        ClientPayloadActions.handleManaSync(payload, context);
    }

    /** Ouvre le Codex du Mage sur le client (Mission J). */
    public static void handleOpenMageCodex(OpenMageCodexPayload payload, IPayloadContext context) {
        ClientPayloadActions.handleOpenMageCodex(payload, context);
    }

    /** Ouvre/rafraîchit l'écran d'échange points → coins (Mission M6 shop). */
    public static void handleOpenExchange(OpenExchangePayload payload, IPayloadContext context) {
        ClientPayloadActions.handleOpenExchange(payload, context);
    }

    public static void handleOpenMagicBank(OpenMagicBankPayload payload, IPayloadContext context) {
        ClientPayloadActions.handleOpenMagicBank(payload, context);
    }

    /**
     * Door for the reverse bridge — emulate a Tensura keybind press/release on the
     * client so Tensura's full native flow runs (magic circle, charge, projectile).
     */
    public static void handleOpenForgeShop(OpenForgeShopPayload payload, IPayloadContext context) {
        ClientPayloadActions.handleOpenForgeShop(payload, context);
    }

    public static void handleBridgeTensuraSkill(BridgeTensuraSkillPayload payload, IPayloadContext context) {
        ClientPayloadActions.handleBridgeTensuraSkill(payload, context);
    }
}
