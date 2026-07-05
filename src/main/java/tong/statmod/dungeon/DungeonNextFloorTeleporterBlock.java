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
import tong.statmod.storage.ModAttachments;
import tong.statmod.storage.PlayerStatData;

/**
 * Mission M6 — Phase ζ.
 *
 * <p>Bloc placé au bord de chaque île par {@link IslandGenerator}. Right-click tp vers
 * l'étage suivant, à condition que celui-ci soit débloqué (i.e. floorReached ≥ nextFloor).
 *
 * <p>L'étage courant est déduit de la position XZ du bloc via
 * {@link DungeonTeleportHandler#floorAtPos}. Aucun BlockEntity n'est nécessaire.
 */
public class DungeonNextFloorTeleporterBlock extends Block {

    public DungeonNextFloorTeleporterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        int currentFloor = DungeonTeleportHandler.floorAtPos(pos.getX(), pos.getZ());
        int nextFloor = currentFloor + 1;

        PlayerStatData data = sp.getData(ModAttachments.STATS);
        if (nextFloor > data.getDungeonFloorReached()) {
            // Message spécifique à l'objectif : le joueur sait EXACTEMENT quoi faire pour ouvrir la voie.
            DungeonObjective objective = DungeonObjective.forFloor(currentFloor);
            sp.displayClientMessage(Component.translatable(
                    "block.statmod.next_floor_teleporter.locked_objective",
                    Component.translatable(objective.translationKey())), true);
            return InteractionResult.CONSUME;
        }

        DungeonTeleportHandler.enterFloor(sp, nextFloor);
        return InteractionResult.CONSUME;
    }
}
