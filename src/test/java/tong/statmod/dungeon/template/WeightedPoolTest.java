package tong.statmod.dungeon.template;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class WeightedPoolTest {

    @Test
    void deterministicSelection() {
        var pool = new WeightedPool<>(List.of(
                new WeightedPool.Entry<>("a", 1.0),
                new WeightedPool.Entry<>("b", 1.0)));
        assertEquals(pool.select(42L, 0.5, 0.5), pool.select(42L, 0.5, 0.5));
    }

    @Test
    void differentSeedsGiveDifferentResults() {
        var pool = new WeightedPool<>(List.of(
                new WeightedPool.Entry<>("a", 1.0),
                new WeightedPool.Entry<>("b", 1.0)));
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 50; i++) seen.add(pool.select(i, i * 0.1, i * 0.2));
        assertTrue(seen.size() > 1, "should select different values");
    }

    @Test
    void heavierEntriesSelectedMoreOften() {
        var pool = new WeightedPool<>(List.of(
                new WeightedPool.Entry<>("common", 100.0),
                new WeightedPool.Entry<>("rare", 1.0)));
        int common = 0;
        for (int i = 0; i < 200; i++) {
            if ("common".equals(pool.select(i, i * 0.1, i * 0.2))) common++;
        }
        assertTrue(common > 150, "common should be chosen more often: " + common);
    }

    @Test
    void emptyPoolThrows() {
        var pool = new WeightedPool<>(List.of());
        assertThrows(IllegalStateException.class, () -> pool.select(1L, 0, 0));
    }

    @Test
    void singleEntryAlwaysSelected() {
        var pool = new WeightedPool<>(List.of(new WeightedPool.Entry<>("only", 1.0)));
        for (int i = 0; i < 20; i++) assertEquals("only", pool.select(i, 0, 0));
    }
}
