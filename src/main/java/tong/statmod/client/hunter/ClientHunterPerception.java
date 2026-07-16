package tong.statmod.client.hunter;

import java.util.OptionalInt;
import tong.statmod.network.TrackedPreyMessage;

public final class ClientHunterPerception {
    private static final TrackedPreyCache MARK = new TrackedPreyCache();
    private static long clientTick;

    private ClientHunterPerception() {
    }

    public static void accept(TrackedPreyMessage message) {
        MARK.accept(message, clientTick);
    }

    public static void tickClock() {
        if (clientTick < Long.MAX_VALUE) {
            clientTick++;
        }
    }

    public static OptionalInt markedEntityId() {
        return MARK.entityId(clientTick);
    }

    public static void clear() {
        MARK.clear();
        clientTick = 0L;
    }
}
