package tong.statmod.storage;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie que le round-trip serialize → deserialize préserve l'état magique sous la nouvelle
 * économie unifiée (Mission δ).
 */
class ModAttachmentsMagicSerializationTest {

    @Test
    void round_trip_preserves_unified_magic_state() {
        PlayerStatData d = new PlayerStatData();
        d.setMagicPoints(7);
        d.setSchoolMasteryProgress(MagicBranch.FIRE, 42);
        d.addMagicNode("common/foundation/arcane_focus");
        d.learnSpell("irons_spellbooks:firebolt");
        d.setMagicRace(MagicRace.ELF);
        d.setChosenStartBranch(MagicBranch.FIRE);

        CompoundTag tag = MagicStateSerializer.serialize(d);
        PlayerStatData restored = new PlayerStatData();
        MagicStateSerializer.deserialize(tag, restored);

        assertEquals(7, restored.getMagicPoints());
        assertEquals(42, restored.getSchoolMasteryProgress(MagicBranch.FIRE));
        assertTrue(restored.hasMagicNode("common/foundation/arcane_focus"));
        assertTrue(restored.hasLearnedSpell("irons_spellbooks:firebolt"));
        assertEquals(MagicRace.ELF, restored.getMagicRace());
        assertEquals(MagicBranch.FIRE, restored.getChosenStartBranch());
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacy_save_migrates_to_unified_pool_on_load() {
        // Simule un save pré-Mission-δ : arcanePoints + schoolPoints, pas de magicPoints.
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putInt("arcanePoints", 10);
        int[] sp = new int[MagicBranch.values().length];
        sp[MagicBranch.FIRE.ordinal()] = 5;
        sp[MagicBranch.WATER.ordinal()] = 3;
        legacyTag.putIntArray("schoolPoints", sp);

        PlayerStatData restored = new PlayerStatData();
        MagicStateSerializer.deserialize(legacyTag, restored);

        // Migration : 5 (default) + 10 + 5 + 3 = 23 magic points unifiés
        assertEquals(23, restored.getMagicPoints());
        // Pools legacy drainés
        assertEquals(0, restored.getArcanePoints());
        assertEquals(0, restored.getSchoolPoints(MagicBranch.FIRE));
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacy_save_with_huge_stockpile_is_capped() {
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putInt("arcanePoints", 9999);
        PlayerStatData restored = new PlayerStatData();
        MagicStateSerializer.deserialize(legacyTag, restored);
        assertEquals(PlayerStatData.MIGRATION_CAP, restored.getMagicPoints());
    }
}
