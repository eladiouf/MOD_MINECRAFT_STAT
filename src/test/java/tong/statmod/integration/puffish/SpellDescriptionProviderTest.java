package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpellDescriptionProviderTest {

    @Test
    void ironsSpellbooksGuide_isExtracted() {
        String desc = SpellDescriptionProvider.get("irons_spellbooks:fireball");
        assertNotNull(desc, "expected fireball guide from irons_spellbooks JAR");
        assertTrue(desc.contains("explod") || desc.contains("ball of fire"),
            "description should describe the spell, got: " + desc);
    }

    @Test
    void ironsSpellbooksHasAllGuides() {
        assertNotNull(SpellDescriptionProvider.get("irons_spellbooks:firebolt"));
        assertNotNull(SpellDescriptionProvider.get("irons_spellbooks:blaze_storm"));
        assertNotNull(SpellDescriptionProvider.get("irons_spellbooks:burning_dash"));
    }

    @Test
    void windSpellbooksGuide_isExtracted() {
        String desc = SpellDescriptionProvider.get("wind_spellbooks:wind_jump");
        assertNotNull(desc);
        assertFalse(desc.isBlank());
    }

    @Test
    void legendarymageGuide_isExtracted() {
        String desc = SpellDescriptionProvider.get("legendarymage:pyromaniac");
        assertNotNull(desc);
        assertFalse(desc.isBlank());
    }

    @Test
    void gametechbcsGuide_isExtracted() {
        String desc = SpellDescriptionProvider.get("gametechbcs_spellbooks:nullflare");
        assertNotNull(desc);
        assertFalse(desc.isBlank());
    }

    @Test
    void override_takesPrecedenceOverJar() {
        String desc = SpellDescriptionProvider.get("spells_gone_wrong:shotgun_creeper");
        assertNotNull(desc);
        assertTrue(desc.contains("creeper") || desc.contains("explo"),
            "description should reference creepers, got: " + desc);
    }

    @Test
    void tensuraOverride_isProvided() {
        String desc = SpellDescriptionProvider.get("tensura:fire_bolt");
        assertNotNull(desc, "tensura spells should have override descriptions");
        assertTrue(desc.contains("flame") || desc.contains("fire") || desc.contains("bolt"),
            "description should describe the spell, got: " + desc);
    }

    @Test
    void missingSpell_returnsNull() {
        assertNull(SpellDescriptionProvider.get("nonexistent:missing_spell"));
    }
}
