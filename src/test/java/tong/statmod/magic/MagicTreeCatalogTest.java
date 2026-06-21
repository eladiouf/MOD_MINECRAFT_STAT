package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import java.util.List;
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
    void fire_branch_has_opener_three_tiers_and_hybrid_signatures() {
        List<MagicNode> fire = MagicTreeCatalog.byBranch(MagicBranch.FIRE);
        long openers = fire.stream().filter(n -> n.kind() == MagicNodeKind.BRANCH_OPENER).count();
        long tiers = fire.stream().filter(n -> n.kind() == MagicNodeKind.BRANCH_TIER).count();
        long sigs = fire.stream().filter(n -> n.kind() == MagicNodeKind.SIGNATURE_SPELL).count();
        assertEquals(1, openers);
        assertEquals(3, tiers);
        assertEquals(7, sigs);
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
    void other_elemental_and_late_game_branches_are_locked_placeholders() {
        for (MagicBranch b : MagicBranch.values()) {
            if (b == MagicBranch.COMMON || b == MagicBranch.FIRE) continue;
            List<MagicNode> nodes = MagicTreeCatalog.byBranch(b);
            assertEquals(1, nodes.size(), "branch " + b + " has more than placeholder");
            MagicNode anchor = nodes.get(0);
            assertEquals(b.id + "/locked/anchor", anchor.id());
            assertTrue(anchor.prerequisites().contains("__never__"));
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
}
