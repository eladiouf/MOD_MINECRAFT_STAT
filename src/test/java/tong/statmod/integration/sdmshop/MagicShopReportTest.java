package tong.statmod.integration.sdmshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MagicShopReportTest {
    @Test
    void aggregatesCatalogCountsInStableJson() {
        MagicShopReport report = new MagicShopReport();
        report.recordSpell("addon", "addon:wind", "rare", 2);
        report.recordSpell("addon", "addon:wind", "rare", 3);
        report.recordScrollEntry();
        report.recordScrollEntry();
        report.recordSaleEntry();
        report.recordSkipped("broken:spell", "missing school");

        String json = report.toJson();

        assertEquals(json, report.toJson());
        assertTrue(json.contains("\"spellCount\": 1"));
        assertTrue(json.contains("\"scrollEntryCount\": 2"));
        assertTrue(json.contains("\"saleEntryCount\": 1"));
        assertTrue(json.contains("\"addon\": 2"));
        assertTrue(json.contains("\"addon:wind\": 2"));
        assertTrue(json.contains("\"rare\": 2"));
        assertTrue(json.contains("\"2\": 1"));
        assertTrue(json.contains("broken:spell"));
    }
}
