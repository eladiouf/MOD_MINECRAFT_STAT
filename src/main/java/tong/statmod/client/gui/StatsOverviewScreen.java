package tong.statmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import tong.statmod.client.ClientStatCache;
import tong.statmod.stats.StatType;
import tong.statmod.storage.PlayerStatData;

@OnlyIn(Dist.CLIENT)
public class StatsOverviewScreen extends Screen {
    private static final int LINE_HEIGHT = 12;

    public StatsOverviewScreen() {
        super(Component.literal("Stats"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int y = 20;
        for (StatType stat : StatType.values()) {
            int level = ClientStatCache.getLevel(stat.index);
            int xp = ClientStatCache.getXp(stat.index);
            int needed = PlayerStatData.requiredXp(level);
            String text = String.format("%s: Lv.%d (%d/%d)", stat.displayName, level, xp, needed);
            graphics.drawString(font, text, 20, y, stat.hasPerks() ? 0xFFFFFFFF : 0xFF808080);
            y += LINE_HEIGHT;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    public static void open() {
        Minecraft.getInstance().setScreen(new StatsOverviewScreen());
    }
}
