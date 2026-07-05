package tong.statmod.dungeon;

import java.util.ArrayList;
import java.util.List;

import static tong.statmod.dungeon.DungeonArchitect.HX;
import static tong.statmod.dungeon.DungeonArchitect.HZ;

/**
 * Mission M6 — Layout en <b>chaîne de pièces</b> (serpentin, 2026-07-05).
 *
 * <p>Un étage n'est plus une seule grande salle : c'est une <b>grille de pièces</b> traversée par
 * un <b>chemin serpentin</b> (boustrophedon) déterministe. On apparaît dans la 1ʳᵉ pièce, on
 * progresse de pièce en pièce (portes entre pièces consécutives), et la <b>dernière</b> pièce
 * contient le bloc pour descendre à l'étage suivant.
 *
 * <p>Géométrie pure (aucune dépendance monde) → testable. Les coordonnées sont <b>locales</b> au
 * centre de l'île ({@code sp} dans {@link DungeonArchitect}). Repère : x∈[-HX,HX], z∈[-HZ,HZ].
 */
public final class DungeonLayout {

    /** Colonnes / rangées de la grille de pièces. */
    public static final int COLS = 4;
    public static final int ROWS = 3;
    /** Nombre total de pièces d'un étage. */
    public static final int ROOM_COUNT = COLS * ROWS;

    /** Direction cardinale d'une porte (vers la pièce suivante). */
    public enum Dir { NORTH, SOUTH, EAST, WEST, NONE }

    /** Une pièce : sa case (col,row), ses bornes locales, et la direction de sa porte de sortie. */
    public record Room(int index, int col, int row,
                       int minX, int maxX, int minZ, int maxZ,
                       Dir exitDoor, boolean isFirst, boolean isLast) {
        public int centerX() { return (minX + maxX) / 2; }
        public int centerZ() { return (minZ + maxZ) / 2; }
    }

    private DungeonLayout() {}

    private static int cellW() { return (2 * HX) / COLS; }
    private static int cellD() { return (2 * HZ) / ROWS; }

    /** Bornes X locales de la colonne {@code col}. */
    private static int colMinX(int col) { return -HX + col * cellW(); }
    private static int colMaxX(int col) { return -HX + (col + 1) * cellW() - 1; }
    /** Bornes Z locales de la rangée {@code row}. */
    private static int rowMinZ(int row) { return -HZ + row * cellD(); }
    private static int rowMaxZ(int row) { return -HZ + (row + 1) * cellD() - 1; }

    /** Case (col,row) de la n-ième pièce du chemin serpentin (boustrophedon). */
    private static int[] cellAtPathIndex(int index) {
        int row = index / COLS;
        int posInRow = index % COLS;
        int col = (row % 2 == 0) ? posInRow : (COLS - 1 - posInRow); // rangées impaires inversées
        return new int[]{col, row};
    }

    /** Direction de la porte reliant la pièce {@code index} à la suivante ({@code index+1}). */
    private static Dir exitDoorFor(int index) {
        if (index >= ROOM_COUNT - 1) return Dir.NONE; // dernière pièce : pas de porte de sortie
        int[] a = cellAtPathIndex(index);
        int[] b = cellAtPathIndex(index + 1);
        if (b[1] > a[1]) return Dir.SOUTH;   // descend d'une rangée (+z)
        if (b[0] > a[0]) return Dir.EAST;    // avance d'une colonne (+x)
        if (b[0] < a[0]) return Dir.WEST;    // recule d'une colonne (-x)
        return Dir.NORTH;
    }

    /** Construit toutes les pièces de l'étage, dans l'ordre du chemin serpentin. */
    public static List<Room> rooms() {
        List<Room> out = new ArrayList<>(ROOM_COUNT);
        for (int i = 0; i < ROOM_COUNT; i++) {
            int[] cell = cellAtPathIndex(i);
            int col = cell[0], row = cell[1];
            out.add(new Room(i, col, row,
                    colMinX(col), colMaxX(col), rowMinZ(row), rowMaxZ(row),
                    exitDoorFor(i), i == 0, i == ROOM_COUNT - 1));
        }
        return out;
    }

    /** Pièce d'apparition (première du chemin). */
    public static Room spawnRoom() { return rooms().get(0); }

    /** Pièce de sortie (dernière du chemin, contient le téléporteur). */
    public static Room exitRoom() { return rooms().get(ROOM_COUNT - 1); }
}
