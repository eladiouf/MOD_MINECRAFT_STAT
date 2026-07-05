package tong.statmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.network.ConvertPointsPayload;

/** Écran d'échange points → coins. Ouvert par OpenExchangePayload ; envoie ConvertPointsPayload. */
public class PointExchangeScreen extends Screen {

    private int points;
    private long coins;
    private EditBox amountBox;

    public PointExchangeScreen(int points, long coins) {
        super(Component.translatable("shop.exchange.title"));
        this.points = points;
        this.coins = coins;
    }

    /** Ouvre l'écran s'il n'est pas déjà ouvert, sinon met à jour ses valeurs (rafraîchissement). */
    public static void openOrRefresh(int points, long coins) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PointExchangeScreen s) {
            s.points = points;
            s.coins = coins;
        } else {
            mc.setScreen(new PointExchangeScreen(points, coins));
        }
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        amountBox = new EditBox(this.font, cx - 60, cy - 10, 120, 20, Component.translatable("shop.exchange.amount"));
        amountBox.setValue("");
        addRenderableWidget(amountBox);
        addRenderableWidget(Button.builder(Component.translatable("shop.exchange.convert"),
                b -> convert(parseAmount())).bounds(cx - 60, cy + 16, 58, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("shop.exchange.max_safe"),
                b -> convert(Math.max(0, points - 1))).bounds(cx + 2, cy + 16, 58, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                b -> onClose()).bounds(cx - 60, cy + 40, 120, 20).build());
    }

    private int parseAmount() {
        try { return Integer.parseInt(amountBox.getValue().trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private void convert(int amount) {
        if (amount > 0) PacketDistributor.sendToServer(new ConvertPointsPayload(amount));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int cy = this.height / 2;
        g.drawCenteredString(this.font, this.title, cx, cy - 60, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("shop.exchange.points", points), cx, cy - 44, 0xFFE066);
        g.drawCenteredString(this.font, Component.translatable("shop.exchange.coins", coins), cx, cy - 32, 0x66FF66);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
