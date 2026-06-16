package tong.statmod.integration.tensura;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TempBuffManager {
    private static final long AWAKENING_BUFF_DURATION_TICKS = 20L * 30L;
    private static final int AWAKENING_BUFF_BONUS = 10;
    private static final Map<UUID, Long> awakeningBuffExpiresAt = new ConcurrentHashMap<>();

    private TempBuffManager() {}

    public static void grantAwakeningBuff(Player player, long currentGameTime) {
        if (player == null) return;
        grantAwakeningBuff(player.getUUID(), currentGameTime);
    }

    public static int getAwakeningBonus(Player player, long currentGameTime) {
        if (player == null) return 0;
        return getAwakeningBonus(player.getUUID(), currentGameTime);
    }

    public static void grantAwakeningBuff(UUID playerId, long currentGameTime) {
        if (playerId == null) return;
        awakeningBuffExpiresAt.put(playerId, currentGameTime + AWAKENING_BUFF_DURATION_TICKS);
    }

    public static int getAwakeningBonus(UUID playerId, long currentGameTime) {
        if (playerId == null) return 0;
        Long expiresAt = awakeningBuffExpiresAt.get(playerId);
        if (expiresAt == null) return 0;
        if (currentGameTime > expiresAt) {
            awakeningBuffExpiresAt.remove(playerId);
            return 0;
        }
        return AWAKENING_BUFF_BONUS;
    }

    public static void clear(Player player) {
        if (player != null) {
            awakeningBuffExpiresAt.remove(player.getUUID());
        }
    }
}
