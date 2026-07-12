package tong.statmod.economy;

import org.junit.jupiter.api.Test;
import tong.statmod.item.ForgingIntermediateIds;

import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ForgeShopPricesTest {

    static final List<String> BLUEPRINT_IDS = List.of(
            "blueprint_universal_blade",
            "blueprint_universal_pole",
            "blueprint_runic_blade",
            "blueprint_legendary"
    );

    static final List<String> GRIP_IDS = List.of(
            "wooden_grip", "leather_wrap", "wire_wrap", "runic_grip"
    );

    static final List<String> COMPONENT_IDS = List.of(
            "katana_tsuba", "rapier_guard", "claymore_pommel",
            "halberd_socket", "warhammer_core", "staff_focus"
    );

    static final List<String> TOOL_IDS = List.of(
            "basic_forge_tongs", "basic_smithing_hammer"
    );

    @Test
    void allRoughsHavePrice() {
        for (String id : ForgingIntermediateIds.allIds()) {
            OptionalLong price = ForgeShopPrices.priceOf(id);
            assertTrue(price.isPresent(), "Missing price for " + id);
            assertTrue(price.getAsLong() > 0, "Zero/negative price for " + id);
        }
    }

    @Test
    void roughPriceIncreasesWithTier() {
        assertTrue(ForgeShopPrices.priceOf("rough_blade_gold").getAsLong() <=
                ForgeShopPrices.priceOf("rough_blade_diamond").getAsLong(),
                "gold should cost <= diamond");
        assertTrue(ForgeShopPrices.priceOf("rough_blade_diamond").getAsLong() <=
                        ForgeShopPrices.priceOf("rough_blade_hihiirokane").getAsLong(),
                "diamond should cost <= hihiirokane");
    }

    @Test
    void allBlueprintsHavePrice() {
        for (String id : BLUEPRINT_IDS) {
            OptionalLong price = ForgeShopPrices.priceOf(id);
            assertTrue(price.isPresent(), "Missing price for " + id);
            assertTrue(price.getAsLong() >= 500, "Blueprint " + id + " must be expensive");
        }
    }

    @Test
    void allGripsHavePrice() {
        for (String id : GRIP_IDS) {
            OptionalLong price = ForgeShopPrices.priceOf(id);
            assertTrue(price.isPresent(), "Missing price for " + id);
            assertTrue(price.getAsLong() > 0, "Zero/negative price for " + id);
        }
    }

    @Test
    void allComponentsHavePrice() {
        for (String id : COMPONENT_IDS) {
            OptionalLong price = ForgeShopPrices.priceOf(id);
            assertTrue(price.isPresent(), "Missing price for " + id);
            assertTrue(price.getAsLong() > 0, "Zero/negative price for " + id);
        }
    }

    @Test
    void allToolsHavePrice() {
        for (String id : TOOL_IDS) {
            OptionalLong price = ForgeShopPrices.priceOf(id);
            assertTrue(price.isPresent(), "Missing price for " + id);
            assertTrue(price.getAsLong() > 0, "Zero/negative price for " + id);
        }
    }

    @Test
    void unknownItemReturnsEmpty() {
        assertEquals(OptionalLong.empty(), ForgeShopPrices.priceOf("heated_gold_ingot"));
        assertEquals(OptionalLong.empty(), ForgeShopPrices.priceOf("nonexistent_item"));
    }

    @Test
    void allRoughsCoverEveryMaterial() {
        for (String mat : ForgingIntermediateIds.MATERIALS) {
            String id = "rough_blade_" + mat;
            assertTrue(ForgeShopPrices.priceOf(id).isPresent(),
                    "Material " + mat + " should have a priced rough");
        }
    }
}
