package tong.statmod.client.gui;

import org.junit.jupiter.api.Test;
import tong.statmod.perks.Perk;
import tong.statmod.stats.StatType;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalentTreePanelTest {
    @Test
    void consumesClicksOnPerkNodesEvenWhenUnlockFails() throws Exception {
        TalentTreePanel panel = new TalentTreePanel(StatType.BRUTE_FORCE, 0, 0, 300, 300);
        replaceNodes(panel, List.of(new RejectingNode()));

        assertTrue(panel.mouseClicked(15, 15, 0));
    }

    @Test
    void ignoresClicksOutsidePerkNodes() throws Exception {
        TalentTreePanel panel = new TalentTreePanel(StatType.BRUTE_FORCE, 0, 0, 300, 300);
        replaceNodes(panel, List.of(new RejectingNode()));

        assertFalse(panel.mouseClicked(250, 250, 0));
    }

    @Test
    void showsFeedbackWhenNodeRejectsForLowLevel() throws Exception {
        TalentTreePanel panel = new TalentTreePanel(StatType.BRUTE_FORCE, 0, 0, 300, 300);
        replaceNodes(panel, List.of(new RejectingNode(PerkNodeWidget.ClickResult.LEVEL_TOO_LOW)));
        AtomicReference<PerkFeedbackToast.ToastPayload> payload = new AtomicReference<>();
        PerkFeedbackToast.setSinkForTests(payload::set);

        assertTrue(panel.mouseClicked(15, 15, 0));
        assertEquals("Perk unavailable", payload.get().title().getString());
        assertEquals("Stat level too low", payload.get().message().getString());
        PerkFeedbackToast.resetSinkForTests();
    }

    @Test
    void showsFeedbackWhenNodeRejectsForMissingPoints() throws Exception {
        TalentTreePanel panel = new TalentTreePanel(StatType.BRUTE_FORCE, 0, 0, 300, 300);
        replaceNodes(panel, List.of(new RejectingNode(PerkNodeWidget.ClickResult.NOT_ENOUGH_POINTS)));
        AtomicReference<PerkFeedbackToast.ToastPayload> payload = new AtomicReference<>();
        PerkFeedbackToast.setSinkForTests(payload::set);

        assertTrue(panel.mouseClicked(15, 15, 0));
        assertEquals("Not enough perk points", payload.get().message().getString());
        PerkFeedbackToast.resetSinkForTests();
    }

    @Test
    void doesNotShowToastAfterSuccessfulUnlockClick() throws Exception {
        TalentTreePanel panel = new TalentTreePanel(StatType.BRUTE_FORCE, 0, 0, 300, 300);
        replaceNodes(panel, List.of(new RejectingNode(PerkNodeWidget.ClickResult.UNLOCK_SENT)));
        AtomicReference<PerkFeedbackToast.ToastPayload> payload = new AtomicReference<>();
        PerkFeedbackToast.setSinkForTests(payload::set);

        assertTrue(panel.mouseClicked(15, 15, 0));
        assertNull(payload.get());
        PerkFeedbackToast.resetSinkForTests();
    }

    private static void replaceNodes(TalentTreePanel panel, List<PerkNodeWidget> replacement) throws Exception {
        Field nodesField = TalentTreePanel.class.getDeclaredField("nodes");
        nodesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<PerkNodeWidget> nodes = (List<PerkNodeWidget>) nodesField.get(panel);
        nodes.clear();
        nodes.addAll(replacement);
    }

    private static final class RejectingNode extends PerkNodeWidget {
        private final ClickResult result;

        private RejectingNode() {
            this(ClickResult.LEVEL_TOO_LOW);
        }

        private RejectingNode(ClickResult result) {
            super(Perk.BRUTE_CORE, 10, 10);
            this.result = result;
        }

        @Override
        public boolean isMouseOver(int mouseX, int mouseY) {
            return mouseX >= 10 && mouseX < 100 && mouseY >= 10 && mouseY < 36;
        }

        @Override
        public boolean tryClick() {
            return false;
        }

        @Override
        public ClickResult tryClickResult() {
            return result;
        }
    }
}
