package tong.statmod.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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
     * Construit tout l'intérieur d'un étage en chaîne de pièces. La dernière pièce dépend du rôle :
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
            if (r.exitDoor() != DungeonLayout.Dir.NONE) carveDoor(lv, sp, t, r);
        }

        // (3) Contenu par pièce.
        for (DungeonLayout.Room r : rooms) {
            int ceilH = roomCeilingHeight(floor, r);
            lightRoom(lv, sp, t, r, ceilH);
            if (r.isFirst()) {
                spawnPad(lv, sp, t, r);
            } else if (r.isLast()) {
                // BOSS ne passe PAS par la chaîne (arène dédiée géante) ; sécurité → exitRoom.
                if (role == DungeonArchitect.Role.TREASURE) treasureRoom(lv, sp, t, floor, r);
                else exitRoom(lv, sp, t, r);
            } else if (role == DungeonArchitect.Role.TREASURE) {
                // Étage trésor : les pièces libres deviennent un MARCHÉ (stands de marchands bloqués).
                DungeonMerchant.placeStall(lv, sp, r.centerX(), r.centerZ(), t,
                        r.index() % DungeonMerchant.STALL_KINDS, floor);
            } else {
                decorateCombatRoom(lv, sp, t, r, floor, ceilH);
            }
        }
    }

    /** Hauteur de plafond d'une pièce (spawn/sortie = standard ; sinon variée). */
    private static int roomCeilingHeight(int floor, DungeonLayout.Room r) {
        return (r.isFirst() || r.isLast()) ? WALL_H : ceilingHeight(floor, r.index());
    }

    // ═══════════════ coque d'une pièce (forme + hauteur variables) ═══════════════

    /** Silhouette au sol d'une pièce (varie la FORME, pas seulement la déco intérieure). */
    private enum Shape { RECT, OCTAGON, CHAMFER, CROSS }

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
        Shape shape = fixed ? Shape.RECT : shapeFor(floor, r.index());

        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                S(lv, O(sp, x, -1, z), variedFloor(t, x, z));   // sol continu (toute la case)
                boolean in = inside(r, x, z, shape);
                if (!in) {
                    // Coin « en trop » : masse rocheuse pleine jusqu'au plafond (change la silhouette).
                    for (int y = 0; y <= ceilH; y++) S(lv, O(sp, x, y, z), variedWall(t, x, y, z));
                    continue;
                }
                S(lv, O(sp, x, ceilH, z), B(t.ceiling()));       // plafond à hauteur variable
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1
                        || !inside(r, x - 1, z, shape) || !inside(r, x + 1, z, shape)
                        || !inside(r, x, z - 1, shape) || !inside(r, x, z + 1, shape);
                if (edge) {
                    for (int y = 0; y < ceilH; y++) S(lv, O(sp, x, y, z), variedWall(t, x, y, z));
                    S(lv, O(sp, x, 0, z), B(t.accent()));        // plinthe au ras du sol
                } else {
                    for (int y = 0; y < ceilH; y++) S(lv, O(sp, x, y, z), AIR());
                }
            }
        }
    }

    /** Hauteur de plafond variée : crypte basse (8) → grande salle (20). */
    private static int ceilingHeight(int floor, int idx) {
        int[] h = {WALL_H - 4, WALL_H, WALL_H + 4, WALL_H + 8}; // 8, 12, 16, 20
        return h[Math.floorMod(floor * 13 + idx * 29, h.length)];
    }

    /** Silhouette variée déterministe. */
    private static Shape shapeFor(int floor, int idx) {
        Shape[] s = Shape.values();
        return s[Math.floorMod(floor * 17 + idx * 11, s.length)];
    }

    /**
     * {@code true} si (x,z) est à l'intérieur de la pièce selon sa silhouette. Toutes les formes ne
     * coupent que les COINS → les 4 milieux de bords restent pleins (portes toujours praticables).
     */
    private static boolean inside(DungeonLayout.Room r, int x, int z, Shape shape) {
        int w = r.maxX() - r.minX(), d = r.maxZ() - r.minZ();
        int lx = x - r.minX(), lz = z - r.minZ();
        if (lx < 0 || lz < 0 || lx > w || lz > d) return false;
        int ax = Math.min(lx, w - lx); // distance au mur X le plus proche
        int az = Math.min(lz, d - lz); // distance au mur Z le plus proche
        return switch (shape) {
            case RECT -> true;
            case OCTAGON -> ax + az >= 5;          // petits pans coupés
            case CHAMFER -> ax + az >= 9;          // grands pans coupés
            case CROSS -> !(ax < 9 && az < 9);     // coins carrés retirés → croix
        };
    }

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
        if (n < 12) return B(t.accent());
        if (n < 18) return B(t.decorPrimary());
        return B(t.base());
    }

    /** Bloc de sol varié : surtout {@code base}, parfois accent — blocs pleins sûrs uniquement
     *  (jamais scar/decor qui peuvent être glace-poudreuse, magma ou toile → dangereux au sol). */
    private static BlockState variedFloor(BlockPalette t, int x, int z) {
        int n = noise(x, 7, z);
        return n < 16 ? B(t.accent()) : B(t.base());
    }

    // ═══════════════ portes ═══════════════

    /** Perce une porte (largeur 3, hauteur DOOR_H) dans le mur de sortie de la pièce vers la suivante. */
    private static void carveDoor(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r) {
        int cx = r.centerX(), cz = r.centerZ();
        BlockState frame = B(t.decorPrimary());
        switch (r.exitDoor()) {
            case EAST -> doorZ(lv, sp, frame, r.maxX() - PAD, r.maxX() - PAD + 1 + 2 * PAD, cz); // traverse les 2 murs mitoyens
            case WEST -> doorZ(lv, sp, frame, r.minX() + PAD - 1 - 2 * PAD, r.minX() + PAD, cz);
            case SOUTH -> doorX(lv, sp, frame, r.maxZ() - PAD, r.maxZ() - PAD + 1 + 2 * PAD, cx);
            case NORTH -> doorX(lv, sp, frame, r.minZ() + PAD - 1 - 2 * PAD, r.minZ() + PAD, cx);
            case NONE -> { }
        }
    }

    /** Ouverture sur un plan X constant (porte est/ouest) : perce de xa à xb à z=cz±DOOR_HALF. */
    private static void doorZ(ServerLevel lv, BlockPos sp, BlockState frame, int xa, int xb, int cz) {
        int lo = Math.min(xa, xb), hi = Math.max(xa, xb);
        for (int x = lo; x <= hi; x++) {
            for (int dz = -DOOR_HALF; dz <= DOOR_HALF; dz++) {
                for (int y = 0; y < DOOR_H; y++) S(lv, O(sp, x, y, cz + dz), AIR());
            }
            // Linteau + montants de cadre.
            S(lv, O(sp, x, DOOR_H, cz - DOOR_HALF), frame);
            S(lv, O(sp, x, DOOR_H, cz + DOOR_HALF), frame);
        }
    }

    /** Ouverture sur un plan Z constant (porte nord/sud) : perce de za à zb à x=cx±DOOR_HALF. */
    private static void doorX(ServerLevel lv, BlockPos sp, BlockState frame, int za, int zb, int cx) {
        int lo = Math.min(za, zb), hi = Math.max(za, zb);
        for (int z = lo; z <= hi; z++) {
            for (int dx = -DOOR_HALF; dx <= DOOR_HALF; dx++) {
                for (int y = 0; y < DOOR_H; y++) S(lv, O(sp, cx + dx, y, z), AIR());
            }
            S(lv, O(sp, cx - DOOR_HALF, DOOR_H, z), frame);
            S(lv, O(sp, cx + DOOR_HALF, DOOR_H, z), frame);
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
        int cx = r.centerX(), cz = r.centerZ();
        S(lv, O(sp, cx, ceilH, cz), light); // plafond central (remplace le plafond → supporté)
        int qx = Math.max(4, (r.maxX() - r.minX()) / 4);
        int qz = Math.max(4, (r.maxZ() - r.minZ()) / 4);
        for (int dx : new int[]{-qx, qx}) {
            S(lv, O(sp, cx + dx, 4, r.minZ()), light);
            S(lv, O(sp, cx + dx, 4, r.maxZ()), light);
        }
        for (int dz : new int[]{-qz, qz}) {
            S(lv, O(sp, r.minX(), 4, cz + dz), light);
            S(lv, O(sp, r.maxX(), 4, cz + dz), light);
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
        // Escaliers d'accès sur les 4 côtés.
        S(lv, O(sp, cx, 0, cz - 2), stair(t.stair(), Direction.SOUTH));
        S(lv, O(sp, cx, 0, cz + 2), stair(t.stair(), Direction.NORTH));
        S(lv, O(sp, cx - 2, 0, cz), stair(t.stair(), Direction.EAST));
        S(lv, O(sp, cx + 2, 0, cz), stair(t.stair(), Direction.WEST));
        // Le bloc de descente.
        S(lv, O(sp, cx, 0, cz), B(DungeonBlocks.NEXT_FLOOR_TELEPORTER.get()));
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
        for (int[] c : new int[][]{{-2, 0}, {2, 0}}) {
            for (int y = 0; y <= 3; y++) S(lv, O(sp, cx + c[0], y, tz), pillar);
            S(lv, O(sp, cx + c[0], 4, tz), B(t.light()));
        }
        S(lv, O(sp, cx, 0, tz), B(DungeonBlocks.NEXT_FLOOR_TELEPORTER.get()));
    }

    /**
     * Décore une pièce de combat avec une <b>forme intérieure variée</b> (déterministe par étage +
     * index de pièce) : hall à piliers, estrade centrale, anneau de colonnes, bassin thématique,
     * ou quatre grands piliers d'angle. Toutes restent traversables (aucune ne bloque les portes).
     */
    private static void decorateCombatRoom(ServerLevel lv, BlockPos sp, BlockPalette t,
                                           DungeonLayout.Room r, int floor, int ceilH) {
        int variant = Math.floorMod(floor * 31 + r.index() * 7, 5);
        switch (variant) {
            case 0 -> pillarHall(lv, sp, t, r, ceilH);
            case 1 -> centralDais(lv, sp, t, r);
            case 2 -> columnRing(lv, sp, t, r, ceilH);
            case 3 -> themedPool(lv, sp, t, r);
            default -> quadPillars(lv, sp, t, r, ceilH);
        }
    }

    /** Hall à piliers : grille de colonnes montant jusqu'au plafond de la pièce, allées libres. */
    private static void pillarHall(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int ceilH) {
        BlockState pillar = B(t.decorPrimary());
        for (int x = r.minX() + 4; x <= r.maxX() - 4; x += 6) {
            for (int z = r.minZ() + 4; z <= r.maxZ() - 4; z += 6) {
                for (int y = 0; y < ceilH - 1; y++) S(lv, O(sp, x, y, z), pillar);
                S(lv, O(sp, x, ceilH - 1, z), B(t.accent()));
            }
        }
    }

    /** Estrade centrale surélevée (7×7) avec 4 accès en escalier — relief central. */
    private static void centralDais(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r) {
        int cx = r.centerX(), cz = r.centerZ();
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 3;
            S(lv, O(sp, cx + dx, 0, cz + dz), rim ? B(t.slab()) : B(t.accent()));
        }
        S(lv, O(sp, cx, 1, cz - 4), stair(t.stair(), Direction.SOUTH));
        S(lv, O(sp, cx, 1, cz + 4), stair(t.stair(), Direction.NORTH));
        S(lv, O(sp, cx - 4, 1, cz), stair(t.stair(), Direction.EAST));
        S(lv, O(sp, cx + 4, 1, cz), stair(t.stair(), Direction.WEST));
        S(lv, O(sp, cx, 1, cz), B(t.light()));
    }

    /** Anneau de colonnes autour du centre (8 colonnes montant au plafond) — arène circulaire. */
    private static void columnRing(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int ceilH) {
        int cx = r.centerX(), cz = r.centerZ();
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int dx = (int) Math.round(Math.cos(a) * 6);
            int dz = (int) Math.round(Math.sin(a) * 6);
            for (int y = 0; y < ceilH - 2; y++) S(lv, O(sp, cx + dx, y, cz + dz), B(t.decorPrimary()));
            S(lv, O(sp, cx + dx, ceilH - 2, cz + dz), B(t.light()));
        }
    }

    /** Bassin thématique 5×5 encastré dans le sol, bordé de dalles. */
    private static void themedPool(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r) {
        int cx = r.centerX(), cz = r.centerZ();
        boolean infernal = t.light() == Blocks.SHROOMLIGHT || t.base() == Blocks.POLISHED_BLACKSTONE_BRICKS;
        BlockState fluid = infernal ? B(Blocks.LAVA) : B(Blocks.WATER);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            if (rim) S(lv, O(sp, cx + dx, 0, cz + dz), B(t.slab()));
            else {
                S(lv, O(sp, cx + dx, -1, cz + dz), fluid);
                S(lv, O(sp, cx + dx, -2, cz + dz), B(t.accent()));
            }
        }
    }

    /** Quatre grands piliers d'angle (2×2) montant au plafond — cadre imposant, centre dégagé. */
    private static void quadPillars(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r, int ceilH) {
        int ox = Math.max(3, (r.maxX() - r.minX()) / 3);
        int oz = Math.max(3, (r.maxZ() - r.minZ()) / 3);
        for (int[] c : new int[][]{{-ox, -oz}, {ox, -oz}, {-ox, oz}, {ox, oz}}) {
            for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) {
                for (int y = 0; y < ceilH - 1; y++)
                    S(lv, O(sp, r.centerX() + c[0] + dx, y, r.centerZ() + c[1] + dz), B(t.decorPrimary()));
            }
        }
    }

    // ═══════════════ positions monde (pour spawn joueur / mobs) ═══════════════

    /** Position monde du centre de la pièce d'apparition (où téléporter le joueur), y = pad+1. */
    public static BlockPos spawnWorldPos(BlockPos islandCenter) {
        DungeonLayout.Room r = DungeonLayout.spawnRoom();
        return islandCenter.offset(r.centerX(), 1, r.centerZ());
    }

    /** Centres monde des pièces de combat (toutes sauf la pièce d'apparition), pour répartir la vague. */
    public static List<BlockPos> combatRoomCenters(BlockPos islandCenter) {
        List<BlockPos> out = new java.util.ArrayList<>();
        for (DungeonLayout.Room r : DungeonLayout.rooms()) {
            if (r.isFirst()) continue; // pas de mobs dans la pièce d'apparition
            out.add(islandCenter.offset(r.centerX(), 0, r.centerZ()));
        }
        return out;
    }
}
