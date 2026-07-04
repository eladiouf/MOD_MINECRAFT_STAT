package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static net.minecraft.world.level.block.Blocks.*;

/**
 * DungeonMasterpiece — briques partagées du Trial Dungeon.
 *
 * <p>Après la refonte 2026-07-04, cette classe ne conserve que ce qui est réellement utilisé :
 * <ul>
 *   <li>{@link #buildUnderside} — cône rocheux flottant sous chaque île (appelé par
 *       {@link DungeonArchitect#buildFloor}).</li>
 *   <li>{@link #mobPool} / {@link #mobCount} — table des mobs de vague par tier (consommée par
 *       {@link ModdedMobPool} et {@link DungeonMobSpawner}).</li>
 * </ul>
 * L'ancienne île plate circulaire ({@code buildIsland} & co.) a été retirée : la génération passe
 * par {@link DungeonArchitect} + {@link IslandTerrainShaper}.
 */
public final class DungeonMasterpiece {

    static final int R = 50;

    private DungeonMasterpiece() {}

    static BlockState B(Block b) { return b.defaultBlockState(); }
    static void S(ServerLevel lv, BlockPos p, BlockState s) { lv.setBlock(p, s, 3); }
    static BlockPos O(BlockPos p, int x, int y, int z) { return p.offset(x, y, z); }
    static boolean in(int dx, int dz) { return dx*dx + dz*dz <= R*R; }

    // ═══════════════ UNDERSIDE CONE ═══════════════

    /** Cône rocheux décroissant sous l'île (profond au centre, fin au bord). */
    static void buildUnderside(ServerLevel lv, BlockPos sp, FloorPalette t) {
        BlockState u1 = t.underside() == STONE ? B(STONE) : B(t.underside());
        BlockState u2 = t.underside() == STONE ? B(COBBLESTONE) : B(t.decorPrimary());
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            if (!in(dx, dz)) continue;
            double dist = Math.sqrt(dx*dx + dz*dz);
            int depth = (int)((1.0 - dist/R) * (R/3.0)) + 2;
            for (int dy = 1; dy <= depth; dy++)
                S(lv, O(sp, dx, -dy, dz), (dy%3==0) ? u2 : u1);
        }
    }

    // ═══════════════ MOB TABLE (vagues de combat) ═══════════════

    /**
     * Pool de mobs vanilla organisé par difficulté (progression logique).
     * Combiné aux mobs moddés par {@link ModdedMobPool#getCombinedPool}.
     */
    static List<EntityType<?>> mobPool(FloorPalette t) {
        return switch(t) {
            case EARLY -> List.of(
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
                EntityType.HUSK, EntityType.CAVE_SPIDER, EntityType.SILVERFISH,
                EntityType.PILLAGER, EntityType.CREEPER, EntityType.WITCH
            );
            case MID -> List.of(
                EntityType.STRAY, EntityType.DROWNED, EntityType.CAVE_SPIDER,
                EntityType.VINDICATOR, EntityType.EVOKER, EntityType.RAVAGER,
                EntityType.PIGLIN, EntityType.HOGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.BLAZE,
                EntityType.WITCH, EntityType.CREEPER, EntityType.ENDERMAN
            );
            case LATE -> List.of(
                EntityType.VEX, EntityType.RAVAGER, EntityType.VINDICATOR, EntityType.EVOKER,
                EntityType.WITHER_SKELETON, EntityType.BLAZE, EntityType.HOGLIN, EntityType.PIGLIN_BRUTE,
                EntityType.ENDERMAN, EntityType.SHULKER, EntityType.PHANTOM,
                EntityType.WITCH, EntityType.CAVE_SPIDER, EntityType.DROWNED
            );
            case ABYSS -> List.of(
                EntityType.ELDER_GUARDIAN, EntityType.WARDEN, EntityType.PIGLIN_BRUTE,
                EntityType.WITHER_SKELETON, EntityType.BLAZE, EntityType.HOGLIN,
                EntityType.ENDERMAN, EntityType.SHULKER, EntityType.PHANTOM,
                EntityType.RAVAGER, EntityType.VEX, EntityType.VINDICATOR
            );
        };
    }

    /** Nombre de mobs par vague (équilibré : action sans surpopulation). */
    static int mobCount(FloorPalette t) {
        return switch(t) { case EARLY->4; case MID->7; case LATE->10; case ABYSS->12; };
    }
}
