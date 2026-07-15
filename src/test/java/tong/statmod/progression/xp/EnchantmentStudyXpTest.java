package tong.statmod.progression.xp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import tong.statmod.progression.xp.EnchantmentStudyXp.Entry;

class EnchantmentStudyXpTest {
    @Test
    void sumsEveryEnchantmentAndUsesApprovedWeights() {
        assertEquals(5, EnchantmentStudyXp.calculate(List.of(new Entry(1, 1))));
        assertEquals(40, EnchantmentStudyXp.calculate(List.of(new Entry(4, 2))));
        assertEquals(85, EnchantmentStudyXp.calculate(List.of(
                new Entry(1, 1), new Entry(2, 4), new Entry(1, 8))));
    }

    @Test
    void rejectsInvalidEntriesAndClampsOverflow() {
        assertEquals(0, EnchantmentStudyXp.calculate(null));
        assertEquals(0, EnchantmentStudyXp.calculate(List.of(new Entry(0, 8))));
        assertEquals(0, EnchantmentStudyXp.calculate(List.of(new Entry(2, 0))));
        assertEquals(160, EnchantmentStudyXp.calculate(List.of(
                new Entry(Integer.MAX_VALUE, 8))));
    }
}
