package tong.statmod.dungeon;

import org.junit.jupiter.api.Test;
import tong.statmod.storage.PlayerStatData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Système de points de donjon — logique pure (barème + accumulateur), sans Bootstrap.
 */
public class DungeonPointsTest {

    // Le barème de kill dépend désormais des stats réelles du mob (PV/attaque/armure) via
    // difficultyRating(LivingEntity), qui nécessite le Bootstrap Minecraft — couvert en jeu, pas
    // en test unit pur. On teste ici la logique pure : accumulateur, bornes, copie.

    @Test
    void pointsNeverGoNegative() {
        PlayerStatData d = new PlayerStatData();
        d.setDungeonPoints(10);
        d.addDungeonPoints(-999);
        assertEquals(0, d.getDungeonPoints(), "les points ne descendent jamais sous 0");
    }

    @Test
    void addAndSetPoints() {
        PlayerStatData d = new PlayerStatData();
        assertEquals(0, d.getDungeonPoints(), "défaut = 0");
        assertEquals(30, d.addDungeonPoints(30));
        assertEquals(45, d.addDungeonPoints(15));
        d.setDungeonPoints(5);
        assertEquals(5, d.getDungeonPoints());
        d.setDungeonPoints(-3);
        assertEquals(0, d.getDungeonPoints(), "setter borne aussi à 0");
    }

    @Test
    void pointsCopiedOnDataCopy() {
        PlayerStatData src = new PlayerStatData();
        src.addDungeonPoints(77);
        PlayerStatData copy = new PlayerStatData();
        copy.copyFrom(src);
        assertEquals(77, copy.getDungeonPoints(), "copyFrom copie les points");
    }
}
