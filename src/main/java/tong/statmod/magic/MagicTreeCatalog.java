package tong.statmod.magic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MagicTreeCatalog {
    public static final String LOCKED_SENTINEL = "__never__";

    private static final Map<String, MagicNode> BY_ID = new LinkedHashMap<>();
    private static final Map<MagicBranch, List<MagicNode>> BY_BRANCH = new LinkedHashMap<>();

    static {
        // Common trunk - four arcane foundations, linear chain
        add(new MagicNode("common/foundation/arcane_focus",
                MagicBranch.COMMON, MagicNodeKind.TRUNK_FOUNDATION, MagicTier.T1,
                MagicCurrency.ARCANE, 1, List.of(), Set.of()));
        add(new MagicNode("common/foundation/mana_well",
                MagicBranch.COMMON, MagicNodeKind.TRUNK_FOUNDATION, MagicTier.T1,
                MagicCurrency.ARCANE, 1, List.of("common/foundation/arcane_focus"), Set.of()));
        add(new MagicNode("common/foundation/cast_discipline",
                MagicBranch.COMMON, MagicNodeKind.TRUNK_FOUNDATION, MagicTier.T2,
                MagicCurrency.ARCANE, 2, List.of("common/foundation/mana_well"), Set.of()));
        add(new MagicNode("common/foundation/multi_school_gate",
                MagicBranch.COMMON, MagicNodeKind.TRUNK_FOUNDATION, MagicTier.T3,
                MagicCurrency.ARCANE, 3, List.of("common/foundation/cast_discipline"), Set.of()));

        // Fire branch - opener, T1 / T2 / T3 tier nodes, 4 signature spells
        add(new MagicNode("fire/opener/ignition",
                MagicBranch.FIRE, MagicNodeKind.BRANCH_OPENER, MagicTier.T1,
                MagicCurrency.ARCANE, 2,
                List.of("common/foundation/arcane_focus", "common/foundation/mana_well"),
                Set.of()));
        add(new MagicNode("fire/tier/ember_path",
                MagicBranch.FIRE, MagicNodeKind.BRANCH_TIER, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("fire/opener/ignition"), Set.of()));
        add(new MagicNode("fire/tier/flame_path",
                MagicBranch.FIRE, MagicNodeKind.BRANCH_TIER, MagicTier.T2,
                MagicCurrency.SCHOOL, 2, List.of("fire/tier/ember_path"), Set.of()));
        add(new MagicNode("fire/tier/inferno_path",
                MagicBranch.FIRE, MagicNodeKind.BRANCH_TIER, MagicTier.T3,
                MagicCurrency.SCHOOL, 3,
                List.of("fire/tier/flame_path", "common/foundation/cast_discipline"), Set.of()));

        add(new MagicNode("fire/signature/firebolt",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("fire/tier/ember_path"),
                Set.of("irons_spellbooks:firebolt")));
        add(new MagicNode("fire/signature/tensura_fire_bolt",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T1,
                MagicCurrency.SCHOOL, 1, List.of("fire/tier/ember_path"),
                Set.of("tensura:fire_bolt")));
        add(new MagicNode("fire/signature/burning_dash",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T2,
                MagicCurrency.SCHOOL, 1, List.of("fire/tier/flame_path"),
                Set.of("irons_spellbooks:burning_dash")));
        add(new MagicNode("fire/signature/fireball",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T2,
                MagicCurrency.SCHOOL, 2, List.of("fire/tier/flame_path"),
                Set.of("irons_spellbooks:fireball")));
        add(new MagicNode("fire/signature/tensura_fire_storm",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T2,
                MagicCurrency.SCHOOL, 2, List.of("fire/tier/flame_path"),
                Set.of("tensura:fire_storm")));
        add(new MagicNode("fire/signature/fire_breath",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T3,
                MagicCurrency.SCHOOL, 3, List.of("fire/tier/inferno_path"),
                Set.of("irons_spellbooks:fire_breath")));
        add(new MagicNode("fire/signature/tensura_hellfire",
                MagicBranch.FIRE, MagicNodeKind.SIGNATURE_SPELL, MagicTier.T3,
                MagicCurrency.SCHOOL, 3, List.of("fire/tier/inferno_path"),
                Set.of("tensura:hellfire")));

        // Locked placeholders for the other branches
        for (MagicBranch b : MagicBranch.values()) {
            if (b == MagicBranch.COMMON || b == MagicBranch.FIRE) continue;
            add(new MagicNode(b.id + "/locked/anchor",
                    b, MagicNodeKind.LATEGAME_GATE, MagicTier.T1,
                    MagicCurrency.ARCANE, 0, List.of(LOCKED_SENTINEL), Set.of()));
        }
    }

    private MagicTreeCatalog() {}

    private static void add(MagicNode node) {
        if (BY_ID.put(node.id(), node) != null) {
            throw new IllegalStateException("duplicate node id " + node.id());
        }
        BY_BRANCH.computeIfAbsent(node.branch(), k -> new ArrayList<>()).add(node);
    }

    public static MagicNode byId(String id) {
        return BY_ID.get(id);
    }

    public static List<MagicNode> all() {
        return List.copyOf(BY_ID.values());
    }

    public static List<MagicNode> byBranch(MagicBranch branch) {
        return Collections.unmodifiableList(BY_BRANCH.getOrDefault(branch, List.of()));
    }
}
