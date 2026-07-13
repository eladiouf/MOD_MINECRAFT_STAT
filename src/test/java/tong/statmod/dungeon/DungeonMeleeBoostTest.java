package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Boost des dégâts de mêlée dans le Trial Dungeon (2026-07-13) : les mobs du donjon ont des PV
 * gonflés (L2 Hostility + scaling par étage) — la mêlée des joueurs est multipliée (~×3, config)
 * UNIQUEMENT dans la dimension, sur les coups physiques directs, jamais contre un joueur.
 */
class DungeonMeleeBoostTest {

    @Test
    void boostsDirectPhysicalHitsInDungeon() {
        assertEquals(3.0f, DungeonMeleeBoost.multiplierFor(true, true, true, false, 3.0), 1e-6f);
    }

    @Test
    void noBoostOutsideDungeon() {
        assertEquals(1.0f, DungeonMeleeBoost.multiplierFor(false, true, true, false, 3.0), 1e-6f);
    }

    @Test
    void noBoostForIndirectOrMagicDamage() {
        // Projectiles / sorts : directHit=false — la magie a son propre équilibrage.
        assertEquals(1.0f, DungeonMeleeBoost.multiplierFor(true, false, true, false, 3.0), 1e-6f);
        // Coup direct mais rôle non-physique (ex. dégâts magiques de contact).
        assertEquals(1.0f, DungeonMeleeBoost.multiplierFor(true, true, false, false, 3.0), 1e-6f);
    }

    @Test
    void neverBoostsAgainstPlayers() {
        assertEquals(1.0f, DungeonMeleeBoost.multiplierFor(true, true, true, true, 3.0), 1e-6f);
    }

    @Test
    void configBelowOneIsClampedToNeutral() {
        assertEquals(1.0f, DungeonMeleeBoost.multiplierFor(true, true, true, false, 0.5), 1e-6f);
    }
}
