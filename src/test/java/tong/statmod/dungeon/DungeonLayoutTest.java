package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Layout serpentin — invariants du chemin de pièces (pur, sans Bootstrap).
 */
public class DungeonLayoutTest {

    @Test
    void roomCountMatchesGrid() {
        assertEquals(DungeonLayout.COLS * DungeonLayout.ROWS, DungeonLayout.ROOM_COUNT);
        assertEquals(DungeonLayout.ROOM_COUNT, DungeonLayout.rooms().size());
    }

    @Test
    void firstIsSpawnLastIsExit() {
        List<DungeonLayout.Room> rooms = DungeonLayout.rooms();
        assertTrue(rooms.get(0).isFirst());
        assertTrue(rooms.get(rooms.size() - 1).isLast());
        assertEquals(rooms.get(0), DungeonLayout.spawnRoom());
        assertEquals(rooms.get(rooms.size() - 1), DungeonLayout.exitRoom());
        // Une seule pièce first et une seule last.
        assertEquals(1, rooms.stream().filter(DungeonLayout.Room::isFirst).count());
        assertEquals(1, rooms.stream().filter(DungeonLayout.Room::isLast).count());
    }

    @Test
    void consecutiveRoomsAreAdjacent() {
        List<DungeonLayout.Room> rooms = DungeonLayout.rooms();
        for (int i = 0; i < rooms.size() - 1; i++) {
            DungeonLayout.Room a = rooms.get(i), b = rooms.get(i + 1);
            int dCol = Math.abs(a.col() - b.col());
            int dRow = Math.abs(a.row() - b.row());
            assertEquals(1, dCol + dRow, "pièces " + i + "→" + (i + 1) + " non adjacentes");
            assertNotEquals(DungeonLayout.Dir.NONE, a.exitDoor(), "porte manquante à la pièce " + i);
        }
        // La dernière n'a pas de porte de sortie.
        assertEquals(DungeonLayout.Dir.NONE, rooms.get(rooms.size() - 1).exitDoor());
    }

    @Test
    void everyGridCellUsedExactlyOnce() {
        List<DungeonLayout.Room> rooms = DungeonLayout.rooms();
        boolean[][] seen = new boolean[DungeonLayout.COLS][DungeonLayout.ROWS];
        for (DungeonLayout.Room r : rooms) {
            assertFalse(seen[r.col()][r.row()], "case (" + r.col() + "," + r.row() + ") visitée 2×");
            seen[r.col()][r.row()] = true;
        }
        for (int c = 0; c < DungeonLayout.COLS; c++)
            for (int rw = 0; rw < DungeonLayout.ROWS; rw++)
                assertTrue(seen[c][rw], "case (" + c + "," + rw + ") jamais visitée");
    }

    @Test
    void roomsDoNotOverlapAndAreOrdered() {
        for (DungeonLayout.Room r : DungeonLayout.rooms()) {
            assertTrue(r.minX() <= r.maxX());
            assertTrue(r.minZ() <= r.maxZ());
        }
    }
}
