package tong.statmod.progression.xp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tong.statmod.progression.xp.WeaponClassification.AMBIGUOUS;
import static tong.statmod.progression.xp.WeaponClassification.BLADE;
import static tong.statmod.progression.xp.WeaponClassification.HEAVY;
import static tong.statmod.progression.xp.WeaponClassification.PRECISION;
import static tong.statmod.progression.xp.WeaponClassification.UNCLASSIFIED;

import org.junit.jupiter.api.Test;

class WeaponClassifierTest {
    @Test
    void resolvesProjectilePriorityAndAmbiguousMeleeDeterministically() {
        assertEquals(PRECISION, WeaponClassifier.resolve(true, false, false, true));
        assertEquals(AMBIGUOUS, WeaponClassifier.resolve(true, true, false, false));
        assertEquals(HEAVY, WeaponClassifier.resolve(true, false, false, false));
        assertEquals(BLADE, WeaponClassifier.resolve(false, true, false, false));
        assertEquals(PRECISION, WeaponClassifier.resolve(false, false, true, false));
        assertEquals(UNCLASSIFIED, WeaponClassifier.resolve(false, false, false, false));
    }
}
