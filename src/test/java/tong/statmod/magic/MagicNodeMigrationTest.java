package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MagicNodeMigrationTest {

    private final PlayerStatData data = new PlayerStatData();
    private final ConditionContext ctx = ConditionContext.defaultContext();

    @Test void migrated_firebolt_basic_element_t1_no_conditions() {
        assertNull(MagicNodeMigration.defaultCondition(firebolt()),
                "Basic element T1 spells should have no conditions");
    }

    @Test void migrated_fire_opener_basic_element_t1_no_conditions() {
        assertNull(MagicNodeMigration.defaultCondition(fireOpener()),
                "Basic element T1 openers should have no conditions");
    }

    @Test void migrated_trunk_root_only_needs_erudition() {
        Condition c = MagicNodeMigration.defaultCondition(trunk());
        data.setLevel(StatType.ERUDITION.index, 1);
        assertTrue(c.evaluate(data, ctx));
        assertInstanceOf(Condition.StatCondition.class, c);
        Condition.StatCondition sc = (Condition.StatCondition) c;
        assertEquals(StatType.ERUDITION, sc.stat());
        assertEquals(1, sc.minLevel());
    }

    @Test void migrated_meteor_requires_intimidation5() {
        Condition c = MagicNodeMigration.defaultCondition(meteor());
        data.setLevel(StatType.ARCANE_POWER.index, 30);
        data.setLevel(StatType.ERUDITION.index, 20);
        data.setLevel(StatType.INTIMIDATION.index, 5);
        assertTrue(c.evaluate(data, ctx));
    }

    @Test void migrated_burningDash_requires_agility3() {
        Condition c = MagicNodeMigration.defaultCondition(burningDash());
        data.setLevel(StatType.ARCANE_POWER.index, 15);
        data.setLevel(StatType.ERUDITION.index, 10);
        data.setLevel(StatType.AGILITY.index, 3);
        assertTrue(c.evaluate(data, ctx));
    }

    private static MagicNode firebolt() {
        return new MagicNode("fire/signature/firebolt", MagicBranch.FIRE,
                MagicNodeKind.SIGNATURE_SPELL, MagicTier.T1, MagicCurrency.SCHOOL,
                1, List.of("fire/tier/ember_path"), Set.of("irons_spellbooks:firebolt"), null);
    }

    private static MagicNode fireOpener() {
        return new MagicNode("fire/opener/ignition", MagicBranch.FIRE,
                MagicNodeKind.BRANCH_OPENER, MagicTier.T1, MagicCurrency.ARCANE,
                2, List.of("common/foundation/arcane_focus"), Set.of(), null);
    }

    private static MagicNode trunk() {
        return new MagicNode("common/foundation/arcane_focus", MagicBranch.COMMON,
                MagicNodeKind.TRUNK_FOUNDATION, MagicTier.T1, MagicCurrency.ARCANE,
                1, List.of(), Set.of(), null);
    }

    private static MagicNode meteor() {
        return new MagicNode("fire/signature/meteor", MagicBranch.FIRE,
                MagicNodeKind.SIGNATURE_SPELL, MagicTier.T3, MagicCurrency.SCHOOL,
                3, List.of("fire/signature/firebolt"), Set.of("irons_spellbooks:meteor"), null);
    }

    private static MagicNode burningDash() {
        return new MagicNode("fire/signature/burning_dash", MagicBranch.FIRE,
                MagicNodeKind.SIGNATURE_SPELL, MagicTier.T2, MagicCurrency.SCHOOL,
                2, List.of("fire/signature/firebolt"), Set.of("irons_spellbooks:burning_dash"), null);
    }
}
