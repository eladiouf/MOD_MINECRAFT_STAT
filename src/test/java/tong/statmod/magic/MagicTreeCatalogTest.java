package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.integration.tensura.TensuraSpellTaxonomy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
    void all_spell_ids_from_supported_lib_jars_are_represented() throws Exception {
        Set<String> covered = new LinkedHashSet<>();
        for (MagicNode node : MagicTreeCatalog.all()) {
            covered.addAll(node.learnedSpells());
        }

        for (String jarName : List.of(
                "irons_spellbooks-1.21.1-3.16.1.jar",
                "darkdoppelganger-3.3.0-1.21.1.jar",
                "gametechbcs_spellbooks-3.0.0-1.21.1.jar",
                "legendarymage-1.0.9.jar",
                "spells_gone_wrong-1.21.1-2.0.0.jar",
                "wind_spellbooks-1.0.4.jar")) {
            for (String spellId : baseSpellIdsFromJar(jarName)) {
                assertTrue(covered.contains(spellId), "missing jar spell in MagicTreeCatalog: " + spellId);
            }
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
    void curated_irons_and_addon_spells_are_present_in_expected_branches() {
        assertSpellsLiveInBranch(MagicBranch.FIRE,
                "irons_spellbooks:blaze_storm",
                "irons_spellbooks:fire_arrow",
                "irons_spellbooks:flaming_barrage",
                "irons_spellbooks:flaming_strike",
                "irons_spellbooks:heat_surge",
                "irons_spellbooks:magma_bomb",
                "irons_spellbooks:raise_hell",
                "irons_spellbooks:scorch",
                "irons_spellbooks:wall_of_fire",
                "gametechbcs_spellbooks:ashen_breath",
                "gametechbcs_spellbooks:flames_reborn",
                "gametechbcs_spellbooks:meteor_storm");
        assertSpellsLiveInBranch(MagicBranch.WATER,
                "gametechbcs_spellbooks:shatterpoint");
        assertSpellsLiveInBranch(MagicBranch.AIR,
                "irons_spellbooks:thunder_step",
                "wind_spellbooks:aeropic",
                "wind_spellbooks:almighty_push",
                "wind_spellbooks:iron_slash",
                "wind_spellbooks:tailwind",
                "wind_spellbooks:tornado",
                "wind_spellbooks:wind_blade",
                "wind_spellbooks:wind_jump");
        assertSpellsLiveInBranch(MagicBranch.EARTH,
                "gametechbcs_spellbooks:acid_rain",
                "gametechbcs_spellbooks:aerial_collapse",
                "gametechbcs_spellbooks:ensnare");
        assertSpellsLiveInBranch(MagicBranch.HOLY,
                "irons_spellbooks:angel_wing",
                "gametechbcs_spellbooks:banish",
                "gametechbcs_spellbooks:nullflare");
        assertSpellsLiveInBranch(MagicBranch.BLOOD,
                "gametechbcs_spellbooks:call_forth_the_dead_king",
                "gametechbcs_spellbooks:crimson_downpour");
        assertSpellsLiveInBranch(MagicBranch.ENDER,
                "irons_spellbooks:gravity_fissure",
                "gametechbcs_spellbooks:astral_sense",
                "gametechbcs_spellbooks:displacement");
        assertSpellsLiveInBranch(MagicBranch.EVOCATION,
                "irons_spellbooks:creeper_revenge",
                "gametechbcs_spellbooks:lingering_strain",
                "spells_gone_wrong:nucreeper_strike",
                "spells_gone_wrong:shotgun_creeper");
        assertSpellsLiveInBranch(MagicBranch.ELDRITCH,
                "irons_spellbooks:void_tentacles",
                "gametechbcs_spellbooks:blackout",
                "gametechbcs_spellbooks:psychic_bolt",
                "gametechbcs_spellbooks:reversal",
                "gametechbcs_spellbooks:spectral_blink");
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

    private static void assertSpellsLiveInBranch(MagicBranch branch, String... spellIds) {
        for (String spellId : spellIds) {
            assertSpellLivesInBranch(spellId, branch);
        }
    }

    private static Set<String> baseSpellIdsFromJar(String jarName) throws IOException {
        Path jarPath = Path.of("libs", jarName);
        Pattern pattern = Pattern.compile("\"spell\\.([a-z0-9_\\-]+)\\.([a-z0-9_]+)\"\\s*:");
        Set<String> spellIds = new LinkedHashSet<>();
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            ZipEntry entry = zip.stream()
                    .filter(e -> e.getName().matches("assets/.+/lang/en_us\\.json"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("missing lang file in " + jarName));
            String json = new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = pattern.matcher(json);
            while (matcher.find()) {
                String spellId = matcher.group(1) + ":" + matcher.group(2);
                if (!spellId.endsWith(":none")) {
                    spellIds.add(spellId);
                }
            }
        }
        return spellIds;
    }
}
