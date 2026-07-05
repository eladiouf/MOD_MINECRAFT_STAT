package tong.statmod.dungeon;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.function.Supplier;

/**
 * Mission M6 — Garde de spawn par <b>liste blanche stricte</b> (refonte 2026-07-04, fix « invasion
 * de mobs »).
 *
 * <p>Le Trial Dungeon ne doit contenir <b>que</b> ce que nous y plaçons délibérément : la vague de
 * combat d'un étage, les boss de l'autel, et leurs invocations. Toute autre entité (spawn naturel,
 * add non désiré) est annulée à la naissance.
 *
 * <p>Un mob est autorisé si :
 * <ol>
 *   <li>il porte le marqueur {@link #AUTHORIZED_TAG} (posé par {@link #spawnAuthorized}), ou</li>
 *   <li>un combat de boss est en cours sur son étage ({@link DungeonBossTracker#isTracked}) et son
 *       type est dans notre liste blanche de mods → c'est une invocation du boss.</li>
 * </ol>
 *
 * <p>L'ancien système de <i>fenêtre temporelle</i> laissait passer un flux continu de mobs pendant
 * 10–45 s à chaque entrée (et sans limite pendant un combat de boss) → « invasion ». Supprimé.
 */
public final class DungeonSpawnGuard {

    public static final String AUTHORIZED_TAG = "statmod_dungeon_authorized";

    /**
     * Profondeur de « spawn autorisé en cours ». {@code EntityType.spawn()} fait naître l'entité
     * <b>et</b> déclenche {@code EntityJoinLevelEvent} de façon synchrone <i>avant</i> de rendre la
     * main — donc avant qu'on ait pu poser {@link #AUTHORIZED_TAG}. Ce compteur (incrémenté autour
     * de l'appel de spawn) dit au garde « l'entité qui rejoint maintenant, c'est nous » → il la
     * laisse passer et pose le tag lui-même. Sans ça, le garde annulait nos propres vagues (« aucun
     * mob »). Serveur mono-thread → un simple compteur statique suffit (spawns imbriqués tolérés).
     */
    private static int authorizingDepth = 0;

    private DungeonSpawnGuard() {}

    /**
     * Spawn + marque l'entité comme autorisée. Le tag est posé <b>pendant</b> le spawn (via
     * {@link #authorizingDepth}) pour que le garde laisse passer l'entité au moment de son join,
     * puis re-posé ici par sécurité (invocations différées, etc.).
     */
    public static <T extends Entity> T spawnAuthorized(Supplier<T> spawnAction) {
        authorizingDepth++;
        T entity;
        try {
            entity = spawnAction.get();
        } finally {
            authorizingDepth--;
        }
        if (entity != null) {
            entity.getPersistentData().putBoolean(AUTHORIZED_TAG, true);
        }
        return entity;
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // CÔTÉ SERVEUR UNIQUEMENT. EntityJoinLevelEvent est bilatéral : il est aussi émis côté
        // client quand celui-ci reçoit le paquet d'apparition et crée sa copie locale. Nos signaux
        // d'autorisation (authorizingDepth, AUTHORIZED_TAG) sont server-only — sur le client ils
        // valent toujours « non autorisé » → on annulait le join côté client → l'entité, bien
        // présente sur le serveur, était détruite chez le client → « mobs présents mais invisibles »
        // (et ils tuaient le joueur sans être visibles). Le contrôle de spawn est purement serveur.
        if (event.getLevel().isClientSide) return;
        if (!event.getLevel().dimension().equals(DungeonDimensions.TRIAL_DUNGEON)) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Mob) || entity instanceof Player) return;

        int floor = DungeonTeleportHandler.floorAtPos(entity.getBlockX(), entity.getBlockZ());

        // ══ Liste blanche stricte (2026-07-04, fix « invasion de mobs ») ══
        // Le donjon ne contient QUE ce que nous spawnons. Un mob est autorisé si :
        //   (a) il naît via spawnAuthorized (authorizingDepth > 0) → c'est notre spawn EN COURS
        //       (le join arrive avant qu'on ait pu poser le tag) : on l'autorise et on tague ici, OU
        //   (b) il porte déjà AUTHORIZED_TAG (invocation différée d'une entité à nous), OU
        //   (c) un combat de boss est en cours sur l'étage → c'est une invocation/add du boss.
        // TOUT le reste est annulé — plus de fenêtre temporelle qui laissait passer un flux continu.
        if (authorizingDepth > 0) {
            entity.getPersistentData().putBoolean(AUTHORIZED_TAG, true);
        } else {
            boolean authorized = entity.getPersistentData().getBoolean(AUTHORIZED_TAG);
            boolean bossSummon = !authorized && DungeonBossTracker.isTracked(floor)
                    && isControlledMob(entity.getType());
            if (!authorized && !bossSummon) {
                event.setCanceled(true);
                return;
            }
        }

        // Cale la difficulté L2 sur l'étage pour toute entité autorisée qui reste (vague, boss,
        // invocations). Remplace l'ancien L2HostilityDisabler cassé : empêche un mob d'être niveau
        // 60 à l'étage 1. Différé pour passer après l'init de L2 (dernière écriture = autoritaire).
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
                   "irons_spellbooks".equals(namespace) ||
                   "tensura".equals(namespace) ||
                   "block_factorys_bosses".equals(namespace) ||
                   "xbbsroaringknightmod".equals(namespace) ||
                   "cataclysm".equals(namespace) ||
                   "born_in_chaos_v1".equals(namespace) ||
                   "mutantmonsters".equals(namespace) ||
                   "mowziesmobs".equals(namespace) ||
                   "alexsmobs".equals(namespace) ||
                   "bosses_of_mass_destruction".equals(namespace);

        } catch (Exception e) {
            return false;
        }
    }
}
