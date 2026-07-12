package tong.statmod.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class ClientCacheLifecycle {
    private ClientCacheLifecycle() {}

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetAll();
    }

    private static void resetAll() {
        ClientStatCache.reset();
        ClientPerkCache.reset();
        ClientStaminaCache.reset();
        ClientMagicCache.reset();
    }
}
