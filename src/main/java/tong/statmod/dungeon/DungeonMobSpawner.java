package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.integration.l2hostility.L2HostilityBridge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Mission M6 — Spawn des mobs de donjon, déclenché à l'entrée du joueur.
 *
 * <p><b>Problème résolu (invisibilité)</b> : si les mobs spawnent pendant la <i>génération</i> de
 * l'île (qui peut arriver depuis l'overworld via {@code /statdungeon regen}, ou avant le
 * {@code teleportTo}), ils naissent dans une dimension qu'aucun joueur ne suit → le client ne
 * reçoit jamais leur paquet d'apparition → <b>mobs présents mais invisibles</b>.
 *
 * <p><b>Fix</b> : la génération ne pose plus que des blocs. Les mobs sont demandés via
 * {@link #requestWave(ServerLevel, int)} <i>depuis {@code enterFloor}, après le teleport</i>,
 * donc quand le joueur est réellement dans le donjon et suit les chunks. Le spawn effectif est
 * encore différé de quelques ticks pour laisser le tracking se stabiliser.
 *
 * <p>Garde anti-doublon : {@link #requestWave} ne fait rien si des mobs autorisés sont encore
 * vivants sur l'étage, ou si une vague est déjà en attente pour cet étage.
 */
public final class DungeonMobSpawner {

    /** Délai avant spawn effectif : le joueur est déjà présent, un court délai suffit. */
    private static final int SPAWN_DELAY_TICKS = 10;
    /** Rayon (blocs) où l'on cherche des mobs déjà vivants sur l'étage. */
    private static final int FLOOR_SCAN_RADIUS = 55;

    /**
     * Délai avant d'appliquer le niveau L2 Hostility. Doit passer APRÈS l'init de L2 (qui calcule
     * une difficulté régionale par distance) pour que notre valeur par étage soit autoritaire —
     * sinon L2 écrase notre niveau (ex : étage 10 à 1800 blocs → L2 forcerait ~27, non monotone).
     */
    private static final int L2_APPLY_DELAY_TICKS = 12;

    private record Pending(ServerLevel level, BlockPos pos, EntityType<?> type, int floor, long dueTick) {}

    private record L2Pending(LivingEntity mob, int floor, long dueTick) {}

    private static final List<Pending> QUEUE = new ArrayList<>();
    private static final List<L2Pending> L2_QUEUE = new ArrayList<>();
    private static final Set<Integer> PENDING_FLOORS = new HashSet<>();
    private static long serverTick = 0L;

    private DungeonMobSpawner() {}

    /**
     * Programme l'application (différée) du niveau L2 Hostility sur {@code mob}, calé sur
     * {@code floor}. À appeler juste après le spawn d'un mob de donjon (vague ou boss). L'apply
     * réel a lieu {@value #L2_APPLY_DELAY_TICKS} ticks plus tard, après l'init de L2, pour être
     * la dernière écriture (autoritaire).
     */
    public static void scheduleL2(LivingEntity mob, int floor) {
        if (mob == null || !L2HostilityBridge.loaded()) return;
        L2_QUEUE.add(new L2Pending(mob, floor, serverTick + L2_APPLY_DELAY_TICKS));
    }

    /**
     * Demande la vague de mobs de l'étage {@code floor}. À appeler depuis {@code enterFloor}
     * après le teleport. No-op si des mobs sont déjà vivants ou en attente pour cet étage.
     */
    public static void requestWave(ServerLevel lv, int floor) {
        if (!isCombatFloor(floor)) return; // boss (×10) / trésor (×5) : pas de vague
        if (PENDING_FLOORS.contains(floor)) return;
        if (countAlive(lv, floor) > 0) return;

        int full = DungeonMasterpiece.mobCount(FloorPalette.forFloor(floor));
        int queued = enqueueWave(lv, floor, full);
        STATMod.LOGGER.info("[TrialDungeon] Vague étage {} demandée : {} mobs en file", floor, queued);
    }

    /** {@code true} si l'étage a une vague de combat (ni boss ni trésor). */
    private static boolean isCombatFloor(int floor) {
        return floor > 0 && floor % 10 != 0 && floor % 5 != 0;
    }

    /** Nombre de mobs autorisés (nos mobs) encore vivants sur l'étage. */
    private static int countAlive(ServerLevel lv, int floor) {
        BlockPos sp = DungeonTeleportHandler.floorSpawnPos(floor);
        AABB area = new AABB(sp).inflate(FLOOR_SCAN_RADIUS);
        return lv.getEntitiesOfClass(Mob.class, area,
                m -> m.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)).size();
    }

    /**
     * Met en file jusqu'à {@code want} mobs de la vague de l'étage, placés dans l'anneau de combat
     * sur des positions valides. Marque l'étage comme « en attente ». Retourne le nombre
     * effectivement mis en file.
     */
    private static int enqueueWave(ServerLevel lv, int floor, int want) {
        if (want <= 0) return 0;
        FloorPalette tier = FloorPalette.forFloor(floor);
        List<EntityType<?>> pool = ModdedMobPool.getCombinedPool(tier); // Vanilla + mods contrôlés
        if (pool.isEmpty()) return 0;

        BlockPos sp = DungeonTeleportHandler.floorSpawnPos(floor);
        PENDING_FLOORS.add(floor);
        // Les mobs sont marqués AUTHORIZED_TAG par spawnAuthorized → ils passent le garde.

        int spawned = 0;
        int attempts = 0;
        while (spawned < want && attempts < want * 5) { // Max 5 essais par mob
            attempts++;

            // Position aléatoire dans l'anneau de combat (10-18 blocs du centre)
            double angle = lv.random.nextDouble() * Math.PI * 2;
            double distance = 10.0 + lv.random.nextDouble() * 8.0; // 10-18 blocs
            int dx = (int) Math.round(Math.cos(angle) * distance);
            int dz = (int) Math.round(Math.sin(angle) * distance);
            BlockPos pos = sp.offset(dx, 0, dz);

            if (!isValidSpawnPosition(lv, pos)) continue;

            // Dégage une colonne d'air 1×2 au point de spawn
            lv.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            lv.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);

            EntityType<?> type = pool.get(lv.random.nextInt(pool.size()));
            QUEUE.add(new Pending(lv, pos, type, floor, serverTick + SPAWN_DELAY_TICKS));
            spawned++;
        }
        return spawned;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        serverTick++;
        flushL2Queue();

        if (QUEUE.isEmpty()) return;

        int spawned = 0;
        Iterator<Pending> it = QUEUE.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (serverTick < p.dueTick) continue;
            it.remove();
            try {
                var entity = DungeonSpawnGuard.spawnAuthorized(
                        () -> p.type.spawn(p.level, p.pos, MobSpawnType.STRUCTURE));
                if (entity != null) {
                    spawned++;
                    // La difficulté L2 par étage est calée par DungeonSpawnGuard.onEntityJoinLevel
                    // (chokepoint unique couvrant vagues, boss et invocations).
                }
            } catch (RuntimeException e) {
                STATMod.LOGGER.warn("[TrialDungeon] Spawn différé fail {} @ {}: {}",
                        p.type, p.pos, e.getMessage());
            }
        }

        // Retire des PENDING_FLOORS les étages dont la file est vidée.
        if (spawned > 0) {
            Set<Integer> stillQueued = new HashSet<>();
            for (Pending p : QUEUE) stillQueued.add(p.floor);
            PENDING_FLOORS.retainAll(stillQueued);
            STATMod.LOGGER.info("[TrialDungeon] {} mobs spawnés (joueur présent, visibles)", spawned);
        }
    }

    /** Vérifie si une position est valide pour le spawn d'un mob. */
    private static boolean isValidSpawnPosition(ServerLevel lv, BlockPos pos) {
        // Check que le sol est solide (pas l'air, pas de blocs non-walkables)
        if (lv.getBlockState(pos.below()).isAir()) return false;
        if (!lv.getBlockState(pos.below()).isSolidRender(lv, pos.below())) return false;

        // Check qu'il y a de l'espace pour le mob (2 blocs de hauteur)
        if (!lv.getBlockState(pos).isAir()) return false;
        if (!lv.getBlockState(pos.above()).isAir()) return false;

        // Check qu'il n'y a pas d'autre mob trop proche (éviter stacking)
        AABB checkArea = new AABB(pos).inflate(2.0);
        if (!lv.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, checkArea).isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Applique les niveaux L2 échus (après l'init de L2 → autoritaire).
     *
     * <p><b>Anti-CME</b> : on extrait d'abord les entrées dues dans une liste locale, PUIS on
     * applique. {@code applyFloorLevel} peut faire naître des entités (parties de boss, invocations)
     * → {@code EntityJoinLevelEvent} → {@code DungeonSpawnGuard.scheduleL2} → {@code L2_QUEUE.add}.
     * Si on appliquait pendant l'itération de {@code L2_QUEUE}, cet ajout ré-entrant provoquerait un
     * {@link java.util.ConcurrentModificationException} (crash observé au spawn du boss étage 10).
     */
    private static void flushL2Queue() {
        if (L2_QUEUE.isEmpty()) return;

        List<L2Pending> due = new ArrayList<>();
        Iterator<L2Pending> it = L2_QUEUE.iterator();
        while (it.hasNext()) {
            L2Pending p = it.next();
            if (serverTick < p.dueTick) continue;
            it.remove();
            due.add(p);
        }

        // Application HORS itération : un scheduleL2 ré-entrant s'ajoute sans risque à L2_QUEUE.
        for (L2Pending p : due) {
            if (p.mob.isAlive()) {
                L2HostilityBridge.applyFloorLevel(p.mob, p.floor);
            }
        }
    }
}
