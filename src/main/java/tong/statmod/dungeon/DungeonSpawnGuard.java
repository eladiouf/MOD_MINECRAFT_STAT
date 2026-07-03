package tong.statmod.dungeon;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import tong.statmod.STATMod;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Mission M6 — Anti-spawn dans le Trial Dungeon (deux couches).
 *
 * <p><b>Couche 1</b> — {@link FinalizeSpawnEvent} : cancel les {@link MobSpawnType} d'environnement
 * (natural, chunk-gen, spawner, patrol, jockey, event). Rapide et cheap pour la vanilla path.
 *
 * <p><b>Couche 2</b> — {@link EntityJoinLevelEvent} + contexte autorisé : cancel toute Mob qui
 * rejoint le donjon en dehors d'un appel {@link #spawnAuthorized(Supplier)}. Ce mécanisme attrape
 * les spawns custom des mods (SLU, irons_spellbooks, etc.) qui bypass la spawn pipeline vanilla.
 *
 * <p><b>Piège évité</b> : {@code EntityJoinLevelEvent} se déclenche <i>de façon synchrone à
 * l'intérieur</i> de {@code EntityType.spawn(...)} — marquer l'entité <i>après</i> l'appel (via
 * NBT) arrive donc trop tard, le join a déjà été refusé. Il faut autoriser <i>avant</i> l'appel,
 * pas après. D'où {@link #spawnAuthorized(Supplier)} qui enveloppe le spawn dans un contexte
 * actif pendant toute la durée de l'appel.
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class DungeonSpawnGuard {

    /** Marker NBT persistant — laisse rejoindre une entité légitime après rechargement du monde. */
    public static final String AUTHORIZED_TAG = "statmod_dungeon_authorized";

    /** Types de spawn interdits côté FinalizeSpawnEvent (première ligne de défense). */
    private static final Set<MobSpawnType> BLOCKED_TYPES = Set.of(
            MobSpawnType.NATURAL,
            MobSpawnType.CHUNK_GENERATION,
            MobSpawnType.SPAWNER,
            MobSpawnType.REINFORCEMENT,
            MobSpawnType.PATROL,
            MobSpawnType.JOCKEY,
            MobSpawnType.EVENT,
            MobSpawnType.TRIGGERED
    );

    /**
     * {@code true} pendant qu'un spawn autorisé par STAT MOD est en cours. Simple champ statique
     * (pas ThreadLocal) : toute la logique dungeon tourne sur le thread serveur unique.
     */
    private static boolean authorizedContext = false;

    private DungeonSpawnGuard() {}

    /**
     * Exécute {@code spawnAction} avec le guard désactivé le temps de l'appel — à utiliser pour
     * tout spawn de mob émis par le code dungeon (vague de combat, boss d'altar). Le marker NBT
     * {@link #AUTHORIZED_TAG} est posé automatiquement sur l'entité retournée pour qu'elle
     * survive à un reload de chunk/monde.
     */
    public static <T extends Entity> T spawnAuthorized(Supplier<T> spawnAction) {
        boolean previous = authorizedContext;
        authorizedContext = true;
        try {
            T entity = spawnAction.get();
            if (entity != null) {
                entity.getPersistentData().putBoolean(AUTHORIZED_TAG, true);
            }
            return entity;
        } finally {
            authorizedContext = previous;
        }
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!event.getLevel().getLevel().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) {
            return;
        }
        if (BLOCKED_TYPES.contains(event.getSpawnType())) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) {
            return;
        }
        Entity entity = event.getEntity();
        // Laisse passer : joueurs, items, projectiles, particules — tout ce qui n'est pas une Mob.
        if (!(entity instanceof Mob)) return;
        if (entity instanceof Player) return;

        // Spawn en cours via spawnAuthorized() → laisse passer.
        if (authorizedContext) return;
        // Entité survivante d'un chunk reload, déjà marquée lors de son spawn initial.
        if (entity.getPersistentData().getBoolean(AUTHORIZED_TAG)) return;

        // Sinon → mob custom d'un autre mod (SLU, etc.) qui essaie de spawn. Cancel.
        event.setCanceled(true);
        STATMod.LOGGER.debug("[TrialDungeon] Cancel unauthorized mob spawn: {} ({})",
                entity.getName().getString(),
                entity.getType().builtInRegistryHolder().key().location());
    }
}
