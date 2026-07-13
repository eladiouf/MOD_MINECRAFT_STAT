package tong.statmod.dungeon.city;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import tong.statmod.dungeon.QuarkDungeonDecorator;

/** Soft-resolved furniture catalog for the floor-0 interiors. */
final class CityFurniture {
    private CityFurniture() {}

    static Block chair(String wood) { return block("mcwfurnitures:" + wood + "_chair", Blocks.OAK_STAIRS); }
    static Block table(String wood) { return block("mcwfurnitures:" + wood + "_table", Blocks.OAK_PLANKS); }
    static Block desk(String wood) { return block("mcwfurnitures:" + wood + "_desk", Blocks.LECTERN); }
    static Block wardrobe(String wood) { return block("mcwfurnitures:" + wood + "_wardrobe", Blocks.BARREL); }
    static Block bookshelf(String wood) { return block("mcwfurnitures:" + wood + "_bookshelf", Blocks.BOOKSHELF); }
    static Block counter(String wood) { return block("mcwfurnitures:" + wood + "_counter", Blocks.BARREL); }
    static Block drawer(String wood) { return block("mcwfurnitures:" + wood + "_double_drawer", Blocks.CHEST); }
    static Block sink(String wood) { return block("mcwfurnitures:" + wood + "_kitchen_sink", Blocks.CAULDRON); }
    static Block lamp(String color) { return block("mcwlights:" + color + "_lamp", Blocks.LANTERN); }
    static Block ceilingLight(String color) { return block("mcwlights:" + color + "_ceiling_light", Blocks.GLOWSTONE); }
    static Block chandelier() { return block("mcwlights:copper_chandelier", Blocks.LANTERN); }
    static Block streetLamp() { return block("mcwlights:classic_street_lamp", Blocks.LANTERN); }
    static Block window(String material) { return block("mcwwindows:" + material + "_window", Blocks.GLASS); }
    static Block door(String wood) { return block("mcwdoors:" + wood + "_cottage_door", Blocks.OAK_DOOR); }
    static Block roof(String material) { return block("mcwroofs:" + material + "_roof", Blocks.BRICKS); }

    private static Block block(String id, Block fallback) {
        Block resolved = QuarkDungeonDecorator.resolve(id);
        return resolved == null ? fallback : resolved;
    }
}
