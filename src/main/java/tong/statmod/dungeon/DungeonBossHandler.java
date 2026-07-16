package tong.statmod.dungeon;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.capability.StatCapabilities;
import java.util.List;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class DungeonBossHandler {

    static final long BOSS_COOLDOWN_TICKS = 144_000L;

    public static final TagKey<EntityType<?>> DUNGEON_BOSS_TAG = TagKey.create(
            Registries.ENTITY_TYPE,
            new ResourceLocation(StatMod.MOD_ID, "dungeon_boss"));

    private DungeonBossHandler() {}

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();

        if (target instanceof ServerPlayer) return;
        if (!target.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;

        ServerPlayer sp = findAttacker(event, target);
        if (sp == null) {
            StatMod.LOGGER.debug("[TrialDungeon] Death sans attaquant identifié : {}",
                    target.getName().getString());
            return;
        }

        int floor = DungeonTeleportHandler.floorAtPos(sp.getBlockX(), sp.getBlockZ());
        if (floor <= 0) return;

        if (target.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)) {
            DungeonPoints.awardMobKill(sp, target, floor);
        }

        DungeonObjective objective = DungeonObjective.forFloor(floor);
        switch (objective) {
            case SLAY_BOSS -> handleBossFloor(sp, floor, target);
            case CLEAR_WAVE -> handleCombatFloor(sp, floor, target);
            case LOOT_VAULT -> {}
        }
    }

    private static void handleBossFloor(ServerPlayer sp, int floor, LivingEntity target) {
        if (StatCapabilities.get(sp).getDungeonFloorReached() > floor) return;

        if (DungeonBossTracker.isTracked(floor)) {
            java.util.UUID id = target.getUUID();
            if (!DungeonBossTracker.isTrackedBoss(floor, id)) return;
            boolean allDead = DungeonBossTracker.onBossDeath(floor, id);
            if (!allDead) {
                int left = DungeonBossTracker.remaining(floor);
                sp.displayClientMessage(Component.translatable(
                        "block.statmod.dungeon_portal.boss_remaining", left), true);
                return;
            }
            DungeonProgress.completeFloor(sp, floor, DungeonObjective.SLAY_BOSS, true);
            setBossCooldown(sp, floor);
            return;
        }

        if (!target.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)) return;
        DungeonProgress.completeFloor(sp, floor, DungeonObjective.SLAY_BOSS, true);
        setBossCooldown(sp, floor);
    }

    /**
     * Conquête pièce par pièce : seul le mob de la pièce ACTIVE est pris en compte.
     * Quand tous les mobs de la pièce sont morts → on avance à la suivante.
     * Dernière pièce nettoyée → étage conquis.
     */
    private static void handleCombatFloor(ServerPlayer sp, int floor, LivingEntity target) {
        if (StatCapabilities.get(sp).getDungeonFloorReached() > floor) return;
        if (!target.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)) return;

        // Vérifie que ce mob appartient à la pièce actuellement active
        int targetRoom = target.getPersistentData().getInt("statmod_dungeon_room");
        int activeRoom = DungeonMobSpawner.getActiveRoom(floor);
        if (targetRoom >= 0 && activeRoom >= 0 && targetRoom != activeRoom) return;

        // Compte les mobs autorisés restants dans CETTE pièce (exclut le mourant)
        int remaining = DungeonMobSpawner.countAliveInRoom(
                sp.serverLevel(), floor, activeRoom >= 0 ? activeRoom : targetRoom, target);
        if (remaining > 0) return;

        // La pièce est nettoyée !
        if (DungeonMobSpawner.isLastCombatRoom(floor)) {
            // Dernière pièce → étage conquis
            DungeonProgress.completeFloor(sp, floor, DungeonObjective.CLEAR_WAVE, false);
        } else {
            sp.displayClientMessage(Component.literal(
                    "§aSalle nettoyée §7— poursuivez vers la suite"), true);
            DungeonMobSpawner.advanceToNextRoom(sp.serverLevel(), floor);
        }
    }

    private static int livingAuthorizedCount(ServerLevel lv, int floor, LivingEntity dying) {
        var sp = DungeonTeleportHandler.floorSpawnPos(floor);
        AABB area = new AABB(sp).inflate(DungeonMobSpawner.FLOOR_SCAN_RADIUS);
        List<Mob> alive = lv.getEntitiesOfClass(Mob.class, area,
                m -> m != dying && m.isAlive()
                        && m.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG));
        return alive.size();
    }

    private static void setBossCooldown(ServerPlayer sp, int floor) {
        long unlockTick = sp.serverLevel().getGameTime() + BOSS_COOLDOWN_TICKS;
        for (ServerPlayer p : sp.serverLevel().getEntitiesOfClass(
                ServerPlayer.class,
                new AABB(DungeonTeleportHandler.floorSpawnPos(floor))
                        .inflate(DungeonMobSpawner.FLOOR_SCAN_RADIUS),
                pl -> pl.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON))) {
            StatCapabilities.get(p).setBossCooldown(floor, unlockTick);
        }
        StatCapabilities.get(sp).setBossCooldown(floor, unlockTick);
    }

    private static ServerPlayer findAttacker(LivingDeathEvent event, LivingEntity target) {
        Player direct = resolveAttacker(
                event.getSource().getEntity(), event.getSource().getDirectEntity());
        if (direct instanceof ServerPlayer sp) return sp;
        if (target.getLastHurtByMob() instanceof ServerPlayer sp) return sp;
        List<ServerPlayer> nearby = target.level().getEntitiesOfClass(
                ServerPlayer.class,
                new AABB(target.blockPosition()).inflate(60),
                p -> p.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON));
        return nearby.isEmpty() ? null : nearby.get(0);
    }

    private static Player resolveAttacker(net.minecraft.world.entity.Entity source, net.minecraft.world.entity.Entity direct) {
        if (source instanceof Player player) return player;
        if (source instanceof net.minecraft.world.entity.projectile.Projectile projectile
                && projectile.getOwner() instanceof Player player) return player;
        if (direct instanceof net.minecraft.world.entity.projectile.Projectile projectile
                && projectile.getOwner() instanceof Player player) return player;
        return null;
    }
}
