package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.sound.ModSounds;

public class DungeonPortalBlock extends Block {

    public DungeonPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        serverPlayer.getCapability(StatCapabilities.PLAYER_STATS).ifPresent(data -> {
            data.setLastOverworldDimensionId(level.dimension().location().toString());
            data.setLastOverworldPos(pos.asLong());
        });

        boolean ok = DungeonTeleportHandler.enterFloor(serverPlayer, 0, true);
        if (ok) {
            serverPlayer.playNotifySound(ModSounds.DUNGEON_PORTAL_ENTER.get(), SoundSource.BLOCKS, 0.7f, 1.3f);
            serverPlayer.displayClientMessage(
                    Component.translatable("block.statmod.dungeon_portal.enter_hub"), true);
        } else {
            serverPlayer.displayClientMessage(
                    Component.translatable("block.statmod.dungeon_portal.failed"), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            checkAndCreateMagicCircle(level, pos);
        }
    }

    private void checkAndCreateMagicCircle(Level level, BlockPos pos) {
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                BlockPos center = pos.offset(cx, 0, cz);
                if (isMagicCircleGrid(level, center)) {
                    transformToMagicCircleGrid(level, center);
                    return;
                }
            }
        }
    }

    private boolean isMagicCircleGrid(Level level, BlockPos center) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos p = center.offset(x, 0, z);
                if (!(level.getBlockState(p).getBlock() instanceof DungeonPortalBlock)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void transformToMagicCircleGrid(Level level, BlockPos center) {
        BlockState circleState = DungeonBlocks.MAGIC_TELEPORT_CIRCLE.get().defaultBlockState();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos p = center.offset(x, 0, z);
                MagicTeleportCircleBlock.CirclePart part;
                if (x == -1 && z == -1) part = MagicTeleportCircleBlock.CirclePart.NORTH_WEST;
                else if (x == 0 && z == -1) part = MagicTeleportCircleBlock.CirclePart.NORTH;
                else if (x == 1 && z == -1) part = MagicTeleportCircleBlock.CirclePart.NORTH_EAST;
                else if (x == -1 && z == 0) part = MagicTeleportCircleBlock.CirclePart.WEST;
                else if (x == 0 && z == 0) part = MagicTeleportCircleBlock.CirclePart.CENTER;
                else if (x == 1 && z == 0) part = MagicTeleportCircleBlock.CirclePart.EAST;
                else if (x == -1 && z == 1) part = MagicTeleportCircleBlock.CirclePart.SOUTH_WEST;
                else if (x == 0 && z == 1) part = MagicTeleportCircleBlock.CirclePart.SOUTH;
                else part = MagicTeleportCircleBlock.CirclePart.SOUTH_EAST;

                level.setBlock(p, circleState.setValue(MagicTeleportCircleBlock.PART, part), 3);
            }
        }

        double cxDouble = center.getX() + 0.5D;
        double cyDouble = center.getY() + 0.5D;
        double czDouble = center.getZ() + 0.5D;
        level.playSound(null, cxDouble, cyDouble, czDouble, net.minecraft.sounds.SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 1.0F, 1.2F);

        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos p = center.offset(x, 0, z);
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 3, 0.2D, 0.2D, 0.2D, 0.1D);
                    serverLevel.sendParticles(ParticleTypes.PORTAL, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 10, 0.4D, 0.4D, 0.4D, 0.2D);
                }
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
