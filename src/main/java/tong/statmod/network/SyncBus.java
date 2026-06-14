package tong.statmod.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache server-side de l'état des perks unlocked par joueur.
 * Stocke un BitSet sérialisé sous forme d'int[] ids pour collaborer avec PerkManager
 * en attendant qu'un attachment dédié soit créé.
 */
public final class SyncBus {
    private static final Map<UUID, int[]> cache = new ConcurrentHashMap<>();

    private SyncBus() {}

    public static int[] cachedUnlockedIds(ServerPlayer player) {
        return cachedUnlockedIds(player.getUUID());
    }

    public static int[] cachedUnlockedIds(UUID uuid) {
        return cache.getOrDefault(uuid, new int[0]);
    }

    public static void cacheUnlockedIds(ServerPlayer player, int[] ids) {
        cache.put(player.getUUID(), ids.clone());
    }

    public static void clear(UUID uuid) {
        cache.remove(uuid);
    }
}
