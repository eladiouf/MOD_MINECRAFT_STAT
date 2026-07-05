package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import tong.statmod.STATMod;

/**
 * Mission M6 — Génération d'un étage du Trial Dungeon.
 *
 * <p>Pipeline (dans l'ordre) :
 * <ol>
 *   <li>{@link DungeonArchitect#buildFloor} — forteresse flottante "The Descent" (underside conique,
 *       remparts, tours, avenue, ailes, cœur selon le rôle). Elle pose son propre sol plat.</li>
 *   <li>{@link IslandTerrainShaper#buildIslandGround} — relief organique + végétation sur le
 *       <b>pourtour</b> de l'île uniquement (hors emprise de la forteresse), pour l'aspect « île
 *       flottante vivante » sans percer le sol de jeu.</li>
 * </ol>
 *
 * <p>Déterministe : même étage → même île (seed dérivé du numéro d'étage).
 */
public final class IslandGenerator {

    static final int R = 80;

    private IslandGenerator() {}

    public static boolean generateFloor(ServerLevel lv, int floor) {
        BlockPos sp = DungeonTeleportHandler.floorSpawnPos(floor);
        if (!lv.getBlockState(sp.below()).isAir()) return false;
        FloorPalette tier = FloorPalette.forFloor(floor);
        ThemePalette theme = ThemePalette.forFloor(floor); // identité matérielle par thème/arc

        DungeonArchitect.Role role =
                (floor % 10 == 0) ? DungeonArchitect.Role.BOSS
              : (floor % 5 == 0) ? DungeonArchitect.Role.TREASURE
              : DungeonArchitect.Role.COMBAT;

        // 1. Forteresse (pose l'underside conique + le sol plat intérieur + toute la structure).
        DungeonArchitect.buildFloor(lv, sp, floor, role);

        // 2. Relief organique du pourtour (hors emprise) — surface par thème, végétation par tier.
        long islandSeed = IslandShaper.seedFor(floor);
        new IslandTerrainShaper(islandSeed, floor, R).buildIslandGround(lv, sp, theme, tier);

        STATMod.LOGGER.info("[TrialDungeon] Floor {} generated ({}, {})", floor, theme.name(), role);
        return true;
    }

    public static BoundingBox floorBoundingBox(int floor) {
        BlockPos sp = DungeonTeleportHandler.floorSpawnPos(floor);
        // Y : de l'underside conique (~ -30 avec R=80) jusqu'au sommet des tours (~ +22). Couvre
        // tout pour que /statdungeon regen efface la structure entière avant reconstruction.
        return new BoundingBox(sp.getX()-R, sp.getY()-34, sp.getZ()-R, sp.getX()+R, sp.getY()+24, sp.getZ()+R);
    }

    static int radiusFor(int floor) { return R; }
}
