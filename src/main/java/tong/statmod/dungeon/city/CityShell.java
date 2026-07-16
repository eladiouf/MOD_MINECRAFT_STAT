package tong.statmod.dungeon.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Coque de la caverne-cité : sol (2 épaisseurs), rempart périmétral, dalle de plafond et
 * cristaux lumineux suspendus. Enfilée en bandes de 4 blocs en X pour rester sous le budget
 * par tick. Déterministe (seed fixe pour les cristaux).
 */
final class CityShell {

    /** Flag 2 : pas de neighbor updates — indispensable pour un build de masse. */
    private static final int FLAG = 2;

    private CityShell() {}

    static void enqueue(ServerLevel lv, CityBuildQueue q) {
        for (int x0 = -CityPlan.RADIUS; x0 <= CityPlan.RADIUS; x0 += 4) {
            final int xs = x0;
            final int xe = Math.min(x0 + 3, CityPlan.RADIUS);
            q.add(() -> buildStrip(lv, xs, xe));
        }
        // Stalactites suspendues — positions déterministes (même seed → même ciel).
        Random rng = new Random(0xC17ADE5L);
        for (int i = 0; i < 90; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double dist = rng.nextDouble() * (CityPlan.WALL_INNER - 30);
            final int cx = CityPlan.CENTER_X + (int) (Math.cos(angle) * dist);
            final int cz = CityPlan.CENTER_Z + (int) (Math.sin(angle) * dist);
            final int len = 3 + rng.nextInt(6);
            final boolean dripstone = rng.nextBoolean();
            q.add(() -> buildStalactite(lv, cx, cz, len, dripstone));
        }
    }

    private static void buildStrip(ServerLevel lv, int x0, int x1) {
        BlockState deep = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState pave = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState plazaPave = Blocks.POLISHED_DIORITE.defaultBlockState();
        BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();

        for (int x = x0; x <= x1; x++) {
            for (int z = CityPlan.CENTER_Z - CityPlan.RADIUS; z <= CityPlan.CENTER_Z + CityPlan.RADIUS; z++) {
                if (!CityPlan.inCity(x, z)) continue;

                // Sol : soubassement + surface (pavage sur place/avenues, pierre ailleurs).
                lv.setBlock(new BlockPos(x, CityPlan.GROUND_Y - 1, z), deep, FLAG);
                BlockState surface = CityPlan.inPlaza(x, z) ? plazaPave
                        : CityPlan.onAvenue(x, z) || CityPlan.onRingRoad(x, z)
                        || CityPlan.onDistrictConnector(x, z) ? pave
                        : stone;
                lv.setBlock(new BlockPos(x, CityPlan.GROUND_Y, z), surface, FLAG);

                // Rempart périmétral plein (sauf couloir de la porte).
                if (CityPlan.inWallRing(x, z) && !CityPlan.inGateOpening(x, z)) {
                    for (int y = CityPlan.GROUND_Y + 1; y <= CityPlan.WALL_TOP_Y; y++) {
                        lv.setBlock(new BlockPos(x, y, z), wall, FLAG);
                    }
                }

                // Plafond de grotte : corps rocheux ondulé de heightAt → CEILING_Y (coque fermée).
                int ch = CityCeiling.heightAt(x, z);
                BlockState rock = ((x * 7 + z * 13) & 3) == 0
                        ? Blocks.TUFF.defaultBlockState()
                        : ((x + z) & 1) == 0 ? Blocks.DEEPSLATE.defaultBlockState()
                        : Blocks.STONE.defaultBlockState();
                for (int y = ch; y <= CityPlan.CEILING_Y; y++) {
                    lv.setBlock(new BlockPos(x, y, z), rock, FLAG);
                }
            }
        }
    }

    /** Suspension accrochée sous le plafond réel : gaine + pointe (améthyste ou dripstone). */
    private static void buildStalactite(ServerLevel lv, int x, int z, int len, boolean dripstone) {
        int top = CityCeiling.heightAt(x, z);
        for (int i = 0; i < len; i++) {
            int y = top - 1 - i;
            if (y <= CityPlan.GROUND_Y + 4) break;
            BlockState body = dripstone ? Blocks.DRIPSTONE_BLOCK.defaultBlockState()
                    : Blocks.AMETHYST_BLOCK.defaultBlockState();
            lv.setBlock(new BlockPos(x, y, z), body, FLAG);
            if (i == len / 2 && !dripstone) {
                lv.setBlock(new BlockPos(x + 1, y, z), Blocks.SEA_LANTERN.defaultBlockState(), FLAG);
            }
        }
        int tipY = top - 1 - len;
        if (tipY > CityPlan.GROUND_Y + 4) {
            BlockState tip = dripstone
                    ? Blocks.POINTED_DRIPSTONE.defaultBlockState()
                    : Blocks.AMETHYST_CLUSTER.defaultBlockState();
            lv.setBlock(new BlockPos(x, tipY, z), tip, FLAG);
        }
    }
}
