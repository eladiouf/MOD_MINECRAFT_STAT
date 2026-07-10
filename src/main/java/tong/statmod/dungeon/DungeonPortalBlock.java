package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import tong.statmod.sound.ModSounds;
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

/**
 * Mission M6 — Phase β.
 *
 * <p>Bloc {@code statmod:dungeon_portal} — la porte d'entrée du Trial Dungeon.
 *
 * <p>MVP : right-click téléporte le joueur au plus haut étage débloqué. Un menu de sélection
 * multi-étages viendra en phase β.b.
 */
public class DungeonPortalBlock extends Block {

    public DungeonPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        PlayerStatData data = serverPlayer.getData(ModAttachments.STATS);
        int floor = data.getDungeonFloorReached();

        data.setLastOverworldDimensionId(level.dimension().location().toString());
        data.setLastOverworldPos(pos.asLong());

        boolean ok = DungeonTeleportHandler.enterFloor(serverPlayer, floor);
        if (ok) {
            serverPlayer.playNotifySound(ModSounds.DUNGEON_PORTAL_ENTER.get(), SoundSource.BLOCKS, 0.7f, 1.3f);
            serverPlayer.displayClientMessage(
                    Component.translatable("block.statmod.dungeon_portal.enter", floor), true);
        } else {
            serverPlayer.displayClientMessage(
                    Component.translatable("block.statmod.dungeon_portal.failed"), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            checkAndCreateMagicCircle(level, pos);
        }
    }

    private void checkAndCreateMagicCircle(Level level, BlockPos pos) {
        // AXE X (East-West)
        BlockPos[] lineX1 = { pos.west(), pos, pos.east() };
        BlockPos[] lineX2 = { pos, pos.east(), pos.east(2) };
        BlockPos[] lineX3 = { pos.west(2), pos.west(), pos };

        if (isPortalLine(level, lineX1)) {
            transformToMagicCircle(level, lineX1);
            return;
        }
        if (isPortalLine(level, lineX2)) {
            transformToMagicCircle(level, lineX2);
            return;
        }
        if (isPortalLine(level, lineX3)) {
            transformToMagicCircle(level, lineX3);
            return;
        }

        // AXE Z (North-South)
        BlockPos[] lineZ1 = { pos.north(), pos, pos.south() };
        BlockPos[] lineZ2 = { pos, pos.south(), pos.south(2) };
        BlockPos[] lineZ3 = { pos.north(2), pos.north(), pos };

        if (isPortalLine(level, lineZ1)) {
            transformToMagicCircle(level, lineZ1);
            return;
        }
        if (isPortalLine(level, lineZ2)) {
            transformToMagicCircle(level, lineZ2);
            return;
        }
        if (isPortalLine(level, lineZ3)) {
            transformToMagicCircle(level, lineZ3);
            return;
        }
    }

    private boolean isPortalLine(Level level, BlockPos[] positions) {
        for (BlockPos p : positions) {
            if (!(level.getBlockState(p).getBlock() instanceof DungeonPortalBlock)) {
                return false;
            }
        }
        return true;
    }

    private void transformToMagicCircle(Level level, BlockPos[] positions) {
        BlockState circleState = DungeonBlocks.MAGIC_TELEPORT_CIRCLE.get().defaultBlockState();
        for (BlockPos p : positions) {
            level.setBlock(p, circleState, 3);
        }

        double cx = (positions[0].getX() + positions[2].getX()) / 2.0 + 0.5D;
        double cy = positions[1].getY() + 0.5D;
        double cz = (positions[0].getZ() + positions[2].getZ()) / 2.0 + 0.5D;
        level.playSound(null, cx, cy, cz, net.minecraft.sounds.SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 1.0F, 1.2F);

        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (BlockPos p : positions) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 10, 0.2D, 0.2D, 0.2D, 0.1D);
                serverLevel.sendParticles(ParticleTypes.PORTAL, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 30, 0.4D, 0.4D, 0.4D, 0.2D);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 3; i++) {
            double x = pos.getX() + 0.25D + random.nextDouble() * 0.5D;
            double y = pos.getY() + 1.0D + random.nextDouble() * 0.5D;
            double z = pos.getZ() + 0.25D + random.nextDouble() * 0.5D;
            double dx = (pos.getX() + 0.5D - x) * 0.02D;
            double dy = 0.03D + random.nextDouble() * 0.03D;
            double dz = (pos.getZ() + 0.5D - z) * 0.02D;
            level.addParticle(ParticleTypes.PORTAL, x, y, z, dx, dy, dz);
        }
    }
}
