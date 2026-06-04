package tong.statmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.stats.StatType;

public class DebugScreen extends Screen {
    private static final int BG_COLOR = 0xE0000000;
    private int scrollOffset = 0;
    private EditBox searchBox;
    private String filterText = "";

    public DebugScreen() { super(Component.literal("STAT Mod Debug")); }

    @Override
    protected void init() {
        super.init();
        this.searchBox = new EditBox(this.font, this.width / 2 - 80, 10, 160, 20, Component.literal("Filter..."));
        this.searchBox.setResponder(s -> filterText = s.toLowerCase());
        addRenderableWidget(this.searchBox);

        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> {})
            .pos(this.width / 2 + 90, 8).size(60, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
            .pos(this.width / 2 + 155, 8).size(50, 20).build());

        int x = 10;
        int y = 40;
        for (StatType stat : StatType.values()) {
            if (!filterText.isEmpty() && !stat.displayName.toLowerCase().contains(filterText)
                && !stat.name().toLowerCase().contains(filterText)) continue;
            addRenderableWidget(Button.builder(Component.literal("+"), b -> {})
                .pos(x + 170, y).size(16, 16).build());
            addRenderableWidget(Button.builder(Component.literal("-"), b -> {})
                .pos(x + 188, y).size(16, 16).build());
            y += 22;
            if (y > this.height - 30) break;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, BG_COLOR);
        super.render(graphics, mouseX, mouseY, partialTick);

        int x = 10;
        int y = 40;
        for (StatType stat : StatType.values()) {
            if (!filterText.isEmpty() && !stat.displayName.toLowerCase().contains(filterText)
                && !stat.name().toLowerCase().contains(filterText)) continue;
            int level = ClientStatsCache.getLevel(stat);
            String text = stat.displayName + ": " + level + "/100";
            int color = level >= 100 ? 0xFF55FF55 : level >= 80 ? 0xFFFFAA00 : 0xFFFFFFFF;
            graphics.drawString(this.font, text, x, y + 4, color);
            y += 22;
            if (y > this.height - 30) break;
        }

        int infoX = this.width - 280;
        int infoY = 40;
        int globalLevel = ClientStatsCache.getGlobalLevel();
        graphics.drawString(this.font, "Global Level: " + globalLevel, infoX, infoY, 0xFFFFAA00);
        infoY += 12;
        graphics.drawString(this.font, "Fatigue: " + String.format("%.0f", ClientStatsCache.getFatigue())
            + "/" + ClientStatsCache.getMaxFatigue(), infoX, infoY, 0xFFAAAAAA);
        infoY += 12;
        graphics.drawString(this.font, "Thirst: " + String.format("%.0f", ClientStatsCache.getThirst()), infoX, infoY, 0xFFAAAAAA);

        String dmgInfo = calcDamageInfo();
        infoY += 20;
        graphics.drawString(this.font, "Damage multipliers:", infoX, infoY, 0xFFCCCCCC);
        infoY += 12;
        String[] lines = dmgInfo.split("\n");
        for (String line : lines) {
            graphics.drawString(this.font, line, infoX + 10, infoY, 0xFF999999);
            infoY += 10;
        }
    }

    private String calcDamageInfo() {
        int bf = ClientStatsCache.getLevel(StatType.BRUTE_FORCE);
        int bt = ClientStatsCache.getLevel(StatType.BLADE_TECHNIQUE);
        int pr = ClientStatsCache.getLevel(StatType.PRECISION);
        int pe = ClientStatsCache.getLevel(StatType.PHYSICAL_RESISTANCE);

        return "Brute Force: +" + Math.round(tong.statmod.stats.StatCalculator.getDamageBonus(bf) * 100) + "%\n"
            + "Blade Tech: +" + Math.round(tong.statmod.stats.StatCalculator.getBladeDamageBonus(bt) * 100) + "%\n"
            + "Crit Chance: " + Math.round(tong.statmod.stats.StatCalculator.getCritChance(pr) * 100) + "%\n"
            + "Dmg Reduction: " + Math.round(tong.statmod.stats.StatCalculator.getDamageReduction(pe) * 100) + "%\n"
            + "Endurance: +" + Math.round(tong.statmod.stats.StatCalculator.getEnduranceHearts(ClientStatsCache.getLevel(StatType.PHYSICAL_ENDURANCE)) * 10) / 10f + " hearts";
    }

    @Override
    public void tick() {
        if (this.searchBox != null) this.searchBox.tick();
        super.tick();
    }

    @Override
    public boolean isPauseScreen() { return true; }
}
