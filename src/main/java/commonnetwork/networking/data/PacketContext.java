package commonnetwork.networking.data;

import net.minecraft.server.level.ServerPlayer;

public record PacketContext<T>(T message, Side side, ServerPlayer sender) {
}
