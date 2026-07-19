package tong.statmod.dungeon.city;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import tong.statmod.dungeon.QuarkDungeonDecorator;

/** Soft-resolved modded materials for floor 0; every entry has a vanilla fallback. */
final class CityMaterialPalette {
    private CityMaterialPalette() {}

    static final Style HUMAN = new Style(
            mod("quark:limestone_bricks", Blocks.STONE_BRICKS),
            mod("quark:limestone_pillar", Blocks.POLISHED_ANDESITE),
            Blocks.SPRUCE_PLANKS,
            mod("quark:framed_glass", Blocks.GLASS),
            mod("quark:stone_lamp", Blocks.LANTERN),
            Blocks.BLUE_WOOL);

    static final Style ELF = new Style(
            mod("quark:midori_block", Blocks.MOSS_BLOCK),
            mod("quark:midori_pillar", Blocks.STRIPPED_BIRCH_LOG),
            mod("quark:ancient_planks", Blocks.BIRCH_PLANKS),
            mod("quark:framed_glass", Blocks.LIME_STAINED_GLASS),
            mod("quark:paper_lantern_sakura", Blocks.LANTERN),
            Blocks.GREEN_WOOL);

    static final Style DWARF = new Style(
            mod("quark:jasper_bricks", Blocks.DEEPSLATE_BRICKS),
            mod("quark:jasper_pillar", Blocks.POLISHED_BLACKSTONE_BRICKS),
            mod("quark:iron_plate", Blocks.COPPER_BLOCK),
            mod("quark:grate", Blocks.IRON_BARS),
            mod("quark:orange_corundum_cluster", Blocks.LANTERN),
            Blocks.ORANGE_WOOL);

    static final Style BEAST = new Style(
            mod("quark:shale", Blocks.MUD_BRICKS),
            mod("quark:hollow_dark_oak_log", Blocks.DARK_OAK_LOG),
            Blocks.DARK_OAK_PLANKS,
            mod("quark:framed_glass", Blocks.BROWN_STAINED_GLASS),
            mod("quark:paper_lantern", Blocks.SOUL_LANTERN),
            Blocks.RED_WOOL);

    static Block rope() { return mod("quark:rope", Blocks.CHAIN); }
    static Block ancientBookshelf() { return mod("quark:ancient_bookshelf", Blocks.BOOKSHELF); }
    static Block stool() { return mod("quark:brown_stool", Blocks.OAK_STAIRS); }
    static Block basket() { return Blocks.BARREL; }
    static Block cookingPot() { return Blocks.CAULDRON; }
    static Block cuttingBoard() { return Blocks.OAK_PRESSURE_PLATE; }
    static Block bountyBoard() { return mod("bountiful:bountyboard", Blocks.LECTERN); }

    private static Block mod(String id, Block fallback) {
        Block resolved = QuarkDungeonDecorator.resolve(id);
        return resolved == null ? fallback : resolved;
    }

    record Style(Block wall, Block pillar, Block roof, Block window, Block light, Block banner) {}
}
