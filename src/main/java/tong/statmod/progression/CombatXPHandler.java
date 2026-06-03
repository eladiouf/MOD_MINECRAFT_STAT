package tong.statmod.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.stats.StatType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class CombatXPHandler {
    private static final Map<UUID, HitTracker> hitTrackers = new HashMap<>();
    private static final Map<UUID, ComboTracker> comboTrackers = new HashMap<>();
    private static final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private static final Map<UUID, DamageSourceTracker> dmgSourceTrackers = new HashMap<>();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        // ★★ Intermediate: combo (3 hits in 2s)
        trackCombo(player);

        // ★★ Intermediate: overkill (would kill with >20 damage)
        if (event.getEntity().getHealth() - event.getAmount() <= 0 && event.getAmount() > 20) {
            ActionXpHelper.awardXp(player, StatType.BRUTE_FORCE.index, ActionXpHelper.XpTier.INTERMEDIATE);
        }

        // ★★ Intermediate: hit without being touched for 5s
        long now = System.currentTimeMillis();
        Long lastHit = lastDamageTime.get(player.getUUID());
        if (lastHit != null && now - lastHit > 5000) {
            ActionXpHelper.awardXp(player, StatType.BLADE_TECHNIQUE.index, ActionXpHelper.XpTier.INTERMEDIATE);
        }

        // ★★ Intermediate: rapid hits (5 hits in 2s)
        trackRapidHit(player);
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        lastDamageTime.put(player.getUUID(), System.currentTimeMillis());

        // ★ Common: take damage
        ActionXpHelper.awardXp(player, StatType.PHYSICAL_RESISTANCE.index, ActionXpHelper.XpTier.COMMON);

        // ★★ Intermediate: survive with <4 hearts
        if (player.getHealth() / player.getMaxHealth() < 0.2f) {
            ActionXpHelper.awardXp(player, StatType.PHYSICAL_RESISTANCE.index, ActionXpHelper.XpTier.INTERMEDIATE);
        }

        // ★★ Intermediate: block
        if (player.isBlocking()) {
            ActionXpHelper.awardXp(player, StatType.PHYSICAL_ENDURANCE.index, ActionXpHelper.XpTier.INTERMEDIATE);
        }

        // ★★ Intermediate: take damage from multiple sources
        trackDamageSources(player, event.getSource().getMsgId());

        // ★ Common: low hp willpower
        if (player.getHealth() / player.getMaxHealth() < 0.3f) {
            ActionXpHelper.awardXp(player, StatType.WILLPOWER.index, ActionXpHelper.XpTier.COMMON);
        }

        // ★★★ Rare: survive with 1/2 heart
        if (player.getHealth() - event.getAmount() <= 1.0f && player.getHealth() > 0) {
            ActionXpHelper.awardXp(player, StatType.WILLPOWER.index, ActionXpHelper.XpTier.RARE);
        }
    }

    private static void trackRapidHit(ServerPlayer player) {
        long now = System.currentTimeMillis();
        HitTracker tracker = hitTrackers.computeIfAbsent(player.getUUID(), k -> new HitTracker());
        tracker.addHit(now);
        if (tracker.getHitCount(2000) >= 5) {
            ActionXpHelper.awardXp(player, StatType.RAPIDITE.index, ActionXpHelper.XpTier.INTERMEDIATE);
            tracker.reset();
        }
    }

    private static void trackCombo(ServerPlayer player) {
        long now = System.currentTimeMillis();
        ComboTracker combo = comboTrackers.computeIfAbsent(player.getUUID(), k -> new ComboTracker());
        combo.addHit(now);
        if (combo.getHitCount(2000) >= 3) {
            ActionXpHelper.awardXp(player, StatType.BLADE_TECHNIQUE.index, ActionXpHelper.XpTier.INTERMEDIATE);
            combo.reset();
        }
    }

    private static void trackDamageSources(ServerPlayer player, String sourceType) {
        DamageSourceTracker tracker = dmgSourceTrackers.computeIfAbsent(player.getUUID(), k -> new DamageSourceTracker());
        tracker.recordSource(sourceType);
        if (tracker.getUniqueSourceCount() >= 3) {
            ActionXpHelper.awardXp(player, StatType.PHYSICAL_RESISTANCE.index, ActionXpHelper.XpTier.RARE);
            tracker.reset();
        }
    }

    private static class HitTracker {
        private final long[] hits = new long[20];
        private int index = 0;
        void addHit(long time) { hits[index % hits.length] = time; index++; }
        int getHitCount(long windowMs) {
            long threshold = System.currentTimeMillis() - windowMs;
            int count = 0;
            for (long h : hits) { if (h >= threshold) count++; }
            return count;
        }
        void reset() { for (int i = 0; i < hits.length; i++) hits[i] = 0; }
    }

    private static class ComboTracker {
        private final long[] hits = new long[10];
        private int index = 0;
        void addHit(long time) { hits[index % hits.length] = time; index++; }
        int getHitCount(long windowMs) {
            long threshold = System.currentTimeMillis() - windowMs;
            int count = 0;
            for (long h : hits) { if (h >= threshold) count++; }
            return count;
        }
        void reset() { for (int i = 0; i < hits.length; i++) hits[i] = 0; }
    }

    private static class DamageSourceTracker {
        private final java.util.HashSet<String> sources = new java.util.HashSet<>();
        private long lastReset = System.currentTimeMillis();
        void recordSource(String sourceType) {
            if (System.currentTimeMillis() - lastReset > 10000) { sources.clear(); lastReset = System.currentTimeMillis(); }
            sources.add(sourceType);
        }
        int getUniqueSourceCount() { return sources.size(); }
        void reset() { sources.clear(); lastReset = System.currentTimeMillis(); }
    }

    @SubscribeEvent
    public static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            hitTrackers.remove(player.getUUID());
            comboTrackers.remove(player.getUUID());
            lastDamageTime.remove(player.getUUID());
            dmgSourceTrackers.remove(player.getUUID());
        }
    }
}
