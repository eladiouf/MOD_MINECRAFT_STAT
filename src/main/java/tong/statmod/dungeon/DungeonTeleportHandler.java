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

    /** {@code true} si l'étage est un étage de combat (vague), pas boss (×10)/trésor (×5). */
    public static boolean isCombatFloor(int floor) {
        return floor > 0 && floor % 10 != 0 && floor % 5 != 0;
    }

    /** {@code true} si l'étage est une chaîne de pièces (combat OU trésor), pas un boss (×10). */
    public static boolean isRoomChainFloor(int floor) {
        return floor > 0 && floor % 10 != 0;
    }

    /**
     * Position où <b>téléporter le joueur</b> à l'entrée d'un étage. Chaîne de pièces (combat/trésor)
     * → centre de la <b>pièce d'apparition</b> ({@link DungeonRoomChain}). Boss (×10) → bord sud de
     * l'arène géante (l'autel occupe le centre).
     */
    public static BlockPos floorPlayerSpawnPos(int floor) {
        BlockPos island = floorSpawnPos(floor);
        if (isRoomChainFloor(floor)) return DungeonRoomChain.spawnWorldPos(island);
        // Boss (générique OU arène importée) : apparaître sur la PLATEFORME au bord sud, hors de la
        // structure/arène (qui est centrée et plus petite que l'emprise) → jamais dans un bloc.
        return island.offset(0, 1, DungeonArchitect.HZ - 12);
    }

    /** Clé NBT persistante : le joueur a déjà vu le tutoriel d'accueil du donjon. */
    private static final String INTRO_SEEN_TAG = "statmod_dungeon_intro_seen";

    /** Envoie une fois par joueur un court tutoriel expliquant les mécaniques du donjon. */
    private static void sendIntroIfFirstTime(ServerPlayer player) {
        if (player.getPersistentData().getBoolean(INTRO_SEEN_TAG)) return;
        player.getPersistentData().putBoolean(INTRO_SEEN_TAG, true);
        for (int i = 1; i <= 4; i++) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("dungeon.intro." + i), false);
        }
    }

    /**
     * Corrige la position de spawn pour ne JAMAIS apparaître dans un bloc : scanne la colonne autour
     * du Y voulu et renvoie le 1ᵉʳ emplacement libre (2 blocs d'air sur un sol solide). Filet de
     * sécurité générique (utile surtout sous les arènes importées dont le sol varie).
     */
    private static BlockPos safeSpawn(ServerLevel lv, BlockPos want) {
        int x = want.getX(), z = want.getZ();
        // Cherche vers le haut depuis un peu sous le Y voulu : premier bloc plein avec 2 d'air au-dessus.
        for (int y = want.getY() - 3; y <= want.getY() + 40; y++) {
            BlockPos foot = new BlockPos(x, y, z);
            if (!lv.getBlockState(foot.below()).isAir()
                    && lv.getBlockState(foot).isAir()
                    && lv.getBlockState(foot.above()).isAir()) {
                return foot;
            }
        }
        return want; // fallback : rien trouvé, on garde la position demandée
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

        IslandGenerator.generateFloor(dungeon, floor);
        // Spawn au centre de la pièce d'apparition (combat/trésor) ou au bord sud de la plateforme (boss).
        BlockPos spawn = safeSpawn(dungeon, floorPlayerSpawnPos(floor));

        // NOTE (« vraie aventure », 2026-07-04) : plus d'auto-unlock à l'entrée. Chaque étage doit
        // être CONQUIS (objectif accompli — cf. DungeonObjective/DungeonProgress) pour débloquer la
        // sortie vers l'étage suivant. Le donjon n'est plus un couloir.

        player.teleportTo(dungeon, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                Set.of(), player.getYRot(), player.getXRot());

        // Spawn de la vague de combat MAINTENANT que le joueur est dans le donjon et suit les
        // chunks → les mobs sont trackés dès leur apparition → visibles. Une seule vague par
        // étage : elle est le défi à nettoyer pour conquérir l'étage (pas de réalimentation).
        DungeonMobSpawner.requestWave(dungeon, floor);

        // Dungeon Rush : l'étage démarre « sans-faute » — le conquérir sans un coup reçu double
        // la récompense de conquête.
        DungeonRush.beginFloor(player.getUUID());

        // Sync des données donjon (points + max floor) au client dès l'entrée, pour que le HUD
        // affiche les bonnes valeurs immédiatement (l'attachment n'est pas auto-synchronisé).
        tong.statmod.network.SyncHelper.syncStats(player);

        // Annonce du thème de l'étage (chaque étage a le sien).
        DungeonThemes.Theme theme = DungeonThemes.forFloor(floor);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                theme.displayName()), false);
        // Objectif de l'étage en barre d'action (le joueur sait quoi faire dès l'entrée).
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "dungeon.enter.objective",
                net.minecraft.network.chat.Component.translatable(
                        DungeonObjective.forFloor(floor).translationKey())), true);

        // Tutoriel d'accueil, une seule fois par joueur (onboarding des mécaniques).
        sendIntroIfFirstTime(player);

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
