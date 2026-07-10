package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static tong.statmod.dungeon.DungeonArchitect.AIR;
import static tong.statmod.dungeon.DungeonArchitect.B;
import static tong.statmod.dungeon.DungeonArchitect.O;
import static tong.statmod.dungeon.DungeonArchitect.S;

/**
 * Centralized Quark block resolver and decorator.
 *
 * <p>Provides soft-resolved Quark block accessors (returns vanilla fallback or {@code null} if
 * Quark is absent) and themed decoration methods that scatter Quark-exclusive blocks across
 * combat rooms, safehouses, chandeliers, mezzanines, and more.</p>
 *
 * <p>Inspired by IDAS's approach of enriching base structures with zone-specific props.</p>
 *
 * <p>All public methods are no-ops when Quark is not installed.</p>
 */
public final class QuarkDungeonDecorator {

    private QuarkDungeonDecorator() {}

    // ═══════════════ lazy block cache ═══════════════

    private static final Map<String, Block> CACHE = new ConcurrentHashMap<>();
    private static Boolean quarkPresent;

    /** Returns {@code true} if the Quark mod is loaded. */
    public static boolean isQuarkPresent() {
        if (quarkPresent == null) {
            quarkPresent = resolve("quark:limestone") != null;
        }
        return quarkPresent;
    }

    /** Resolves a Quark block by id, or {@code null} if absent. Results are cached. */
    public static Block resolve(String id) {
        return CACHE.computeIfAbsent(id, k -> {
            ResourceLocation loc = ResourceLocation.tryParse(k);
            if (loc != null && BuiltInRegistries.BLOCK.containsKey(loc)) {
                return BuiltInRegistries.BLOCK.get(loc);
            }
            return null;
        });
    }

    /** Resolves a Quark block, returning fallback if absent. */
    public static Block resolveOr(String id, Block fallback) {
        Block b = resolve(id);
        return b != null ? b : fallback;
    }

    // ═══════════════ named block accessors ═══════════════

    // --- Stone types ---
    public static Block limestone()            { return resolveOr("quark:limestone", Blocks.STONE); }
    public static Block limestoneBricks()       { return resolveOr("quark:limestone_bricks", Blocks.STONE_BRICKS); }
    public static Block limestonePillar()       { return resolveOr("quark:limestone_pillar", Blocks.STONE_BRICKS); }
    public static Block chiseledLimestone()     { return resolveOr("quark:chiseled_limestone_bricks", Blocks.CHISELED_STONE_BRICKS); }
    public static Block limestoneBricksStairs() { return resolveOr("quark:limestone_bricks_stairs", Blocks.STONE_BRICK_STAIRS); }
    public static Block limestoneBricksSlab()   { return resolveOr("quark:limestone_bricks_slab", Blocks.STONE_BRICK_SLAB); }
    public static Block limestoneBricksWall()   { return resolveOr("quark:limestone_bricks_wall", Blocks.COBBLESTONE_WALL); }

    public static Block jasper()               { return resolveOr("quark:jasper", Blocks.GRANITE); }
    public static Block jasperBricks()         { return resolveOr("quark:jasper_bricks", Blocks.GRANITE); }
    public static Block jasperPillar()         { return resolveOr("quark:jasper_pillar", Blocks.GRANITE); }
    public static Block chiseledJasper()       { return resolveOr("quark:chiseled_jasper_bricks", Blocks.POLISHED_GRANITE); }
    public static Block jasperBricksStairs()   { return resolveOr("quark:jasper_bricks_stairs", Blocks.GRANITE_STAIRS); }
    public static Block jasperBricksSlab()     { return resolveOr("quark:jasper_bricks_slab", Blocks.GRANITE_SLAB); }
    public static Block jasperBricksWall()     { return resolveOr("quark:jasper_bricks_wall", Blocks.COBBLESTONE_WALL); }

    public static Block myalite()              { return resolveOr("quark:myalite", Blocks.PURPUR_BLOCK); }
    public static Block myaliteBricks()        { return resolveOr("quark:myalite_bricks", Blocks.PURPUR_BLOCK); }
    public static Block myalitePillar()        { return resolveOr("quark:myalite_pillar", Blocks.PURPUR_PILLAR); }
    public static Block chiseledMyalite()      { return resolveOr("quark:chiseled_myalite_bricks", Blocks.PURPUR_BLOCK); }
    public static Block myaliteBricksStairs()  { return resolveOr("quark:myalite_bricks_stairs", Blocks.PURPUR_STAIRS); }
    public static Block myaliteBricksSlab()    { return resolveOr("quark:myalite_bricks_slab", Blocks.PURPUR_SLAB); }
    public static Block myaliteBricksWall()    { return resolveOr("quark:myalite_bricks_wall", Blocks.COBBLESTONE_WALL); }
    public static Block duskyMyalite()         { return resolveOr("quark:dusky_myalite", Blocks.OBSIDIAN); }
    public static Block myaliteCrystal()       { return resolveOr("quark:myalite_crystal", Blocks.AMETHYST_CLUSTER); }

    public static Block shale()                { return resolveOr("quark:shale", Blocks.DEEPSLATE); }
    public static Block chiseledShale()        { return resolveOr("quark:chiseled_shale_bricks", Blocks.DEEPSLATE_BRICKS); }

    public static Block permafrost()           { return resolveOr("quark:permafrost", Blocks.PACKED_ICE); }
    public static Block permafrostBricks()     { return resolveOr("quark:permafrost_bricks", Blocks.PACKED_ICE); }

    // --- Metal & industrial ---
    public static Block ironPlate()            { return resolveOr("quark:iron_plate", Blocks.IRON_BLOCK); }
    public static Block ironPillar()           { return resolveOr("quark:iron_pillar", Blocks.IRON_BARS); }
    public static Block grate()                { return resolveOr("quark:grate", Blocks.IRON_BARS); }

    // --- Corundum crystals (colored) ---
    public static Block orangeCorundum()       { return resolveOr("quark:orange_corundum_cluster", Blocks.TORCH); }
    public static Block blueCorundum()         { return resolveOr("quark:blue_corundum_cluster", Blocks.SOUL_LANTERN); }
    public static Block indigoCorundum()      { return resolveOr("quark:indigo_corundum_cluster", Blocks.AMETHYST_CLUSTER); }
    public static Block greenCorundum()        { return resolveOr("quark:green_corundum_cluster", Blocks.SEA_LANTERN); }
    public static Block blackCorundum()        { return resolveOr("quark:black_corundum_cluster", Blocks.OBSIDIAN); }

    // --- Lighting ---
    public static Block stoneLamp()            { return resolveOr("quark:stone_lamp", Blocks.SEA_LANTERN); }
    public static Block paperLantern()         { return resolveOr("quark:paper_lantern", Blocks.LANTERN); }
    public static Block paperLanternSakura()   { return resolveOr("quark:paper_lantern_sakura", Blocks.LANTERN); }

    // --- Wood & organic ---
    public static Block ancientPlanks()        { return resolveOr("quark:ancient_planks", Blocks.DARK_OAK_PLANKS); }
    public static Block ancientBookshelf()     { return resolveOr("quark:ancient_bookshelf", Blocks.BOOKSHELF); }
    public static Block ancientLadder()        { return resolveOr("quark:ancient_ladder", Blocks.LADDER); }
    public static Block ancientFence()         { return resolveOr("quark:ancient_fence", Blocks.DARK_OAK_FENCE); }
    public static Block ancientChest()         { return resolveOr("quark:ancient_chest", Blocks.CHEST); }
    public static Block hollowDarkOakLog()     { return resolveOr("quark:hollow_dark_oak_log", Blocks.DARK_OAK_LOG); }
    public static Block hollowSpruceLog()      { return resolveOr("quark:hollow_spruce_log", Blocks.SPRUCE_LOG); }
    public static Block rope()                 { return resolveOr("quark:rope", Blocks.CHAIN); }

    // --- Decorative panels ---
    public static Block paperWall()            { return resolveOr("quark:paper_wall", Blocks.WHITE_WOOL); }
    public static Block framedGlass()          { return resolveOr("quark:framed_glass", Blocks.GLASS); }
    public static Block smallChain()           { return resolveOr("quark:chain_small", Blocks.CHAIN); }

    // --- Botanical ---
    public static Block midoriBlock()          { return resolveOr("quark:midori_block", Blocks.MOSS_BLOCK); }
    public static Block midoriPillar()         { return resolveOr("quark:midori_pillar", Blocks.MOSS_BLOCK); }

    // --- Furniture ---
    public static Block brownStool()           { return resolveOr("quark:brown_stool", Blocks.OAK_STAIRS); }
    public static Block grayStool()            { return resolveOr("quark:gray_stool", Blocks.OAK_STAIRS); }

    // --- Soul sandstone (Quark extension) ---
    public static Block cutSoulSandstone()     { return resolveOr("quark:cut_soul_sandstone", Blocks.SOUL_SAND); }
    public static Block chiseledSoulSandstone(){ return resolveOr("quark:chiseled_soul_sandstone", Blocks.SOUL_SAND); }

    // ═══════════════ decoration methods ═══════════════

    /**
     * Scatters Quark industrial floor plates near doors/transitions.
     * Called after room shell is built, affects a 3-block strip near each door edge.
     */
    public static void ironPlateFloor(ServerLevel lv, BlockPos sp, DungeonLayout.Room r, int yOff) {
        if (!isQuarkPresent()) return;
        BlockState plate = B(ironPlate());
        BlockState grateState = B(grate());
        int cx = r.centerX(), cz = r.centerZ();

        // Place iron plates near room edges for an industrial sub-floor look
        int[][] spots = {
            {r.minX() + 2, r.minZ() + 2}, {r.maxX() - 2, r.minZ() + 2},
            {r.minX() + 2, r.maxZ() - 2}, {r.maxX() - 2, r.maxZ() - 2}
        };
        for (int[] s : spots) {
            S(lv, O(sp, s[0], yOff - 1, s[1]), plate);
            S(lv, O(sp, s[0] + 1, yOff - 1, s[1]), grateState);
        }
    }

    /**
     * Scatters corundum crystal clusters on walls and ceilings.
     * Used in ARCANE, DEEP_DARK, and QUARK floors for a geode-like effect.
     */
    public static void corundumClusters(ServerLevel lv, BlockPos sp, DungeonLayout.Room r, int ceilH, int floor) {
        if (!isQuarkPresent()) return;
        Block[] crystals = {orangeCorundum(), blueCorundum(), greenCorundum()};
        // Scatter on ceiling at pseudo-random positions
        int seed = floor * 31 + r.index() * 17;
        for (int i = 0; i < 6; i++) {
            int h = (seed + i * 37) & 0x7FFFFFFF;
            int dx = r.minX() + 3 + (h % Math.max(1, r.maxX() - r.minX() - 6));
            int dz = r.minZ() + 3 + ((h >> 8) % Math.max(1, r.maxZ() - r.minZ() - 6));
            Block crystal = crystals[i % crystals.length];
            S(lv, O(sp, dx, ceilH - 1, dz), B(crystal));
        }
    }

    /**
     * Hangs rope blocks from the ceiling at pseudo-random positions.
     * Used in HUNT and AWAKENING themes for a vine/rope feel.
     */
    public static void hangingRopes(ServerLevel lv, BlockPos sp, DungeonLayout.Room r, int ceilH, int floor) {
        if (!isQuarkPresent()) return;
        BlockState ropeBlock = B(rope());
        int seed = floor * 23 + r.index() * 41;
        for (int i = 0; i < 4; i++) {
            int h = (seed + i * 53) & 0x7FFFFFFF;
            int dx = r.minX() + 4 + (h % Math.max(1, r.maxX() - r.minX() - 8));
            int dz = r.minZ() + 4 + ((h >> 8) % Math.max(1, r.maxZ() - r.minZ() - 8));
            int ropeLen = 2 + (h >> 16) % 3;
            for (int y = 0; y < ropeLen && ceilH - 1 - y > 2; y++) {
                S(lv, O(sp, dx, ceilH - 1 - y, dz), ropeBlock);
            }
        }
    }

    /**
     * Places hollow logs as decorative pipe segments in wall niches.
     * Used in HUNT and HARVEST themes.
     */
    public static void hollowLogPipes(ServerLevel lv, BlockPos sp, DungeonLayout.Room r, int floor) {
        if (!isQuarkPresent()) return;
        Block log = (floor % 2 == 0) ? hollowDarkOakLog() : hollowSpruceLog();
        BlockState logState = B(log);
        // Place a few horizontal log segments against the north wall
        int cx = r.centerX();
        int z = r.minZ() + 1;
        for (int dx = -2; dx <= 2; dx++) {
            S(lv, O(sp, cx + dx, 3, z), logState);
        }
    }

    /**
     * Decorates the boss arena for Quark-themed floors with a corundum crystal ring.
     * Alternating colored crystal pillars in a circle around the anchor.
     */
    public static void crystalBossArena(ServerLevel lv, BlockPos anchor, int floor) {
        if (!isQuarkPresent()) return;
        Block[] crystals = {orangeCorundum(), blueCorundum(), greenCorundum(), resolve("quark:indigo_corundum_cluster")};
        Block[] bases = {jasperBricks(), myaliteBricks(), limestoneBricks(), jasperBricks()};

        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            int dx = (int) Math.round(Math.cos(a) * 10);
            int dz = (int) Math.round(Math.sin(a) * 8);
            Block base = bases[i % bases.length];
            Block crystal = crystals[i % crystals.length];
            if (crystal == null) crystal = orangeCorundum(); // fallback

            // Crystal pillar: 3 blocks of stone + crystal on top
            for (int y = 0; y <= 2; y++) {
                S(lv, O(anchor, dx, y, dz), B(base));
            }
            S(lv, O(anchor, dx, 3, dz), B(crystal));
        }

        // Mosaic floor pattern around the center
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (dx * dx + dz * dz > 25) continue;
                if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) continue; // keep altar area
                boolean alt = (dx + dz) % 2 == 0;
                S(lv, O(anchor, dx, -1, dz), B(alt ? jasperBricks() : myaliteBricks()));
            }
        }
    }

    /**
     * Enhances the safehouse room with Quark furniture and decorations.
     * Called after the base safehouse is built.
     *
     * @param sp room-local spawn point
     * @param cx center X of the room
     * @param cz center Z of the room
     */
    public static void enhanceSafehouse(ServerLevel lv, BlockPos sp, int cx, int cz) {
        if (!isQuarkPresent()) return;

        // Replace vanilla bookshelves with ancient bookshelves
        S(lv, O(sp, cx - 3, 0, cz - 3), B(ancientBookshelf()));
        S(lv, O(sp, cx - 2, 0, cz - 3), B(ancientBookshelf()));

        // Paper lantern sakura accent lighting
        S(lv, O(sp, cx + 3, 1, cz - 3), B(paperLanternSakura()));

        // Framed glass "windows" on opposite walls
        S(lv, O(sp, cx - 4, 2, cz), B(framedGlass()));
        S(lv, O(sp, cx + 4, 2, cz), B(framedGlass()));
        S(lv, O(sp, cx - 4, 3, cz), B(framedGlass()));
        S(lv, O(sp, cx + 4, 3, cz), B(framedGlass()));

        // Replace oak stair benches with stools
        Block stool = brownStool();
        S(lv, O(sp, cx, 0, cz - 2), B(stool));
        S(lv, O(sp, cx, 0, cz + 2), B(stool));
        S(lv, O(sp, cx - 2, 0, cz), B(stool));
        S(lv, O(sp, cx + 2, 0, cz), B(stool));
    }

    /**
     * Uses small chains instead of vanilla chains in chandeliers, and paper lanterns
     * as the light source on Quark-themed floors.
     *
     * @return the chain block to use (small chain if Quark present, vanilla chain otherwise)
     */
    public static Block chandelierChain() {
        if (!isQuarkPresent()) return Blocks.CHAIN;
        return smallChain();
    }

    /**
     * Enhances mezzanine railings with ancient fence blocks and replaces ladders
     * with ancient ladders.
     */
    public static Block mezzanineRailing() {
        if (!isQuarkPresent()) return null; // caller uses default
        return ancientFence();
    }

    public static Block mezzanineLadder() {
        if (!isQuarkPresent()) return Blocks.LADDER;
        return ancientLadder();
    }

    /**
     * Scatters permafrost blocks into the FROZEN theme for visual variety.
     * Replaces some packed ice blocks with permafrost bricks.
     */
    public static void permafrostAccents(ServerLevel lv, BlockPos sp, DungeonLayout.Room r, int floor) {
        if (!isQuarkPresent()) return;
        BlockState pf = B(permafrostBricks());
        int seed = floor * 19 + r.index() * 47;
        for (int i = 0; i < 8; i++) {
            int h = (seed + i * 61) & 0x7FFFFFFF;
            int dx = r.minX() + 2 + (h % Math.max(1, r.maxX() - r.minX() - 4));
            int dz = r.minZ() + 2 + ((h >> 8) % Math.max(1, r.maxZ() - r.minZ() - 4));
            S(lv, O(sp, dx, -1, dz), pf); // replace some floor tiles
        }
    }

    /**
     * Adds midori (prismarine-like) accents to HUNT and SUNKEN themes.
     */
    public static void midoriAccents(ServerLevel lv, BlockPos sp, DungeonLayout.Room r, int ceilH) {
        if (!isQuarkPresent()) return;
        int cx = r.centerX(), cz = r.centerZ();
        // Midori pillar flanking the room center
        S(lv, O(sp, cx - 5, 0, cz), B(midoriPillar()));
        S(lv, O(sp, cx - 5, 1, cz), B(midoriPillar()));
        S(lv, O(sp, cx + 5, 0, cz), B(midoriPillar()));
        S(lv, O(sp, cx + 5, 1, cz), B(midoriPillar()));
    }

    /**
     * Adds iron pillar decorations to WARBAND and INFERNAL themes.
     */
    public static void ironPillarAccents(ServerLevel lv, BlockPos sp, DungeonLayout.Room r, int ceilH) {
        // Disabled as freestanding pillars blocks player pathing
    }

    /**
     * Adds soul sandstone accents to RESTLESS_DEAD and UNDEAD themes.
     */
    public static void soulSandstoneAccents(ServerLevel lv, BlockPos sp, DungeonLayout.Room r) {
        if (!isQuarkPresent()) return;
        BlockState cut = B(cutSoulSandstone());
        BlockState chiseled = B(chiseledSoulSandstone());
        int cx = r.centerX(), cz = r.centerZ();
        // Accent blocks near room corners
        S(lv, O(sp, r.minX() + 3, 0, r.minZ() + 3), chiseled);
        S(lv, O(sp, r.maxX() - 3, 0, r.minZ() + 3), cut);
        S(lv, O(sp, r.minX() + 3, 0, r.maxZ() - 3), cut);
        S(lv, O(sp, r.maxX() - 3, 0, r.maxZ() - 3), chiseled);
    }

    /**
     * Master method: called from DungeonRoomChain.decorateCombatRoom() to apply
     * theme-appropriate Quark decorations to a combat room.
     */
    public static void decorateRoom(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r,
                                    int floor, int ceilH) {
        if (!isQuarkPresent()) return;

        ThemePalette theme = (t instanceof ThemePalette) ? (ThemePalette) t : ThemePalette.forFloor(floor);

        switch (theme) {
            case TRIBUS -> {
                ironPlateFloor(lv, sp, r, 0);
            }
            case LEGION -> {
                soulSandstoneAccents(lv, sp, r);
            }
            case FAUVES -> {
                hollowLogPipes(lv, sp, r, floor);
                hangingRopes(lv, sp, r, ceilH, floor);
                midoriAccents(lv, sp, r, ceilH);
            }
            case ABYSSES -> {
                midoriAccents(lv, sp, r, ceilH);
            }
            case MAGES, NEANT -> {
                corundumClusters(lv, sp, r, ceilH, floor);
            }
            case FOURNAISE -> {
                ironPlateFloor(lv, sp, r, 0);
            }
            default -> {}
        }
    }
}
