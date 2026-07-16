package tong.statmod.dungeon;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Mission M6 — Phase ε.
 *
 * <p>Tier de palette de blocs pour les îles du Trial Dungeon, changeant par palier de progression :
 * <ul>
 *   <li>{@link #EARLY} (étages 1-10) — cobblestone + oak, torches</li>
 *   <li>{@link #MID} (étages 11-25) — deepslate + dark oak, soul lanterns</li>
 *   <li>{@link #LATE} (étages 26-50) — blackstone + crimson, shroomlights</li>
 *   <li>{@link #ABYSS} (étages 51-100) — obsidian + purpur, end rods</li>
 * </ul>
 *
 * <p>Le mapping Tier → {@link Block} est résolu paresseusement pour permettre les tests unit
 * sans initialiser {@code Blocks} (qui déclenche toute la chaîne SoundEvents / Bootstrap).
 */
public enum FloorPalette implements BlockPalette {

    EARLY,
    MID,
    LATE,
    ABYSS;

    public static FloorPalette forFloor(int floor) {
        if (floor <= 10) return EARLY;
        if (floor <= 25) return MID;
        if (floor <= 50) return LATE;
        return ABYSS;
    }

    /** Résolu lazy pour ne pas trigger Blocks init en env test. */
    public Block base() {
        return switch (this) {
            case EARLY -> Blocks.COBBLESTONE;
            case MID -> Blocks.DEEPSLATE_TILES;
            case LATE -> Blocks.BLACKSTONE;
            case ABYSS -> Blocks.OBSIDIAN;
        };
    }

    public Block accent() {
        return switch (this) {
            case EARLY -> Blocks.OAK_PLANKS;
            case MID -> Blocks.DARK_OAK_PLANKS;
            case LATE -> Blocks.CRIMSON_PLANKS;
            case ABYSS -> Blocks.PURPUR_BLOCK;
        };
    }

    public Block light() {
        return switch (this) {
            case EARLY -> Blocks.TORCH;
            case MID -> Blocks.SOUL_LANTERN;
            case LATE -> Blocks.SHROOMLIGHT;
            case ABYSS -> Blocks.END_ROD;
        };
    }

    /** Pierre du cône underside — contraste avec la surface (redesign 2026-07-03). */
    public Block underside() {
        return switch (this) {
            case EARLY -> Blocks.STONE;
            case MID -> Blocks.DEEPSLATE;
            case LATE -> Blocks.BASALT;
            case ABYSS -> Blocks.END_STONE;
        };
    }

    /** Swap in-platform dispersé (~8 % des cellules de surface). */
    public Block decorPrimary() {
        return switch (this) {
            case EARLY -> Blocks.MOSSY_COBBLESTONE;
            case MID -> Blocks.DEEPSLATE_BRICKS;
            case LATE -> Blocks.MAGMA_BLOCK;
            case ABYSS -> Blocks.AMETHYST_BLOCK;
        };
    }

    /** Élément posé sur la plateforme (~1/25 cellules, hauteur 1-2). */
    public Block decorSecondary() {
        return switch (this) {
            case EARLY -> Blocks.OAK_LEAVES;
            case MID -> Blocks.COBBLED_DEEPSLATE;
            case LATE -> Blocks.POLISHED_BLACKSTONE_BRICKS;
            case ABYSS -> Blocks.CRYING_OBSIDIAN;
        };
    }

    /** Mur architectural (fence wall) pour les bâtiments. */
    public Block wallBlock() {
        return switch (this) {
            case EARLY -> Blocks.COBBLESTONE_WALL;
            case MID -> Blocks.DEEPSLATE_TILE_WALL;
            case LATE -> Blocks.BLACKSTONE_WALL;
            case ABYSS -> Blocks.END_STONE_BRICK_WALL;
        };
    }

    /** Escalier pour marches et toits. */
    public Block stair() {
        return switch (this) {
            case EARLY -> Blocks.COBBLESTONE_STAIRS;
            case MID -> Blocks.DEEPSLATE_TILE_STAIRS;
            case LATE -> Blocks.BLACKSTONE_STAIRS;
            case ABYSS -> Blocks.PURPUR_STAIRS;
        };
    }

    /** Dalle pour gradins et détails. */
    public Block slab() {
        return switch (this) {
            case EARLY -> Blocks.COBBLESTONE_SLAB;
            case MID -> Blocks.DEEPSLATE_TILE_SLAB;
            case LATE -> Blocks.BLACKSTONE_SLAB;
            case ABYSS -> Blocks.PURPUR_SLAB;
        };
    }

    /** Plafond des pièces fermées. */
    public Block ceiling() {
        return switch (this) {
            case EARLY -> Blocks.OAK_PLANKS;
            case MID -> Blocks.DARK_OAK_PLANKS;
            case LATE -> Blocks.POLISHED_BLACKSTONE;
            case ABYSS -> Blocks.OBSIDIAN;
        };
    }

    /** Cicatrice au sol (dressing narratif). */
    public Block scar() {
        return switch (this) {
            case EARLY -> Blocks.GRAVEL;
            case MID -> Blocks.COBBLED_DEEPSLATE;
            case LATE -> Blocks.MAGMA_BLOCK;
            case ABYSS -> Blocks.CRYING_OBSIDIAN;
        };
    }

    /** Bannière déchirée (dressing narratif). */
    public Block banner() {
        return switch (this) {
            case EARLY -> Blocks.RED_WOOL;
            case MID -> Blocks.BLUE_WOOL;
            case LATE -> Blocks.NETHER_WART_BLOCK;
            case ABYSS -> Blocks.PURPLE_WOOL;
        };
    }
}
