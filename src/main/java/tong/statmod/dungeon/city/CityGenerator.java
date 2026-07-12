package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import tong.statmod.STATMod;
import tong.statmod.dungeon.DungeonDimensions;
import tong.statmod.dungeon.DungeonTeleportHandler;
import tong.statmod.dungeon.IslandGenerator;

/**
 * Orchestrateur de la Cité des Aventuriers : enfile la construction (coque → place → camp →
 * porte → portails) dans une {@link CityBuildQueue} drainée à budget fixe par tick. Démarre
 * en tâche de fond au démarrage du serveur ; marqueur d'achèvement versionné en SavedData
 * ({@link CitySavedData}) — incrémenter {@link #CITY_VERSION} force une reconstruction.
 */
@EventBusSubscriber(modid = STATMod.MODID)
public final class CityGenerator {

    /** Incrémenter à chaque évolution de la génération pour reconstruire les mondes existants. */
    public static final int CITY_VERSION = 1;
    /** ~2 bandes de 4×600 par tick ≈ 15-30k blocs/tick : invisible en jeu, cité en ~1-2 min. */
    private static final int JOBS_PER_TICK = 2;

    private static final CityBuildQueue QUEUE = new CityBuildQueue();
    private static boolean building = false;

    private CityGenerator() {}

    public static boolean isBuilt(ServerLevel dungeonLevel) {
        return CitySavedData.get(dungeonLevel).builtVersion() >= CITY_VERSION;
    }

    public static boolean isBuilding() { return building; }

    public static int progressPercent() { return QUEUE.progressPercent(); }

    /** Programme la construction si nécessaire. Idempotent (no-op si construite ou en cours). */
    public static void ensureCity(ServerLevel lv) {
        if (building || isBuilt(lv)) return;
        QUEUE.clear();
        enqueueLegacyCleanupIfNeeded(lv, QUEUE);
        enqueueBuild(lv, QUEUE);
        building = true;
        STATMod.LOGGER.info("[City] Construction de la Cité des Aventuriers programmée ({} segments)",
                QUEUE.totalJobs());
    }

    /** Regen forcée (/statdungeon regen 0) : purge l'intérieur puis reconstruit. */
    public static void regen(ServerLevel lv) {
        QUEUE.clear();
        CitySavedData.get(lv).setBuiltVersion(0);
        enqueueInteriorClear(lv, QUEUE);
        enqueueBuild(lv, QUEUE);
        building = true;
        STATMod.LOGGER.info("[City] Regen de la cité programmée ({} segments)", QUEUE.totalJobs());
    }

    private static void enqueueBuild(ServerLevel lv, CityBuildQueue q) {
        CityShell.enqueue(lv, q);
        q.add(() -> PlazaBuilder.build(lv));
        q.add(() -> ArtisanCampBuilder.build(lv));
        DungeonGateBuilder.build(lv, q);
        q.add(() -> PortalCourtBuilder.build(lv));
    }

    /**
     * Mondes existants : l'ancienne île hub (temple + cage barrière + relief, bbox ±152 autour
     * de (0,100,-300)) est DANS l'emprise de la cité. Détectée par un bloc non-air sous l'ancien
     * spawn → purge ciblée de sa bounding box (scan-and-clear, jamais de setBlock sur de l'air).
     */
    private static void enqueueLegacyCleanupIfNeeded(ServerLevel lv, CityBuildQueue q) {
        BlockPos oldSpawn = new BlockPos(0, 100, -300);
        boolean legacy = !lv.getBlockState(oldSpawn.below()).isAir()
                || !lv.getBlockState(oldSpawn.offset(150, 0, 0)).isAir(); // coin de cage barrière
        if (!legacy) return;
        var box = IslandGenerator.floorBoundingBox(0);
        for (int x0 = box.minX(); x0 <= box.maxX(); x0 += 8) {
            final int xs = x0, xe = Math.min(x0 + 7, box.maxX());
            q.add(() -> {
                for (int x = xs; x <= xe; x++)
                    for (int y = box.minY(); y <= box.maxY(); y++)
                        for (int z = box.minZ(); z <= box.maxZ(); z++) {
                            BlockPos p = new BlockPos(x, y, z);
                            if (!lv.getBlockState(p).isAir()) {
                                lv.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                            }
                        }
            });
        }
        STATMod.LOGGER.info("[City] Ancien temple/cage détectés — purge legacy programmée");
    }

    /** Purge de l'intérieur de la cité (au-dessus du sol, sous le plafond) pour la regen. */
    private static void enqueueInteriorClear(ServerLevel lv, CityBuildQueue q) {
        for (int x0 = -CityPlan.RADIUS; x0 <= CityPlan.RADIUS; x0 += 4) {
            final int xs = x0, xe = Math.min(x0 + 3, CityPlan.RADIUS);
            q.add(() -> {
                for (int x = xs; x <= xe; x++)
                    for (int z = CityPlan.CENTER_Z - CityPlan.RADIUS; z <= CityPlan.CENTER_Z + CityPlan.RADIUS; z++) {
                        if (!CityPlan.inCity(x, z)) continue;
                        for (int y = CityPlan.GROUND_Y + 1; y <= CityPlan.CEILING_Y + 2; y++) {
                            BlockPos p = new BlockPos(x, y, z);
                            if (!lv.getBlockState(p).isAir()) {
                                lv.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                            }
                        }
                    }
            });
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel lv = event.getServer().getLevel(DungeonDimensions.TRIAL_DUNGEON);
        if (lv != null) ensureCity(lv);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!building) return;
        ServerLevel lv = event.getServer().getLevel(DungeonDimensions.TRIAL_DUNGEON);
        if (lv == null) { building = false; return; }

        QUEUE.tick(JOBS_PER_TICK);

        // Progression aux joueurs présents dans la cité, toutes les 2 s.
        if (event.getServer().getTickCount() % 40 == 0) {
            for (ServerPlayer p : DungeonTeleportHandler.playersOnFloor(lv, 0)) {
                p.displayClientMessage(Component.translatable("dungeon.city.building",
                        QUEUE.progressPercent()), true);
            }
        }

        if (QUEUE.isDone()) {
            building = false;
            CitySavedData.get(lv).setBuiltVersion(CITY_VERSION);
            STATMod.LOGGER.info("[City] Cité des Aventuriers construite ({} segments)", QUEUE.totalJobs());
        }
    }
}
