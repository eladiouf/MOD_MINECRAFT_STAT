package tong.statmod.client.stats;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import tong.statmod.client.ClientKeyMappings;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.client.ClientStatsState;
import tong.statmod.stats.StatFamily;

public final class StatsOverviewScreen extends Screen {
    private static final int PANEL_BACKGROUND = 0xF2_17110C;
    private static final int PANEL_BORDER = 0xFF_B38A4A;
    private static final int GAP = 6;
    private static StatFamily selectedFamily = StatFamily.values()[0];

    private StatsScreenModel model;
    private double scrollOffset;
    private int panelX;
    private int panelWidth;
    private int cardTop;
    private int cardBottom;
    private int cardWidth;
    private int columns;
    private int perkTop;

    public StatsOverviewScreen() {
        super(Component.translatable("screen.statmod.stats.title"));
        model = StatsScreenModel.from(ClientStatsCache.state());
    }

    @Override
    protected void init() {
        panelWidth = Math.min(width - 24, 520);
        panelX = (width - panelWidth) / 2;
        int buttonGap = 4;
        int buttonWidth = (panelWidth - 20 - buttonGap * 2) / 3;
        for (int index = 0; index < StatFamily.values().length; index++) {
            StatFamily family = StatFamily.values()[index];
            int x = panelX + 10 + (index % 3) * (buttonWidth + buttonGap);
            int y = 42 + (index / 3) * 22;
            addRenderableWidget(Button.builder(
                    Component.translatable("family.statmod." + family.slug()),
                    button -> selectFamily(family))
                    .bounds(x, y, buttonWidth, 20)
                    .build());
        }
        perkTop = 89;
        cardTop = 132;
        cardBottom = height - 18;
        columns = panelWidth >= 400 ? 2 : 1;
        cardWidth = (panelWidth - 20 - (columns - 1) * GAP) / columns;
        clampScroll();
    }

    private void selectFamily(StatFamily family) {
        selectedFamily = family;
        scrollOffset = 0;
        clampScroll();
    }

    @Override
    public void tick() {
        ClientStatsState state = ClientStatsCache.state();
        if (state.revision() != model.revision()) {
            model = StatsScreenModel.from(state);
            clampScroll();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(panelX - 2, 12, panelX + panelWidth + 2, height - 10, PANEL_BORDER);
        graphics.fill(panelX, 14, panelX + panelWidth, height - 12, PANEL_BACKGROUND);
        graphics.drawCenteredString(font, title, width / 2, 23, 0xFF_F4E4BE);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderActivePerks(graphics, mouseX, mouseY);

        List<StatsScreenModel.StatCard> cards = selectedCards();
        graphics.enableScissor(panelX + 8, cardTop, panelX + panelWidth - 8, cardBottom);
        StatsScreenModel.StatCard hovered = null;
        for (int index = 0; index < cards.size(); index++) {
            int column = index % columns;
            int row = index / columns;
            int x = panelX + 10 + column * (cardWidth + GAP);
            int y = cardTop + row * (StatCardRenderer.HEIGHT + GAP) - (int) scrollOffset;
            if (y + StatCardRenderer.HEIGHT < cardTop || y > cardBottom) {
                continue;
            }
            StatsScreenModel.StatCard card = cards.get(index);
            StatCardRenderer.render(graphics, font, card, x, y, cardWidth);
            if (mouseX >= x && mouseX < x + cardWidth
                    && mouseY >= y && mouseY < y + StatCardRenderer.HEIGHT
                    && mouseY >= cardTop && mouseY < cardBottom) {
                hovered = card;
            }
        }
        graphics.disableScissor();
        if (hovered != null) {
            StatCardRenderer.renderTooltip(graphics, font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollOffset -= delta * 18;
        clampScroll();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ClientKeyMappings.OPEN_STATS.matches(keyCode, scanCode)
                || minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public Component getNarrationMessage() {
        MutableComponent narration = Component.translatable("narration.statmod.family",
                Component.translatable("family.statmod." + selectedFamily.slug()));
        for (StatsScreenModel.StatCard card : selectedCards()) {
            narration.append(Component.literal(". "))
                    .append(StatCardRenderer.narration(card));
        }
        for (ActivePerkPresentation perk : model.activePerks()) {
            narration.append(Component.literal(". "))
                    .append(Component.translatable(perk.nameKey()));
        }
        return narration;
    }

    private void renderActivePerks(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = panelX + 10;
        int availableWidth = panelWidth - 20;
        graphics.drawString(font,
                Component.translatable("screen.statmod.stats.active_perks"),
                left, perkTop, 0xFF_DDBB78, false);
        if (model.activePerks().isEmpty()) {
            return;
        }

        MutableComponent names = Component.empty();
        int visibleCount = 0;
        List<FormattedCharSequence> lines = List.of();
        for (ActivePerkPresentation perk : model.activePerks()) {
            MutableComponent candidate = names.copy();
            if (visibleCount > 0) {
                candidate.append(Component.literal(", "));
            }
            candidate.append(Component.translatable(perk.nameKey()));
            List<FormattedCharSequence> candidateLines = font.split(candidate, availableWidth);
            if (candidateLines.size() > 3) {
                break;
            }
            names = candidate;
            lines = candidateLines;
            visibleCount++;
        }
        int hidden = model.activePerks().size() - visibleCount;
        if (hidden > 0) {
            names.append(Component.translatable("screen.statmod.stats.more_perks", hidden));
            lines = font.split(names, availableWidth);
        }
        for (int index = 0; index < Math.min(3, lines.size()); index++) {
            graphics.drawString(font, lines.get(index), left,
                    perkTop + 12 + index * 10, 0xFF_E8D8B5, false);
        }

        if (mouseX >= left && mouseX < left + availableWidth
                && mouseY >= perkTop && mouseY < cardTop) {
            List<Component> tooltip = model.activePerks().stream()
                    .limit(Math.max(1, visibleCount))
                    .map(perk -> (Component) Component.translatable(perk.descriptionKey()))
                    .toList();
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private List<StatsScreenModel.StatCard> selectedCards() {
        return model.families().stream()
                .filter(section -> section.family() == selectedFamily)
                .findFirst()
                .map(StatsScreenModel.FamilySection::cards)
                .orElseGet(ArrayList::new);
    }

    private void clampScroll() {
        int rows = (selectedCards().size() + columns - 1) / Math.max(1, columns);
        int contentHeight = rows * (StatCardRenderer.HEIGHT + GAP) - GAP;
        int viewportHeight = Math.max(0, cardBottom - cardTop);
        double max = Math.max(0, contentHeight - viewportHeight);
        scrollOffset = Math.max(0, Math.min(max, scrollOffset));
    }
}
