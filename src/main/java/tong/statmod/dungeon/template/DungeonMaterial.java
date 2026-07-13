package tong.statmod.dungeon.template;

import net.minecraft.world.level.block.Block;
import tong.statmod.dungeon.BlockPalette;

public record DungeonMaterial(Block wall, Block floor, Block ceiling, Block support,
                               Block decoration, Block light, Block special) {
    public static DungeonMaterial fromPalette(BlockPalette t) {
        return new DungeonMaterial(t.base(), t.base(), t.ceiling(), t.decorPrimary(),
                t.accent(), t.light(), t.scar());
    }

    public Block resolve(String category) {
        return switch (category) {
            case "wall" -> wall;
            case "floor" -> floor;
            case "ceiling" -> ceiling;
            case "support" -> support;
            case "decoration" -> decoration;
            case "light" -> light;
            case "special" -> special;
            default -> wall;
        };
    }
}
