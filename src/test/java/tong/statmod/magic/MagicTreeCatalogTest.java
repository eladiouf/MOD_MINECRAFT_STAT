package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.integration.tensura.TensuraSpellTaxonomy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MagicTreeCatalogTest {
    @Test
    void common_trunk_has_four_foundation_nodes() {
        List<MagicNode> common = MagicTreeCatalog.byBranch(MagicBranch.COMMON);
        assertEquals(4, common.size());
        for (MagicNode n : common) {
            assertEquals(MagicNodeKind.TRUNK_FOUNDATION, n.kind());
            assertEquals(MagicCurrency.ARCANE, n.currency());
        }
        assertNotNull(MagicTreeCatalog.byId("common/foundation/arcane_focus"));
    }

    @Test
    void fire_branch_has_opener_three_tiers_and_full_hybrid_signatures() {
        List<MagicNode> fire = MagicTreeCatalog.byBranch(MagicBranch.FIRE);
        long openers = fire.stream().filter(n -> n.kind() == MagicNodeKind.BRANCH_OPENER).count();
        long tiers = fire.stream().filter(n -> n.kind() == MagicNodeKind.BRANCH_TIER).count();
        long sigs = fire.stream().filter(n -> n.kind() == MagicNodeKind.SIGNATURE_SPELL).count();
        assertEquals(1, openers);
        assertEquals(3, tiers);
        assertTrue(sigs >= 11, "fire branch should expose Iron and Tensura fire lines together");
    }

    @Test
    void fire_opener_requires_first_two_common_foundations() {
        MagicNode opener = MagicTreeCatalog.byId("fire/opener/ignition");
        assertTrue(opener.prerequisites().contains("common/foundation/arcane_focus"));
        assertTrue(opener.prerequisites().contains("common/foundation/mana_well"));
    }

    @Test
    void fire_signature_spells_reference_real_irons_ids() {
        MagicNode firebolt = MagicTreeCatalog.byId("fire/signature/firebolt");
        assertTrue(firebolt.learnedSpells().contains("irons_spellbooks:firebolt"));
    }

    @Test
    void fire_branch_also_contains_tensura_signature_nodes() {
        MagicNode tensuraFireBolt = MagicTreeCatalog.byId("fire/signature/tensura_fire_bolt");
        MagicNode tensuraHellfire = MagicTreeCatalog.byId("fire/signature/tensura_hellfire");
        assertNotNull(tensuraFireBolt);
        assertNotNull(tensuraHellfire);
        assertTrue(tensuraFireBolt.learnedSpells().contains("tensura:fire_bolt"));
        assertTrue(tensuraHellfire.learnedSpells().contains("tensura:hellfire"));
    }

    @Test
    void all_canonical_tensura_spells_are_represented_in_magic_tree_nodes() {
        Set<String> covered = new LinkedHashSet<>();
        for (MagicNode node : MagicTreeCatalog.all()) {
            covered.addAll(node.learnedSpells());
        }
        for (String skillId : TensuraSpellTaxonomy.allSkillIds()) {
            assertTrue(covered.contains(skillId), "missing Tensura spell in MagicTreeCatalog: " + skillId);
        }
    }

    @Test
    void advanced_tensura_lines_land_in_coherent_branches() {
        assertSpellLivesInBranch("tensura:magic_barrier", MagicBranch.HOLY);
        assertSpellLivesInBranch("tensura:teleport", MagicBranch.ENDER);
        assertSpellLivesInBranch("tensura:analyze", MagicBranch.EVOCATION);
        assertSpellLivesInBranch("tensura:true_darkness", MagicBranch.ELDRITCH);
    }

    @Test
    void each_branch_has_opener_tiers_and_signature_spells() {
        for (MagicBranch b : MagicBranch.values()) {
            if (b == MagicBranch.COMMON) continue;
            List<MagicNode> nodes = MagicTreeCatalog.byBranch(b);
            assertTrue(nodes.size() >= 5, "branch " + b + " has too few nodes");
            assertEquals(1, nodes.stream().filter(n -> n.kind() == MagicNodeKind.BRANCH_OPENER).count(),
                    "branch " + b + " must have exactly one opener");
            assertEquals(3, nodes.stream().filter(n -> n.kind() == MagicNodeKind.BRANCH_TIER).count(),
                    "branch " + b + " must have exactly three tier nodes");
            long sigCount = nodes.stream().filter(n -> n.kind() == MagicNodeKind.SIGNATURE_SPELL).count();
            assertTrue(sigCount >= 1, "branch " + b + " must have at least one signature spell");
        }
    }

    @Test
    void every_prerequisite_resolves_or_is_locked_sentinel() {
        for (MagicNode n : MagicTreeCatalog.all()) {
            for (String prereq : n.prerequisites()) {
                if ("__never__".equals(prereq)) continue;
                assertNotNull(MagicTreeCatalog.byId(prereq),
                        "missing prereq " + prereq + " on " + n.id());
            }
        }
    }

    private static void assertSpellLivesInBranch(String spellId, MagicBranch branch) {
        boolean found = MagicTreeCatalog.byBranch(branch).stream()
                .anyMatch(node -> node.learnedSpells().contains(spellId));
        assertTrue(found, "expected " + spellId + " to live in branch " + branch);
    }
}
