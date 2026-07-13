package tong.statmod.dungeon.template;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record ConnectionPoint(int relX, int relY, int relZ, String type, String pool) {
    public BlockPos worldPos(BlockPos origin, Direction rotation) {
        return switch (rotation) {
            case NORTH -> origin.offset(relX, relY, relZ);
            case SOUTH -> origin.offset(-relX, relY, -relZ);
            case EAST -> origin.offset(-relZ, relY, relX);
            case WEST -> origin.offset(relZ, relY, -relX);
            default -> origin.offset(relX, relY, relZ);
        };
    }
}
