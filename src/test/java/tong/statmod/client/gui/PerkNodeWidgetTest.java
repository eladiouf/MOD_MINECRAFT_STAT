package tong.statmod.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PerkNodeWidgetTest {
    @Test
    void selectedNodeUsesDifferentFillThanHoveredNode() {
        int hovered = PerkNodeWidget.fillColor(PerkNodeVisualState.AVAILABLE, true, false);
        int selected = PerkNodeWidget.fillColor(PerkNodeVisualState.AVAILABLE, false, true);

        assertNotEquals(hovered, selected);
    }
}
