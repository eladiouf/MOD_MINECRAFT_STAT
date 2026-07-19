package tong.statmod.client.hunter;

import java.util.OptionalInt;
import tong.statmod.network.TrackedPreyMessage;

public final class TrackedPreyCache {
    private int entityId = TrackedPreyMessage.CLEAR_ENTITY_ID;
    private long expiresAt = Long.MIN_VALUE;

    public void accept(TrackedPreyMessage message, long currentTick) {
        if (message == null || message.isClear()) {
            clear();
            return;
        }
        entityId = message.entityId();
        long duration = message.durationTicks();
        expiresAt = currentTick > Long.MAX_VALUE - duration
                ? Long.MAX_VALUE : currentTick + duration;
    }

    public OptionalInt entityId(long currentTick) {
        if (entityId < 0 || currentTick >= expiresAt) {
            clear();
            return OptionalInt.empty();
        }
        return OptionalInt.of(entityId);
    }

    public void clear() {
        entityId = TrackedPreyMessage.CLEAR_ENTITY_ID;
        expiresAt = Long.MIN_VALUE;
    }
}
