package tong.statmod.dungeon;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import tong.statmod.config.Config;

/**
 * Mission M6 — Phase γ.
 *
 * <p>Helper pur : calcule le multiplier d'XP à appliquer quand le joueur tue un mob dans le
 * Trial Dungeon. Formule : {@code base + floor * perFloor}. Le floor est déduit de la position
 * XZ du joueur via {@link DungeonTeleportHandler#floorAtPos}.
 *
 * <p>Hors du Trial Dungeon, retourne toujours 1.0 → aucun impact sur les kills overworld.
 */
public final class DungeonXpMultiplier {

    private DungeonXpMultiplier() {}

    /**
     * Multiplier d'XP à appliquer pour un kill dans la dimension {@code dim}, à la position
     * (x,z). {@code 1.0} si le joueur n'est pas dans le donjon.
     */
    public static double multiplierFor(ResourceKey<Level> dim, int x, int z) {
        if (dim == null || !DungeonDimensions.TRIAL_DUNGEON.equals(dim)) {
            return 1.0;
        }
        int floor = DungeonTeleportHandler.floorAtPos(x, z);
        return Config.getDungeonXpBaseMultiplier() + floor * Config.getDungeonXpPerFloor();
    }

    /**
     * Applique le multiplier à un XP de base. Retourne au moins 1 pour ne jamais tuer le gain.
     */
    public static int applyToXp(int baseXp, ResourceKey<Level> dim, int x, int z) {
        double mult = multiplierFor(dim, x, z);
        return Math.max(1, (int) Math.round(baseXp * mult));
    }
}
