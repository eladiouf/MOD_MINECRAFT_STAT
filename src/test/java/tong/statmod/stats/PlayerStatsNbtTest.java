package tong.statmod.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class PlayerStatsNbtTest {
    @Test
    void roundTripsEveryKnownValue() {
        PlayerStats source = new PlayerStats();
        source.setLevel(StatType.FORGING, 15);
        source.addXp(StatType.FORGING, 37);
        PlayerStats loaded = new PlayerStats();

        loaded.deserializeNbt(source.serializeNbt());

        assertEquals(source.snapshot(), loaded.snapshot());
    }

    @Test
    void ignoresUnknownAndNormalizesInvalidEntries() {
        CompoundTag root = new CompoundTag();
        CompoundTag entries = new CompoundTag();
        CompoundTag known = new CompoundTag();
        known.putInt("level", -7);
        known.putInt("xp", -2);
        entries.put(StatType.AGILITY.id(), known);
        entries.putString("removed_stat", "bad-entry");
        root.put("stats", entries);
        PlayerStats loaded = new PlayerStats();

        loaded.deserializeNbt(root);

        assertEquals(new StatValue(0, 0), loaded.get(StatType.AGILITY));
        assertEquals(23, loaded.snapshot().size());
    }

    @Test
    void oversizedXpUsesNormalLevelUpRules() {
        CompoundTag root = new CompoundTag();
        CompoundTag entries = new CompoundTag();
        CompoundTag arcane = new CompoundTag();
        arcane.putInt("level", 0);
        arcane.putInt("xp", 55);
        entries.put(StatType.ARCANE_POWER.id(), arcane);
        root.put("stats", entries);
        PlayerStats loaded = new PlayerStats();

        loaded.deserializeNbt(root);

        assertEquals(new StatValue(2, 5), loaded.get(StatType.ARCANE_POWER));
    }
}
