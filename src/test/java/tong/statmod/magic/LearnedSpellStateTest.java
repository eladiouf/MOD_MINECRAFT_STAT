package tong.statmod.magic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class LearnedSpellStateTest {
    @Test
    void keepsTheHighestLearnedLevel() {
        LearnedSpellState state = new LearnedSpellState();

        assertEquals(LearnedSpellState.LearnResult.NEW,
                state.learn("irons_spellbooks:fireball", 2));
        assertEquals(LearnedSpellState.LearnResult.DUPLICATE,
                state.learn("irons_spellbooks:fireball", 2));
        assertEquals(LearnedSpellState.LearnResult.DUPLICATE,
                state.learn("irons_spellbooks:fireball", 1));
        assertEquals(LearnedSpellState.LearnResult.UPGRADED,
                state.learn("irons_spellbooks:fireball", 4));
        assertEquals(4, state.level("irons_spellbooks:fireball"));
    }

    @Test
    void rejectsInvalidEntriesAndCapacityOverflow() {
        LearnedSpellState state = new LearnedSpellState();

        assertEquals(LearnedSpellState.LearnResult.INVALID, state.learn("", 1));
        assertEquals(LearnedSpellState.LearnResult.INVALID, state.learn("bad id", 1));
        assertEquals(LearnedSpellState.LearnResult.INVALID,
                state.learn("irons_spellbooks:fireball", 0));
        for (int index = 0; index < LearnedSpellState.MAX_ENTRIES; index++) {
            assertEquals(LearnedSpellState.LearnResult.NEW,
                    state.learn("addon:spell_" + index, 1));
        }
        assertEquals(LearnedSpellState.LearnResult.FULL,
                state.learn("addon:overflow", 1));
    }

    @Test
    void snapshotsAreImmutableAndCopiesDoNotAlias() {
        LearnedSpellState source = new LearnedSpellState();
        source.learn("addon:wind_blade", 2);
        LearnedSpellState copy = new LearnedSpellState();

        copy.copyFrom(source);
        source.learn("addon:void_shield", 3);

        assertEquals(Map.of("addon:wind_blade", 2), copy.snapshot());
        assertThrows(UnsupportedOperationException.class,
                () -> copy.snapshot().put("addon:mutated", 9));
    }

    @Test
    void malformedNbtIsSanitizedAndDuplicatesKeepHighestLevel() {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        list.add(entry("irons_spellbooks:fireball", 2));
        list.add(entry("irons_spellbooks:fireball", 5));
        list.add(entry("bad id", 7));
        list.add(entry("addon:negative", -1));
        root.put(LearnedSpellState.NBT_KEY, list);
        LearnedSpellState state = new LearnedSpellState();

        state.load(root);

        assertEquals(Map.of("irons_spellbooks:fireball", 5), state.snapshot());
    }

    private static CompoundTag entry(String id, int level) {
        CompoundTag entry = new CompoundTag();
        entry.putString("id", id);
        entry.putInt("level", level);
        return entry;
    }
}
