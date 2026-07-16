package tong.statmod.client.hunter;

import java.util.List;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.client.ClientStatsState;
import tong.statmod.effects.HunterPerceptionRules;
import tong.statmod.network.TrackedPreyMessage;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatValue;

public final class ClientHunterPerception {
    static final int SCAN_INTERVAL_TICKS = 5;
    static final int MAX_THREATS = 64;
    private static final List<String> TRACKING_PERKS = List.of(
            "statmod:tracking_25", "statmod:tracking_50", "statmod:tracking_75");
    private static final List<String> KEEN_SENSES_PERKS = List.of(
            "statmod:keen_senses_25", "statmod:keen_senses_50", "statmod:keen_senses_75");
    private static final TrackedPreyCache MARK = new TrackedPreyCache();
    private static long clientTick;
    private static ClientLevel lastLevel;
    private static List<Integer> threatEntityIds = List.of();

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

    public static void tick(Minecraft minecraft) {
        tickClock();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || !player.isAlive() || player.isSpectator()) {
            clear();
            return;
        }
        if (lastLevel != level) {
            MARK.clear();
            threatEntityIds = List.of();
            lastLevel = level;
        }

        ClientStatsState state = ClientStatsCache.state();
        int trackingLevel = statLevel(state, StatType.TRACKING);
        double trackingRange = HunterPerceptionRules.trackingRangeBlocks(
                trackingLevel, perkScore(state.activePerkIds(), TRACKING_PERKS));
        validateMarkedPrey(level, player, trackingRange);

        if (!player.isCrouching()) {
            threatEntityIds = List.of();
            return;
        }
        int keenLevel = statLevel(state, StatType.KEEN_SENSES);
        double keenRange = HunterPerceptionRules.keenSensesRangeBlocks(
                keenLevel, perkScore(state.activePerkIds(), KEEN_SENSES_PERKS));
        if (!(keenRange > 0.0D) || !Double.isFinite(keenRange)) {
            threatEntityIds = List.of();
            return;
        }
        if (clientTick % SCAN_INTERVAL_TICKS != 0L) {
            return;
        }

        List<ThreatCandidate> candidates = level
                .getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(keenRange))
                .stream()
                .filter(entity -> entity != player)
                .map(entity -> new ThreatCandidate(
                        entity.getId(),
                        player.distanceToSqr(entity),
                        entity instanceof Enemy,
                        entity.isAlive(),
                        entity.isRemoved()))
                .toList();
        threatEntityIds = List.copyOf(HunterThreatSelection.select(candidates, keenRange, MAX_THREATS));
    }

    public static OptionalInt markedEntityId() {
        return MARK.entityId(clientTick);
    }

    public static List<Integer> threatEntityIds() {
        return threatEntityIds;
    }

    public static void clear() {
        MARK.clear();
        clientTick = 0L;
        lastLevel = null;
        threatEntityIds = List.of();
    }

    private static void validateMarkedPrey(ClientLevel level, LocalPlayer player, double trackingRange) {
        OptionalInt markedId = MARK.entityId(clientTick);
        if (markedId.isEmpty()) {
            return;
        }
        if (!(trackingRange > 0.0D) || !Double.isFinite(trackingRange)) {
            MARK.clear();
            return;
        }
        if (!(level.getEntity(markedId.getAsInt()) instanceof LivingEntity marked)
                || !(marked instanceof Enemy)
                || !marked.isAlive()
                || marked.isRemoved()
                || player.distanceToSqr(marked) > trackingRange * trackingRange) {
            MARK.clear();
        }
    }

    private static int statLevel(ClientStatsState state, StatType type) {
        return state.values().getOrDefault(type, new StatValue(0, 0)).level();
    }

    private static double perkScore(List<String> activePerkIds, List<String> relevantPerks) {
        long milestones = relevantPerks.stream().filter(activePerkIds::contains).count();
        return milestones * 0.25D;
    }
}
