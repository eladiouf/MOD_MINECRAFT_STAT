package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class DungeonReturnBeaconBlock extends Block {

    public DungeonReturnBeaconBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        boolean ok = DungeonTeleportHandler.returnToOverworld(sp);
        sp.displayClientMessage(Component.translatable(
                ok ? "block.statmod.return_beacon.success"
                   : "block.statmod.return_beacon.failed"), true);
        return InteractionResult.CONSUME;
    }
}
