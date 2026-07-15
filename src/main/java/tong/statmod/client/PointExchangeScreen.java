package tong.statmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import tong.statmod.network.StatNetwork;

public class PointExchangeScreen extends Screen {

    private int points;
    private long coins;
    private float rate;
    private EditBox amountBox;

    public PointExchangeScreen(int points, long coins, float rate) {
        super(Component.translatable("shop.exchange.title"));
        this.points = points;
        this.coins = coins;
        this.rate = rate;
    }

    public static void openOrRefresh(int points, long coins, float rate) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PointExchangeScreen s) {
            s.points = points;
            s.coins = coins;
            s.rate = rate;
        } else {
            mc.setScreen(new PointExchangeScreen(points, coins, rate));
        }
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        amountBox = new EditBox(this.font, cx - 60, cy - 14, 120, 20, Component.translatable("shop.exchange.amount"));
        amountBox.setValue("");
        amountBox.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        addRenderableWidget(amountBox);
        
        addRenderableWidget(Button.builder(Component.translatable("shop.exchange.convert"),
                b -> convert(parseAmount())).bounds(cx - 60, cy + 12, 58, 20).build());
                
        int safeMax = Math.max(0, points - 1);
        addRenderableWidget(Button.builder(Component.translatable("shop.exchange.max_safe"),
                b -> convert(safeMax)).bounds(cx + 2, cy + 12, 58, 20).build());
                
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                b -> onClose()).bounds(cx - 60, cy + 36, 120, 20).build());
    }

    private int parseAmount() {
        try { return Integer.parseInt(amountBox.getValue().trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private void convert(int amount) {
        if (amount > 0) StatNetwork.sendConvertPoints(amount);
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        super.renderBackground(g);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        
        int cx = this.width / 2;
        int cy = this.height / 2;
        
        // Premium UI Background (Glassmorphism effect)
        int panelWidth = 180;
        int panelHeight = 160;
        int left = cx - panelWidth / 2;
        int top = cy - panelHeight / 2 - 10;
        int right = cx + panelWidth / 2;
        int bottom = cy + panelHeight / 2 + 10;
        
        // Inner gradient
        g.fillGradient(left, top, right, bottom, 0xE0101010, 0xF0050505);
        // Cyan/Gold Border
        int borderColor = 0xFF55FFFF;
        g.fill(left - 1, top - 1, right + 1, top, borderColor);
        g.fill(left - 1, bottom, right + 1, bottom + 1, borderColor);
        g.fill(left - 1, top, left, bottom, borderColor);
        g.fill(right, top, right + 1, bottom, borderColor);

        g.drawCenteredString(this.font, this.title, cx, top + 10, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("shop.exchange.points", points), cx, top + 26, 0xFFE066);
        g.drawCenteredString(this.font, Component.translatable("shop.exchange.coins", coins), cx, top + 38, 0x66FF66);
        g.drawCenteredString(this.font, Component.translatable("shop.exchange.rate", String.format("%.2f", rate)), cx, top + 50, 0xAAAAAA);

        super.render(g, mouseX, mouseY, partialTick);
    }
}
