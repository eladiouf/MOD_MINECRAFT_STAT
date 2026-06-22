package tong.statmod.integration.ironspells.bridge;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tong.statmod.integration.tensura.TensuraSpellTaxonomy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Vérifie le mapping (Tensura skill id ↔ wrapper id) et la complétude de
 * {@link TensuraSpellWrapperRegistry}. Les tests sont désactivés en CI car
 * {@code DeferredRegister.register(...)} appelle {@code LoadingModList.get()} qui
 * exige le runtime NeoForge complet — non disponible en JUnit standalone.
 * La vérité runtime est observable au démarrage du mod (log de {@code register()}).
 *
 * <p>Le mapping pur (id Tensura → id wrapper) est couvert par {@link TensuraWrapperIdsTest}.
 */
@Disabled("Requires NeoForge runtime (LoadingModList) — verified at mod boot")
class TensuraSpellWrapperRegistryTest {
    @Test
    void wrapperIdForRoundtripsThroughTensuraWrapperIds() {
        assertEquals("statmod:tensura_fire_bolt",
                TensuraSpellWrapperRegistry.wrapperIdFor("tensura:fire_bolt"));
        assertEquals("statmod:tensura_aerial_blade",
                TensuraSpellWrapperRegistry.wrapperIdFor("tensura:aerial_blade"));
    }

    @Test
    void registrySizeMatchesTaxonomyScope() {
        assertEquals(TensuraSpellTaxonomy.allSkillIds().size(), TensuraSpellWrapperRegistry.size());
        assertEquals(56, TensuraSpellWrapperRegistry.size());
    }

    @Test
    void everyTaxonomySkillHasARegisteredWrapper() {
        for (String tensuraId : TensuraSpellTaxonomy.allSkillIds()) {
            assertTrue(TensuraSpellWrapperRegistry.hasWrapperFor(tensuraId),
                    "missing wrapper for " + tensuraId);
        }
    }

    @Test
    void unknownTensuraIdHasNoWrapper() {
        assertFalse(TensuraSpellWrapperRegistry.hasWrapperFor("tensura:does_not_exist"));
        assertFalse(TensuraSpellWrapperRegistry.hasWrapperFor(null));
    }
}
