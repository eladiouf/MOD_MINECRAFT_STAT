package tong.statmod.perks;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PerkState {
    private static final Map<UUID, Map<Integer, Long>> cooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<Integer, Long>> comboCounters = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<Integer, Long>> hitMobTracker = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> crouchStart = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> effectProcessing = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> killStreak = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastKillTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<Integer>> trackedTargets = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastDodgeTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> frenzyStacks = new ConcurrentHashMap<>();

    public static boolean isOnCooldown(UUID uuid, int perkId, long cooldownMs) {
        Map<Integer, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return false;
        Long expiry = playerCooldowns.get(perkId);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    public static void setCooldown(UUID uuid, int perkId) {
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(perkId, System.currentTimeMillis());
    }

    public static void setCooldown(UUID uuid, int perkId, long durationMs) {
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(perkId, System.currentTimeMillis() + durationMs);
    }

    public static void recordComboHit(UUID uuid, long now, long windowMs) {
        Map<Integer, Long> map = comboCounters.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        // Drop expired entries; key by tick id (size+1) so unique entries accumulate within window.
        map.entrySet().removeIf(e -> now - e.getValue() > windowMs);
        map.put(map.size() + 1, now);
    }

    public static int getComboCount(UUID uuid, long now, long windowMs) {
        Map<Integer, Long> playerCombos = comboCounters.get(uuid);
        if (playerCombos == null) return 0;
        int count = 0;
        long threshold = now - windowMs;
        for (long time : playerCombos.values()) {
            if (time >= threshold) count++;
        }
        return count;
    }

    public static void noteTrackedHit(UUID uuid, int mobId) {
        hitMobTracker.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(mobId, System.currentTimeMillis());
    }

    public static int getTrackedHitCount(UUID uuid, long windowMs) {
        Map<Integer, Long> tracker = hitMobTracker.get(uuid);
        if (tracker == null) return 0;
        long threshold = System.currentTimeMillis() - windowMs;
        return (int) tracker.values().stream().filter(t -> t >= threshold).count();
    }

    public static void setCrouching(UUID uuid) { crouchStart.put(uuid, System.currentTimeMillis()); }
    public static void clearCrouching(UUID uuid) { crouchStart.remove(uuid); }
    public static long getCrouchStart(UUID uuid) { return crouchStart.getOrDefault(uuid, 0L); }

    public static boolean tryBeginEffectProcessing(UUID uuid) {
        return effectProcessing.putIfAbsent(uuid, Boolean.TRUE) == null;
    }
    public static void endEffectProcessing(UUID uuid) { effectProcessing.remove(uuid); }

    public static void recordKill(UUID uuid) {
        long now = System.currentTimeMillis();
        if (now - lastKillTime.getOrDefault(uuid, 0L) < 5000) {
            killStreak.merge(uuid, 1, Integer::sum);
        } else {
            killStreak.put(uuid, 1);
        }
        lastKillTime.put(uuid, now);
    }

    public static int getKillStreak(UUID uuid) { return killStreak.getOrDefault(uuid, 0); }
    public static void resetKillStreak(UUID uuid) { killStreak.remove(uuid); }

    public static void addTrackedTarget(UUID uuid, int entityId) {
        trackedTargets.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(entityId);
    }
    public static boolean isTrackedTarget(UUID uuid, int entityId) {
        Set<Integer> targets = trackedTargets.get(uuid);
        return targets != null && targets.contains(entityId);
    }

    public static void setLastDodge(UUID uuid) { lastDodgeTime.put(uuid, System.currentTimeMillis()); }
    public static long getLastDodgeTime(UUID uuid) { return lastDodgeTime.getOrDefault(uuid, 0L); }

    public static int getFrenzyStacks(UUID uuid) { return frenzyStacks.getOrDefault(uuid, 0); }
    public static void addFrenzyStack(UUID uuid) { frenzyStacks.merge(uuid, 1, Integer::sum); }
    public static void resetFrenzy(UUID uuid) { frenzyStacks.remove(uuid); }

    public static void clearPlayer(UUID uuid) {
        cooldowns.remove(uuid);
        comboCounters.remove(uuid);
        hitMobTracker.remove(uuid);
        crouchStart.remove(uuid);
        effectProcessing.remove(uuid);
        killStreak.remove(uuid);
        lastKillTime.remove(uuid);
        trackedTargets.remove(uuid);
        lastDodgeTime.remove(uuid);
        frenzyStacks.remove(uuid);
    }
}
