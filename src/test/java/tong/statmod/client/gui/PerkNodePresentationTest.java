package tong.statmod.client.gui;

import org.junit.jupiter.api.Test;
import net.minecraft.network.chat.Component;
import tong.statmod.perks.Perk;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkNodePresentationTest {
    @Test
    void resolvesAvailableStateWhenLevelPointsAndRequirementsPass() {
        PerkNodePresentation model = PerkNodePresentation.resolve(
                Perk.BRUTE_CORE,
                false,
                12,
                3,
                0,
                true,
                List.of());

        assertEquals(PerkNodeVisualState.AVAILABLE, model.state());
        assertEquals("Click to unlock", model.statusLine().getString());
    }

    @Test
    void resolvesMixedLockedStateWhenSeveralRequirementsFail() {
        PerkNodePresentation model = PerkNodePresentation.resolve(
                Perk.BRUTE_SYNERGY,
                false,
                8,
                1,
                10,
                false,
                List.of("Monster Combat", "Giant race"));

        assertEquals(PerkNodeVisualState.LOCKED_MIXED, model.state());
        assertEquals("Blocked: requirements not met", model.statusLine().getString());
        assertTrue(model.detailLines().stream().anyMatch(line -> line.getString().contains("Stat requirement: 8 / 40")));
        assertTrue(model.detailLines().stream().anyMatch(line -> line.getString().contains("Perk points: 1 / 2")));
        assertTrue(model.detailLines().stream().anyMatch(line -> line.getString().contains("Physical Endurance: 10 / 40")));
        assertTrue(model.detailLines().stream().anyMatch(line -> line.getString().contains("Requires: Monster Combat")));
        assertTrue(model.detailLines().stream().anyMatch(line -> line.getString().contains("Requires: Giant race")));
    }

    @Test
    void buildsTooltipInStableReadableOrder() {
        PerkNodePresentation model = PerkNodePresentation.resolve(
                Perk.BRUTE_ACTIVE,
                false,
                25,
                0,
                0,
                true,
                List.of());

        List<Component> tooltip = model.tooltipLines(
                Component.literal("Mighty Swing"),
                Component.literal("Charged attack deals +10% damage"),
                Component.literal("ACTIVE • Brute Force"),
                Component.literal("Cost: 1 point"));

        assertEquals("Mighty Swing", tooltip.get(0).getString());
        assertEquals("Charged attack deals +10% damage", tooltip.get(1).getString());
        assertEquals("ACTIVE • Brute Force", tooltip.get(2).getString());
        assertEquals("Cost: 1 point", tooltip.get(3).getString());
        assertEquals("Blocked: not enough perk points", tooltip.get(tooltip.size() - 1).getString());
    }
}
