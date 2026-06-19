package commonnetwork.api;

import net.minecraft.server.level.ServerPlayer;

public interface NetworkHandler {
    void sendToClient(Object message, ServerPlayer player);

    void sendToServer(Object message);
}
