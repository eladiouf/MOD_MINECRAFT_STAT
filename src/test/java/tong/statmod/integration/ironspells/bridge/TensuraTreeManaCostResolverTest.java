package tong.statmod.integration.ironspells.bridge;

import org.junit.jupiter.api.Test;
import tong.statmod.integration.tensura.TensuraSpellTaxonomy;
import tong.statmod.magic.MagicNode;
import tong.statmod.magic.MagicTreeCatalog;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TensuraTreeManaCostResolverTest {
    @Test
    void ignoresNonTensuraSpellIdsEvenWhenHybridTreeContainsOtherSpellFamilies() {
        assertNull(TensuraTreeManaCostResolver.resolve("irons_spellbooks:firebolt", "offense"));
        assertNull(TensuraTreeManaCostResolver.resolve("minecraft:stone", "offense"));
    }

    @Test
    void resolvesEveryKnownTaxonomySkillFromTreeBalance() {
        for (String skillId : TensuraSpellTaxonomy.allSkillIds()) {
            assertNotNull(TensuraTreeManaCostResolver.resolve(skillId, TensuraSpellTaxonomy.discipline(skillId)),
                    "missing tree-balanced mana cost for " + skillId);
        }
    }

    @Test
    void magicTreeDoesNotReferenceUnknownTensuraSkills() {
        Set<String> taxonomy = new LinkedHashSet<>(TensuraSpellTaxonomy.allSkillIds());
        for (MagicNode node : MagicTreeCatalog.all()) {
            for (String spellId : node.learnedSpells()) {
                if (!spellId.startsWith("tensura:")) {
                    continue;
                }
                assertTrue(taxonomy.contains(spellId),
                        "magic tree references a Tensura skill with no taxonomy profile: " + spellId);
            }
        }
    }
}
