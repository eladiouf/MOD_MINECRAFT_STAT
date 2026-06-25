package tong.statmod.client.gui;

import org.junit.jupiter.api.Test;
import tong.statmod.stats.StatType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class TalentTreePanelTest {
    @Test
    void ignoresClicksOutsidePanel() {
        TalentTreePanel panel = new TalentTreePanel(List.of(StatType.values()), 0, 0, 300, 300);
        assertFalse(panel.mouseClicked(350, 350, 0));
        assertFalse(panel.mouseClicked(-10, -10, 0));
    }
}