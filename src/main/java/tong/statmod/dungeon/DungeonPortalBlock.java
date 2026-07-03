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
