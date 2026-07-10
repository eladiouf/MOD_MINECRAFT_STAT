package tong.statmod.dungeon;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Palette de blocs par <b>arc thématique</b> (refonte 2026-07-10).
 *
 * <p>10 palettes, une par arc. Chaque palette fournit les 12 blocs de {@link BlockPalette}
 * pour que l'architecture de l'étage (forteresse « The Descent ») soit visuellement cohérente
 * avec son thème narratif — et pas juste son tier (cf. {@link FloorPalette}).
 *
 * <p>Plus de blocs Quark : tous les blocs sont vanilla, zéro dépendance.
 */
public enum ThemePalette implements BlockPalette {

    DECHARNES {
        @Override public Block base() { return Blocks.COBBLESTONE; }
        @Override public Block accent() { return Blocks.CRACKED_STONE_BRICKS; }
        @Override public Block light() { return Blocks.TORCH; }
        @Override public Block underside() { return Blocks.STONE; }
        @Override public Block decorPrimary() { return Blocks.MOSSY_COBBLESTONE; }
        @Override public Block decorSecondary() { return Blocks.GRAVEL; }
        @Override public Block wallBlock() { return Blocks.COBBLESTONE_WALL; }
        @Override public Block stair() { return Blocks.COBBLESTONE_STAIRS; }
        @Override public Block slab() { return Blocks.COBBLESTONE_SLAB; }
        @Override public Block ceiling() { return Blocks.OAK_PLANKS; }
        @Override public Block scar() { return Blocks.GRAVEL; }
        @Override public Block banner() { return Blocks.RED_WOOL; }
    },
    FAUVES {
        @Override public Block base() { return Blocks.MOSSY_COBBLESTONE; }
        @Override public Block accent() { return Blocks.STRIPPED_JUNGLE_LOG; }
        @Override public Block light() { return Blocks.TORCH; }
        @Override public Block underside() { return Blocks.STONE; }
        @Override public Block decorPrimary() { return Blocks.JUNGLE_LEAVES; }
        @Override public Block decorSecondary() { return Blocks.MOSS_BLOCK; }
        @Override public Block wallBlock() { return Blocks.MOSSY_COBBLESTONE_WALL; }
        @Override public Block stair() { return Blocks.MOSSY_COBBLESTONE_STAIRS; }
        @Override public Block slab() { return Blocks.MOSSY_COBBLESTONE_SLAB; }
        @Override public Block ceiling() { return Blocks.JUNGLE_PLANKS; }
        @Override public Block scar() { return Blocks.MOSS_BLOCK; }
        @Override public Block banner() { return Blocks.GREEN_WOOL; }
    },
    TRIBUS {
        @Override public Block base() { return Blocks.STONE_BRICKS; }
        @Override public Block accent() { return Blocks.DARK_OAK_PLANKS; }
        @Override public Block light() { return Blocks.LANTERN; }
        @Override public Block underside() { return Blocks.STONE; }
        @Override public Block decorPrimary() { return Blocks.CHISELED_STONE_BRICKS; }
        @Override public Block decorSecondary() { return Blocks.COBBLESTONE; }
        @Override public Block wallBlock() { return Blocks.STONE_BRICK_WALL; }
        @Override public Block stair() { return Blocks.STONE_BRICK_STAIRS; }
        @Override public Block slab() { return Blocks.STONE_BRICK_SLAB; }
        @Override public Block ceiling() { return Blocks.DARK_OAK_PLANKS; }
        @Override public Block scar() { return Blocks.COBBLESTONE; }
        @Override public Block banner() { return Blocks.GRAY_WOOL; }
    },
    LEGION {
        @Override public Block base() { return Blocks.DEEPSLATE_TILES; }
        @Override public Block accent() { return Blocks.POLISHED_DEEPSLATE; }
        @Override public Block light() { return Blocks.SOUL_LANTERN; }
        @Override public Block underside() { return Blocks.DEEPSLATE; }
        @Override public Block decorPrimary() { return Blocks.DEEPSLATE_BRICKS; }
        @Override public Block decorSecondary() { return Blocks.CRACKED_DEEPSLATE_TILES; }
        @Override public Block wallBlock() { return Blocks.DEEPSLATE_BRICK_WALL; }
        @Override public Block stair() { return Blocks.DEEPSLATE_TILE_STAIRS; }
        @Override public Block slab() { return Blocks.DEEPSLATE_TILE_SLAB; }
        @Override public Block ceiling() { return Blocks.POLISHED_DEEPSLATE; }
        @Override public Block scar() { return Blocks.COBBLED_DEEPSLATE; }
        @Override public Block banner() { return Blocks.BLUE_WOOL; }
    },
    ABYSSES {
        @Override public Block base() { return Blocks.PRISMARINE_BRICKS; }
        @Override public Block accent() { return Blocks.DARK_PRISMARINE; }
        @Override public Block light() { return Blocks.SEA_LANTERN; }
        @Override public Block underside() { return Blocks.PRISMARINE; }
        @Override public Block decorPrimary() { return Blocks.WET_SPONGE; }
        @Override public Block decorSecondary() { return Blocks.DRIED_KELP_BLOCK; }
        @Override public Block wallBlock() { return Blocks.PRISMARINE_WALL; }
        @Override public Block stair() { return Blocks.PRISMARINE_BRICK_STAIRS; }
        @Override public Block slab() { return Blocks.PRISMARINE_BRICK_SLAB; }
        @Override public Block ceiling() { return Blocks.PRISMARINE; }
        @Override public Block scar() { return Blocks.GRAVEL; }
        @Override public Block banner() { return Blocks.CYAN_WOOL; }
    },
    MAGES {
        @Override public Block base() { return Blocks.CALCITE; }
        @Override public Block accent() { return Blocks.AMETHYST_BLOCK; }
        @Override public Block light() { return Blocks.AMETHYST_CLUSTER; }
        @Override public Block underside() { return Blocks.STONE; }
        @Override public Block decorPrimary() { return Blocks.BOOKSHELF; }
        @Override public Block decorSecondary() { return Blocks.CHISELED_STONE_BRICKS; }
        @Override public Block wallBlock() { return Blocks.CALCITE; }
        @Override public Block stair() { return Blocks.STONE_BRICK_STAIRS; }
        @Override public Block slab() { return Blocks.STONE_BRICK_SLAB; }
        @Override public Block ceiling() { return Blocks.POLISHED_ANDESITE; }
        @Override public Block scar() { return Blocks.CRYING_OBSIDIAN; }
        @Override public Block banner() { return Blocks.MAGENTA_WOOL; }
    },
    MOISSON {
        @Override public Block base() { return Blocks.STRIPPED_DARK_OAK_LOG; }
        @Override public Block accent() { return Blocks.CARVED_PUMPKIN; }
        @Override public Block light() { return Blocks.JACK_O_LANTERN; }
        @Override public Block underside() { return Blocks.DARK_OAK_LOG; }
        @Override public Block decorPrimary() { return Blocks.HAY_BLOCK; }
        @Override public Block decorSecondary() { return Blocks.COARSE_DIRT; }
        @Override public Block wallBlock() { return Blocks.DARK_OAK_FENCE; }
        @Override public Block stair() { return Blocks.DARK_OAK_STAIRS; }
        @Override public Block slab() { return Blocks.DARK_OAK_SLAB; }
        @Override public Block ceiling() { return Blocks.SPRUCE_PLANKS; }
        @Override public Block scar() { return Blocks.PODZOL; }
        @Override public Block banner() { return Blocks.ORANGE_WOOL; }
    },
    FOURNAISE {
        @Override public Block base() { return Blocks.POLISHED_BLACKSTONE_BRICKS; }
        @Override public Block accent() { return Blocks.NETHER_BRICKS; }
        @Override public Block light() { return Blocks.SHROOMLIGHT; }
        @Override public Block underside() { return Blocks.BASALT; }
        @Override public Block decorPrimary() { return Blocks.MAGMA_BLOCK; }
        @Override public Block decorSecondary() { return Blocks.GILDED_BLACKSTONE; }
        @Override public Block wallBlock() { return Blocks.POLISHED_BLACKSTONE_BRICK_WALL; }
        @Override public Block stair() { return Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS; }
        @Override public Block slab() { return Blocks.POLISHED_BLACKSTONE_BRICK_SLAB; }
        @Override public Block ceiling() { return Blocks.CRIMSON_PLANKS; }
        @Override public Block scar() { return Blocks.CRIMSON_NYLIUM; }
        @Override public Block banner() { return Blocks.RED_WOOL; }
    },
    GESTE {
        @Override public Block base() { return Blocks.CRYING_OBSIDIAN; }
        @Override public Block accent() { return Blocks.SOUL_SAND; }
        @Override public Block light() { return Blocks.SOUL_TORCH; }
        @Override public Block underside() { return Blocks.END_STONE; }
        @Override public Block decorPrimary() { return Blocks.BONE_BLOCK; }
        @Override public Block decorSecondary() { return Blocks.SHROOMLIGHT; }
        @Override public Block wallBlock() { return Blocks.CRYING_OBSIDIAN; }
        @Override public Block stair() { return Blocks.STONE_BRICK_STAIRS; }
        @Override public Block slab() { return Blocks.STONE_BRICK_SLAB; }
        @Override public Block ceiling() { return Blocks.CRIMSON_PLANKS; }
        @Override public Block scar() { return Blocks.CRYING_OBSIDIAN; }
        @Override public Block banner() { return Blocks.PURPLE_WOOL; }
    },
    NEANT {
        @Override public Block base() { return Blocks.END_STONE; }
        @Override public Block accent() { return Blocks.PURPUR_BLOCK; }
        @Override public Block light() { return Blocks.END_ROD; }
        @Override public Block underside() { return Blocks.END_STONE; }
        @Override public Block decorPrimary() { return Blocks.AMETHYST_BLOCK; }
        @Override public Block decorSecondary() { return Blocks.CHORUS_FLOWER; }
        @Override public Block wallBlock() { return Blocks.END_STONE_BRICK_WALL; }
        @Override public Block stair() { return Blocks.END_STONE_BRICK_STAIRS; }
        @Override public Block slab() { return Blocks.END_STONE_BRICK_SLAB; }
        @Override public Block ceiling() { return Blocks.PURPUR_PILLAR; }
        @Override public Block scar() { return Blocks.CRYING_OBSIDIAN; }
        @Override public Block banner() { return Blocks.PURPLE_WOOL; }
    };

    /** Palette de l'étage, par arc (10 arcs, boucle au-delà de 100). */
    public static ThemePalette forFloor(int floor) {
        if (floor <= 0) return DECHARNES;
        return values()[(floor - 1) / 10 % 10];
    }
}
