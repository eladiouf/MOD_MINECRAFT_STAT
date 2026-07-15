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
import tong.statmod.StatMod;
import java.util.List;

/**
 * Mission M6 — Conquête d'étage (« vraie aventure », refonte 2026-07-04).
 *
 * <p>Écoute les morts de mobs dans le Trial Dungeon et déclenche la conquête d'un étage quand son
 * objectif ({@link DungeonObjective}) est accompli. Deux objectifs passent par ce handler :
 * <ul>
 *   <li><b>Boss (×10)</b> : tous les boss du roster morts → conquête (via {@link DungeonBossTracker},
 *       avec heuristique de secours si le tracking est perdu au restart).</li>
 *   <li><b>Combat (autres)</b> : la <b>dernière</b> vague de mobs autorisés éliminée → conquête.</li>
 * </ul>
 * Les étages trésor (×5) sont conquis en ouvrant le coffre (cf. loot / vault), pas ici.
 *
 * <p>La récompense et la célébration sont centralisées dans {@link DungeonProgress}. Idempotent.
 */
public final class DungeonBossHandler {

    /** Cooldown d'un boss de palier (×10) : 2 heures de game ticks. */
    static final long BOSS_COOLDOWN_TICKS = 144_000L; // 2h × 60m × 60s × 20t

    public static final TagKey<EntityType<?>> DUNGEON_BOSS_TAG = TagKey.create(
            Registries.ENTITY_TYPE,
            new ResourceLocation(StatMod.MOD_ID, "dungeon_boss"));

    private DungeonBossHandler() {}

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();

        // Ignorer les morts de joueur (gérées par DungeonRespawnHandler).
        if (target instanceof ServerPlayer) return;
        // Ignorer hors de la dimension donjon.
        if (!target.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) return;

        ServerPlayer sp = findAttacker(event, target);
        if (sp == null) {
            StatMod.LOGGER.debug("[TrialDungeon] Death sans attaquant identifié : {}",
                    target.getName().getString());
            return;
        }

        int floor = DungeonTeleportHandler.floorAtPos(sp.getBlockX(), sp.getBlockZ());
        if (floor <= 0) return;

        // Système de points : tout mob de donjon (nos mobs autorisés) tué par le joueur rapporte
        // des points ∝ SA DIFFICULTÉ (PV/attaque/armure) × profondeur. Remplace le drop de cristaux.
        if (target.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)) {
            DungeonPoints.awardMobKill(sp, target, floor);
        }

        // Boss Qliphoth (secret rooms / ultra-vaults) : combat optionnel → points sans conquête
        var targetKey = target.getType().builtInRegistryHolder().key();
        if (targetKey != null && "fdbosses".equals(targetKey.location().getNamespace())) {
            // Points bonus si le tag n'a pas matché (boss spawné par spawner Qliphoth, pas notre tag)
            if (!target.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)) {
                DungeonPoints.awardMobKill(sp, target, floor);
            }
            sp.displayClientMessage(Component.translatable("dungeon.qliphoth.defeated"), true);
            return; // ne pas compléter l'étage
        }

        DungeonObjective objective = DungeonObjective.forFloor(floor);
        switch (objective) {
            case SLAY_BOSS -> handleBossFloor(sp, floor, target);
            case CLEAR_WAVE -> handleCombatFloor(sp, floor, target);
            case LOOT_VAULT -> { /* conquête via ouverture du coffre, pas via kill */ }
        }
    }

    /** Étage boss : conquête quand tous les boss suivis sont morts (ou secours si non tracké). */
    private static void handleBossFloor(ServerPlayer sp, int floor, LivingEntity target) {
        if (tong.statmod.capability.StatCapabilities.get(sp).getDungeonFloorReached() > floor) return;

        if (DungeonBossTracker.isTracked(floor)) {
            java.util.UUID id = target.getUUID();
            if (!DungeonBossTracker.isTrackedBoss(floor, id)) return; // sbire/compagnon
            boolean allDead = DungeonBossTracker.onBossDeath(floor, id);
            if (!allDead) {
                int left = DungeonBossTracker.remaining(floor);
                sp.displayClientMessage(Component.translatable(
                        "block.statmod.dungeon_portal.boss_remaining", left), true);
                return; // il reste des boss
            }
            // Tous les boss morts → conquête.
            DungeonProgress.completeFloor(sp, floor, DungeonObjective.SLAY_BOSS, true);
            setBossCooldown(sp, floor);
            return;
        }
        // Sinon (restart serveur en plein combat) : secours, mais UNIQUEMENT si la cible est un mob
        // de donjon autorisé (AUTHORIZED_TAG) — jamais un mob quelconque qui traînerait sur l'étage
        // (mob apprivoisé amené par le joueur, résidu d'un étage voisin...), sinon n'importe quel kill
        // hors combat conquiert l'étage sans jamais affronter le boss (exploit trouvé en audit).
        if (!target.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)) return;
        DungeonProgress.completeFloor(sp, floor, DungeonObjective.SLAY_BOSS, true);
        setBossCooldown(sp, floor);
    }

    /** Étage de combat : conquête quand la dernière vague de mobs autorisés est éliminée. */
    private static void handleCombatFloor(ServerPlayer sp, int floor, LivingEntity target) {
        if (tong.statmod.capability.StatCapabilities.get(sp).getDungeonFloorReached() > floor) return;

        // Seuls nos mobs autorisés comptent (un passant vanilla égaré ne conquiert pas l'étage).
        if (!target.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)) return;

        // Reste-t-il des mobs autorisés vivants (autre que la cible qui va mourir) ?
        int left = livingAuthorizedCount(sp.serverLevel(), floor, target);
        if (left > 0) {
            sp.displayClientMessage(Component.translatable(
                    "dungeon.wave.remaining", left), true);
            return;
        }

        // La rencontre courante est nettoyée. L'étage reste actif tant qu'un autre secteur
        // obligatoire n'a pas encore été traversé et sécurisé.
        if (!DungeonRoomEncounterDirector.onActiveRoomCleared(floor)) {
            sp.displayClientMessage(Component.literal("§aSecteur nettoyé §7— poursuivez l'exploration"), true);
            return;
        }

        // Tous les secteurs sont nettoyés → étage conquis. Pas de gain de stat (réservé aux boss).
        DungeonProgress.completeFloor(sp, floor, DungeonObjective.CLEAR_WAVE, false);
    }

    /** Nombre de mobs autorisés encore vivants sur l'étage, en excluant {@code dying}. */
    private static int livingAuthorizedCount(ServerLevel lv, int floor, LivingEntity dying) {
        var sp = DungeonTeleportHandler.floorSpawnPos(floor);
        AABB area = new AABB(sp).inflate(DungeonMobSpawner.FLOOR_SCAN_RADIUS);
        List<Mob> alive = lv.getEntitiesOfClass(Mob.class, area,
                m -> m != dying && m.isAlive()
                        && m.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG));
        return alive.size();
    }

    /** Applique le cooldown 2h à tous les joueurs présents sur l'étage. */
    private static void setBossCooldown(ServerPlayer sp, int floor) {
        long unlockTick = sp.serverLevel().getGameTime() + BOSS_COOLDOWN_TICKS;
        for (ServerPlayer p : sp.serverLevel().getEntitiesOfClass(
                ServerPlayer.class,
                new net.minecraft.world.phys.AABB(
                        DungeonTeleportHandler.floorSpawnPos(floor))
                        .inflate(DungeonMobSpawner.FLOOR_SCAN_RADIUS),
                pl -> pl.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON))) {
            tong.statmod.capability.StatCapabilities.get(p).setBossCooldown(floor, unlockTick);
        }
        // Fallback : le joueur à l'origine du kill est toujours cooldowné.
        tong.statmod.capability.StatCapabilities.get(sp).setBossCooldown(floor, unlockTick);
    }

    /**
     * 3 fallbacks pour retrouver le joueur responsable :
     * <ol>
     *   <li>Source directe de dégâts (melee / ranged)</li>
     *   <li>Dernier joueur qui a hurt le mob</li>
     *   <li>Le joueur en dungeon le plus proche (AABB radius 60)</li>
     * </ol>
     */
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
        if (source instanceof Player player) {
            return player;
        }
        if (source instanceof net.minecraft.world.entity.projectile.Projectile projectile && projectile.getOwner() instanceof Player player) {
            return player;
        }
        if (direct instanceof net.minecraft.world.entity.projectile.Projectile projectile && projectile.getOwner() instanceof Player player) {
            return player;
        }
        return null;
    }
}
