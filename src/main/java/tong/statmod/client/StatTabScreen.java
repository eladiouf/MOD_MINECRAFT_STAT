package tong.statmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import tong.statmod.integration.puffish.PuffishSkillsCompat;
import tong.statmod.integration.PlayerDataBridge;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.integration.tensura.TensuraToMagicRaceMapper;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

public class StatTabScreen extends Screen {
    private int scrollOffset;

    public StatTabScreen() {
        super(Component.translatable("statmod.tab.stats"));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBlurredBackground(partialTick);
        graphics.fill(0, 0, width, height, 0xCC111122);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int soulLevel = ClientStatCache.getSoulLevel();
        int maxStat = soulLevel > 0 ? Math.min(soulLevel, 100) : 100;

        String race = "Unknown";
        try {
            String r = PlayerDataBridge.getRaceId(player);
            race = TensuraToMagicRaceMapper.displayRaceName(r);
        } catch (Exception ignored) {}

        int cx = width / 2;
        String head = "\u00a7e\u2726 STAT MOD \u2726\u00a7r  \u00a77Race: \u00a7f" + race + "  \u00a77Soul Lv.\u00a7f" + soulLevel + "  \u00a77Cap \u00a7f" + maxStat;
        graphics.drawString(font, head, cx - font.width(head) / 2, 20, 0xFFFFAA00);

        int cols = 2;
        int totalStats = StatType.values().length;
        int rowsPerCol = (totalStats + cols - 1) / cols;
        int maxScroll = Math.max(0, rowsPerCol - 10);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);

        int colW = Math.min(200, (width - 60) / cols);
        int areaX = cx - (colW * cols) / 2;
        int areaY = 45;

        int startRow = scrollOffset;
        int endRow = Math.min(startRow + 10, rowsPerCol);

        for (int row = startRow; row < endRow; row++) {
            for (int col = 0; col < cols; col++) {
                int idx = col * rowsPerCol + row;
                if (idx >= totalStats) continue;
                StatType stat = StatType.values()[idx];

                int level = RaceEffectApplier.getEffectiveLevel(player, stat.index);
                int xp = ClientStatCache.getXp(stat.index);
                int needed = PlayerStatData.requiredXp(level);
                int color = stat.hasPerks() ? (level >= maxStat ? 0xFFFFAA00 : 0xFFFFFFFF) : 0xFF808080;

                String line = String.format("%s: Lv.%d (%d/%d)", stat.displayName, level, xp, needed);
                graphics.drawString(font, line, areaX + col * colW, areaY + (row - startRow) * 12, color);
            }
        }

        String nav = maxScroll > 0
            ? "\u00a77Ligne " + (scrollOffset + 1) + "/" + rowsPerCol + "  \u00a78\u2191\u2193 molette"
            : "\u00a77ESC fermer  |  P arbre de perks";
        graphics.drawString(font, Component.literal(nav), cx - font.width(nav) / 2, height - 30, 0xFF888888);

        String btnLabel = "\u00a7e\u27f6 Perk Tree\u00a7r";
        int btnW = font.width(btnLabel) + 12;
        int btnH = 14;
        int btnX = cx + Math.min(colW * cols / 2 - 20, 100);
        int btnY = 20;
        boolean hover = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, hover ? 0xFF334466 : 0xFF222244);
        graphics.drawString(font, Component.literal(btnLabel), btnX + 6, btnY + 3, 0xFFFFAA00);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int cx = width / 2;
            int cols = 2;
            int totalStats = StatType.values().length;
            int colW = Math.min(200, (width - 60) / cols);
            String btnLabel = "\u00a7e\u27f6 Perk Tree\u00a7r";
            int btnW = font.width(btnLabel) + 12;
            int btnH = 14;
            int btnX = cx + Math.min(colW * cols / 2 - 20, 100);
            int btnY = 20;
            if (mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                PerkUiRouter.openFromClient(PuffishSkillsCompat.isLoaded());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int cols = 2;
        int totalStats = StatType.values().length;
        int rowsPerCol = (totalStats + cols - 1) / cols;
        int maxScroll = Math.max(0, rowsPerCol - 10);
        scrollOffset = Math.clamp(scrollOffset - (int)scrollY, 0, maxScroll);
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256 || key == 66) { onClose(); return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
