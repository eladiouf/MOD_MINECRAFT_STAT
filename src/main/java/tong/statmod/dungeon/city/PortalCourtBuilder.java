package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import tong.statmod.dungeon.DungeonBlocks;

/**
 * Cour des Portails : 5 arches runiques en arc de cercle. Une seule est active (retour
 * overworld — balise de retour au centre), les 4 autres sont scellées d'obsidienne pleureuse
 * (« bientôt » : villes futures, dimensions, événements — crochets v2 du spec §8).
 */
final class PortalCourtBuilder {

    private PortalCourtBuilder() {}

    static void build(ServerLevel lv) {
        BlockPos c = CityPlan.portalCourt().above(); // Y=101
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(140 + i * 20); // arc orienté vers la place
            BlockPos base = c.offset((int) (Math.cos(angle) * 14), 0, (int) (Math.sin(angle) * 14));
            buildArch(lv, base, i == 2); // seule l'arche centrale est active
        }
    }

    private static void buildArch(ServerLevel lv, BlockPos base, boolean active) {
        BlockState frame = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
        BlockState seal = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        for (int y = 0; y <= 5; y++) {
            lv.setBlock(base.offset(-2, y, 0), frame, 3);
            lv.setBlock(base.offset(2, y, 0), frame, 3);
        }
        for (int x = -2; x <= 2; x++) lv.setBlock(base.offset(x, 6, 0), frame, 3);
        if (active) {
            lv.setBlock(base.offset(0, 0, 0), DungeonBlocks.RETURN_BEACON.get().defaultBlockState(), 3);
            lv.setBlock(base.offset(-1, 6, 0), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
            lv.setBlock(base.offset(1, 6, 0), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
        } else {
            for (int x = -1; x <= 1; x++)
                for (int y = 0; y <= 5; y++)
                    lv.setBlock(base.offset(x, y, 0), seal, 3);
        }
    }
}
