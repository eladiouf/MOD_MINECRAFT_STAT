package tong.statmod.dungeon.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.dungeon.DungeonDimensions;
import tong.statmod.dungeon.DungeonEnemyHealthBalance;
import tong.statmod.dungeon.DungeonMobSpawner;
import tong.statmod.dungeon.DungeonTeleportHandler;
import tong.statmod.dungeon.ai.living.DungeonLivingActor;
import tong.statmod.dungeon.ai.living.DungeonLivingGoals;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class DungeonEncounterDirector {
    private static final int PERIOD = 10;
    private static final int MAX_ACTORS_PER_FLOOR = 64;
    private static final Map<Integer, Integer> MANAGED_COUNTS = new HashMap<>();
    private static int ticks;

    private record Observation(Mob source, ServerPlayer target, long tick) {}

    private DungeonEncounterDirector() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++ticks % PERIOD != 0) return;
        ServerLevel level = event.getServer().getLevel(DungeonDimensions.TRIAL_DUNGEON);
        if (level == null) return;

        List<DungeonDirectorPolicy.PlayerSnapshot> snapshots = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            snapshots.add(new DungeonDirectorPolicy.PlayerSnapshot(
                    DungeonTeleportHandler.floorAtPos(player.getBlockX(), player.getBlockZ()),
                    player.isCreative(), player.isSpectator(), player.isAlive()));
        }
        Set<Integer> occupied = DungeonDirectorPolicy.occupiedFloors(snapshots);
        MANAGED_COUNTS.keySet().retainAll(occupied);
        for (int floor : occupied) coordinateFloor(level, floor);
    }

    private static void coordinateFloor(ServerLevel level, int floor) {
        BlockPos center = DungeonTeleportHandler.floorSpawnPos(floor);
        AABB bounds = new AABB(center).inflate(DungeonMobSpawner.FLOOR_SCAN_RADIUS);
        List<Mob> actors = level.getEntitiesOfClass(Mob.class, bounds, mob ->
                        mob.isAlive()
                                && mob.getPersistentData().contains(DungeonAiActor.FACTION_TAG)
                                && mob.getPersistentData().getInt(DungeonAiActor.FLOOR_TAG) == floor)
                .stream().limit(MAX_ACTORS_PER_FLOOR).toList();
        MANAGED_COUNTS.put(floor, actors.size());
        if (actors.isEmpty()) return;
        for (Mob actor : actors) {
            DungeonLivingGoals.ensureAttached(actor);
            if (!DungeonLivingActor.isNonCombat(actor)) {
                DungeonEnemyHealthBalance.apply(actor);
                DungeonTacticalGoals.ensureAttached(actor);
            }
        }

        List<ServerPlayer> players = level.players().stream()
                .filter(player -> player.isAlive() && !player.isCreative() && !player.isSpectator())
                .filter(player -> DungeonTeleportHandler.floorAtPos(
                        player.getBlockX(), player.getBlockZ()) == floor)
                .toList();
        long now = level.getGameTime();
        Map<String, Observation> squadSightings = new HashMap<>();

        for (Mob actor : actors) {
            if (DungeonAiActor.faction(actor) == DungeonFaction.INHABITANTS) {
                actor.setTarget(null);
                setAlert(actor, DungeonAlertState.IDLE);
                continue;
            }
            ServerPlayer visible = nearestVisible(actor, players);
            boolean lowHealth = actor.getHealth() <= actor.getMaxHealth() * 0.20F;
            if (visible != null) {
                remember(actor, visible, now);
                actor.setTarget(visible);
                String squad = actor.getPersistentData().getString(DungeonAiActor.SQUAD_TAG);
                squadSightings.put(squad, new Observation(actor, visible, now));
                setAlert(actor, DungeonAlertPolicy.next(
                        DungeonAiActor.alert(actor), true, false, true, lowHealth));
            } else {
                long seenTick = actor.getPersistentData().getLong(DungeonAiActor.LAST_SEEN_TICK);
                boolean recent = seenTick > 0L && now - seenTick <= DungeonAlertPolicy.MEMORY_TICKS;
                setAlert(actor, DungeonAlertPolicy.next(
                        DungeonAiActor.alert(actor), false, false, recent, lowHealth));
                if (recent && !lowHealth && actor.getNavigation().isDone()) {
                    actor.getNavigation().moveTo(
                            actor.getPersistentData().getDouble(DungeonAiActor.LAST_SEEN_X),
                            actor.getPersistentData().getDouble(DungeonAiActor.LAST_SEEN_Y),
                            actor.getPersistentData().getDouble(DungeonAiActor.LAST_SEEN_Z), 1.0D);
                }
            }
        }

        for (Mob actor : actors) {
            if (DungeonAiActor.faction(actor) == DungeonFaction.INHABITANTS) continue;
            if (DungeonAiActor.alert(actor) == DungeonAlertState.COMBAT) continue;
            Observation sighting = squadSightings.get(
                    actor.getPersistentData().getString(DungeonAiActor.SQUAD_TAG));
            if (sighting == null || !DungeonAlertPolicy.canShare(
                    sighting.tick(), now, actor.distanceToSqr(sighting.source()))) continue;
            remember(actor, sighting.target(), sighting.tick());
            boolean lowHealth = actor.getHealth() <= actor.getMaxHealth() * 0.20F;
            setAlert(actor, DungeonAlertPolicy.next(
                    DungeonAiActor.alert(actor), false, true, true, lowHealth));
        }
    }

    private static ServerPlayer nearestVisible(Mob actor, List<ServerPlayer> players) {
        ServerPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : players) {
            double distance = actor.distanceToSqr(player);
            if (distance < bestDistance && actor.hasLineOfSight(player)) {
                best = player;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static void remember(Mob actor, ServerPlayer target, long tick) {
        actor.getPersistentData().putDouble(DungeonAiActor.LAST_SEEN_X, target.getX());
        actor.getPersistentData().putDouble(DungeonAiActor.LAST_SEEN_Y, target.getY());
        actor.getPersistentData().putDouble(DungeonAiActor.LAST_SEEN_Z, target.getZ());
        actor.getPersistentData().putLong(DungeonAiActor.LAST_SEEN_TICK, tick);
    }

    private static void setAlert(Mob actor, DungeonAlertState state) {
        actor.getPersistentData().putString(DungeonAiActor.ALERT_TAG, state.name());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MANAGED_COUNTS.clear();
        ticks = 0;
    }
}
