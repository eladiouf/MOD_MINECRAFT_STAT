package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Mission M6 — Phase ζ.
 *
 * <p>Bloc placé automatiquement au spawn de chaque étage par {@link IslandGenerator}. Right-click
 * renvoie le joueur à la position d'où il est entré dans le donjon (via
 * {@link DungeonTeleportHandler#returnToOverworld}).
 */
public class DungeonReturnBeaconBlock extends Block {

    public DungeonReturnBeaconBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        boolean ok = DungeonTeleportHandler.returnToOverworld(sp);
        sp.displayClientMessage(Component.translatable(
                ok ? "block.statmod.return_beacon.success"
                   : "block.statmod.return_beacon.failed"), true);
        return InteractionResult.CONSUME;
    }
}
