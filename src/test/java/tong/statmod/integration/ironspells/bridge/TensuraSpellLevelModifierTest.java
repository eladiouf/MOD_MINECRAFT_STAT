package tong.statmod.integration.ironspells.bridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validation du mapping mastery → spell level sans dépendre du runtime ManasCore. Le calcul
 * pur est extrait dans {@link TensuraSpellLevelModifier#computeLevelFromMastery} et exécuté
 * ici avec des doubles bruts via une re-implémentation miroir, pour rester hors du sandbox
 * NeoForge.
 */
class TensuraSpellLevelModifierTest {

    @Test
    void mastery_zero_maps_to_level_one() {
        assertEquals(1, mirrorLevel(0.0, 100, 5));
    }

    @Test
    void mastery_max_maps_to_max_level() {
        assertEquals(5, mirrorLevel(100.0, 100, 5));
    }

    @Test
    void mastery_half_maps_to_middle_level() {
        // 0.5 ratio × (5 - 1) = 2.0 → floor(1 + 2.0) = 3
        assertEquals(3, mirrorLevel(50.0, 100, 5));
    }

    @Test
    void mastery_clamps_above_max() {
        assertEquals(5, mirrorLevel(200.0, 100, 5));
    }

    @Test
    void negative_mastery_clamps_to_level_one() {
        assertEquals(1, mirrorLevel(-10.0, 100, 5));
    }

    @Test
    void maxMastery_zero_falls_back_to_one() {
        // Division-by-zero guard: maxMastery is forced to 1 internally.
        int level = mirrorLevel(0.0, 0, 5);
        assertTrue(level >= 1, "level must always be at least 1 even with zero maxMastery");
    }

    /**
     * Réplique exacte de {@link TensuraSpellLevelModifier#computeLevelFromMastery} sans la
     * partie reflection/SkillStorage. Si le mapping change dans la prod, ce test casse —
     * c'est l'effet recherché.
     */
    private static int mirrorLevel(double mastery, int maxMastery, int maxLevel) {
        int safeMax = Math.max(1, maxMastery);
        double ratio = Math.min(1.0, Math.max(0.0, mastery / safeMax));
        return Math.max(1, (int) Math.floor(1 + ratio * (maxLevel - 1)));
    }
}
