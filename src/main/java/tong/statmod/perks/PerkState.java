package tong.statmod.perks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PerkState {
    private static final Map<UUID, Map<Integer, Long>> cooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> comboCounter = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> comboTimer = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<Integer>> hitMobTracker = new ConcurrentHashMap<>();
    private static final Set<UUID> effectProcessing = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, ParryState> parryStates = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> crouchStartMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> nextHitBoostMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> killRecordMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> killCountMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> arcAttackMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> arcTicksRemaining = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> afterRollHitsMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> frenzyActiveMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> enduranceTracker = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> dodgeCountMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> territoryActiveMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<Integer, Long>> markedTargets = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> arrowTimer = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> arrowCountMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> eatenFoods = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> foodTimer = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> sprintDistanceMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> colosseActive = new ConcurrentHashMap<>();

    private PerkState() {}

    // ==================== COOLDOWNS ====================

    public static boolean isOnCooldown(UUID uuid, int perkId, long ms) {
        Map<Integer, Long> pc = cooldowns.get(uuid);
        if (pc == null) return false;
        Long last = pc.get(perkId);
        return last != null && (System.currentTimeMillis() - last) < ms;
    }

    public static void setCooldown(UUID uuid, int perkId) {
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(perkId, System.currentTimeMillis());
    }

    // ==================== COMBO ====================

    public static int recordComboHit(UUID uuid, long now, long resetWindowMs) {
        Long lastCombo = comboTimer.get(uuid);
        int count = comboCounter.getOrDefault(uuid, 0);
        if (lastCombo == null || (now - lastCombo) > resetWindowMs) count = 0;
        count++;
        comboCounter.put(uuid, count);
        comboTimer.put(uuid, now);
        return count;
    }

    public static int getComboCount(UUID uuid) { return comboCounter.getOrDefault(uuid, 0); }

    // ==================== TRACKING ====================

    public static void noteTrackedHit(UUID uuid, int entityId) {
        hitMobTracker.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(entityId);
    }

    public static boolean hasTrackedHit(UUID uuid, int entityId) {
        Set<Integer> hitMobs = hitMobTracker.get(uuid);
        return hitMobs != null && hitMobs.contains(entityId);
    }

    // ==================== EFFECT PROCESSING ====================

    public static boolean tryBeginEffectProcessing(UUID uuid) { return effectProcessing.add(uuid); }
    public static void finishEffectProcessing(UUID uuid) { effectProcessing.remove(uuid); }
    public static boolean isEffectProcessing(UUID uuid) { return effectProcessing.contains(uuid); }

    // ==================== PARRY ====================

    public static boolean isParryActive(UUID uuid) { return parryStates.getOrDefault(uuid, ParryState.INACTIVE) == ParryState.ACTIVE; }
    public static void setParryActive(UUID uuid, boolean active) {
        parryStates.put(uuid, active ? ParryState.ACTIVE : ParryState.INACTIVE);
    }

    // ==================== CROUCH ====================

    public static int getCrouchStart(UUID uuid) { return crouchStartMap.getOrDefault(uuid, 0); }
    public static void setCrouchStart(UUID uuid, int tick) { crouchStartMap.put(uuid, tick); }

    // ==================== NEXT HIT BOOST ====================

    public static float getNextHitBoost(UUID uuid) { return nextHitBoostMap.getOrDefault(uuid, 1.0f); }
    public static void setNextHitBoost(UUID uuid, float boost) { nextHitBoostMap.put(uuid, boost); }

    // ==================== KILL TRACKING ====================

    public static void recordKill(UUID uuid, long now, long windowMs) {
        Long lastKill = killRecordMap.get(uuid);
        if (lastKill == null || (now - lastKill) > windowMs) killCountMap.put(uuid, 1);
        else killCountMap.merge(uuid, 1, Integer::sum);
        killRecordMap.put(uuid, now);
    }

    public static int getRecentKills(UUID uuid, long windowMs) {
        Long lastKill = killRecordMap.get(uuid);
        if (lastKill == null || (System.currentTimeMillis() - lastKill) > windowMs) return 0;
        return killCountMap.getOrDefault(uuid, 0);
    }

    // ==================== ARC ATTACK ====================

    public static void setArcAttack(UUID uuid, boolean active, int ticks) {
        arcAttackMap.put(uuid, active);
        if (active) arcTicksRemaining.put(uuid, ticks);
    }

    public static boolean isArcAttackActive(UUID uuid) {
        return arcAttackMap.getOrDefault(uuid, false);
    }

    // ==================== AFTER ROLL ====================

    public static int getAfterRollHits(UUID uuid) { return afterRollHitsMap.getOrDefault(uuid, 0); }
    public static void setAfterRollHits(UUID uuid, int hits) { afterRollHitsMap.put(uuid, hits); }
    public static void decrementAfterRollHits(UUID uuid) {
        afterRollHitsMap.merge(uuid, -1, Integer::sum);
        if (afterRollHitsMap.get(uuid) <= 0) afterRollHitsMap.remove(uuid);
    }

    // ==================== FRENZY ====================

    public static boolean isFrenzyActive(UUID uuid) { return frenzyActiveMap.getOrDefault(uuid, false); }
    public static void setFrenzyActive(UUID uuid, boolean active) { frenzyActiveMap.put(uuid, active); }

    // ==================== ENDURANCE TRACKING ====================

    public static int getEndurance(UUID uuid) { return (int)(float)enduranceTracker.getOrDefault(uuid, 100f); }
    public static void restoreEndurance(UUID uuid, int amount) {
        enduranceTracker.merge(uuid, (float)amount, Float::sum);
    }

    // ==================== DODGE ====================

    public static int getDodgeCount(UUID uuid) { return dodgeCountMap.getOrDefault(uuid, 0); }
    public static void incrementDodge(UUID uuid) { dodgeCountMap.merge(uuid, 1, Integer::sum); }

    // ==================== TERRITORY ====================

    public static boolean isTerritoryActive(UUID uuid) { return territoryActiveMap.getOrDefault(uuid, false); }
    public static void setTerritoryActive(UUID uuid, boolean active) { territoryActiveMap.put(uuid, active); }

    // ==================== MARKED TARGETS ====================

    public static void markTarget(UUID uuid, int entityId, int durationTicks) {
        markedTargets.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(entityId,
            System.currentTimeMillis() + durationTicks * 50L);
    }

    public static boolean isTargetMarked(UUID uuid, int entityId) {
        Map<Integer, Long> targets = markedTargets.get(uuid);
        if (targets == null) return false;
        Long expiry = targets.get(entityId);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            targets.remove(entityId);
            return false;
        }
        return true;
    }

    // ==================== ARROWS ====================

    public static int getConsecutiveArrows(UUID uuid) {
        Long last = arrowTimer.get(uuid);
        if (last == null || (System.currentTimeMillis() - last) > 20000) return 0;
        return arrowCountMap.getOrDefault(uuid, 0);
    }

    public static void recordArrowShot(UUID uuid, long now, long windowMs) {
        Long last = arrowTimer.get(uuid);
        if (last == null || (now - last) > windowMs) arrowCountMap.put(uuid, 1);
        else arrowCountMap.merge(uuid, 1, Integer::sum);
        arrowTimer.put(uuid, now);
    }

    public static void resetArrows(UUID uuid) { arrowCountMap.remove(uuid); arrowTimer.remove(uuid); }

    // ==================== FOOD TRACKING ====================

    public static void recordFoodEaten(UUID uuid, String foodId) {
        eatenFoods.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(foodId);
        foodTimer.put(uuid, System.currentTimeMillis());
    }

    public static int getDistinctFoods(UUID uuid, long windowMs) {
        Long last = foodTimer.get(uuid);
        if (last == null || (System.currentTimeMillis() - last) > windowMs) return 0;
        Set<String> foods = eatenFoods.get(uuid);
        return foods == null ? 0 : foods.size();
    }

    // ==================== SPRINT DISTANCE ====================

    public static float getSprintDistance(UUID uuid) { return sprintDistanceMap.getOrDefault(uuid, 0f); }
    public static void addSprintDistance(UUID uuid, float dist) { sprintDistanceMap.merge(uuid, dist, Float::sum); }
    public static void resetSprintDistance(UUID uuid) { sprintDistanceMap.remove(uuid); }

    // ==================== COLOSSE ====================

    public static boolean isColosseActive(UUID uuid) { return colosseActive.getOrDefault(uuid, false); }
    public static void setColosseActive(UUID uuid, boolean active) { colosseActive.put(uuid, active); }

    // ==================== CLEANUP ====================

    public static void clearPlayer(UUID uuid) {
        cooldowns.remove(uuid);
        comboCounter.remove(uuid);
        comboTimer.remove(uuid);
        hitMobTracker.remove(uuid);
        effectProcessing.remove(uuid);
        parryStates.remove(uuid);
        crouchStartMap.remove(uuid);
        nextHitBoostMap.remove(uuid);
        killRecordMap.remove(uuid);
        killCountMap.remove(uuid);
        arcAttackMap.remove(uuid);
        arcTicksRemaining.remove(uuid);
        afterRollHitsMap.remove(uuid);
        frenzyActiveMap.remove(uuid);
        enduranceTracker.remove(uuid);
        dodgeCountMap.remove(uuid);
        territoryActiveMap.remove(uuid);
        markedTargets.remove(uuid);
        arrowTimer.remove(uuid);
        arrowCountMap.remove(uuid);
        eatenFoods.remove(uuid);
        foodTimer.remove(uuid);
        sprintDistanceMap.remove(uuid);
        colosseActive.remove(uuid);
    }

    private enum ParryState { ACTIVE, INACTIVE }
}
