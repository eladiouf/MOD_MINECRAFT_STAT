package tong.statmod.network;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpellBindingRequestThrottle {
    public static final long INTERVAL_TICKS = 10L;
    private static final ConcurrentHashMap<UUID, Long> LAST_REQUEST = new ConcurrentHashMap<>();

    private SpellBindingRequestThrottle() {
    }

    public static boolean allow(UUID playerId, long gameTime) {
        if (playerId == null) {
            return false;
        }
        Long previous = LAST_REQUEST.get(playerId);
        if (previous != null && gameTime >= previous && gameTime - previous < INTERVAL_TICKS) {
            return false;
        }
        LAST_REQUEST.put(playerId, gameTime);
        return true;
    }

    public static void clear(UUID playerId) {
        if (playerId != null) {
            LAST_REQUEST.remove(playerId);
        }
    }

    static void clearAll() {
        LAST_REQUEST.clear();
    }
}
