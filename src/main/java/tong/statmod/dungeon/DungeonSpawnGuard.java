package tong.statmod.dungeon;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import tong.statmod.STATMod;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Mission M6 — Régulation des spawns par <b>fenêtre d'autorisation temporelle</b> (2026-07-03).
 *
 * <p>Concept : à l'entrée d'un étage, une fenêtre de spawn s'ouvre pour une durée qui croît avec
 * la difficulté (tier). Tant que la fenêtre est ouverte, les mobs peuvent apparaître sur l'étage
 * (au-dessus de la plateforme). Une fois fermée, tout spawn est coupé. <b>Plus l'étage est dur,
 * plus la fenêtre reste ouverte longtemps → plus les mobs ont le temps de spawner.</b>
 *
 * <p>Deux règles combinées dans {@link #onEntityJoinLevel} :
 * <ol>
 *   <li>Mob <b>sous</b> la plateforme ({@code Y < }{@value #MIN_SPAWN_Y}) → toujours annulé
 *       (junk SLU qui tombe dans le vide).</li>
 *   <li>Mob sur la plateforme mais <b>hors fenêtre</b> (fermée ou jamais ouverte) → annulé.</li>
 * </ol>
 *
 * <p>La fenêtre est indexée par étage (grille XZ), donc chaque étage a sa propre régulation.
 */
public final class DungeonSpawnGuard {

    public static final String AUTHORIZED_TAG = "statmod_dungeon_authorized";

    /** Niveau Y des plateformes (voir {@code DungeonTeleportHandler.FLOOR_Y}). */
    private static final int PLATFORM_Y = 100;
    /** En-dessous de ce Y → mob sous l'île → toujours annulé. */
    private static final int MIN_SPAWN_Y = PLATFORM_Y - 3;

    /** Durées de fenêtre par tier (en ticks ; 20 ticks = 1 s). */
    private static final int WINDOW_EARLY = 200;   // 10 s
    private static final int WINDOW_MID   = 400;   // 20 s
    private static final int WINDOW_LATE  = 600;   // 30 s
    private static final int WINDOW_ABYSS = 900;   // 45 s
    /** Fenêtre minimale garantie lors d'un spawn de boss (compagnons). */
    private static final int WINDOW_BOSS_MIN = 200; // 10 s

    /** Étage → tick de fin de fenêtre de spawn. */
    private static final Map<Integer, Long> WINDOW_END = new HashMap<>();
    private static long serverTick = 0L;

    private DungeonSpawnGuard() {}

    /** Durée de fenêtre (ticks) selon la difficulté de l'étage. */
    public static int windowTicksForFloor(int floor) {
        return switch (FloorPalette.forFloor(floor)) {
            case EARLY -> WINDOW_EARLY;
            case MID -> WINDOW_MID;
            case LATE -> WINDOW_LATE;
            case ABYSS -> WINDOW_ABYSS;
        };
    }

    /**
     * Ouvre (ou prolonge) la fenêtre de spawn de l'étage {@code floor} pour sa durée de tier.
     * À appeler à l'entrée du joueur.
     */
    public static void openWindowForFloor(int floor) {
        openWindow(floor, windowTicksForFloor(floor));
    }

    /** Ouvre/prolonge la fenêtre de l'étage à au moins {@code ticks} ticks à partir de maintenant. */
    public static void openWindow(int floor, int ticks) {
        long end = serverTick + ticks;
        WINDOW_END.merge(floor, end, Math::max);
    }

    /**
     * Spawn + marque l'entité. Simple wrapper — n'annule jamais rien. L'ouverture de la fenêtre
     * doit être faite <b>avant</b> l'appel par l'appelant ({@code enterFloor} pour les vagues,
     * l'autel pour les boss), sinon un compagnon qui naît pendant ce spawn serait bloqué.
     */
    public static <T extends Entity> T spawnAuthorized(Supplier<T> spawnAction) {
        T entity = spawnAction.get();
        if (entity != null) {
            entity.getPersistentData().putBoolean(AUTHORIZED_TAG, true);
        }
        return entity;
    }

    /** Fenêtre minimale garantie pour un spawn de boss (compagnons). */
    public static void openBossWindow(int floor) {
        openWindow(floor, WINDOW_BOSS_MIN);
    }

    /** @deprecated remplacé par le système de fenêtre ; conservé no-op pour compat. */
    @Deprecated
    public static void openGrace(int ticks) {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        serverTick++;
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Mob) || entity instanceof Player) return;

        // Règle 1 : sous la plateforme → junk dans le vide → annulé.
        if (entity.getY() < MIN_SPAWN_Y) {
            event.setCanceled(true);
            return;
        }

        // Règle 2 : Filtre les mods tiers non-contrôlés → SEULEMENT vanilla + mods autorisés
        if (!isControlledMob(entity.getType())) {
            event.setCanceled(true);
            STATMod.LOGGER.debug("[TrialDungeon] Blocked uncontrolled mod mob: {}", entity.getType());
            return;
        }

        int floor = DungeonTeleportHandler.floorAtPos(entity.getBlockX(), entity.getBlockZ());
        boolean preAuthorized = entity.getPersistentData().getBoolean(AUTHORIZED_TAG);

        // Règle 3 : hors fenêtre de spawn → annulé. Exception : les entités déjà marquées
        // autorisées (invoquées par spawnAuthorized) et les invocations d'un combat de boss en
        // cours (phase-2, adds) — sinon un boss multi-phase perdrait ses parties après 10 s.
        if (!preAuthorized && !DungeonBossTracker.isTracked(floor)) {
            Long end = WINDOW_END.get(floor);
            if (end == null || serverTick > end) {
                event.setCanceled(true);
                return;
            }
        }

        // Règle 4 : cale la difficulté L2 sur l'étage pour TOUTE entité autorisée qui reste
        // (vague, boss, sbires, invocations). Remplace l'ancien L2HostilityDisabler cassé :
        // c'est ce qui empêche un mob d'être niveau 60 à l'étage 1 (la difficulté régionale L2
        // par distance est écrasée par notre niveau par étage). Différé pour passer après l'init
        // de L2 (dernière écriture = autoritaire).
        if (entity instanceof LivingEntity le) {
            DungeonMobSpawner.scheduleL2(le, floor);
        }
    }

    /**
     * Vérifie si le mob est autorisé (vanilla ou mod contrôlé).
     * Les mobs autorisés sont ceux présents dans nos pools définis.
     */
    private static boolean isControlledMob(EntityType<?> type) {
        // Vanilla toujours autorisé
        try {
            var registryKey = type.builtInRegistryHolder().key();
            if (registryKey != null && "minecraft".equals(registryKey.location().getNamespace())) {
                return true;
            }
        } catch (Exception e) {
            // Erreur d'accès → bloquer pour sécurité
            return false;
        }

        // Mobs contrôlés : ceux qui sont dans nos pools
        // Pour l'instant, on autorise les mobs vanilla + SLU + Iron's Spellbooks + Overgeared
        // À terme, on pourrait avoir une liste blanche plus précise
        return isControlledModEntity(type);
    }

    /** Vérifie si l'EntityType est dans notre liste blanche de mods contrôlés. */
    private static boolean isControlledModEntity(EntityType<?> type) {
        try {
            var registryKey = type.builtInRegistryHolder().key();
            if (registryKey == null) return false;

            String namespace = registryKey.location().getNamespace();

            // Autoriser les mods de notre liste blanche (modId réels, cf. ModdedMobPool)
            return "slu".equals(namespace) ||
                   "irons_spellbooks".equals(namespace);

        } catch (Exception e) {
            return false;
        }
    }
}
