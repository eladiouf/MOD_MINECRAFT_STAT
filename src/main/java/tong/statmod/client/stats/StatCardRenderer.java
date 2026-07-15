package tong.statmod.client.stats;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class StatCardRenderer {
    public static final int HEIGHT = 66;

    private static final int BACKGROUND = 0xE6_241B13;
    private static final int BORDER = 0xFF_9B7542;
    private static final int TEXT = 0xFF_F4E4BE;
    private static final int MUTED = 0xFF_C7AE82;
    private static final int BAR_BACKGROUND = 0xFF_38291D;
    private static final int BAR_FILL = 0xFF_B88632;

    private StatCardRenderer() {
    }

    public static void render(GuiGraphics graphics, Font font,
            StatsScreenModel.StatCard card, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + HEIGHT, BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + HEIGHT - 1, BACKGROUND);

        Component name = Component.translatable(card.presentation().nameKey());
        Component level = card.maxLevel()
                ? Component.translatable("screen.statmod.max")
                : Component.translatable("screen.statmod.level", card.value().level());
        Component status = Component.translatable(card.presentation().state()
                == StatDisplayState.ACTIVE
                        ? "screen.statmod.status.active"
                        : "screen.statmod.status.foundation");
        Component xp = card.maxLevel()
                ? Component.translatable("screen.statmod.max")
                : Component.translatable("screen.statmod.xp",
                        card.value().xp(), card.requiredXp());

        graphics.drawString(font, name, x + 7, y + 6, TEXT, false);
        graphics.drawString(font, level, x + 7, y + 20, MUTED, false);
        graphics.drawString(font, status, x + 7, y + 33, MUTED, false);
        graphics.drawString(font, xp, x + width - 7 - font.width(xp), y + 46, MUTED, false);

        int barX = x + 7;
        int barY = y + 57;
        int barWidth = width - 14;
        graphics.fill(barX, barY, barX + barWidth, barY + 4, BAR_BACKGROUND);
        int filled = (int) Math.round(barWidth * card.progress());
        graphics.fill(barX, barY, barX + filled, barY + 4, BAR_FILL);
    }

    public static void renderTooltip(GuiGraphics graphics, Font font,
            StatsScreenModel.StatCard card, int mouseX, int mouseY) {
        graphics.renderComponentTooltip(font,
                List.of(Component.translatable(card.presentation().descriptionKey())),
                mouseX, mouseY);
    }

    public static Component narration(StatsScreenModel.StatCard card) {
        Component name = Component.translatable(card.presentation().nameKey());
        Component xp = card.maxLevel()
                ? Component.translatable("screen.statmod.max")
                : Component.translatable("screen.statmod.xp",
                        card.value().xp(), card.requiredXp());
        Component status = Component.translatable(card.presentation().state()
                == StatDisplayState.ACTIVE
                        ? "screen.statmod.status.active"
                        : "screen.statmod.status.foundation");
        return Component.translatable("narration.statmod.card",
                name, card.value().level(), xp, status);
    }
}
