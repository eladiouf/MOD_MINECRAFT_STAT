package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import tong.statmod.STATMod;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

import java.util.Set;

/**
 * Mission M6 — Phase β / Phase ε (layout horizontal).
 *
 * <p>Point d'entrée serveur pour toute téléportation liée au Trial Dungeon.
 *
 * <p>Layout horizontal : les îles sont disposées en grille XZ (Y=100 constant),
 * espacées de {@value #FLOOR_SPACING} blocs centre-à-centre. Une grille de
 * {@value #GRID_COLS} colonnes × rangées infinies. Plus de limite de hauteur :
 * le nombre d'étages est illimité.
 */
public final class DungeonTeleportHandler {

    /** Espacement horizontal entre centres d'îles (assez pour une arène boss 40×40). */
    public static final int FLOOR_SPACING = 200;
    /** Nombre d'étages par rangée de la grille. */
    public static final int GRID_COLS = 10;
    /** Y constant pour toutes les îles. */
    private static final int FLOOR_Y = 100;

    private DungeonTeleportHandler() {}

    /**
     * Position exacte du spawn pad d'un étage dans la grille XZ.
     */
    public static BlockPos floorSpawnPos(int floor) {
        int idx = Math.max(0, floor - 1);
        int col = idx % GRID_COLS;
        int row = idx / GRID_COLS;
        return new BlockPos(col * FLOOR_SPACING, FLOOR_Y, row * FLOOR_SPACING);
    }

    /**
     * Déduit l'étage le plus proche à partir d'une position (X,Z) dans la dimension.
     * Utilisé pour l'XP multiplier, le loot, le HUD.
     */
    public static int floorAtPos(int x, int z) {
        int col = Math.floorDiv(x + FLOOR_SPACING / 2, FLOOR_SPACING);
        int row = Math.floorDiv(z + FLOOR_SPACING / 2, FLOOR_SPACING);
        int floor = row * GRID_COLS + col + 1;
        return Math.max(1, floor);
    }

    /**
     * Sauvegarde la position actuelle du joueur (dimension + BlockPos) puis tp vers l'étage
     * demandé du Trial Dungeon. Ne fait rien si :
     * <ul>
     *   <li>le joueur est déjà dans le donjon,</li>
     *   <li>l'étage demandé dépasse {@code floorReached},</li>
     *   <li>le serveur ne peut pas résoudre la dimension (datapack absent).</li>
     * </ul>
     *
     * @return {@code true} si le tp a bien eu lieu, {@code false} sinon.
     */
    public static boolean enterFloor(ServerPlayer player, int floor) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        if (floor < 1 || floor > data.getDungeonFloorReached()) {
            STATMod.LOGGER.info("[TrialDungeon] Refus tp: floor={} reached={}", floor, data.getDungeonFloorReached());
            return false;
        }

        ServerLevel dungeon = server.getLevel(DungeonDimensions.TRIAL_DUNGEON);
        if (dungeon == null) {
            STATMod.LOGGER.warn("[TrialDungeon] Dimension statmod:trial_dungeon introuvable — datapack ok ?");
            return false;
        }

        ResourceKey<Level> currentDim = player.level().dimension();
        if (!currentDim.equals(DungeonDimensions.TRIAL_DUNGEON)) {
            data.setLastOverworldDimensionId(currentDim.location().toString());
            data.setLastOverworldPos(player.blockPosition().asLong());
        }

        BlockPos spawn = floorSpawnPos(floor);
        IslandGenerator.generateFloor(dungeon, floor);

        // NOTE (« vraie aventure », 2026-07-04) : plus d'auto-unlock à l'entrée. Chaque étage doit
        // être CONQUIS (objectif accompli — cf. DungeonObjective/DungeonProgress) pour débloquer la
        // sortie vers l'étage suivant. Le donjon n'est plus un couloir.

        player.teleportTo(dungeon, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                Set.of(), player.getYRot(), player.getXRot());

        // Ouvre la fenêtre d'autorisation de spawn de l'étage (durée ∝ difficulté) AVANT de
        // demander la vague, pour que les mobs (et compagnons) puissent apparaître.
        DungeonSpawnGuard.openWindowForFloor(floor);

        // Spawn de la vague de combat MAINTENANT que le joueur est dans le donjon et suit les
        // chunks → les mobs sont trackés dès leur apparition → visibles. Garde anti-doublon
        // intégrée (mobs vivants / vague déjà en file).
        DungeonMobSpawner.requestWave(dungeon, floor);

        STATMod.LOGGER.info("[TrialDungeon] {} entre à l'étage {} (X={} Z={})",
                player.getGameProfile().getName(), floor, spawn.getX(), spawn.getZ());
        return true;
    }

    /**
     * Tp le joueur vers la position overworld sauvegardée. Fallback vers son spawn si aucune
     * position n'est stockée.
     */
    public static boolean returnToOverworld(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        PlayerStatData data = player.getData(ModAttachments.STATS);
        if (!data.hasLastOverworldPos() || data.getLastOverworldDimensionId() == null) {
            ServerLevel overworld = server.overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                    Set.of(), player.getYRot(), player.getXRot());
            return true;
        }

        ResourceLocation dimId = ResourceLocation.tryParse(data.getLastOverworldDimensionId());
        if (dimId == null) return false;
        ServerLevel target = server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimId));
        if (target == null) return false;

        BlockPos pos = BlockPos.of(data.getLastOverworldPosPacked());
        player.teleportTo(target, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                Set.of(), player.getYRot(), player.getXRot());
        return true;
    }
}
