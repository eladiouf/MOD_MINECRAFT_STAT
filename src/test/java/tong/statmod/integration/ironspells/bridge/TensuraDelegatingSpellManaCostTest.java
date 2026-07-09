package tong.statmod.integration.ironspells.bridge;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tong.statmod.integration.tensura.TensuraSpellProfile;
import tong.statmod.stats.StatType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TensuraDelegatingSpellManaCostTest {
    private static final String TEST_SKILL_ID = "tensura:test_magicule_spell";

    @AfterEach
    void cleanupCache() {
        TensuraSpellMetadata.invalidate(TEST_SKILL_ID);
    }

    @Test
    void uses_cached_native_magicule_cost_when_available() throws Exception {
        injectMetadata(TEST_SKILL_ID, 23.25);

        TensuraDelegatingSpell spell = new TensuraDelegatingSpell(profile(TEST_SKILL_ID, "offense"));

        assertEquals(24, spell.getManaCost(1));
    }

    @Test
    void falls_back_to_discipline_baseline_when_magicule_cost_is_unresolved() throws Exception {
        injectMetadata(TEST_SKILL_ID, -1.0);

        TensuraDelegatingSpell spell = new TensuraDelegatingSpell(profile(TEST_SKILL_ID, "utility"));

        assertEquals(30, spell.getManaCost(1));
    }

    @Test
    void treeTierBalanceOverridesTinyNativeMagiculeValuesForLowTierOffense() throws Exception {
        injectMetadata("tensura:fire_ball", 3.0);

        TensuraDelegatingSpell spell = new TensuraDelegatingSpell(profile("tensura:fire_ball", "offense"));

        assertEquals(26, spell.getManaCost(1));
    }

    @Test
    void treeTierBalanceOverridesHugeNativeMagiculeValuesForSupportSpells() throws Exception {
        injectMetadata("tensura:healing_rain", 250.0);

        TensuraDelegatingSpell spell = new TensuraDelegatingSpell(profile("tensura:healing_rain", "support"));

        assertEquals(34, spell.getManaCost(1));
    }

    @Test
    void treeTierBalanceKeepsLateGameMobilityBelowLateGameNukes() throws Exception {
        injectMetadata("tensura:gate", 999.0);
        injectMetadata("tensura:maximum_magic_bullet", 1.0);

        TensuraDelegatingSpell gate = new TensuraDelegatingSpell(profile("tensura:gate", "mobility"));
        TensuraDelegatingSpell bullet = new TensuraDelegatingSpell(profile("tensura:maximum_magic_bullet", "offense"));

        assertEquals(55, gate.getManaCost(1));
        assertEquals(66, bullet.getManaCost(1));
    }

    @Test
    void unknown_skill_metadata_defaults_to_unresolved_magicule_cost() {
        String unknownSkillId = "tensura:missing_skill_unit_test";
        TensuraSpellMetadata.invalidate(unknownSkillId);

        assertEquals(-1.0, TensuraSpellMetadata.forSkill(unknownSkillId).baselineMagiculeCost());
    }

    private static TensuraSpellProfile profile(String skillId, String discipline) {
        return new TensuraSpellProfile(
                skillId,
                discipline,
                StatType.FIRE_AFFINITY,
                List.of(StatType.ARCANE_POWER),
                true);
    }

    @SuppressWarnings("unchecked")
    private static void injectMetadata(String skillId, double baselineMagiculeCost) throws Exception {
        TensuraSpellMetadata.invalidate(skillId);

        Constructor<TensuraSpellMetadata> ctor = TensuraSpellMetadata.class.getDeclaredConstructor(
                int.class, boolean.class, ResourceLocation.class, double.class);
        ctor.setAccessible(true);
        TensuraSpellMetadata metadata = ctor.newInstance(0, true, null, baselineMagiculeCost);

        Field cacheField = TensuraSpellMetadata.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        ConcurrentMap<String, TensuraSpellMetadata> cache =
                (ConcurrentMap<String, TensuraSpellMetadata>) cacheField.get(null);
        cache.put(skillId, metadata);
    }
}
