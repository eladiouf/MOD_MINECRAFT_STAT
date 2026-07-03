package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;
import tong.statmod.STATMod;

import java.util.ArrayList;
import java.util.List;

/**
 * Mission M6 — Island Redesign v2 (2026-07-03).
 *
 * <p>Génération de véritables structures architecturales sur les îles du Trial Dungeon.
 * Plus de simples plateformes : chaque île est un bâtiment à part entière.
 *
 * <p>Trois templates massifs :
 * <ul>
 *   <li><b>Combat — Colosseum</b> : arène circulaire 120×120 avec murs crénelés, 8 piliers,
 *       4 portes, arène centrale abaissée. Mobs spawnent dans l'arène.</li>
 *   <li><b>Trésor — Dungeon</b> : donjon 140×140 avec couloir d'entrée, 4 salles
 *       connectées par des portes, salle du trésor centrale avec coffre sur piédestal.</li>
 *   <li><b>Boss — Temple</b> : temple 180×180 avec murs massifs, 4 tours d'angle,
 *       nef centrale à piliers, autel sur plateforme surélevée, fossé de lave (LATE+).</li>
 * </ul>
 */
public final class IslandGenerator {

    private static final int COMBAT_RADIUS = 60;
    private static final int TREASURE_RADIUS = 70;
    private static final int BOSS_RADIUS = 90;

    private IslandGenerator() {}

    public static boolean generateFloor(ServerLevel dungeon, int floor) {
        BlockPos spawn = DungeonTeleportHandler.floorSpawnPos(floor);
        if (!dungeon.getBlockState(spawn.below()).isAir()) {
            return false;
        }
        FloorPalette palette = FloorPalette.forFloor(floor);
        if (floor > 0 && floor % 10 == 0) {
            generateBossTemple(dungeon, spawn, palette);
        } else if (floor > 0 && floor % 5 == 0) {
            generateTreasureDungeon(dungeon, spawn, palette);
        } else {
            generateCombatColosseum(dungeon, spawn, palette, floor);
        }
        STATMod.LOGGER.info("[TrialDungeon] Généré étage {} ({})", floor, palette.name());
        return true;
    }

    // ═════════════════════ COMBAT — COLOSSEUM (120×120) ═════════════════════

    private static void generateCombatColosseum(ServerLevel level, BlockPos spawn,
                                                 FloorPalette palette, int floor) {
        int r = COMBAT_RADIUS;
        BlockState base = palette.base().defaultBlockState();
        BlockState accent = palette.accent().defaultBlockState();
        BlockState wall = palette.wallBlock().defaultBlockState();
        BlockState stair = palette.stair().defaultBlockState();
        BlockState light = palette.light().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // Platform
        placeCircle(level, spawn.below(), r, base, accent, palette);

        // Outer wall 3-5 blocks high with crenellations
        placeRingWall(level, spawn, r, base, 3);

        // 4 entrance archways
        placeArchway(level, spawn.offset(0, 0, r - 1), 3, 3, base, true);
        placeArchway(level, spawn.offset(0, 0, -(r - 1)), 3, 3, base, true);
        placeArchway(level, spawn.offset(r - 1, 0, 0), 3, 3, base, false);
        placeArchway(level, spawn.offset(-(r - 1), 0, 0), 3, 3, base, false);

        // 8 pillars around the inner ring
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            int px = (int)(Math.cos(angle) * (r * 0.65));
            int pz = (int)(Math.sin(angle) * (r * 0.65));
            placePillar(level, spawn.offset(px, 0, pz), 4, accent);
            level.setBlock(spawn.offset(px, 4, pz), light, 3);
        }

        // Sunken central arena (-1 block)
        for (int dx = -12; dx <= 12; dx++)
            for (int dz = -12; dz <= 12; dz++)
                if (dx*dx + dz*dz <= 12*12)
                    level.setBlock(spawn.offset(dx, -1, dz), accent, 3);

        // Gradins (3 steps around the pit)
        for (int step = 0; step < 3; step++) {
            int innerR = 13 + step;
            int outerR = 14 + step;
            for (int dx = -outerR; dx <= outerR; dx++)
                for (int dz = -outerR; dz <= outerR; dz++) {
                    int d2 = dx*dx + dz*dz;
                    if (d2 > innerR*innerR && d2 <= outerR*outerR)
                        level.setBlock(spawn.offset(dx, step - 1, dz), stair, 3);
                }
        }

        // Spawn pad 3x3
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(spawn.offset(dx, -1, dz), accent, 3);
                level.setBlock(spawn.offset(dx, 0, dz), air, 3);
                level.setBlock(spawn.offset(dx, 1, dz), air, 3);
            }

        // Exit blocks
        level.setBlock(spawn.offset(-2, 0, -2),
                DungeonBlocks.RETURN_BEACON.get().defaultBlockState(), 3);
        level.setBlock(spawn.offset(0, 0, r - 4),
                DungeonBlocks.NEXT_FLOOR_TELEPORTER.get().defaultBlockState(), 3);

        // Mobs
        spawnCombatWave(level, spawn, palette, floor, r - 10);
    }

    // ═════════════════════ TRÉSOR — DUNGEON (140×140) ═════════════════════

    private static void generateTreasureDungeon(ServerLevel level, BlockPos spawn,
                                                 FloorPalette palette) {
        int r = TREASURE_RADIUS;
        BlockState base = palette.base().defaultBlockState();
        BlockState accent = palette.accent().defaultBlockState();
        BlockState wall = palette.wallBlock().defaultBlockState();
        BlockState ceil = palette.ceiling().defaultBlockState();
        BlockState light = palette.light().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // Platform
        placeCircle(level, spawn.below(), r, base, accent, palette);

        // Outer wall
        placeRingWall(level, spawn, r, base, 4);

        // Entrance building
        placeEntranceHall(level, spawn.offset(0, 0, r - 6), 7, 5, 4, base, accent, wall, ceil, light, true);

        // 4 rooms connected by doors
        int[] rmOffX = {r/3, -r/3, r/3, -r/3};
        int[] rmOffZ = {r/3, r/3, -r/3, -r/3};
        for (int i = 0; i < 4; i++)
            placeRoom(level, spawn.offset(rmOffX[i], 0, rmOffZ[i]),
                    9, 7, 4, base, accent, wall, ceil, light);

        // Treasure vault at center
        placeTreasureVault(level, spawn, base, accent, wall, ceil, light);

        // Exit blocks
        level.setBlock(spawn.offset(-2, 0, -2),
                DungeonBlocks.RETURN_BEACON.get().defaultBlockState(), 3);
        level.setBlock(spawn.offset(0, 0, r - 4),
                DungeonBlocks.NEXT_FLOOR_TELEPORTER.get().defaultBlockState(), 3);
    }

    // ═════════════════════ BOSS — TEMPLE (180×180) ═════════════════════

    private static void generateBossTemple(ServerLevel level, BlockPos spawn,
                                            FloorPalette palette) {
        int r = BOSS_RADIUS;
        BlockState base = palette.base().defaultBlockState();
        BlockState accent = palette.accent().defaultBlockState();
        BlockState wall = palette.wallBlock().defaultBlockState();
        BlockState ceil = palette.ceiling().defaultBlockState();
        BlockState light = palette.light().defaultBlockState();
        BlockState stair = palette.stair().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // Platform
        placeCircle(level, spawn.below(), r, base, accent, palette);

        // Outer wall 5 blocks high, 2 blocks thick
        placeThickWall(level, spawn, r, base, 5);

        // 4 corner towers (5x5, 9 blocks high)
        int[][] corners = {{r-5,r-5}, {r-5,-(r-5)}, {-(r-5),r-5}, {-(r-5),-(r-5)}};
        for (int[] c : corners)
            placeTower(level, spawn.offset(c[0], 0, c[1]),
                    5, 9, base, accent, ceil, light);

        // Grand entrance archway (south side)
        placeArchway(level, spawn.offset(0, 0, r - 2), 5, 5, base, true);
        // Side entrance (north)
        placeArchway(level, spawn.offset(0, 0, -(r - 2)), 3, 3, base, true);

        // Central nave: 6 pillars on each side
        for (int i = -5; i <= 5; i += 2) {
            if (i == -1 || i == 1) continue;
            placePillar(level, spawn.offset(-8, 0, i * 5), 6, accent);
            placePillar(level, spawn.offset(8, 0, i * 5), 6, accent);
            level.setBlock(spawn.offset(-8, 6, i * 5), light, 3);
            level.setBlock(spawn.offset(8, 6, i * 5), light, 3);
        }

        // Raised altar platform (5x5, 3 blocks high)
        BlockPos altarBase = spawn.offset(0, 0, -8);
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                for (int dy = 0; dy < 3; dy++)
                    level.setBlock(altarBase.offset(dx, dy, dz), accent, 3);

        // Stairs up to altar (south side)
        for (int step = 0; step < 3; step++)
            for (int sx = -2; sx <= 2; sx++)
                level.setBlock(altarBase.offset(sx, step, 2 + step), stair, 3);

        // Altar block on top
        level.setBlock(altarBase.offset(0, 3, 0),
                DungeonBlocks.BOSS_ALTAR.get().defaultBlockState(), 3);

        // Lava moat around altar (LATE/ABYSS only)
        if (palette == FloorPalette.LATE || palette == FloorPalette.ABYSS) {
            for (int dx = -4; dx <= 4; dx++)
                for (int dz = -4; dz <= 4; dz++) {
                    if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) continue;
                    if ((dx == -4 || dx == 4 || dz == -4 || dz == 4) &&
                            (dx == -4 || dx == 4) && (dz == -4 || dz == 4)) continue;
                    level.setBlock(altarBase.offset(dx, 0, dz), Blocks.LAVA.defaultBlockState(), 3);
                }
        }

        // Ceiling/roof over the altar area
        for (int dx = -6; dx <= 6; dx++)
            for (int dz = -11; dz <= -5; dz++)
                level.setBlock(spawn.offset(dx, 7, dz), ceil, 3);

        // Exit blocks
        level.setBlock(spawn.offset(-3, 0, -3),
                DungeonBlocks.RETURN_BEACON.get().defaultBlockState(), 3);
        level.setBlock(spawn.offset(0, 0, r - 4),
                DungeonBlocks.NEXT_FLOOR_TELEPORTER.get().defaultBlockState(), 3);
    }

    // ═════════════════════ ARCHITECTURAL PRIMITIVES ═════════════════════

    private static void placeCircle(ServerLevel level, BlockPos center, int r,
                                     BlockState base, BlockState accent, FloorPalette p) {
        BlockState deco = p.decorPrimary().defaultBlockState();
        int r2 = r * r;
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                if (dx*dx + dz*dz > r2) continue;
                BlockPos pos = center.offset(dx, 0, dz);
                if (Math.abs(dx) % 7 == 0 && Math.abs(dz) % 7 == 0)
                    level.setBlock(pos, deco, 3);
                else
                    level.setBlock(pos, base, 3);
            }
    }

    private static void placeRingWall(ServerLevel level, BlockPos center, int r,
                                       BlockState wall, int height) {
        int outer2 = r * r;
        int inner2 = (r - 1) * (r - 1);
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx*dx + dz*dz;
                if (d2 > outer2 || d2 <= inner2) continue;
                for (int dy = 0; dy < height; dy++)
                    level.setBlock(center.offset(dx, dy, dz), wall, 3);
                // Crenellations every other block
                if (((dx + 10000) % 2 == 0) && ((dz + 10000) % 2 == 0))
                    level.setBlock(center.offset(dx, height, dz), wall, 3);
            }
    }

    private static void placeThickWall(ServerLevel level, BlockPos center, int r,
                                        BlockState wall, int height) {
        int outer2 = r * r;
        int inner2 = (r - 2) * (r - 2);
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx*dx + dz*dz;
                if (d2 > outer2 || d2 <= inner2) continue;
                for (int dy = 0; dy < height; dy++)
                    level.setBlock(center.offset(dx, dy, dz), wall, 3);
                if (((dx + 10000) % 2 == 0) && ((dz + 10000) % 2 == 0))
                    level.setBlock(center.offset(dx, height, dz), wall, 3);
            }
    }

    private static void placePillar(ServerLevel level, BlockPos foot, int height,
                                     BlockState block) {
        for (int dy = 0; dy < height; dy++)
            level.setBlock(foot.above(dy), block, 3);
    }

    private static void placeArchway(ServerLevel level, BlockPos center, int width,
                                      int height, BlockState block, boolean zAxis) {
        int hw = width / 2;
        for (int dy = 0; dy < height; dy++) {
            for (int off = -hw; off <= hw; off++) {
                if (dy == height - 1 && Math.abs(off) > hw - 1) continue;
                BlockPos p = zAxis ? center.offset(off, dy, 0) : center.offset(0, dy, off);
                level.setBlock(p, block, 3);
            }
        }
        // Clear the doorway
        for (int dy = 0; dy < height - 1; dy++)
            for (int off = -(hw - 1); off <= hw - 1; off++) {
                BlockPos p = zAxis ? center.offset(off, dy, 0) : center.offset(0, dy, off);
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
    }

    private static void placeTower(ServerLevel level, BlockPos foot, int size,
                                    int height, BlockState base, BlockState accent,
                                    BlockState ceil, BlockState light) {
        for (int dy = 0; dy < height; dy++)
            for (int dx = 0; dx < size; dx++)
                for (int dz = 0; dz < size; dz++) {
                    if (dx > 0 && dx < size-1 && dz > 0 && dz < size-1 && dy > 0 && dy < height-1)
                        level.setBlock(foot.offset(dx, dy, dz),
                                dy == 0 ? base : Blocks.AIR.defaultBlockState(), 3);
                    else if (dy == height - 1 && dx % 2 == 0 && dz % 2 == 0)
                        level.setBlock(foot.offset(dx, dy, dz), accent, 3);
                    else if (dx == 0 || dx == size-1 || dz == 0 || dz == size-1)
                        level.setBlock(foot.offset(dx, dy, dz), base, 3);
                    else if (dy == 0)
                        level.setBlock(foot.offset(dx, dy, dz), base, 3);
                }
        level.setBlock(foot.offset(size/2, height, size/2), light, 3);
        // Ceiling
        for (int dx = 1; dx < size-1; dx++)
            for (int dz = 1; dz < size-1; dz++)
                level.setBlock(foot.offset(dx, height-1, dz), ceil, 3);
    }

    private static void placeEntranceHall(ServerLevel level, BlockPos pos, int w, int l,
                                           int h, BlockState base, BlockState accent,
                                           BlockState wall, BlockState ceil,
                                           BlockState light, boolean zAxis) {
        for (int dy = 0; dy < h; dy++)
            for (int dx = -w/2; dx <= w/2; dx++)
                for (int dz = -l/2; dz <= l/2; dz++) {
                    boolean isEdge = Math.abs(dx) == w/2 || Math.abs(dz) == l/2;
                    BlockPos p = zAxis ? pos.offset(dx, dy, dz) : pos.offset(dz, dy, dx);
                    if (dy == 0) level.setBlock(p, base, 3);
                    else if (dy == h - 1) level.setBlock(p, ceil, 3);
                    else if (isEdge) level.setBlock(p, wall, 3);
                    else level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
        // Openings at both ends
        for (int dy = 0; dy < h - 1; dy++) {
            for (int dx = -w/2 + 1; dx <= w/2 - 1; dx++) {
                BlockPos p = zAxis ? pos.offset(dx, dy, -l/2) : pos.offset(-l/2, dy, dx);
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                p = zAxis ? pos.offset(dx, dy, l/2) : pos.offset(l/2, dy, dx);
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void placeRoom(ServerLevel level, BlockPos center, int w, int l,
                                   int h, BlockState base, BlockState accent,
                                   BlockState wall, BlockState ceil, BlockState light) {
        for (int dy = 0; dy < h; dy++)
            for (int dx = -w/2; dx <= w/2; dx++)
                for (int dz = -l/2; dz <= l/2; dz++) {
                    boolean isEdge = Math.abs(dx) == w/2 || Math.abs(dz) == l/2;
                    BlockPos p = center.offset(dx, dy, dz);
                    if (dy == 0) level.setBlock(p, base, 3);
                    else if (dy == h - 1) level.setBlock(p, ceil, 3);
                    else if (isEdge) level.setBlock(p, wall, 3);
                    else level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
        // Doorway opening (south wall)
        for (int dy = 0; dy < h - 2; dy++)
            for (int dx = -1; dx <= 1; dx++)
                level.setBlock(center.offset(dx, dy, -l/2),
                        dy == 0 ? accent : Blocks.AIR.defaultBlockState(), 3);
        // Light
        level.setBlock(center.offset(0, h - 1, 0), light, 3);
    }

    private static void placeTreasureVault(ServerLevel level, BlockPos center,
                                            BlockState base, BlockState accent,
                                            BlockState wall, BlockState ceil,
                                            BlockState light) {
        // Vault room 7x7
        placeRoom(level, center, 7, 7, 5, base, accent, wall, ceil, light);
        // Chest on pedestal
        BlockPos chestPos = center.offset(0, 1, 0);
        level.setBlock(chestPos.below(), accent, 3);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        BlockEntity be = level.getBlockEntity(chestPos);
        if (be instanceof RandomizableContainerBlockEntity container) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "chests/dungeon_treasure"));
            container.setLootTable(key, level.random.nextLong());
        }
        // 4 pillar corners
        int[][] corners = {{-3,-3}, {3,-3}, {-3,3}, {3,3}};
        for (int[] c : corners) {
            placePillar(level, center.offset(c[0], 0, c[1]), 3, accent);
            level.setBlock(center.offset(c[0], 3, c[1]), light, 3);
        }
    }

    // ═════════════════════ MOB SPAWNING ═════════════════════

    private static void spawnCombatWave(ServerLevel level, BlockPos spawn,
                                         FloorPalette palette, int floor, int radius) {
        List<EntityType<?>> pool = mobPoolFor(palette);
        int count = mobCountFor(palette);
        double angleStep = (2.0 * Math.PI) / count;
        for (int i = 0; i < count; i++) {
            EntityType<?> type = pool.get(level.random.nextInt(pool.size()));
            double angle = angleStep * i + level.random.nextDouble() * 0.3;
            int dx = (int) (Math.cos(angle) * radius);
            int dz = (int) (Math.sin(angle) * radius);
            spawnAmbientMob(level, spawn.offset(dx, 0, dz), type);
        }
    }

    private static List<EntityType<?>> mobPoolFor(FloorPalette palette) {
        List<EntityType<?>> pool = new ArrayList<>();
        pool.add(EntityType.ZOMBIE);
        pool.add(EntityType.SKELETON);
        pool.add(EntityType.SPIDER);
        switch (palette) {
            case MID -> { pool.add(EntityType.CREEPER); pool.add(EntityType.HUSK); pool.add(EntityType.STRAY); }
            case LATE -> { pool.add(EntityType.CREEPER); pool.add(EntityType.WITCH); pool.add(EntityType.ENDERMAN); pool.add(EntityType.BLAZE); }
            case ABYSS -> { pool.add(EntityType.WITCH); pool.add(EntityType.ENDERMAN); pool.add(EntityType.BLAZE); pool.add(EntityType.WITHER_SKELETON); pool.add(EntityType.PIGLIN_BRUTE); }
            default -> {}
        }
        return pool;
    }

    private static int mobCountFor(FloorPalette palette) {
        return switch (palette) { case EARLY -> 6; case MID -> 10; case LATE -> 14; case ABYSS -> 20; };
    }

    private static void spawnAmbientMob(ServerLevel level, BlockPos pos, EntityType<?> type) {
        try {
            var entity = type.spawn(level, pos, MobSpawnType.STRUCTURE);
            if (entity != null) DungeonSpawnGuard.markAuthorized(entity);
        } catch (RuntimeException e) {
            STATMod.LOGGER.warn("[TrialDungeon] Spawn failed {} at {}: {}", type, pos, e.getMessage());
        }
    }

    // ═════════════════════ BOUNDING BOX ═════════════════════

    public static BoundingBox floorBoundingBox(int floor) {
        BlockPos spawn = DungeonTeleportHandler.floorSpawnPos(floor);
        int radius = radiusFor(floor);
        return new BoundingBox(
                spawn.getX() - radius, spawn.getY() - 2, spawn.getZ() - radius,
                spawn.getX() + radius, spawn.getY() + 12, spawn.getZ() + radius);
    }

    static int radiusFor(int floor) {
        if (floor > 0 && floor % 10 == 0) return BOSS_RADIUS;
        if (floor > 0 && floor % 5 == 0) return TREASURE_RADIUS;
        return COMBAT_RADIUS;
    }
}
