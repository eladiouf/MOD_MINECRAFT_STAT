package tong.statmod.integration.sdmshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class MagicShopIdsTest {
    @Test
    void sameLogicalObjectAlwaysGetsSameUuid() {
        assertEquals(MagicShopIds.tab("irons_spellbooks:fire"),
                MagicShopIds.tab("irons_spellbooks:fire"));
        assertEquals(MagicShopIds.scroll("irons_spellbooks:fireball", 3),
                MagicShopIds.scroll("irons_spellbooks:fireball", 3));
    }

    @Test
    void levelAndObjectKindArePartOfTheIdentity() {
        assertNotEquals(MagicShopIds.scroll("irons_spellbooks:fireball", 2),
                MagicShopIds.scroll("irons_spellbooks:fireball", 3));
        assertNotEquals(MagicShopIds.tab("shared"), MagicShopIds.sale("shared"));
    }

    @Test
    void normalizesSurroundingWhitespace() {
        assertEquals(MagicShopIds.sale("diamond"), MagicShopIds.sale("  diamond  "));
    }
}
