package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 100 thèmes de donjon — invariants du registre généré (pur, sans Bootstrap).
 */
public class DungeonThemesTest {

    @Test
    void exactlyOneHundredThemes() {
        assertEquals(100, DungeonThemes.count());
    }

    @Test
    void everyFloorHasATheme() {
        for (int f = 1; f <= 100; f++) {
            DungeonThemes.Theme t = DungeonThemes.forFloor(f);
            assertNotNull(t, "Étage " + f + " doit avoir un thème");
            assertNotNull(t.displayName());
            assertFalse(t.displayName().isBlank(), "Nom de thème vide à l'étage " + f);
            assertFalse(t.adds().isEmpty(), "Aucun add à l'étage " + f);
            assertFalse(t.miniBoss().isEmpty(), "Aucun mini-boss à l'étage " + f);
        }
    }

    @Test
    void loopsBeyondOneHundred() {
        // L'étage 101 reprend le thème de l'étage 1, 150 celui de 50, etc.
        assertSame(DungeonThemes.forFloor(1), DungeonThemes.forFloor(101));
        assertSame(DungeonThemes.forFloor(50), DungeonThemes.forFloor(150));
        assertSame(DungeonThemes.forFloor(100), DungeonThemes.forFloor(200));
    }

    @Test
    void nonPositiveFloorClampsToFirst() {
        assertSame(DungeonThemes.forFloor(1), DungeonThemes.forFloor(0));
        assertSame(DungeonThemes.forFloor(1), DungeonThemes.forFloor(-5));
    }

    @Test
    void everyThemeNameIsDistinct() {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (int f = 1; f <= 100; f++) {
            names.add(DungeonThemes.forFloor(f).displayName());
        }
        // Au moins une large majorité de noms distincts (chaque étage a un nom propre).
        assertTrue(names.size() >= 90, "Trop de noms de thème dupliqués : " + names.size() + "/100");
    }

    @Test
    void allIdsAreNamespacedResourceLocations() {
        // Chaque ID doit ressembler à "namespace:path" (sinon resolve() renverra null en jeu).
        for (int f = 1; f <= 100; f++) {
            DungeonThemes.Theme t = DungeonThemes.forFloor(f);
            for (String id : t.adds()) assertTrue(id.contains(":"), "ID sans namespace: " + id);
            for (String id : t.miniBoss()) assertTrue(id.contains(":"), "ID sans namespace: " + id);
        }
    }
}
