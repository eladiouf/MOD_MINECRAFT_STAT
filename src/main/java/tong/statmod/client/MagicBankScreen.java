package tong.statmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.network.MagicBankActionPayload;

/** Interface Premium du Banquier magique. */
public final class MagicBankScreen extends Screen {
    private long balance;
    private long physical;
    private EditBox amount;

    public MagicBankScreen(long balance, long physical) {
        super(Component.translatable("banker.magic.title"));
        this.balance = balance;
        this.physical = physical;
    }

    public static void openOrRefresh(long balance, long physical) {
        if (Minecraft.getInstance().screen instanceof MagicBankScreen screen) {
            screen.balance = balance;
            screen.physical = physical;
        } else Minecraft.getInstance().setScreen(new MagicBankScreen(balance, physical));
    }

    @Override protected void init() {
        int cx = width / 2, cy = height / 2;
        amount = new EditBox(font, cx - 70, cy - 6, 140, 20, Component.translatable("banker.magic.amount"));
        // Filtre : uniquement des chiffres
        amount.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        addRenderableWidget(amount);
        
        addRenderableWidget(Button.builder(Component.translatable("banker.magic.deposit_all"), b ->
                PacketDistributor.sendToServer(new MagicBankActionPayload(MagicBankActionPayload.DEPOSIT_ALL, 0)))
                .bounds(cx - 70, cy + 20, 68, 20).build());
                
        addRenderableWidget(Button.builder(Component.translatable("banker.magic.withdraw"), b -> withdraw(parse()))
                .bounds(cx + 2, cy + 20, 68, 20).build());
                
        addRenderableWidget(Button.builder(Component.translatable("banker.magic.withdraw_max"), b ->
                withdraw(balance - balance % 50L)).bounds(cx - 70, cy + 44, 140, 20).build());
                
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(cx - 70, cy + 68, 140, 20).build());
    }

    private long parse() {
        try { return Long.parseLong(amount.getValue().trim()); }
        catch (NumberFormatException ignored) { return 0L; }
    }

    private void withdraw(long value) {
        if (value > 0 && value % 50 == 0) {
            PacketDistributor.sendToServer(new MagicBankActionPayload(MagicBankActionPayload.WITHDRAW, value));
        }
    }

    /**
     * 1.21 : le flou d'arrière-plan est appliqué par {@code renderBackground} en post-process
     * sur tout ce qui est déjà dessiné. Le panneau et les textes doivent donc être dessinés
     * ICI (après le flou du monde, avant les widgets) — les dessiner dans {@code render()}
     * avant {@code super.render()} les ferait flouter par la passe de fond de Screen.
     */
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2, cy = height / 2;
        
        // Premium UI Background (Glassmorphism effect)
        int panelWidth = 180;
        int panelHeight = 160;
        int left = cx - panelWidth / 2;
        int top = cy - panelHeight / 2 - 10;
        int right = cx + panelWidth / 2;
        int bottom = cy + panelHeight / 2 + 10;
        
        // Inner gradient
        graphics.fillGradient(left, top, right, bottom, 0xE0101010, 0xF0050505);
        // Golden Border
        int borderColor = 0xFFD4AF37;
        graphics.fill(left - 1, top - 1, right + 1, top, borderColor);
        graphics.fill(left - 1, bottom, right + 1, bottom + 1, borderColor);
        graphics.fill(left - 1, top, left, bottom, borderColor);
        graphics.fill(right, top, right + 1, bottom, borderColor);

        graphics.drawCenteredString(font, title, cx, top + 10, 0xFFD65A);
        graphics.drawCenteredString(font, Component.translatable("banker.magic.balance", balance), cx, top + 30, 0x66FF99);
        graphics.drawCenteredString(font, Component.translatable("banker.magic.physical", physical), cx, top + 42, 0xFFE9A8);
        
        // Warning messages if typed amount is invalid
        long typed = parse();
        if (typed > 0) {
            if (typed % 50 != 0) {
                graphics.drawCenteredString(font, Component.translatable("Billets de 50 FDP minimum"), cx, top + 56, 0xFF5555);
            } else if (typed > balance) {
                graphics.drawCenteredString(font, Component.translatable("Solde insuffisant"), cx, top + 56, 0xFF5555);
            }
        }
    }

    /** Pas de flou du monde derrière l'UI (choix visuel du mod). */
    @Override protected void renderBlurredBackground(float partialTick) {}

    @Override public boolean isPauseScreen() { return false; }
}
