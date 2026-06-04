package tong.statmod.perks;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PerkState {
    public static final Map<UUID, Map<Integer, Long>> cooldowns = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> comboCounter = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> comboTimer = new ConcurrentHashMap<>();
    public static final Map<UUID, Set<Integer>> hitMobTracker = new ConcurrentHashMap<>();
    public static final Set<UUID> effectProcessing = ConcurrentHashMap.newKeySet();

    private PerkState() {}

    public static boolean isOnCooldown(UUID uuid, int perkId, long ms) {
        Map<Integer, Long> pc = cooldowns.get(uuid);
        if (pc == null) return false;
        Long last = pc.get(perkId);
        return last != null && (System.currentTimeMillis() - last) < ms;
    }

    public static void setCooldown(UUID uuid, int perkId) {
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(perkId, System.currentTimeMillis());
    }

    public static void clearPlayer(UUID uuid) {
        cooldowns.remove(uuid);
        comboCounter.remove(uuid);
        comboTimer.remove(uuid);
        hitMobTracker.remove(uuid);
    }
}
