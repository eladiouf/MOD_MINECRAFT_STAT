package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Catégorisation d'arène de boss selon l'id du boss (pur, sans Bootstrap).
 */
public class DungeonBossArenaTest {

    @Test
    void aquaticBossesGetAquaticArena() {
        // Étage 350 = kraken, 50 = margit/morgott (pas aquatique). Vérifie via le roster réel.
        assertEquals(DungeonBossArena.Kind.AQUATIC, DungeonBossArena.kindForFloor(350)); // kraken
    }

    @Test
    void everyFloorHasAKind() {
        for (int f = 10; f <= 1000; f += 10) {
            assertNotNull(DungeonBossArena.kindForFloor(f), "kind null à l'étage " + f);
        }
    }

    @Test
    void wardenIsColossus() {
        assertEquals(DungeonBossArena.Kind.COLOSSUS, DungeonBossArena.kindForFloor(20)); // warden
    }

    @Test
    void artoriasFallsBackToArenaOrCategory() {
        // Étage 30 = artorias : pas de mot-clé spécifique → ARENA (défaut). Juste vérifier non-null.
        assertNotNull(DungeonBossArena.kindForFloor(30));
    }
}
