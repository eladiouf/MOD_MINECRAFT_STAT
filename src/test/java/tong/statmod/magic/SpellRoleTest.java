package tong.statmod.magic;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellRoleTest {

    @Test
    void elemental_damage_roles_map_to_matching_affinity() {
        assertEquals(StatType.FIRE_AFFINITY, SpellRole.ELEMENTAL_DAMAGE_FIRE.tertiaryStat());
        assertEquals(StatType.WATER_AFFINITY, SpellRole.ELEMENTAL_DAMAGE_WATER.tertiaryStat());
        assertEquals(StatType.AIR_AFFINITY, SpellRole.ELEMENTAL_DAMAGE_AIR.tertiaryStat());
        assertEquals(StatType.EARTH_AFFINITY, SpellRole.ELEMENTAL_DAMAGE_EARTH.tertiaryStat());
    }

    @Test
    void aoe_dot_mobility_map_to_design_stats() {
        assertEquals(StatType.INTIMIDATION, SpellRole.AOE_BLAST.tertiaryStat());
        assertEquals(StatType.WILLPOWER, SpellRole.DOT_ZONE.tertiaryStat());
        assertEquals(StatType.AGILITY, SpellRole.MOBILITY.tertiaryStat());
    }

    @Test
    void heal_corruption_use_willpower() {
        assertEquals(StatType.WILLPOWER, SpellRole.HEAL.tertiaryStat());
        assertEquals(StatType.WILLPOWER, SpellRole.CORRUPTION_CURSE.tertiaryStat());
    }

    @Test
    void summon_uses_tracking() {
        assertEquals(StatType.TRACKING, SpellRole.SUMMON.tertiaryStat());
    }

    @Test
    void control_bind_uses_casting_speed() {
        assertEquals(StatType.CASTING_SPEED, SpellRole.CONTROL_BIND.tertiaryStat());
    }

    @Test
    void barrier_uses_magic_resistance() {
        assertEquals(StatType.MAGIC_RESISTANCE, SpellRole.BARRIER_DEFENSIVE.tertiaryStat());
    }

    @Test
    void precision_strike_uses_precision() {
        assertEquals(StatType.PRECISION, SpellRole.PRECISION_STRIKE.tertiaryStat());
    }

    @Test
    void spatial_void_uses_keen_senses() {
        assertEquals(StatType.KEEN_SENSES, SpellRole.SPATIAL_VOID.tertiaryStat());
    }

    @Test
    void buff_uses_physical_endurance() {
        assertEquals(StatType.PHYSICAL_ENDURANCE, SpellRole.BUFF_EMPOWERMENT.tertiaryStat());
    }

    @Test
    void structural_roles_have_no_fixed_tertiary() {
        assertNull(SpellRole.TRUNK_FOUNDATION.tertiaryStat());
        assertNull(SpellRole.BRANCH_OPENER.tertiaryStat());
        assertNull(SpellRole.BRANCH_TIER.tertiaryStat());
        assertTrue(SpellRole.TRUNK_FOUNDATION.isStructural());
        assertTrue(SpellRole.BRANCH_OPENER.isStructural());
        assertTrue(SpellRole.BRANCH_TIER.isStructural());
    }

    @Test
    void spell_roles_are_not_structural() {
        for (SpellRole role : SpellRole.values()) {
            if (role == SpellRole.TRUNK_FOUNDATION
                    || role == SpellRole.BRANCH_OPENER
                    || role == SpellRole.BRANCH_TIER) {
                continue;
            }
            assertFalse(role.isStructural(), role + " should be a spell role");
            assertNotNull(role.tertiaryStat(), role + " must have a tertiary stat");
        }
    }
}
