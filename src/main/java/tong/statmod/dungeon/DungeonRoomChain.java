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
        for (DungeonLayout.Room r : rooms) shell(lv, sp, t, r);

        // (2) Portes entre pièces consécutives (après les coques → jamais re-murées).
        for (DungeonLayout.Room r : rooms) {
            if (r.exitDoor() != DungeonLayout.Dir.NONE) carveDoor(lv, sp, t, r);
        }

        // (3) Contenu par pièce.
        for (DungeonLayout.Room r : rooms) {
            lightRoom(lv, sp, t, r);
            if (r.isFirst()) {
                spawnPad(lv, sp, t, r);
            } else if (r.isLast()) {
                // BOSS ne passe PAS par la chaîne (arène dédiée géante) ; sécurité → exitRoom.
                if (role == DungeonArchitect.Role.TREASURE) treasureRoom(lv, sp, t, floor, r);
                else exitRoom(lv, sp, t, r);
            } else {
                combatDressing(lv, sp, t, r, floor, rng);
            }
        }
    }

    // ═══════════════ coque d'une pièce ═══════════════

    /** Sol (y=-1), murs pleins (y=0..WALL_H-1), plafond (y=WALL_H), intérieur dégagé. */
    private static void shell(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r) {
        int x0 = r.minX() + PAD, x1 = r.maxX() - PAD;
        int z0 = r.minZ() + PAD, z1 = r.maxZ() - PAD;
        BlockState floorB = B(t.base());
        BlockState wallB = B(t.wallBlock());
        BlockState ceilB = B(t.ceiling());

        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = (x == x0 || x == x1 || z == z0 || z == z1);
                S(lv, O(sp, x, -1, z), floorB);                 // sol
                S(lv, O(sp, x, WALL_H, z), ceilB);              // plafond
                if (edge) {
                    for (int y = 0; y < WALL_H; y++) S(lv, O(sp, x, y, z), wallB);
                } else {
                    for (int y = 0; y < WALL_H; y++) S(lv, O(sp, x, y, z), AIR());
                }
            }
        }
        // Accent au ras du sol le long des murs (plinthe) — donne du relief.
        BlockState accent = B(t.accent());
        for (int x = x0; x <= x1; x++) {
            S(lv, O(sp, x, 0, z0), accent);
            S(lv, O(sp, x, 0, z1), accent);
        }
        for (int z = z0; z <= z1; z++) {
            S(lv, O(sp, x0, 0, z), accent);
            S(lv, O(sp, x1, 0, z), accent);
        }
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

    /** Éclairage : une source lumineuse au plafond au centre + aux angles bas. */
    private static void lightRoom(ServerLevel lv, BlockPos sp, BlockPalette t, DungeonLayout.Room r) {
        BlockState light = B(t.light());
        S(lv, O(sp, r.centerX(), WALL_H - 1, r.centerZ()), light);
        int x0 = r.minX() + PAD + 1, x1 = r.maxX() - PAD - 1;
        int z0 = r.minZ() + PAD + 1, z1 = r.maxZ() - PAD - 1;
        for (int[] c : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
            S(lv, O(sp, c[0], 2, c[1]), light);
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

    /** Pièce de combat intermédiaire : quelques colonnes + décor (les mobs viennent du spawner). */
    private static void combatDressing(ServerLevel lv, BlockPos sp, BlockPalette t,
                                       DungeonLayout.Room r, int floor, Random rng) {
        BlockState pillar = B(t.decorPrimary());
        // 2 colonnes décoratives placées symétriquement (pas au centre → laisse la place au combat).
        int ox = Math.max(2, (r.maxX() - r.minX()) / 4);
        int oz = Math.max(2, (r.maxZ() - r.minZ()) / 4);
        for (int[] c : new int[][]{{-ox, -oz}, {ox, oz}}) {
            for (int y = 0; y < WALL_H - 2; y++) S(lv, O(sp, r.centerX() + c[0], y, r.centerZ() + c[1]), pillar);
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
