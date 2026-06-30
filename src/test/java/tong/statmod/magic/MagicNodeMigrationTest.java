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

    @Test void migrated_firebolt_requires_arcane2_erudition2_affinity1() {
        Condition c = MagicNodeMigration.defaultCondition(firebolt());
        data.setLevel(StatType.ARCANE_POWER.index, 2);
        data.setLevel(StatType.ERUDITION.index, 2);
        data.setLevel(StatType.FIRE_AFFINITY.index, 1);
        assertTrue(c.evaluate(data, ctx));
    }

    @Test void migrated_firebolt_fails_without_affinity() {
        Condition c = MagicNodeMigration.defaultCondition(firebolt());
        data.setLevel(StatType.ARCANE_POWER.index, 2);
        data.setLevel(StatType.ERUDITION.index, 2);
        data.setLevel(StatType.FIRE_AFFINITY.index, 0);
        assertFalse(c.evaluate(data, ctx));
    }

    @Test void migrated_fire_opener_requires_arcane2_erudition1_affinity2() {
        Condition c = MagicNodeMigration.defaultCondition(fireOpener());
        data.setLevel(StatType.ARCANE_POWER.index, 2);
        data.setLevel(StatType.ERUDITION.index, 1);
        data.setLevel(StatType.FIRE_AFFINITY.index, 2);
        assertTrue(c.evaluate(data, ctx));
    }

    @Test void migrated_trunk_has_no_tertiary() {
        Condition c = MagicNodeMigration.defaultCondition(trunk());
        data.setLevel(StatType.ARCANE_POWER.index, 1);
        data.setLevel(StatType.ERUDITION.index, 1);
        assertTrue(c.evaluate(data, ctx));
        assertInstanceOf(Condition.And.class, c);
        Condition.And and = (Condition.And) c;
        assertEquals(2, and.children().size(), "Trunk should only have 2 conditions (no tertiary)");
    }

    @Test void migrated_meteor_requires_intimidation5() {
        Condition c = MagicNodeMigration.defaultCondition(meteor());
        data.setLevel(StatType.ARCANE_POWER.index, 6);
        data.setLevel(StatType.ERUDITION.index, 5);
        data.setLevel(StatType.INTIMIDATION.index, 5);
        assertTrue(c.evaluate(data, ctx));
    }

    @Test void migrated_burningDash_requires_agility3() {
        Condition c = MagicNodeMigration.defaultCondition(burningDash());
        data.setLevel(StatType.ARCANE_POWER.index, 4);
        data.setLevel(StatType.ERUDITION.index, 3);
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
