package tong.statmod.dungeon;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Mission M6 — Palette de blocs par thème/arc (2026-07-05).
 *
 * <p>Donne à chaque arc de 10 étages une <b>identité matérielle</b> propre, pour que
 * l'architecture <i>ressemble</i> à son thème (glace pour Frozen Reach, os/blackstone pour les
 * morts-vivants, prismarine pour Sunken Halls…) et pas seulement à son tier.
 *
 * <p>Résolue par {@link #forFloor(int)} : 10 palettes fixes indexées par arc
 * ({@code (floor-1)/10 % 10}). Au-delà de 100 étages, ça boucle (comme les thèmes).
 *
 * <p>Chaque bloc est résolu paresseusement (méthode) pour ne pas déclencher l'init de {@code Blocks}
 * en environnement de test — même contrainte que {@link FloorPalette}.
 */
public enum ThemePalette implements BlockPalette {

    /** Arc 1 — The Awakening : pierre & bois, donjon classique. */
    AWAKENING(Blocks.STONE_BRICKS, Blocks.OAK_PLANKS, Blocks.TORCH, Blocks.STONE,
            Blocks.MOSSY_STONE_BRICKS, Blocks.COBBLESTONE, Blocks.COBBLESTONE_WALL,
            Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICK_SLAB, Blocks.OAK_PLANKS,
            Blocks.GRAVEL, Blocks.WHITE_WOOL),

    /** Arc 2 — Restless Dead : os, mousse, pierre fissurée. */
    RESTLESS_DEAD(Blocks.BONE_BLOCK, Blocks.CRACKED_STONE_BRICKS, Blocks.SOUL_LANTERN, Blocks.DEEPSLATE,
            Blocks.MOSSY_COBBLESTONE, Blocks.COBWEB, Blocks.MOSSY_COBBLESTONE_WALL,
            Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICK_SLAB, Blocks.BONE_BLOCK,
            Blocks.COARSE_DIRT, Blocks.GRAY_WOOL),

    /** Arc 3 — Warband : forteresse militaire, briques & fer. */
    WARBAND(Blocks.STONE_BRICKS, Blocks.DARK_OAK_PLANKS, Blocks.LANTERN, Blocks.STONE,
            Blocks.POLISHED_ANDESITE, Blocks.IRON_BARS, Blocks.STONE_BRICK_WALL,
            Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICK_SLAB, Blocks.DARK_OAK_PLANKS,
            Blocks.GRAVEL, Blocks.RED_WOOL),

    /** Arc 4 — The Hunt : jungle/os, repaire de bêtes. */
    HUNT(Blocks.MOSSY_COBBLESTONE, Blocks.STRIPPED_JUNGLE_LOG, Blocks.TORCH, Blocks.STONE,
            Blocks.MOSS_BLOCK, Blocks.ROOTED_DIRT, Blocks.MOSSY_COBBLESTONE_WALL,
            Blocks.MOSSY_COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE_SLAB, Blocks.JUNGLE_PLANKS,
            Blocks.COARSE_DIRT, Blocks.GREEN_WOOL),

    /** Arc 5 — Sunken Halls : prismarine & mer. */
    SUNKEN(Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE, Blocks.SEA_LANTERN, Blocks.PRISMARINE,
            Blocks.PRISMARINE, Blocks.SEA_LANTERN, Blocks.COBBLESTONE_WALL,
            Blocks.PRISMARINE_BRICK_STAIRS, Blocks.PRISMARINE_BRICK_SLAB, Blocks.DARK_PRISMARINE,
            Blocks.GRAVEL, Blocks.CYAN_WOOL),

    /** Arc 6 — Frozen Reach : glace, neige, packed ice. */
    FROZEN(Blocks.PACKED_ICE, Blocks.SNOW_BLOCK, Blocks.SEA_LANTERN, Blocks.BLUE_ICE,
            Blocks.BLUE_ICE, Blocks.ICE, Blocks.COBBLESTONE_WALL,
            Blocks.DIORITE_STAIRS, Blocks.DIORITE_SLAB, Blocks.SNOW_BLOCK,
            Blocks.POWDER_SNOW, Blocks.LIGHT_BLUE_WOOL),

    /** Arc 7 — Hollow Harvest : citrouille, halloween, bois sombre. */
    HARVEST(Blocks.CARVED_PUMPKIN, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.JACK_O_LANTERN, Blocks.DEEPSLATE,
            Blocks.PUMPKIN, Blocks.HAY_BLOCK, Blocks.DARK_OAK_FENCE,
            Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_PLANKS,
            Blocks.COARSE_DIRT, Blocks.ORANGE_WOOL),

    /** Arc 8 — Arcane Storm : améthyste, calcite, magie. */
    ARCANE(Blocks.CALCITE, Blocks.AMETHYST_BLOCK, Blocks.AMETHYST_CLUSTER, Blocks.SMOOTH_BASALT,
            Blocks.BUDDING_AMETHYST, Blocks.CRYING_OBSIDIAN, Blocks.COBBLESTONE_WALL,
            Blocks.POLISHED_DEEPSLATE_STAIRS, Blocks.POLISHED_DEEPSLATE_SLAB, Blocks.PURPUR_BLOCK,
            Blocks.GRAVEL, Blocks.PURPLE_WOOL),

    /** Arc 9 — Infernal Legion : nether, blackstone, feu. */
    INFERNAL(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.NETHER_BRICKS, Blocks.SHROOMLIGHT, Blocks.BASALT,
            Blocks.MAGMA_BLOCK, Blocks.GILDED_BLACKSTONE, Blocks.BLACKSTONE_WALL,
            Blocks.BLACKSTONE_STAIRS, Blocks.BLACKSTONE_SLAB, Blocks.RED_NETHER_BRICKS,
            Blocks.MAGMA_BLOCK, Blocks.RED_WOOL),

    /** Arc 10 — The Abyss : obsidienne, end, void. */
    ABYSS(Blocks.OBSIDIAN, Blocks.END_STONE_BRICKS, Blocks.END_ROD, Blocks.END_STONE,
            Blocks.CRYING_OBSIDIAN, Blocks.PURPUR_PILLAR, Blocks.END_STONE_BRICK_WALL,
            Blocks.PURPUR_STAIRS, Blocks.PURPUR_SLAB, Blocks.OBSIDIAN,
            Blocks.CRYING_OBSIDIAN, Blocks.BLACK_WOOL),

    /** Arc 11 — Deep Dark : sculk, deepslate et silence. */
    DEEP_DARK(Blocks.SCULK, Blocks.POLISHED_DEEPSLATE, Blocks.SCULK_CATALYST, Blocks.DEEPSLATE,
            Blocks.SCULK_VEIN, Blocks.SCULK_SENSOR, Blocks.POLISHED_DEEPSLATE_WALL,
            Blocks.POLISHED_DEEPSLATE_STAIRS, Blocks.POLISHED_DEEPSLATE_SLAB, Blocks.SCULK,
            Blocks.SCULK_SHRIEKER, Blocks.BLACK_WOOL),

    /** Arc 12 — Quark Limestone Catacombs : ancient, Roman-like crypt with stone lamps. */
    QUARK_LIMESTONE(Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS, Blocks.SEA_LANTERN, Blocks.STONE,
            Blocks.COBBLESTONE, Blocks.STONE_BRICKS, Blocks.COBBLESTONE_WALL,
            Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICK_SLAB, Blocks.STONE_BRICKS,
            Blocks.GRAVEL, Blocks.WHITE_WOOL) {
        @Override public Block base()          { return resolve("quark:limestone_bricks", super.base()); }
        @Override public Block accent()        { return resolve("quark:chiseled_limestone_bricks", super.accent()); }
        @Override public Block light()         { return resolve("quark:stone_lamp", super.light()); }
        @Override public Block underside()     { return resolve("quark:limestone", super.underside()); }
        @Override public Block decorPrimary()  { return resolve("quark:limestone_pillar", super.decorPrimary()); }
        @Override public Block decorSecondary(){ return resolve("quark:shale", super.decorSecondary()); }
        @Override public Block wallBlock()     { return resolve("quark:limestone_bricks_wall", super.wallBlock()); }
        @Override public Block stair()         { return resolve("quark:limestone_bricks_stairs", super.stair()); }
        @Override public Block slab()          { return resolve("quark:limestone_bricks_slab", super.slab()); }
        @Override public Block ceiling()       { return resolve("quark:limestone_bricks", super.ceiling()); }
        @Override public Block scar()          { return resolve("quark:chiseled_shale_bricks", super.scar()); }
    },

    /** Arc 13 — Quark Jasper Forge : red-hot industrial mine with iron and paper lanterns. */
    QUARK_JASPER(Blocks.GRANITE, Blocks.POLISHED_GRANITE, Blocks.LANTERN, Blocks.STONE,
            Blocks.POLISHED_ANDESITE, Blocks.IRON_BARS, Blocks.COBBLESTONE_WALL,
            Blocks.GRANITE_STAIRS, Blocks.GRANITE_SLAB, Blocks.GRANITE,
            Blocks.GRAVEL, Blocks.RED_WOOL) {
        @Override public Block base()          { return resolve("quark:jasper_bricks", super.base()); }
        @Override public Block accent()        { return resolve("quark:chiseled_jasper_bricks", super.accent()); }
        @Override public Block light()         { return resolve("quark:paper_lantern", super.light()); }
        @Override public Block underside()     { return resolve("quark:jasper", super.underside()); }
        @Override public Block decorPrimary()  { return resolve("quark:jasper_pillar", super.decorPrimary()); }
        @Override public Block decorSecondary(){ return resolve("quark:iron_plate", super.decorSecondary()); }
        @Override public Block wallBlock()     { return resolve("quark:jasper_bricks_wall", super.wallBlock()); }
        @Override public Block stair()         { return resolve("quark:jasper_bricks_stairs", super.stair()); }
        @Override public Block slab()          { return resolve("quark:jasper_bricks_slab", super.slab()); }
        @Override public Block ceiling()       { return resolve("quark:jasper_bricks", super.ceiling()); }
        @Override public Block scar()          { return resolve("quark:orange_corundum_cluster", super.scar()); }
    },

    /** Arc 14 — Quark Myalite Abyss : alien end-like crystal cave with blue corundum. */
    QUARK_MYALITE(Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.AMETHYST_CLUSTER, Blocks.END_STONE,
            Blocks.CRYING_OBSIDIAN, Blocks.OBSIDIAN, Blocks.COBBLESTONE_WALL,
            Blocks.PURPUR_STAIRS, Blocks.PURPUR_SLAB, Blocks.PURPUR_BLOCK,
            Blocks.CRYING_OBSIDIAN, Blocks.PURPLE_WOOL) {
        @Override public Block base()          { return resolve("quark:myalite_bricks", super.base()); }
        @Override public Block accent()        { return resolve("quark:chiseled_myalite_bricks", super.accent()); }
        @Override public Block light()         { return resolve("quark:blue_corundum_cluster", super.light()); }
        @Override public Block underside()     { return resolve("quark:dusky_myalite", super.underside()); }
        @Override public Block decorPrimary()  { return resolve("quark:myalite_pillar", super.decorPrimary()); }
        @Override public Block decorSecondary(){ return resolve("quark:myalite_crystal", super.decorSecondary()); }
        @Override public Block wallBlock()     { return resolve("quark:myalite_bricks_wall", super.wallBlock()); }
        @Override public Block stair()         { return resolve("quark:myalite_bricks_stairs", super.stair()); }
        @Override public Block slab()          { return resolve("quark:myalite_bricks_slab", super.slab()); }
        @Override public Block ceiling()       { return resolve("quark:myalite_bricks", super.ceiling()); }
        @Override public Block scar()          { return resolve("quark:indigo_corundum_cluster", super.scar()); }
    };

    private final Block base, accent, light, underside, decorPrimary, decorSecondary,
            wallBlock, stair, slab, ceiling, scar, banner;

    ThemePalette(Block base, Block accent, Block light, Block underside, Block decorPrimary,
                 Block decorSecondary, Block wallBlock, Block stair, Block slab, Block ceiling,
                 Block scar, Block banner) {
        this.base = base; this.accent = accent; this.light = light; this.underside = underside;
        this.decorPrimary = decorPrimary; this.decorSecondary = decorSecondary;
        this.wallBlock = wallBlock; this.stair = stair; this.slab = slab; this.ceiling = ceiling;
        this.scar = scar; this.banner = banner;
    }

    /** Palette du thème/arc de l'étage. Boucle au-delà de 100 (comme {@link DungeonThemes}). */
    public static ThemePalette forFloor(int floor) {
        if (floor < 1) floor = 1;
        int arc = ((floor - 1) / 10) % values().length;
        return values()[arc];
    }

    @Override public Block base() { return base; }
    @Override public Block accent() { return accent; }
    @Override public Block light() { return light; }
    @Override public Block underside() { return underside; }
    @Override public Block decorPrimary() { return decorPrimary; }
    @Override public Block decorSecondary() { return decorSecondary; }
    @Override public Block wallBlock() { return wallBlock; }
    @Override public Block stair() { return stair; }
    @Override public Block slab() { return slab; }
    @Override public Block ceiling() { return ceiling; }
    @Override public Block scar() { return scar; }
    @Override public Block banner() { return banner; }

    private static Block resolve(String id, Block fallback) {
        net.minecraft.resources.ResourceLocation loc = net.minecraft.resources.ResourceLocation.tryParse(id);
        if (loc != null && net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(loc)) {
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(loc);
        }
        return fallback;
    }
}
