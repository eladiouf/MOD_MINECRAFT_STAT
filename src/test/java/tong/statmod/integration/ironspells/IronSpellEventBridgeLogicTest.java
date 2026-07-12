package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import tong.statmod.storage.PlayerStatData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IronSpellEventBridgeLogicTest {
    private static final Path SOURCE = Path.of(
            "src", "main", "java", "tong", "statmod", "integration", "ironspells", "IronSpellEventBridge.java");

    @Test
    void unlearned_spell_pre_cast_is_cancelled() {
        PlayerStatData d = new PlayerStatData();
        assertTrue(IronSpellEventBridge.shouldCancelPreCast(d, "irons_spellbooks:firebolt"));
    }

    @Test
    void learned_spell_pre_cast_is_allowed() {
        PlayerStatData d = new PlayerStatData();
        d.learnSpell("irons_spellbooks:firebolt");
        assertFalse(IronSpellEventBridge.shouldCancelPreCast(d, "irons_spellbooks:firebolt"));
    }

    @Test
    void spell_level_clamped_to_player_tier_access() {
        PlayerStatData d = new PlayerStatData();
        d.addMagicNode("fire/opener/ignition");
        d.addMagicNode("fire/tier/ember_path");
        assertEquals(2, IronSpellEventBridge.clampSpellLevel(d, MagicBranch.FIRE, 9));

        d.addMagicNode("fire/tier/flame_path");
        assertEquals(4, IronSpellEventBridge.clampSpellLevel(d, MagicBranch.FIRE, 9));

        d.addMagicNode("fire/tier/inferno_path");
        assertEquals(6, IronSpellEventBridge.clampSpellLevel(d, MagicBranch.FIRE, 9));
    }

    @Test
    void no_tier_access_clamps_to_one() {
        PlayerStatData d = new PlayerStatData();
        assertEquals(1, IronSpellEventBridge.clampSpellLevel(d, MagicBranch.FIRE, 9));
    }

    @Test
    void unknown_branch_returns_input_unchanged() {
        PlayerStatData d = new PlayerStatData();
        assertEquals(7, IronSpellEventBridge.clampSpellLevel(d, null, 7));
    }

    @Test
    void bankable_impact_is_consumed_from_verified_tracker_not_mana_fraction() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("CAST_IMPACT_TRACKER.consume"),
                "post-cast progression must consume server-side impact evidence");
        assertFalse(source.contains("manaFrac >= 0.25"),
                "mana fraction must not decide whether a cast had impact");
    }
}
