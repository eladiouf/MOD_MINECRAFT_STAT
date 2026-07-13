package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import tong.statmod.dungeon.layout.NoiseShapeRoom;
import tong.statmod.dungeon.layout.RoomLike;
import tong.statmod.dungeon.layout.RoomProvider;
import tong.statmod.dungeon.layout.OrganicRoomLayout;
import tong.statmod.dungeon.template.*;

import java.util.List;
import java.util.Random;

import static tong.statmod.dungeon.DungeonArchitect.AIR;
import static tong.statmod.dungeon.DungeonArchitect.B;
import static tong.statmod.dungeon.DungeonArchitect.O;
import static tong.statmod.dungeon.DungeonArchitect.S;
import static tong.statmod.dungeon.DungeonArchitect.WALL_H;
import static tong.statmod.dungeon.DungeonArchitect.stair;

/**
 * Mission M6 — Génération d'un étage en <b>chaîne de pièces</b> (2026-07-05).
 *
 * <p>Construit l'intérieur d'un étage de combat comme une suite de pièces reliées (voir
 * {@link DungeonLayout}) : chaque pièce est une salle fermée (sol/murs/plafond thématisés),
 * reliée à la suivante par une porte. On apparaît dans la 1ʳᵉ pièce ; la <b>dernière</b> contient
 * le téléporteur vers l'étage suivant. Thématisé par {@link BlockPalette}.
 *
 * <p>Ordre de construction : (1) toutes les coques, (2) toutes les portes (pour ne pas re-murer une
 * porte), (3) contenu (pad de spawn, téléporteur, éclairage, détails).
 */
public final class DungeonRoomChain {

    /** Demi-largeur de l'ouverture d'une porte (porte de largeur 2·H+1). */
    private static final int DOOR_HALF = 1;
    /** Hauteur de l'ouverture d'une porte. */
    private static final int DOOR_H = 4;
    /**
     * Marge entre le bord de case et le mur. À 0 : les pièces sont contiguës → le sol est continu
     * d'une pièce à l'autre (pas de trou dans la porte) et deux murs mitoyens séparent les pièces.
     */
    private static final int PAD = 0;

    private DungeonRoomChain() {}

    /**
     * Construit tout l'intérieur d'un étage en utilisant {@link RoomProvider}.
     * Version organique de {@link #build(ServerLevel, BlockPos, BlockPalette, int, DungeonArchitect.Role, Random)}.
     */
    public static void build(ServerLevel lv, BlockPos sp, BlockPalette t, int floor,
                             DungeonArchitect.Role role, RoomProvider provider) {
        List<? extends RoomLike> rooms = provider.rooms();
        int count = rooms.size();

        // (1) Coques — convertit RoomLike en Room local pour shell legacy.
        for (RoomLike r : rooms) shellFromLike(lv, sp, t, r, floor);

        // (2) Portes entre pièces connectées (déduites de connectedTo).
        for (RoomLike r : rooms) {
            int[] conn = r.connectedTo();
            if (conn.length > 0) carveDoorFromLike(lv, sp, t, r, floor);
        }

        // (3) Contenu par pièce.
        for (RoomLike r : rooms) {
            int yOffset = roomYOffset(r.index(), count, floor);
            BlockPos roomSp = sp.above(yOffset);
            int ceilH = roomCeilingHeight(floor, r, count);
            lightRoomFromLike(lv, roomSp, t, r, ceilH);
            if (r.isFirst()) {
                spawnPad(lv, roomSp, t, toLayoutRoom(r));
            } else if (r.isLast()) {
                if (role == DungeonArchitect.Role.TREASURE) treasureRoom(lv, roomSp, t, floor, toLayoutRoom(r));
                else exitRoom(lv, roomSp, t, toLayoutRoom(r));
            } else if (role == DungeonArchitect.Role.TREASURE) {
                DungeonMerchant.placeStall(lv, roomSp, r.centerX(), r.centerZ(), t,
                        r.index() % DungeonMerchant.STALL_KINDS, floor);
            } else if (role == DungeonArchitect.Role.COMBAT && r.index() == count / 2) {
                safehouseRoom(lv, roomSp, t, floor, toLayoutRoom(r));
            } else {
                placePathwayFromLike(lv, roomSp, t, r, floor);
                decorateCombatRoom(lv, roomSp, t, toLayoutRoom(r), floor, ceilH);
                tryPlaceTemplate(lv, roomSp, r, t, floor);
            }
            DungeonWayfinding.mark(lv, roomSp, toLayoutRoom(r), floor);
        }
    }

    /**
     * Construit tout l'intérieur d'un étage en chaîne de pièces (layout grille 5×4 legacy).
     * La dernière pièce dépend du rôle :
     * combat → téléporteur ; trésor → coffres + aménagement ; boss → autel + arène + aménagement
     * (le téléporteur y est posé mais scellé tant que l'objectif — vague/loot/boss — n'est pas rempli).
     */
    public static void build(ServerLevel lv, BlockPos sp, BlockPalette t, int floor,
                             DungeonArchitect.Role role, Random rng) {
        List<DungeonLayout.Room> rooms = DungeonLayout.rooms();

        // (1) Coques de toutes les pièces.
        for (DungeonLayout.Room r : rooms) shell(lv, sp, t, r, floor);

        // (2) Portes entre pièces consécutives (après les coques → jamais re-murées).
        for (DungeonLayout.Room r : rooms) {
            if (r.exitDoor() != DungeonLayout.Dir.NONE) carveDoor(lv, sp, t, r, floor);
        }

        // (3) Contenu par pièce.
        for (DungeonLayout.Room r : rooms) {
            int yOffset = roomYOffset(r.index(), floor);
            BlockPos roomSp = sp.above(yOffset);
            int ceilH = roomCeilingHeight(floor, r);
            lightRoom(lv, roomSp, t, r, ceilH);
            if (r.isFirst()) {
                spawnPad(lv, roomSp, t, r);
            } else if (r.isLast()) {
                // BOSS ne passe PAS par la chaîne (arène dédiée géante) ; sécurité → exitRoom.
                if (role == DungeonArchitect.Role.TREASURE) treasureRoom(lv, roomSp, t, floor, r);
                else exitRoom(lv, roomSp, t, r);
            } else if (role == DungeonArchitect.Role.TREASURE) {
                // Étage trésor : les pièces libres deviennent un MARCHÉ (stands de marchands bloqués).
                DungeonMerchant.placeStall(lv, roomSp, r.centerX(), r.centerZ(), t,
                        r.index() % DungeonMerchant.STALL_KINDS, floor);
            } else if (role == DungeonArchitect.Role.COMBAT && r.index() == DungeonLayout.ROOM_COUNT / 2) {
                // Havre de paix / repos
                safehouseRoom(lv, roomSp, t, floor, r);
            } else {
                placePathway(lv, roomSp, t, r, floor);
                decorateCombatRoom(lv, roomSp, t, r, floor, ceilH);
                tryPlaceTemplate(lv, roomSp, r, t, floor);
            }
            // Balisage EN DERNIER (après le décor) : traînée + fanal vers la porte de sortie → guide
            // le joueur, jamais perdu ni à rebrousser chemin.
            DungeonWayfinding.mark(lv, roomSp, r, floor);
        }
    }

    /** Hauteur de plafond d'une pièce (spawn/sortie = standard ; sinon variée). */
    private static int roomCeilingHeight(int floor, DungeonLayout.Room r) {
        return (r.isFirst() || r.isLast()) ? WALL_H : ceilingHeight(floor, r.index());
    }

    // ═══════════════ coque d'une pièce (forme + hauteur variables) ═══════════════

    // Silhouette au sol désormais gérée par NoiseShapeRoom (OpenSimplex2S).

    /**
     * Coque d'une pièce avec <b>forme et hauteur variables</b> (déterministe par étage+pièce). La
     * hauteur de plafond change (crypte basse → grande salle haute), et la silhouette au sol varie
     * (rectangle / octogone / pans coupés / croix) : les coins « en trop » sont comblés en roche
     * pleine. Le sol est posé sur toute la case (jamais de trou) ; les portes restent aux milieux
     * des bords (toujours pleins) → jamais bloquées. Pièces de spawn/sortie forcées en rectangle.
     */
    private static void shell(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int floor) {
        int x0 = r.minX(), x1 = r.maxX(), z0 = r.minZ(), z1 = r.maxZ();
        boolean fixed = r.isFirst() || r.isLast();
        int ceilH = roomCeilingHeight(floor, r);
        NoiseShapeRoom shape = shapeForRoom(floor, r.index());
        int yOffset = roomYOffset(r.index(), floor);

        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = -7; y < yOffset - 1; y++) {
                    S(lv, O(sp, x, y, z), B(t.base()));
                }
                S(lv, O(sp, x, yOffset - 1, z), variedFloor(t, x, z));
                boolean in = fixed || isInside(r, shape, x, z);
                if (!in) {
                    for (int y = 0; y <= ceilH; y++) S(lv, O(sp, x, yOffset + y, z), variedWall(t, x, yOffset + y, z));
                    continue;
                }
                S(lv, O(sp, x, yOffset + ceilH, z), B(t.ceiling()));
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1
                        || !isInside(r, shape, x - 1, z) || !isInside(r, shape, x + 1, z)
                        || !isInside(r, shape, x, z - 1) || !isInside(r, shape, x, z + 1);
                if (edge) {
                    for (int y = 0; y < ceilH; y++) S(lv, O(sp, x, yOffset + y, z), variedWall(t, x, yOffset + y, z));
                    S(lv, O(sp, x, yOffset, z), B(t.accent()));
                } else {
                    for (int y = 0; y < ceilH; y++) S(lv, O(sp, x, yOffset + y, z), AIR());
                }
            }
        }
    }

    /** Hauteur de plafond variée : crypte basse (8) → grande salle (20). */
    private static int ceilingHeight(int floor, int idx) {
        int[] h = {WALL_H - 4, WALL_H, WALL_H + 4, WALL_H + 8}; // 8, 12, 16, 20
        return h[Math.floorMod(floor * 13 + idx * 29, h.length)];
    }

    /** Forme organique : déléguée à NoiseShapeRoom.inside() (OpenSimplex2S). */

    /** Hash déterministe [0,100) d'une position — bruit reproductible (même étage → même donjon). */
    private static int noise(int x, int y, int z) {
        int h = (int) (x * 374761393L + y * 668265263L + z * 2246822519L);
        h = (h ^ (h >>> 13)) * 1274126177;
        return Math.floorMod(h ^ (h >>> 16), 100);
    }

    /**
     * Bloc de mur varié — <b>blocs pleins</b> uniquement (jamais {@code wallBlock()} qui est un bloc
     * de type barrière/muret ajouré). Surtout {@code base}, parfois accent/decorPrimary pour casser
     * la platitude tout en gardant un mur solide (pas de mob qui voit à travers).
     */
    private static BlockState variedWall(BlockPalette t, int x, int y, int z) {
        int n = noise(x, y, z);
        BlockState state;
        if (n < 12) state = B(t.accent());
        else if (n < 18) state = B(t.decorPrimary());
        else state = B(t.base());

        // Weathering variations for stone-based blocks
        if (state.is(Blocks.STONE_BRICKS)) {
            int weather = noise(x, y + 10, z);
            if (weather < 10) return B(Blocks.CRACKED_STONE_BRICKS);
            if (weather < 18) return B(Blocks.MOSSY_STONE_BRICKS);
        } else if (state.is(Blocks.DEEPSLATE_BRICKS)) {
            int weather = noise(x, y + 10, z);
            if (weather < 12) return B(Blocks.CRACKED_DEEPSLATE_BRICKS);
        } else if (state.is(Blocks.COBBLESTONE)) {
            int weather = noise(x, y + 10, z);
            if (weather < 15) return B(Blocks.MOSSY_COBBLESTONE);
        }
        return state;
    }

    /** Bloc de sol varié : surtout {@code base}, parfois accent — blocs pleins sûrs uniquement
     *  (jamais scar/decor qui peuvent être glace-poudreuse, magma ou toile → dangereux au sol). */
    private static BlockState variedFloor(BlockPalette t, int x, int z) {
        int n = noise(x, 7, z);
        return n < 16 ? B(t.accent()) : B(t.base());
    }
    // ═══════════════ portes ═══════════════

    /** Perce une porte (largeur 3, hauteur DOOR_H) dans le mur de sortie de la pièce vers la suivante, avec escalier si dénivelé. */
    private static void carveDoor(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int floor) {
        int i = r.index();
        int y0 = roomYOffset(i, floor);
        int y1 = roomYOffset(i + 1, floor);
        if (r.exitDoor() == DungeonLayout.Dir.NONE) return;

        int cx = r.centerX(), cz = r.centerZ();
        BlockState frame = B(t.decorPrimary());

        int wallX0 = 0, wallX1 = 0, wallZ0 = 0, wallZ1 = 0;
        int dx = 0, dz = 0; // direction du décalage (1 ou -1)

        switch (r.exitDoor()) {
            case EAST -> {
                wallX0 = r.maxX();
                wallX1 = r.maxX() + 1;
                dx = 1;
            }
            case WEST -> {
                wallX0 = r.minX() - 1;
                wallX1 = r.minX();
                dx = -1;
            }
            case SOUTH -> {
                wallZ0 = r.maxZ();
                wallZ1 = r.maxZ() + 1;
                dz = 1;
            }
            case NORTH -> {
                wallZ0 = r.minZ() - 1;
                wallZ1 = r.minZ();
                dz = -1;
            }
            case NONE -> {}
        }

        int minY = Math.min(y0, y1);
        int maxY = Math.max(y0, y1);

        // 1. Carve the door opening (clear air from minY to maxY + DOOR_H)
        if (dx != 0) { // East/West door
            for (int x = wallX0; x <= wallX1; x++) {
                for (int z = cz - DOOR_HALF; z <= cz + DOOR_HALF; z++) {
                    for (int y = minY; y < maxY + DOOR_H; y++) {
                        S(lv, O(sp, x, y, z), AIR());
                    }
                }
                // Lintels
                S(lv, O(sp, x, maxY + DOOR_H, cz - DOOR_HALF - 1), frame);
                S(lv, O(sp, x, maxY + DOOR_H, cz + DOOR_HALF + 1), frame);
            }
        } else { // North/South door
            for (int z = wallZ0; z <= wallZ1; z++) {
                for (int x = cx - DOOR_HALF; x <= cx + DOOR_HALF; x++) {
                    for (int y = minY; y < maxY + DOOR_H; y++) {
                        S(lv, O(sp, x, y, z), AIR());
                    }
                }
                // Lintels
                S(lv, O(sp, cx - DOOR_HALF - 1, maxY + DOOR_H, z), frame);
                S(lv, O(sp, cx + DOOR_HALF + 1, maxY + DOOR_H, z), frame);
            }
        }

        // 1.5. Place doors and side bars
        if (dx != 0) { // East/West door
            Direction facing = (dx == 1) ? Direction.EAST : Direction.WEST;
            placeDoorWayDoor(lv, O(sp, wallX0, minY, cz), O(sp, wallX0, minY, cz - 1), O(sp, wallX0, minY, cz + 1), facing, t);
        } else { // North/South door
            Direction facing = (dz == 1) ? Direction.SOUTH : Direction.NORTH;
            placeDoorWayDoor(lv, O(sp, cx, minY, wallZ0), O(sp, cx - 1, minY, wallZ0), O(sp, cx + 1, minY, wallZ0), facing, t);
        }

        // 2. Place stairs if there is a vertical difference
        if (y0 != y1) {
            // Determine direction of climb
            Direction stairDir = Direction.NORTH;
            if (dx == 1) stairDir = (y1 > y0) ? Direction.EAST : Direction.WEST;
            else if (dx == -1) stairDir = (y1 > y0) ? Direction.WEST : Direction.EAST;
            else if (dz == 1) stairDir = (y1 > y0) ? Direction.SOUTH : Direction.NORTH;
            else if (dz == -1) stairDir = (y1 > y0) ? Direction.NORTH : Direction.SOUTH;

            BlockState stairBlock = stair(t.stair(), stairDir);
            BlockState baseBlock = B(t.base());

            int sig = Integer.signum(y1 - y0);
            int step1Y = (sig > 0) ? y0 : y0 - 1;
            int step2Y = (sig > 0) ? y0 + 1 : y0 - 2;
            int step3Y = (sig > 0) ? y0 + 2 : y0 - 3;

            if (dx != 0) { // East/West stairs
                int x1 = (dx == 1) ? wallX0 - 1 : wallX1 + 1;
                int x2 = (dx == 1) ? wallX0 : wallX1;
                int x3 = (dx == 1) ? wallX1 : wallX0;

                for (int z = cz - DOOR_HALF; z <= cz + DOOR_HALF; z++) {
                    // Step 1
                    S(lv, O(sp, x1, step1Y - 1, z), baseBlock);
                    S(lv, O(sp, x1, step1Y, z), stairBlock);
                    for (int y = step1Y + 1; y < step1Y + DOOR_H; y++) S(lv, O(sp, x1, y, z), AIR());

                    // Step 2 (filled dynamically to prevent hollow space)
                    for (int y = minY - 1; y < step2Y; y++) S(lv, O(sp, x2, y, z), baseBlock);
                    S(lv, O(sp, x2, step2Y, z), stairBlock);
                    for (int y = step2Y + 1; y < step2Y + DOOR_H; y++) S(lv, O(sp, x2, y, z), AIR());

                    // Step 3 (filled dynamically to prevent hollow space)
                    for (int y = minY - 1; y < step3Y; y++) S(lv, O(sp, x3, y, z), baseBlock);
                    S(lv, O(sp, x3, step3Y, z), stairBlock);
                    for (int y = step3Y + 1; y < step3Y + DOOR_H; y++) S(lv, O(sp, x3, y, z), AIR());
                }
            } else { // North/South stairs
                int z1 = (dz == 1) ? wallZ0 - 1 : wallZ1 + 1;
                int z2 = (dz == 1) ? wallZ0 : wallZ1;
                int z3 = (dz == 1) ? wallZ1 : wallZ0;

                for (int x = cx - DOOR_HALF; x <= cx + DOOR_HALF; x++) {
                    // Step 1
                    S(lv, O(sp, x, step1Y - 1, z1), baseBlock);
                    S(lv, O(sp, x, step1Y, z1), stairBlock);
                    for (int y = step1Y + 1; y < step1Y + DOOR_H; y++) S(lv, O(sp, x, y, z1), AIR());

                    // Step 2 (filled dynamically to prevent hollow space)
                    for (int y = minY - 1; y < step2Y; y++) S(lv, O(sp, x, y, z2), baseBlock);
                    S(lv, O(sp, x, step2Y, z2), stairBlock);
                    for (int y = step2Y + 1; y < step2Y + DOOR_H; y++) S(lv, O(sp, x, y, z2), AIR());

                    // Step 3 (filled dynamically to prevent hollow space)
                    for (int y = minY - 1; y < step3Y; y++) S(lv, O(sp, x, y, z3), baseBlock);
                    S(lv, O(sp, x, step3Y, z3), stairBlock);
                    for (int y = step3Y + 1; y < step3Y + DOOR_H; y++) S(lv, O(sp, x, y, z3), AIR());
                }
            }
        }
    }

    // ═══════════════ contenu des pièces ═══════════════

    /**
     * Éclairage adapté à la hauteur de la pièce : une source au centre du plafond + des lumières
     * encastrées dans les 4 murs (aux quarts, y=4, posées sur le mur en dessous → supportées, même
     * pour les torches). Évite les torches flottantes qui tombent.
     */
    private static void lightRoom(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int ceilH) {
        BlockState light = B(t.light());
        Block macawWallLight = MacawDungeonDecorator.wallLantern();
        BlockState wallLight = macawWallLight != null ? macawWallLight.defaultBlockState() : light;

        int cx = r.centerX(), cz = r.centerZ();
        if (ceilH >= 12 && !r.isFirst() && !r.isLast()) {
            hangingChandelier(lv, sp, t, cx, cz, ceilH);
        } else {
            Block macawCeilingLight = MacawDungeonDecorator.chainLamp();
            S(lv, O(sp, cx, ceilH, cz), macawCeilingLight != null ? macawCeilingLight.defaultBlockState() : light);
        }
        int qx = Math.max(4, (r.maxX() - r.minX()) / 4);
        int qz = Math.max(4, (r.maxZ() - r.minZ()) / 4);
        for (int dx : new int[]{-qx, qx}) {
            S(lv, O(sp, cx + dx, 4, r.minZ()), wallLight);
            S(lv, O(sp, cx + dx, 4, r.maxZ()), wallLight);
        }
        for (int dz : new int[]{-qz, qz}) {
            S(lv, O(sp, r.minX(), 4, cz + dz), wallLight);
            S(lv, O(sp, r.maxX(), 4, cz + dz), wallLight);
        }
    }

    /** Pièce d'apparition : pad 3×3 dégagé au centre + repère lumineux (bleu accueillant). */
    private static void spawnPad(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r) {
        int cx = r.centerX(), cz = r.centerZ();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            S(lv, O(sp, cx + dx, -1, cz + dz), B(t.accent()));
            S(lv, O(sp, cx + dx, 0, cz + dz), AIR());
            S(lv, O(sp, cx + dx, 1, cz + dz), AIR());
        }
        S(lv, O(sp, cx, -1, cz), B(Blocks.SEA_LANTERN)); // marqueur de spawn lisible

        // Portal Columns topped with lanterns
        for (int y = 0; y <= 2; y++) {
            S(lv, O(sp, cx - 3, y, cz - 3), B(t.decorPrimary()));
            S(lv, O(sp, cx + 3, y, cz - 3), B(t.decorPrimary()));
        }
        S(lv, O(sp, cx - 3, 3, cz - 3), B(t.light()));
        S(lv, O(sp, cx + 3, 3, cz - 3), B(t.light()));

        // Initial Supply Chest
        BlockPos chestPos = O(sp, cx, 0, cz - 3);
        S(lv, chestPos, Blocks.CHEST.defaultBlockState());
        if (lv.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity c) {
            c.setLootTable(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE,
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("chests/spawn_bonus_chest")), lv.random.nextLong());
        }
    }

    /** Dernière pièce : socle central + téléporteur vers l'étage suivant, mis en valeur. */
    private static void exitRoom(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r) {
        int cx = r.centerX(), cz = r.centerZ();
        BlockState pillar = B(t.decorPrimary());
        BlockState light = B(t.light());
        // Socle 3×3 surélevé.
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            S(lv, O(sp, cx + dx, -1, cz + dz), B(t.accent()));
            S(lv, O(sp, cx + dx, 0, cz + dz), (dx == 0 && dz == 0) ? AIR() : B(t.slab()));
        }
        // Colonnes + lumières encadrant le téléporteur.
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            for (int y = 0; y <= 3; y++) S(lv, O(sp, cx + c[0], y, cz + c[1]), pillar);
            S(lv, O(sp, cx + c[0], 4, cz + c[1]), light);
        }
        // Majestic stone brick arches linking the columns at y=4
        for (int dx = -2; dx <= 2; dx++) {
            S(lv, O(sp, cx + dx, 4, cz - 2), pillar);
            S(lv, O(sp, cx + dx, 4, cz + 2), pillar);
        }
        for (int dz = -2; dz <= 2; dz++) {
            S(lv, O(sp, cx - 2, 4, cz + dz), pillar);
            S(lv, O(sp, cx + 2, 4, cz + dz), pillar);
        }
        // Hanging chain chandelier above the teleporter
        S(lv, O(sp, cx, 5, cz), Blocks.CHAIN.defaultBlockState());
        S(lv, O(sp, cx, 4, cz), Blocks.SOUL_LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));

        // Escaliers d'accès sur les 4 côtés.
        S(lv, O(sp, cx, 0, cz - 2), stair(t.stair(), Direction.SOUTH));
        S(lv, O(sp, cx, 0, cz + 2), stair(t.stair(), Direction.NORTH));
        S(lv, O(sp, cx - 2, 0, cz), stair(t.stair(), Direction.EAST));
        S(lv, O(sp, cx + 2, 0, cz), stair(t.stair(), Direction.WEST));
        // Le bloc de descente.
        S(lv, O(sp, cx, 0, cz), B(DungeonBlocks.NEXT_FLOOR_TELEPORTER.get()));
    }

    private static void placeSafehouseBarrel(ServerLevel lv, BlockPos pos, net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> lootTable) {
        S(lv, pos, Blocks.BARREL.defaultBlockState());
        if (lv.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity c) {
            c.setLootTable(lootTable, lv.random.nextLong());
        }
    }

    /**
     * Dernière pièce d'un étage TRÉSOR : coffres au trésor + aménagement (fontaine, waystone, armor
     * stands, piédestaux) + téléporteur scellé (s'ouvre au pillage du coffre, cf. DungeonVaultHandler).
     */
    private static void treasureRoom(ServerLevel lv, BlockPos sp, BlockPalette t, int floor,
                                     DungeonLayout.Room r) {
        int cx = r.centerX(), cz = r.centerZ();
        BlockState pillar = B(t.decorPrimary());
        // Estrade centrale + coffres (loot table dungeon_treasure) posés dessus.
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            S(lv, O(sp, cx + dx, -1, cz + dz), B(t.accent()));
        }
        DungeonArchitect.placeChest(lv, O(sp, cx, 0, cz));
        DungeonArchitect.placeChest(lv, O(sp, cx + 2, 0, cz));
        DungeonArchitect.placeChest(lv, O(sp, cx - 2, 0, cz));
        // Aménagement autour de l'estrade (anchor au niveau walkable de la pièce).
        DungeonRoomDressing.dressTreasureRoom(lv, O(sp, cx, 0, cz), t, floor);
        // Téléporteur mis en valeur au nord de la pièce (scellé tant que le coffre n'est pas ouvert).
        int tz = r.minZ() + 3;
        S(lv, O(sp, cx, 0, tz), B(DungeonBlocks.NEXT_FLOOR_TELEPORTER.get()));

        // Villageois changeur (points → coins) au bord ouest de la salle au trésor.
        DungeonExchanger.spawn(lv, O(sp, r.minX() + 3, 0, cz));
    }

    private static void decorateCombatRoom(ServerLevel lv, BlockPos sp, BlockPalette t,
                                           DungeonLayout.Room r, int floor, int ceilH) {
        NoiseShapeRoom shape = shapeForRoom(floor, r.index());

        // (1) Piliers organiques (OpenSimplex2S, placement par bruit)
        shape.placePillars(lv, sp, t, r.minX(), r.minZ(), r.maxX(), r.maxZ(), ceilH);

        // (2) Éléments de sol organiques (plateformes/fosses/bassins)
        shape.placeFloorFeatures(lv, sp, t, r.minX(), r.minZ(), r.maxX(), r.maxZ(), floor);

        // (3) Secret Vault Puzzle (~25% chance in combat rooms)
        if (Math.floorMod(floor * 17 + r.index() * 43, 100) < 25) {
            placeSecretVaultPuzzle(lv, sp, t, r, floor);
        }

        // (4) Archetype detailing and themed props
        int archetype = Math.floorMod(floor * 13 + r.index() * 29, 6);
        switch (archetype) {
            case 0 -> archiveRoom(lv, sp, t, r, ceilH);
            case 1 -> forgeRoom(lv, sp, t, r, ceilH);
            case 2 -> cryptRoom(lv, sp, t, r, ceilH);
            case 3 -> prisonRoom(lv, sp, t, r, ceilH);
            case 4 -> greenhouseRoom(lv, sp, t, r, ceilH);
            default -> treasuryRoom(lv, sp, t, r, ceilH);
        }
        if (ceilH >= 12) {
            buildMezzaninesAndBridges(lv, sp, t, r, floor, shape, ceilH);

            // Hanging chains/ropes from ceiling
            int q = Math.max(4, Math.min(r.maxX() - r.minX(), r.maxZ() - r.minZ()) / 4);
            int hx = r.centerX(), hz = r.centerZ();
            BlockState rope = QuarkDungeonDecorator.resolve("quark:rope") != null ?
                    QuarkDungeonDecorator.resolve("quark:rope").defaultBlockState() : Blocks.CHAIN.defaultBlockState();

            int[][] hangPos = {
                {hx - q, hz - q}, {hx + q, hz - q},
                {hx - q, hz + q}, {hx + q, hz + q}
            };
            for (int[] p : hangPos) {
                for (int y = ceilH; y >= 5; y--) {
                    BlockPos bp = O(sp, p[0], y, p[1]);
                    if (lv.isEmptyBlock(bp)) {
                        S(lv, bp, rope);
                    }
                }
            }
        }
        // Props d'ambiance disséminés partout : coffres épars, toiles, statues moddées, braseros, crânes.
        DungeonProps.decorate(lv, sp, t, r, floor, ceilH);
        // Quark decorations — iron plates, corundum, ropes, hollow logs, etc. (no-op if Quark absent).
        QuarkDungeonDecorator.decorateRoom(lv, sp, t, r, floor, ceilH);
        // Pièges (Simply Traps) au sol — dangereux à traverser.
        DungeonTraps.place(lv, sp, r, floor);
        // Salle secrète à récompenses (≈ 1 par étage) — placée EN DERNIER pour ne rien écraser.
        DungeonSecretRoom.maybePlace(lv, sp, t, r, floor);
        // Chambre-forte ULTRA-secrète (~1 étage de combat sur 7) : plaque de tp cachée ici,
        // chambre flottante construite au-dessus de l'île.
        maybePlaceUltraVaultEntry(lv, sp, t, r, floor);
    }

    /**
     * Sanctuaire caché vers la {@link DungeonUltraVault} : une pierre de guidage surmontée d'un
     * cristal d'améthyste, posée au sol d'une pièce élue, dans un quadrant — l'anomalie discrète
     * que l'explorateur attentif remarque. Clic droit sur la pierre → téléportation vers la
     * chambre-forte. Construit aussi la chambre-forte elle-même.
     */
    private static void maybePlaceUltraVaultEntry(ServerLevel lv, BlockPos sp, BlockPalette t,
                                                  DungeonLayout.Room r, int floor) {
        if (!DungeonUltraVault.isVaultFloor(floor)) return;
        if (r.isFirst() || r.isLast()) return;
        if (r.index() != 2 + Math.floorMod(floor * 5 + 3, 7)) return;

        NoiseShapeRoom shape = shapeForRoom(floor, r.index());
        int cx = r.centerX(), cz = r.centerZ();
        for (int x = cx + 3; x <= r.maxX() - 2; x++) {
            for (int z = cz + 3; z <= r.maxZ() - 2; z++) {
                if (x == cx || z == cz) continue;
                if (!isInside(r, shape, x, z) || getWallDistance(r, shape, x, z) < 2) continue;
                DungeonUltraVault.placeShrine(lv, O(sp, x, 0, z));
                DungeonUltraVault.build(lv, sp, t, floor);
                return;
            }
        }
    }

    /** Piliers organiques : délégué à NoiseShapeRoom.placePillars(). */
    private static void pillarHall(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int ceilH, int floor) {
        NoiseShapeRoom shape = shapeForRoom(floor, r.index());
        shape.placePillars(lv, sp, t, r.minX(), r.minZ(), r.maxX(), r.maxZ(), ceilH);
    }

    // ═══════════════ positions monde (pour spawn joueur / mobs) ═══════════════

    /** Position monde du centre de la pièce d'apparition (où téléporter le joueur), y = pad+1. */
    public static BlockPos spawnWorldPos(BlockPos islandCenter) {
        return spawnWorldPos(islandCenter, DungeonLayout.gridProvider());
    }

    /** Position monde avec {@link RoomProvider} (organic layout). */
    public static BlockPos spawnWorldPos(BlockPos islandCenter, RoomProvider provider) {
        RoomLike r = provider.spawnRoom();
        return islandCenter.offset(r.centerX(), 1, r.centerZ());
    }

    /** Centres monde des pièces de combat (toutes sauf la pièce d'apparition), pour répartir la vague. */
    public static List<BlockPos> combatRoomCenters(BlockPos islandCenter, int floor) {
        List<BlockPos> out = new java.util.ArrayList<>();
        for (DungeonLayout.Room r : DungeonLayout.rooms()) {
            if (r.isFirst()) continue; // pas de mobs dans la pièce d'apparition
            if (r.index() == DungeonLayout.ROOM_COUNT / 2) continue; // pièce de repos (Safehouse)
            int yOffset = roomYOffset(r.index(), floor);
            out.add(islandCenter.offset(r.centerX(), yOffset, r.centerZ()));
        }
        return out;
    }

    private static void placePathway(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int floor) {
        BlockState pathBlock = B(t.accent());
        int cx = r.centerX(), cz = r.centerZ();
        
        // Center pad
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                S(lv, O(sp, cx + dx, -1, cz + dz), pathBlock);
            }
        }
        
        // Draw path to exit
        if (r.exitDoor() != DungeonLayout.Dir.NONE) {
            int i = r.index();
            int y0 = roomYOffset(i, floor);
            int y1 = roomYOffset(i + 1, floor);
            boolean hasStairs = (y0 != y1);
            drawPathSegment(lv, sp, cx, cz, r.exitDoor(), r, pathBlock, hasStairs);
        }
        
        // Draw path from entrance
        if (!r.isFirst()) {
            List<DungeonLayout.Room> rooms = DungeonLayout.rooms();
            DungeonLayout.Room prev = rooms.get(r.index() - 1);
            DungeonLayout.Dir entranceDir = switch (prev.exitDoor()) {
                case EAST -> DungeonLayout.Dir.WEST;
                case WEST -> DungeonLayout.Dir.EAST;
                case SOUTH -> DungeonLayout.Dir.NORTH;
                case NORTH -> DungeonLayout.Dir.SOUTH;
                default -> DungeonLayout.Dir.NONE;
            };
            if (entranceDir != DungeonLayout.Dir.NONE) {
                int i = r.index();
                int y0 = roomYOffset(i, floor);
                int y1 = roomYOffset(i - 1, floor);
                boolean hasStairs = (y0 != y1);
                drawPathSegment(lv, sp, cx, cz, entranceDir, r, pathBlock, hasStairs);
            }
        }
    }

    private static void drawPathSegment(ServerLevel lv, BlockPos sp, int cx, int cz, DungeonLayout.Dir dir, DungeonLayout.Room r, BlockState pathBlock, boolean hasStairs) {
        int x0 = cx, z0 = cz;
        int x1 = cx, z1 = cz;
        switch (dir) {
            case EAST -> { x1 = r.maxX(); }
            case WEST -> { x1 = r.minX(); }
            case SOUTH -> { z1 = r.maxZ(); }
            case NORTH -> { z1 = r.minZ(); }
            default -> {}
        }
        
        int sx = Integer.signum(x1 - x0), sz = Integer.signum(z1 - z0);
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
        int limit = hasStairs ? steps - 2 : steps;
        
        for (int i = 1; i <= limit; i++) {
            int x = x0 + sx * i;
            int z = z0 + sz * i;
            if (sx != 0) {
                S(lv, O(sp, x, -1, z), pathBlock);
                S(lv, O(sp, x, -1, z - 1), pathBlock);
                S(lv, O(sp, x, -1, z + 1), pathBlock);
            } else {
                S(lv, O(sp, x, -1, z), pathBlock);
                S(lv, O(sp, x - 1, -1, z), pathBlock);
                S(lv, O(sp, x + 1, -1, z), pathBlock);
            }
        }
    }

    private static void safehouseRoom(ServerLevel lv, BlockPos sp, BlockPalette t, int floor, DungeonLayout.Room r) {
        int cx = r.centerX(), cz = r.centerZ();
        BlockPos campfirePos = O(sp, cx, 0, cz);
        S(lv, campfirePos, Blocks.CAMPFIRE.defaultBlockState());
        DungeonHealHandler.registerHealSpot(lv.dimension(), campfirePos);

        // Cooking pot on the campfire (Farmer's Delight cooking pot if available)
        Block cookingPot = QuarkDungeonDecorator.resolve("farmersdelight:cooking_pot");
        S(lv, campfirePos.above(), cookingPot != null ? cookingPot.defaultBlockState() : Blocks.CAULDRON.defaultBlockState());

        Block macawChair = MacawDungeonDecorator.oakChair();
        BlockState benchN = macawChair != null ? macawChair.defaultBlockState() : stair(Blocks.OAK_STAIRS, Direction.SOUTH);
        BlockState benchS = macawChair != null ? macawChair.defaultBlockState() : stair(Blocks.OAK_STAIRS, Direction.NORTH);
        BlockState benchW = macawChair != null ? macawChair.defaultBlockState() : stair(Blocks.OAK_STAIRS, Direction.EAST);
        BlockState benchE = macawChair != null ? macawChair.defaultBlockState() : stair(Blocks.OAK_STAIRS, Direction.WEST);

        S(lv, O(sp, cx, 0, cz - 2), benchN);
        S(lv, O(sp, cx, 0, cz + 2), benchS);
        S(lv, O(sp, cx - 2, 0, cz), benchW);
        S(lv, O(sp, cx + 2, 0, cz), benchE);

        // Cozy furniture (Library & Jukebox)
        S(lv, O(sp, cx - 3, 0, cz - 3), Blocks.BOOKSHELF.defaultBlockState());
        S(lv, O(sp, cx - 3, 0, cz - 2), Blocks.JUKEBOX.defaultBlockState());
        S(lv, O(sp, cx - 2, 0, cz - 3), Blocks.BOOKSHELF.defaultBlockState());

        // Safehouse Food and Tool Barrels
        placeSafehouseBarrel(lv, O(sp, cx + 3, 0, cz + 3), net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.LOOT_TABLE,
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("chests/village/village_plains_house")));
        placeSafehouseBarrel(lv, O(sp, cx + 3, 0, cz + 2), net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.LOOT_TABLE,
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("chests/village/village_toolsmith")));

        // Altar of Blessing (Lodestone + hanging soul lantern)
        S(lv, O(sp, cx - 3, 0, cz + 3), Blocks.LODESTONE.defaultBlockState());
        S(lv, O(sp, cx - 3, 1, cz + 3), Blocks.SOUL_LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));

        Block cuttingBoard = QuarkDungeonDecorator.resolve("farmersdelight:cutting_board");
        if (cuttingBoard != null) {
            S(lv, O(sp, cx + 3, 0, cz + 1), cuttingBoard.defaultBlockState());
        }
        Block basket = QuarkDungeonDecorator.resolve("farmersdelight:basket");
        if (basket != null) {
            S(lv, O(sp, cx + 3, 0, cz - 1), basket.defaultBlockState());
        }

        DungeonExchanger.spawn(lv, O(sp, cx + 3, 0, cz - 3));

        // Quark safehouse enhancements — stools, ancient bookshelves, paper lanterns, framed glass
        QuarkDungeonDecorator.enhanceSafehouse(lv, sp, cx, cz);
    }

    private static void buildMezzaninesAndBridges(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int floor, NoiseShapeRoom shape, int ceilH) {
        int x0 = r.minX(), x1 = r.maxX(), z0 = r.minZ(), z1 = r.maxZ();

        BlockState walkwayBlock = B(t.accent());
        Block macawRailing = MacawDungeonDecorator.oakStockadeFence();
        BlockState railingBlock = macawRailing != null ? B(macawRailing) : (QuarkDungeonDecorator.mezzanineRailing() != null ? B(QuarkDungeonDecorator.mezzanineRailing()) : B(t.wallBlock()));

        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (isInside(r, shape, x, z)) {
                    int dist = getWallDistance(r, shape, x, z);
                    if (dist == 1) {
                        S(lv, O(sp, x, 4, z), walkwayBlock);
                        S(lv, O(sp, x, 5, z), AIR());
                        S(lv, O(sp, x, 6, z), AIR());
                    } else if (dist == 2) {
                        S(lv, O(sp, x, 4, z), walkwayBlock);
                        S(lv, O(sp, x, 5, z), railingBlock);
                        S(lv, O(sp, x, 6, z), AIR());
                    }
                }
            }
        }

        BlockPos ladderPos = null;
        Direction ladderFacing = null;
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            for (int z = z0 + 1; z <= z1 - 1; z++) {
                if (Math.abs(x - r.centerX()) <= 2 || Math.abs(z - r.centerZ()) <= 2) continue;
                if (isInside(r, shape, x, z) && getWallDistance(r, shape, x, z) == 1) {
                    if (isWall(r, shape, x + 1, z)) { ladderPos = O(sp, x, 0, z); ladderFacing = Direction.EAST; break; }
                    if (isWall(r, shape, x - 1, z)) { ladderPos = O(sp, x, 0, z); ladderFacing = Direction.WEST; break; }
                    if (isWall(r, shape, x, z + 1)) { ladderPos = O(sp, x, 0, z); ladderFacing = Direction.SOUTH; break; }
                    if (isWall(r, shape, x, z - 1)) { ladderPos = O(sp, x, 0, z); ladderFacing = Direction.NORTH; break; }
                }
            }
            if (ladderPos != null) break;
        }

        if (ladderPos != null && ladderFacing != null) {
            net.minecraft.world.level.block.Block ladderBlock = QuarkDungeonDecorator.mezzanineLadder();
            BlockState ladderState = ladderBlock.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, ladderFacing);
            for (int y = 0; y <= 4; y++) {
                S(lv, ladderPos.above(y), ladderState);
            }
        }

        int cx = r.centerX(), cz = r.centerZ();
        if (x1 - x0 > 10 && z1 - z0 > 10) {
            int vertFeature = Math.floorMod(floor * 19 + r.index() * 31, 3);
            if (vertFeature == 0) {
                buildSuspendedPlatform(lv, sp, t, r, ceilH);
            } else if (vertFeature == 1) {
                buildCentralHazardPit(lv, sp, t, r);
            } else {
                buildSpiralStaircase(lv, sp, t, r);
            }
        }
    }

    // ───── Helpers NoiseShapeRoom ─────

    private static NoiseShapeRoom shapeForRoom(int floor, int roomIndex) {
        return new NoiseShapeRoom(IslandShaper.seedFor(floor), roomIndex);
    }

    private static boolean isInside(DungeonLayout.Room r, NoiseShapeRoom shape, int x, int z) {
        return shape.inside(x - r.minX(), z - r.minZ(), r.maxX() - r.minX(), r.maxZ() - r.minZ());
    }

    private static boolean isWall(DungeonLayout.Room r, NoiseShapeRoom shape, int x, int z) {
        if (x == r.minX() || x == r.maxX() || z == r.minZ() || z == r.maxZ()) return true;
        if (!isInside(r, shape, x, z)) return true;
        return !isInside(r, shape, x - 1, z) || !isInside(r, shape, x + 1, z)
            || !isInside(r, shape, x, z - 1) || !isInside(r, shape, x, z + 1);
    }

    private static int getWallDistance(DungeonLayout.Room r, NoiseShapeRoom shape, int x, int z) {
        if (isWall(r, shape, x, z)) return 0;
        int dist = 99;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (isWall(r, shape, x + dx, z + dz)) {
                    int d = Math.max(Math.abs(dx), Math.abs(dz));
                    if (d < dist) dist = d;
                }
            }
        }
        return dist;
    }

    private static void hangingChandelier(ServerLevel lv, BlockPos sp, BlockPalette t, int cx, int cz, int ceilH) {
        BlockState chain = QuarkDungeonDecorator.chandelierChain().defaultBlockState();
        BlockState body = B(t.decorPrimary());
        BlockState arm = B(t.wallBlock());
        BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true);
        Block macawLamp = MacawDungeonDecorator.chainLamp();
        if (macawLamp != null) {
            lantern = macawLamp.defaultBlockState();
        } else if (t.light() == Blocks.SOUL_LANTERN || t.light() == Blocks.SOUL_TORCH) {
            lantern = Blocks.SOUL_LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true);
        }

        for (int y = ceilH - 1; y >= ceilH - 3; y--) {
            S(lv, O(sp, cx, y, cz), chain);
        }

        S(lv, O(sp, cx, ceilH - 4, cz), body);
        S(lv, O(sp, cx + 1, ceilH - 4, cz), arm);
        S(lv, O(sp, cx - 1, ceilH - 4, cz), arm);
        S(lv, O(sp, cx, ceilH - 4, cz + 1), arm);
        S(lv, O(sp, cx, ceilH - 4, cz - 1), arm);

        S(lv, O(sp, cx + 1, ceilH - 5, cz), lantern);
        S(lv, O(sp, cx - 1, ceilH - 5, cz), lantern);
        S(lv, O(sp, cx, ceilH - 5, cz + 1), lantern);
        S(lv, O(sp, cx, ceilH - 5, cz - 1), lantern);
    }

    private static void archiveRoom(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int ceilH) {
        int cx = r.centerX(), cz = r.centerZ();
        Block bookshelf = QuarkDungeonDecorator.resolve("quark:ancient_bookshelf");
        BlockState shelf = B(bookshelf != null ? bookshelf : Blocks.BOOKSHELF);
        Block macawTable = MacawDungeonDecorator.oakDesk();
        BlockState table = B(macawTable != null ? macawTable : Blocks.DARK_OAK_SLAB);
        BlockState leg = macawTable != null ? AIR() : B(Blocks.DARK_OAK_FENCE);
        Block macawChair = MacawDungeonDecorator.oakChair();
        BlockState seat = macawChair != null ? B(macawChair) : stair(Blocks.OAK_STAIRS, Direction.SOUTH);
        BlockState seatNorth = macawChair != null ? B(macawChair) : stair(Blocks.OAK_STAIRS, Direction.NORTH);

        // Place rows of bookshelves in the corners
        int[][] positions = {
            {r.minX() + 4, r.minZ() + 4}, {r.maxX() - 8, r.minZ() + 4},
            {r.minX() + 4, r.maxZ() - 8}, {r.maxX() - 8, r.maxZ() - 8}
        };
        for (int[] p : positions) {
            for (int dx = 0; dx < 4; dx++) {
                for (int dz = 0; dz < 4; dz++) {
                    if (dx == 0 || dz == 0 || dx == 3 || dz == 3) {
                        S(lv, O(sp, p[0] + dx, 0, p[1] + dz), shelf);
                        S(lv, O(sp, p[0] + dx, 1, p[1] + dz), shelf);
                    }
                }
            }
        }

        // Reading desks in the center
        if (macawTable != null) {
            S(lv, O(sp, cx - 2, 0, cz), table);
            S(lv, O(sp, cx + 2, 0, cz), table);
        } else {
            S(lv, O(sp, cx - 2, 0, cz), leg);
            S(lv, O(sp, cx - 2, 1, cz), table);
            S(lv, O(sp, cx + 2, 0, cz), leg);
            S(lv, O(sp, cx + 2, 1, cz), table);
        }
        S(lv, O(sp, cx - 2, 0, cz + 1), seat);
        S(lv, O(sp, cx + 2, 0, cz - 1), seatNorth);

        // Lectern
        BlockPos lecternPos = O(sp, cx, 0, cz);
        S(lv, lecternPos, Blocks.LECTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LecternBlock.HAS_BOOK, true));
        if (lv.getBlockEntity(lecternPos) instanceof net.minecraft.world.level.block.entity.LecternBlockEntity lecternBE) {
            net.minecraft.world.item.ItemStack bookStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK);
            bookStack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("§6Secrets du Donjon"));
            bookStack.set(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                net.minecraft.network.chat.Component.literal("§7Le seul bloc de bois au sol révèle"),
                net.minecraft.network.chat.Component.literal("§7un passage secret vers le labyrinthe."),
                net.minecraft.network.chat.Component.literal("§7Les boutons cachés sur les piliers"),
                net.minecraft.network.chat.Component.literal("§7ouvrent les grilles scellées.")
            )));
            lecternBE.setBook(bookStack);
        }
    }

    private static void forgeRoom(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int ceilH) {
        int cx = r.centerX(), cz = r.centerZ();
        BlockState furnace = Blocks.BLAST_FURNACE.defaultBlockState();
        BlockState anvil = Blocks.ANVIL.defaultBlockState();
        BlockState magma = B(Blocks.MAGMA_BLOCK);
        BlockState slab = B(t.slab());

        // Central forge area
        S(lv, O(sp, cx, 0, cz), anvil);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            S(lv, O(sp, cx, -1, cz).relative(dir), Blocks.LAVA.defaultBlockState());
            S(lv, O(sp, cx, 0, cz).relative(dir), slab); // slab rim to walk on
        }

        // Furnaces against the walls
        int[][] positions = {
            {r.minX() + 6, cz - 2}, {r.minX() + 6, cz + 2},
            {r.maxX() - 6, cz - 2}, {r.maxX() - 6, cz + 2}
        };
        for (int[] p : positions) {
            S(lv, O(sp, p[0], 0, p[1]), furnace);
            S(lv, O(sp, p[0], 1, p[1]), magma);
            S(lv, O(sp, p[0], 0, p[1] - 1), B(Blocks.IRON_BARS));
            S(lv, O(sp, p[0], 0, p[1] + 1), B(Blocks.IRON_BARS));
        }
    }

    private static void cryptRoom(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int ceilH) {
        int cx = r.centerX(), cz = r.centerZ();
        BlockState slab = B(t.slab());
        BlockState skull = B(Blocks.SKELETON_SKULL);
        BlockState candle = Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 3).setValue(net.minecraft.world.level.block.CandleBlock.LIT, true);

        // Scattered sarcophagi
        int[][] positions = {
            {cx - 8, cz - 8}, {cx + 8, cz - 8},
            {cx - 8, cz + 8}, {cx + 8, cz + 8}
        };
        for (int[] p : positions) {
            // Sarcophagus structure
            S(lv, O(sp, p[0], 0, p[1]), B(t.base()));
            S(lv, O(sp, p[0] + 1, 0, p[1]), B(t.base()));
            S(lv, O(sp, p[0], 1, p[1]), slab);
            S(lv, O(sp, p[0] + 1, 1, p[1]), candle);
            S(lv, O(sp, p[0] - 1, 0, p[1]), skull);
        }
    }

    private static void prisonRoom(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int ceilH) {
        int cx = r.centerX(), cz = r.centerZ();
        BlockState bars = B(Blocks.IRON_BARS);
        BlockState hay = B(Blocks.HAY_BLOCK);
        BlockState chain = Blocks.CHAIN.defaultBlockState();
        Block macawJailDoor = MacawDungeonDecorator.ironJailDoor();

        // Prison cells in corners
        int[][] cells = {
            {r.minX() + 4, r.minZ() + 4}, {r.maxX() - 9, r.minZ() + 4},
            {r.minX() + 4, r.maxZ() - 9}, {r.maxX() - 9, r.maxZ() - 9}
        };
        for (int[] c : cells) {
            for (int dx = 0; dx < 5; dx++) {
                for (int dz = 0; dz < 5; dz++) {
                    boolean wall = dx == 0 || dz == 0;
                    if (wall) {
                        for (int y = 0; y <= 2; y++) {
                            if (macawJailDoor != null && dx == 2 && dz == 0) {
                                if (y == 0) {
                                    S(lv, O(sp, c[0] + dx, y, c[1] + dz), macawJailDoor.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
                                } else if (y == 1) {
                                    S(lv, O(sp, c[0] + dx, y, c[1] + dz), macawJailDoor.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
                                } else {
                                    S(lv, O(sp, c[0] + dx, y, c[1] + dz), bars);
                                }
                            } else {
                                S(lv, O(sp, c[0] + dx, y, c[1] + dz), bars);
                            }
                        }
                    }
                }
            }
            // Bedding & chains inside the cell
            S(lv, O(sp, c[0] + 2, 0, c[1] + 2), hay);
            S(lv, O(sp, c[0] + 2, 2, c[1] + 2), chain);
            S(lv, O(sp, c[0] + 2, 1, c[1] + 2), chain);
        }
    }

    private static void greenhouseRoom(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int ceilH) {
        int cx = r.centerX(), cz = r.centerZ();
        BlockState moss = B(Blocks.MOSS_BLOCK);
        BlockState leaves = B(Blocks.OAK_LEAVES);
        BlockState water = B(Blocks.WATER);

        // Plants and foliage growing in corners
        int[][] corners = {
            {r.minX() + 4, r.minZ() + 4}, {r.maxX() - 6, r.minZ() + 4},
            {r.minX() + 4, r.maxZ() - 6}, {r.maxX() - 6, r.maxZ() - 6}
        };
        for (int[] c : corners) {
            for (int dx = 0; dx <= 2; dx++) {
                for (int dz = 0; dz <= 2; dz++) {
                    S(lv, O(sp, c[0] + dx, 0, c[1] + dz), moss);
                    if (dx == 1 && dz == 1) {
                        S(lv, O(sp, c[0] + dx, 1, c[1] + dz), leaves);
                        S(lv, O(sp, c[0] + dx, 2, c[1] + dz), leaves);
                    }
                }
            }
        }

        // Overgrown center water feature
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                boolean rim = Math.abs(dx) == 4 || Math.abs(dz) == 4;
                if (rim) {
                    S(lv, O(sp, cx + dx, 0, cz + dz), moss);
                } else {
                    S(lv, O(sp, cx + dx, -1, cz + dz), water);
                    S(lv, O(sp, cx + dx, 0, cz + dz), B(Blocks.LILY_PAD));
                }
            }
        }
    }

    private static void treasuryRoom(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int ceilH) {
        int cx = r.centerX(), cz = r.centerZ();
        BlockState gold = B(Blocks.GOLD_BLOCK);
        BlockState lapis = B(Blocks.LAPIS_BLOCK);
        BlockState base = B(t.base());
        BlockState slab = B(t.slab());

        // Wealthy floor pattern in the center
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean alt = (dx + dz) % 2 == 0;
                S(lv, O(sp, cx + dx, -1, cz + dz), alt ? gold : lapis);
            }
        }

        // Showoff pedestals on the sides of the pattern
        int[][] pedestals = {{-4, 0}, {4, 0}, {0, -4}, {0, 4}};
        for (int[] p : pedestals) {
            BlockPos b = O(sp, cx + p[0], 0, cz + p[1]);
            S(lv, b, base);
            S(lv, b.above(), slab);
            S(lv, b.above(2), gold);
        }
    }

    private static void buildSuspendedPlatform(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int ceilH) {
        int cx = r.centerX(), cz = r.centerZ();
        BlockState platBlock = B(t.accent());
        BlockState chain = Blocks.CHAIN.defaultBlockState();
        BlockState air = AIR();
        Block quarkRailing = QuarkDungeonDecorator.mezzanineRailing();
        Block macawRailing = MacawDungeonDecorator.oakStockadeFence();
        BlockState railing = macawRailing != null ? B(macawRailing) : (quarkRailing != null ? B(quarkRailing) : B(t.wallBlock()));

        // Platform 7x7 at y = 4
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                S(lv, O(sp, cx + dx, 4, cz + dz), platBlock);
                S(lv, O(sp, cx + dx, 5, cz + dz), air);
                S(lv, O(sp, cx + dx, 6, cz + dz), air);
            }
        }
        // Railings around the platform (with openings for bridges)
        for (int dx = -3; dx <= 3; dx++) {
            if (dx != 0) {
                S(lv, O(sp, cx + dx, 5, cz - 3), railing);
                S(lv, O(sp, cx + dx, 5, cz + 3), railing);
            }
        }
        for (int dz = -3; dz <= 3; dz++) {
            if (dz != 0) {
                S(lv, O(sp, cx - 3, 5, cz + dz), railing);
                S(lv, O(sp, cx + 3, 5, cz + dz), railing);
            }
        }

        // Hanging chains at the 4 corners from ceiling down to y=5
        int[][] corners = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}};
        for (int[] c : corners) {
            for (int y = 5; y < ceilH; y++) {
                S(lv, O(sp, cx + c[0], y, cz + c[1]), chain);
            }
        }

        // Bridges connecting the platform to the room walls
        Block macawBridge = MacawDungeonDecorator.oakRopeBridge();
        BlockState bridgeMat = macawBridge != null ? B(macawBridge) : platBlock;
        BlockState bridgeRailing = macawBridge != null ? AIR() : railing;

        // Walkway bridges along X axis
        for (int x = r.minX() + 2; x < cx - 3; x++) {
            S(lv, O(sp, x, 4, cz), bridgeMat);
            S(lv, O(sp, x, 5, cz - 1), bridgeRailing);
            S(lv, O(sp, x, 5, cz + 1), bridgeRailing);
        }
        for (int x = cx + 4; x <= r.maxX() - 2; x++) {
            S(lv, O(sp, x, 4, cz), bridgeMat);
            S(lv, O(sp, x, 5, cz - 1), bridgeRailing);
            S(lv, O(sp, x, 5, cz + 1), bridgeRailing);
        }
    }

    private static void buildCentralHazardPit(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r) {
        int cx = r.centerX(), cz = r.centerZ();
        BlockState fluid = (t.light() == Blocks.SHROOMLIGHT || t.base() == Blocks.POLISHED_BLACKSTONE_BRICKS) ? Blocks.LAVA.defaultBlockState() : Blocks.WATER.defaultBlockState();
        BlockState border = B(t.slab());

        // 7x7 pit in the floor
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                if (rim) {
                    S(lv, O(sp, cx + dx, 0, cz + dz), border);
                } else {
                    S(lv, O(sp, cx + dx, -1, cz + dz), fluid);
                    S(lv, O(sp, cx + dx, -2, cz + dz), B(t.underside()));
                }
            }
        }
    }

    private static void buildSpiralStaircase(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r) {
        int cx = r.centerX(), cz = r.centerZ();
        BlockState pillar = B(t.decorPrimary());

        // Central pillar 3x3
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = 0; y <= 5; y++) {
                    S(lv, O(sp, cx + dx, y, cz + dz), pillar);
                }
            }
        }

        // Spiral stairs wrapping around the 3x3 pillar
        // y=0: South side facing North
        S(lv, O(sp, cx, 0, cz + 2), stair(t.stair(), Direction.NORTH));
        S(lv, O(sp, cx + 1, 0, cz + 2), stair(t.stair(), Direction.NORTH));

        // y=1: East side facing West
        S(lv, O(sp, cx + 2, 1, cz + 1), stair(t.stair(), Direction.WEST));
        S(lv, O(sp, cx + 2, 1, cz), stair(t.stair(), Direction.WEST));

        // y=2: North side facing South
        S(lv, O(sp, cx + 1, 2, cz - 2), stair(t.stair(), Direction.SOUTH));
        S(lv, O(sp, cx, 2, cz - 2), stair(t.stair(), Direction.SOUTH));

        // y=3: West side facing East
        S(lv, O(sp, cx - 2, 3, cz - 1), stair(t.stair(), Direction.EAST));
        S(lv, O(sp, cx - 2, 3, cz), stair(t.stair(), Direction.EAST));

        // y=4: South side landing
        S(lv, O(sp, cx - 1, 4, cz + 2), B(t.slab()));
        S(lv, O(sp, cx, 4, cz + 2), B(t.slab()));
    }

    /**
     * Porte de passage entre deux pièces : porte Macaw's <b>assortie au thème de l'étage</b> si
     * présente, sinon porte en bois vanilla. <b>Jamais de porte en fer ni de levier</b> (feedback
     * playtest 2026-07-09 : les mécanismes redstone cassaient le flow) — toutes les portes du
     * donjon s'ouvrent à la main.
     */
    private static void placeDoorWayDoor(ServerLevel lv, BlockPos doorPos, BlockPos bars1, BlockPos bars2, Direction facing, BlockPalette t) {
        Block macawDoor = t instanceof ThemePalette theme
                ? MacawDungeonDecorator.themedDoor(theme)
                : MacawDungeonDecorator.whisperOakDoor();
        if (macawDoor == null) macawDoor = MacawDungeonDecorator.whisperOakDoor();
        Block doorBlock = macawDoor != null ? macawDoor : Blocks.SPRUCE_DOOR;

        // Place door blocks
        BlockState doorLower = doorBlock.defaultBlockState()
                .setValue(net.minecraft.world.level.block.DoorBlock.FACING, facing)
                .setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
        BlockState doorUpper = doorBlock.defaultBlockState()
                .setValue(net.minecraft.world.level.block.DoorBlock.FACING, facing)
                .setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);

        S(lv, doorPos, doorLower);
        S(lv, doorPos.above(), doorUpper);

        // Place side bars / decor
        BlockState barMat = B(Blocks.IRON_BARS);
        for (int y = 0; y <= 2; y++) {
            S(lv, bars1.above(y), barMat);
            S(lv, bars2.above(y), barMat);
        }
    }

    private static void placeSecretVaultPuzzle(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int floor) {
        // Vault entrance at North wall
        int vx = r.minX() + 8;
        int vz = r.minZ();

        // (1) Carve alcove (2x3x2 air space behind the wall)
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = -2; dz <= 0; dz++) {
                for (int y = 0; y <= 2; y++) {
                    S(lv, O(sp, vx + dx, y, vz + dz), AIR());
                }
            }
        }

        // (2) Place walls/ceiling around the alcove so it's not open to the void
        BlockState wallState = B(t.base());
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -3; dz <= 1; dz++) {
                for (int y = -1; y <= 3; y++) {
                    boolean isInside = (dx >= 0 && dx <= 1) && (dz >= -2 && dz <= 0) && (y >= 0 && y <= 2);
                    if (!isInside) {
                        S(lv, O(sp, vx + dx, y, vz + dz), wallState);
                    }
                }
            }
        }

        // (3) Porte de l'alcôve : Macaw's assortie au thème si présente, sinon bois vanilla —
        // ouvrable à la MAIN. (L'ancien puzzle porte de fer + bouton + poudre de redstone sous le
        // sol ne fonctionnait pas de manière fiable et cassait le flow — playtest 2026-07-09.)
        Block macawDoor = t instanceof ThemePalette theme
                ? MacawDungeonDecorator.themedDoor(theme)
                : MacawDungeonDecorator.whisperOakDoor();
        if (macawDoor == null) macawDoor = MacawDungeonDecorator.whisperOakDoor();
        Block vaultDoor = macawDoor != null ? macawDoor : Blocks.SPRUCE_DOOR;
        BlockState doorLower = vaultDoor.defaultBlockState()
                .setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.SOUTH)
                .setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
        BlockState doorUpper = vaultDoor.defaultBlockState()
                .setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.SOUTH)
                .setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
        S(lv, O(sp, vx, 0, vz), doorLower);
        S(lv, O(sp, vx, 1, vz), doorUpper);

        // Place iron bars next to the door for style
        for (int y = 0; y <= 2; y++) {
            S(lv, O(sp, vx + 1, y, vz), Blocks.IRON_BARS.defaultBlockState());
        }

        // (4) Place valuable chest inside
        DungeonArchitect.placeChest(lv, O(sp, vx, 0, vz - 2));
    }

    // ───── Phase 2 : helpers pour RoomLike / RoomProvider ─────

    /** Compatibilité grille 5×4 : utilise {@link DungeonLayout#ROOM_COUNT}. */
    public static int roomYOffset(int roomIndex, int floor) {
        return roomYOffset(roomIndex, DungeonLayout.ROOM_COUNT, floor);
    }

    /** Décalage vertical d'une pièce dans un layout à {@code roomCount} pièces. */
    public static int roomYOffset(int roomIndex, int roomCount, int floor) {
        java.util.Random rng = new java.util.Random(floor * 997L + 123L);
        int[] offsets = new int[roomCount];
        offsets[0] = 0;
        for (int i = 1; i < roomCount; i++) {
            int step = rng.nextInt(3) - 1;
            offsets[i] = offsets[i - 1] + step * 3;
            if (offsets[i] < -6) offsets[i] = -6;
            if (offsets[i] > 9) offsets[i] = 9;
        }
        return offsets[roomIndex];
    }

    private static DungeonLayout.Dir exitDirToward(RoomLike from, RoomLike to) {
        int dx = to.centerX() - from.centerX();
        int dz = to.centerZ() - from.centerZ();
        if (Math.abs(dx) >= Math.abs(dz)) return dx > 0 ? DungeonLayout.Dir.EAST : DungeonLayout.Dir.WEST;
        return dz > 0 ? DungeonLayout.Dir.SOUTH : DungeonLayout.Dir.NORTH;
    }

    private static DungeonLayout.Dir exitDirToFirst(RoomLike r, List<? extends RoomLike> all) {
        int[] conn = r.connectedTo();
        if (conn.length == 0) return DungeonLayout.Dir.NONE;
        RoomLike target = null;
        for (RoomLike candidate : all) {
            if (candidate.index() == conn[0]) { target = candidate; break; }
        }
        return target != null ? exitDirToward(r, target) : DungeonLayout.Dir.NONE;
    }

    private static DungeonLayout.Room toLayoutRoom(RoomLike r) {
        return toLayoutRoom(r, java.util.Collections.emptyList());
    }

    private static DungeonLayout.Room toLayoutRoom(RoomLike r, List<? extends RoomLike> all) {
        DungeonLayout.Dir dir = all.isEmpty() ? DungeonLayout.Dir.NONE : exitDirToFirst(r, all);
        return new DungeonLayout.Room(r.index(), 0, 0, r.minX(), r.maxX(), r.minZ(), r.maxZ(),
                dir, r.isFirst(), r.isLast());
    }

    private static void shellFromLike(ServerLevel lv, BlockPos sp, BlockPalette t, RoomLike r, int floor) {
        List<? extends RoomLike> all = java.util.Collections.singletonList(r);
        shell(lv, sp, t, toLayoutRoom(r, all), floor);
    }

    private static void carveDoorFromLike(ServerLevel lv, BlockPos sp, BlockPalette t, RoomLike r, int floor) {
        carveDoor(lv, sp, t, toLayoutRoom(r), floor);
    }

    private static int roomCeilingHeight(int floor, RoomLike r, int roomCount) {
        int mid = roomCount / 2;
        boolean isCenter = r.index() == mid;
        if (isCenter) return ceilingHeight(floor, r.index());
        int idx = r.index() > mid ? r.index() - 1 : r.index();
        return ceilingHeight(floor, idx);
    }

    private static void lightRoomFromLike(ServerLevel lv, BlockPos sp, BlockPalette t, RoomLike r, int ceilH) {
        lightRoom(lv, sp, t, toLayoutRoom(r), ceilH);
    }

    private static void placePathwayFromLike(ServerLevel lv, BlockPos sp, BlockPalette t, RoomLike r, int floor) {
        placePathway(lv, sp, t, toLayoutRoom(r), floor);
    }

    // ── Phase 5: optional NBT template decoration overlay ──

    private static void tryPlaceTemplate(ServerLevel lv, BlockPos sp,
                                          DungeonLayout.Room r, BlockPalette t, int floor) {
        if (!TemplateRegistry.isLoaded()) return;
        String pool = "combat";
        int cx = r.centerX();
        int cz = r.centerZ();
        RoomTemplate tmpl = TemplateRegistry.select(pool, floor, cx, cz);
        if (tmpl == null) return;
        DungeonMaterial mat = DungeonMaterial.fromPalette(t);
        int ox = cx - tmpl.width() / 2;
        int oz = cz - tmpl.depth() / 2;
        BlockPos origin = sp.offset(ox, 0, oz);
        tmpl.place(lv, origin, Direction.NORTH, mat, TemplateProperty.EMPTY);
    }

    private static void tryPlaceTemplate(ServerLevel lv, BlockPos sp,
                                          RoomLike r, BlockPalette t, int floor) {
        if (!TemplateRegistry.isLoaded()) return;
        String pool = "combat";
        int cx = r.centerX();
        int cz = r.centerZ();
        RoomTemplate tmpl = TemplateRegistry.select(pool, floor, cx, cz);
        if (tmpl == null) return;
        DungeonMaterial mat = DungeonMaterial.fromPalette(t);
        int ox = cx - tmpl.width() / 2;
        int oz = cz - tmpl.depth() / 2;
        BlockPos origin = sp.offset(ox, 0, oz);
        tmpl.place(lv, origin, Direction.NORTH, mat, TemplateProperty.EMPTY);
    }
}
