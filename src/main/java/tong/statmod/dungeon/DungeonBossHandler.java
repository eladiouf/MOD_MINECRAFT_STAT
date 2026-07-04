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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import tong.statmod.STATMod;
import tong.statmod.progression.CombatXPHandler;

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

    public static final TagKey<EntityType<?>> DUNGEON_BOSS_TAG = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "dungeon_boss"));

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
            STATMod.LOGGER.debug("[TrialDungeon] Death sans attaquant identifié : {}",
                    target.getName().getString());
            return;
        }

        int floor = DungeonTeleportHandler.floorAtPos(sp.getBlockX(), sp.getBlockZ());
        if (floor <= 0) return;

        DungeonObjective objective = DungeonObjective.forFloor(floor);
        switch (objective) {
            case SLAY_BOSS -> handleBossFloor(sp, floor, target);
            case CLEAR_WAVE -> handleCombatFloor(sp, floor, target);
            case LOOT_VAULT -> { /* conquête via ouverture du coffre, pas via kill */ }
        }
    }

    /** Étage boss : conquête quand tous les boss suivis sont morts (ou secours si non tracké). */
    private static void handleBossFloor(ServerPlayer sp, int floor, LivingEntity target) {
        if (sp.getData(tong.statmod.storage.ModAttachments.STATS).getDungeonFloorReached() > floor) return;

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
        }
        // Sinon (restart serveur en plein combat, étage sans altar) → ce kill conquiert directement.
        DungeonProgress.completeFloor(sp, floor, DungeonObjective.SLAY_BOSS, true);
    }

    /** Étage de combat : conquête quand la dernière vague de mobs autorisés est éliminée. */
    private static void handleCombatFloor(ServerPlayer sp, int floor, LivingEntity target) {
        if (sp.getData(tong.statmod.storage.ModAttachments.STATS).getDungeonFloorReached() > floor) return;

        // Seuls nos mobs autorisés comptent (un passant vanilla égaré ne conquiert pas l'étage).
        if (!target.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)) return;

        // Reste-t-il des mobs autorisés vivants (autre que la cible qui va mourir) ?
        int left = livingAuthorizedCount(sp.serverLevel(), floor, target);
        if (left > 0) {
            sp.displayClientMessage(Component.translatable(
                    "dungeon.wave.remaining", left), true);
            return;
        }

        // Vague nettoyée → étage conquis. Pas de gain de stat (réservé aux boss).
        DungeonProgress.completeFloor(sp, floor, DungeonObjective.CLEAR_WAVE, false);
    }

    /** Nombre de mobs autorisés encore vivants sur l'étage, en excluant {@code dying}. */
    private static int livingAuthorizedCount(ServerLevel lv, int floor, LivingEntity dying) {
        var sp = DungeonTeleportHandler.floorSpawnPos(floor);
        AABB area = new AABB(sp).inflate(55);
        List<Mob> alive = lv.getEntitiesOfClass(Mob.class, area,
                m -> m != dying && m.isAlive()
                        && m.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG));
        return alive.size();
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
        Player direct = CombatXPHandler.resolveAttacker(
                event.getSource().getEntity(), event.getSource().getDirectEntity());
        if (direct instanceof ServerPlayer sp) return sp;
        if (target.getLastHurtByMob() instanceof ServerPlayer sp) return sp;
        List<ServerPlayer> nearby = target.level().getEntitiesOfClass(
                ServerPlayer.class,
                new AABB(target.blockPosition()).inflate(60),
                p -> p.level().dimension().equals(DungeonDimensions.TRIAL_DUNGEON));
        return nearby.isEmpty() ? null : nearby.get(0);
    }
}
