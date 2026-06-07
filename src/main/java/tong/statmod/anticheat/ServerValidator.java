package tong.statmod.anticheat;

import net.minecraft.server.level.ServerPlayer;
import tong.statmod.STATMod;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.capability.PlayerStats;
import java.util.*;

/**
 * Server-side anti-cheat for stat modification.
 * Validates packets, detects impossible values, and logs violations.
 */
public class ServerValidator {
    private static final Map<UUID, Long> lastXpAward = new HashMap<>();
    private static final Map<UUID, Integer> xpPerSecond = new HashMap<>();
    private static final int MAX_XP_PER_SECOND = 500;
    private static final int MAX_LEVEL = 100;

    public static boolean isValidStatIndex(int statIndex) {
        return statIndex >= 0 && statIndex < PlayerStats.STAT_COUNT;
    }

    public static boolean validateSetLevel(ServerPlayer player, int statIndex, int newLevel) {
        if (!isValidStatIndex(statIndex)) {
            warn(player, "Invalid stat index " + statIndex
                + " (must be 0-" + (PlayerStats.STAT_COUNT - 1) + ")");
            return false;
        }
        if (newLevel < 0 || newLevel > MAX_LEVEL) {
            warn(player, "Invalid level " + newLevel + " for stat " + statIndex);
            return false;
        }
        return true;
    }

    public static boolean validateXpAward(ServerPlayer player, int amount) {
        UUID id = player.getUUID();
        long now = System.currentTimeMillis();
        Long last = lastXpAward.get(id);
        if (last != null && now - last < 1000) {
            int current = xpPerSecond.merge(id, amount, Integer::sum);
            if (current > MAX_XP_PER_SECOND) {
                warn(player, "XP rate limit exceeded: " + current + "/s (max " + MAX_XP_PER_SECOND + ")");
                return false;
            }
        } else {
            xpPerSecond.put(id, amount);
            lastXpAward.put(id, now);
        }
        return true;
    }

    public static void validateAllStats(ServerPlayer player) {
        CapabilityHelper.withStats(player, stats -> {
            for (int i = 0; i < PlayerStats.STAT_COUNT; i++) {
                int level = stats.getLevel(i);
                if (level < 0 || level > MAX_LEVEL) {
                    warn(player, "Corrupted stat " + i + " = " + level + " — resetting");
                    stats.setLevel(i, 0);
                    stats.setXp(i, 0);
                }
            }
        });
    }

    private static void warn(ServerPlayer player, String msg) {
        STATMod.LOGGER.warn("[AntiCheat] {} ({}): {}",
            player.getDisplayName().getString(), player.getUUID(), msg);
    }

    public static void cleanup(UUID uuid) {
        lastXpAward.remove(uuid);
        xpPerSecond.remove(uuid);
    }
}
