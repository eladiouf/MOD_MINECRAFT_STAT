package tong.statmod.perks;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PerkState {
    private static final Map<UUID, Map<Integer, Long>> cooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> comboCounter = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> comboTimer = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<Integer>> hitMobTracker = new ConcurrentHashMap<>();
    private static final Set<UUID> effectProcessing = ConcurrentHashMap.newKeySet();

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

    public static int recordComboHit(UUID uuid, long now, long resetWindowMs) {
        Long lastCombo = comboTimer.get(uuid);
        int count = comboCounter.getOrDefault(uuid, 0);
        if (lastCombo == null || (now - lastCombo) > resetWindowMs) {
            count = 0;
        }
        count++;
        comboCounter.put(uuid, count);
        comboTimer.put(uuid, now);
        return count;
    }

    public static int getComboCount(UUID uuid) {
        return comboCounter.getOrDefault(uuid, 0);
    }

    public static void resetCombo(UUID uuid) {
        comboCounter.remove(uuid);
        comboTimer.remove(uuid);
    }

    public static void noteTrackedHit(UUID uuid, int entityId) {
        hitMobTracker.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(entityId);
    }

    public static boolean hasTrackedHit(UUID uuid, int entityId) {
        Set<Integer> hitMobs = hitMobTracker.get(uuid);
        return hitMobs != null && hitMobs.contains(entityId);
    }

    public static boolean tryBeginEffectProcessing(UUID uuid) {
        return effectProcessing.add(uuid);
    }

    public static void finishEffectProcessing(UUID uuid) {
        effectProcessing.remove(uuid);
    }

    public static boolean isEffectProcessing(UUID uuid) {
        return effectProcessing.contains(uuid);
    }

    public static void clearPlayer(UUID uuid) {
        cooldowns.remove(uuid);
        comboCounter.remove(uuid);
        comboTimer.remove(uuid);
        hitMobTracker.remove(uuid);
        effectProcessing.remove(uuid);
    }
}
