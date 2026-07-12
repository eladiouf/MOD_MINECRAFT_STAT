package tong.statmod.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mission M5 — Phase β tests.
 *
 * <p>Tests sur {@link ForgingIntermediateIds} (constantes pures, pas de Minecraft).
 * {@link ForgingIntermediates} touche DeferredRegister donc n'est pas testable hors runtime.
 */
class ForgingIntermediatesTest {

    @Test
    void exactly_90_intermediates_declared() {
        assertEquals(6 * 15, ForgingIntermediateIds.count(),
                "expected 6 classes × 15 materials = 90 rough intermediates");
    }

    @Test
    void weaponClasses_listIsCorrect() {
        assertEquals(6, ForgingIntermediateIds.WEAPON_CLASSES.size());
        assertEquals("blade", ForgingIntermediateIds.WEAPON_CLASSES.get(0));
        assertEquals("dagger_blade", ForgingIntermediateIds.WEAPON_CLASSES.get(5));
        assertTrue(ForgingIntermediateIds.WEAPON_CLASSES.contains("staff_core"));
    }

    @Test
    void materials_listMatchesPhaseAlpha() {
        assertEquals(15, ForgingIntermediateIds.MATERIALS.size());
        assertEquals("gold", ForgingIntermediateIds.MATERIALS.get(0));
        assertEquals("netherite", ForgingIntermediateIds.MATERIALS.get(14));
        // Material identities preserved (no generic tier_N collapse)
        assertTrue(ForgingIntermediateIds.MATERIALS.contains("orichalcum"));
        assertTrue(ForgingIntermediateIds.MATERIALS.contains("adamantite"));
        assertTrue(ForgingIntermediateIds.MATERIALS.contains("netherite"));
    }

    @Test
    void isKnownId_findsKnownIntermediate() {
        assertTrue(ForgingIntermediateIds.isKnownId("rough_blade_hihiirokane"));
        assertTrue(ForgingIntermediateIds.isKnownId("rough_dagger_blade_gold"));
        assertTrue(ForgingIntermediateIds.isKnownId("rough_staff_core_arcane"));
        assertTrue(ForgingIntermediateIds.isKnownId("rough_axe_head_orichalcum"));
        assertTrue(ForgingIntermediateIds.isKnownId("rough_spear_tip_netherite"));
        assertTrue(ForgingIntermediateIds.isKnownId("rough_bow_limb_netherite"));
    }

    @Test
    void isKnownId_unknownReturnsFalse() {
        assertFalse(ForgingIntermediateIds.isKnownId("rough_blade_iron"),
                "iron is handled by overgeared native, not in Phase β scope");
        assertFalse(ForgingIntermediateIds.isKnownId("rough_blade_unicorn"));
        assertFalse(ForgingIntermediateIds.isKnownId("foo_bar"));
        assertFalse(ForgingIntermediateIds.isKnownId(null));
    }

    @Test
    void allIds_listHas90UniqueEntries() {
        assertEquals(90, ForgingIntermediateIds.allIds().size());
        long unique = ForgingIntermediateIds.allIds().stream().distinct().count();
        assertEquals(90, unique, "all 90 ids must be unique");
    }

    @Test
    void allCombinations_areInAllIds() {
        for (String cls : ForgingIntermediateIds.WEAPON_CLASSES) {
            for (String mat : ForgingIntermediateIds.MATERIALS) {
                String id = "rough_" + cls + "_" + mat;
                assertTrue(ForgingIntermediateIds.isKnownId(id), "missing id: " + id);
            }
        }
    }
}
