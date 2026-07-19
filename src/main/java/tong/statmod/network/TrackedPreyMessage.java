package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;

public record TrackedPreyMessage(int entityId, int durationTicks) {
    public static final int CLEAR_ENTITY_ID = -1;
    public static final int MAX_DURATION_TICKS = 400;

    public TrackedPreyMessage {
        boolean clear = entityId == CLEAR_ENTITY_ID && durationTicks == 0;
        boolean mark = entityId >= 0 && durationTicks > 0
                && durationTicks <= MAX_DURATION_TICKS;
        if (!clear && !mark) {
            throw new IllegalArgumentException("invalid tracked prey payload");
        }
    }

    public static TrackedPreyMessage clear() {
        return new TrackedPreyMessage(CLEAR_ENTITY_ID, 0);
    }

    public boolean isClear() {
        return entityId == CLEAR_ENTITY_ID;
    }

    public static void encode(TrackedPreyMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeVarInt(message.durationTicks);
    }

    public static TrackedPreyMessage decode(FriendlyByteBuf buffer) {
        return new TrackedPreyMessage(buffer.readVarInt(), buffer.readVarInt());
    }
}
