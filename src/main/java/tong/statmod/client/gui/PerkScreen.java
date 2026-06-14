package tong.statmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.client.ClientStatCache;
import tong.statmod.client.gui.perks.TalentTreePanel;
import tong.statmod.network.UnlockPerkPayload;
import tong.statmod.stats.StatType;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class PerkScreen extends Screen {
    private static final int PANEL_WIDTH = 130;
    private static final int PANEL_HEIGHT = 200;
    private static final int COLS = 4;
    private final List<TalentTreePanel> panels = new ArrayList<>();

    public PerkScreen() {
        super(Component.literal("Perks"));
    }

    @Override
    protected void init() {
        panels.clear();
        int startX = (width - COLS * (PANEL_WIDTH + 10)) / 2;
        int startY = 40;
        int col = 0;
        int row = 0;

        for (StatType stat : StatType.values()) {
            if (!stat.hasPerks()) continue;
            int x = startX + col * (PANEL_WIDTH + 10);
            int y = startY + row * (PANEL_HEIGHT + 10);
            TalentTreePanel panel = new TalentTreePanel(x, y, PANEL_WIDTH, PANEL_HEIGHT, stat,
                    perk -> PacketDistributor.sendToServer(new UnlockPerkPayload(perk.id)));
            panels.add(panel);
            addRenderableWidget(panel);

            col++;
            if (col >= COLS) {
                col = 0;
                row++;
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int global = 0;
        int count = 0;
        for (StatType s : StatType.values()) {
            if (s.hasPerks()) {
                global += ClientStatCache.getLevel(s.index);
                count++;
            }
        }
        String levelText = "Global Level: " + (count > 0 ? global / count : 0);
        graphics.drawString(font, levelText, 10, 10, 0xFFD4FF00);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    public static void open() {
        Minecraft.getInstance().setScreen(new PerkScreen());
    }
}
