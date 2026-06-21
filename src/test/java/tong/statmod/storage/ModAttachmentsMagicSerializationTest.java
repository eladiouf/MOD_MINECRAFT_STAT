package tong.statmod.storage;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;
import static org.junit.jupiter.api.Assertions.*;

class ModAttachmentsMagicSerializationTest {
    @Test
    void round_trip_preserves_magic_state() {
        PlayerStatData d = new PlayerStatData();
        d.setArcanePoints(7);
        d.addSchoolPoints(MagicBranch.FIRE, 3);
        d.setSchoolMasteryProgress(MagicBranch.FIRE, 42);
        d.addMagicNode("common/foundation/arcane_focus");
        d.learnSpell("irons_spellbooks:firebolt");
        d.setMagicRace(MagicRace.ELF);
        d.setChosenStartBranch(MagicBranch.FIRE);

        CompoundTag tag = MagicStateSerializer.serialize(d);
        PlayerStatData restored = new PlayerStatData();
        MagicStateSerializer.deserialize(tag, restored);

        assertEquals(7, restored.getArcanePoints());
        assertEquals(3, restored.getSchoolPoints(MagicBranch.FIRE));
        assertEquals(42, restored.getSchoolMasteryProgress(MagicBranch.FIRE));
        assertTrue(restored.hasMagicNode("common/foundation/arcane_focus"));
        assertTrue(restored.hasLearnedSpell("irons_spellbooks:firebolt"));
        assertEquals(MagicRace.ELF, restored.getMagicRace());
        assertEquals(MagicBranch.FIRE, restored.getChosenStartBranch());
    }
}
