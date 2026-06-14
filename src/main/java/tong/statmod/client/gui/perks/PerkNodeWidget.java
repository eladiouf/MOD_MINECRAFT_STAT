package tong.statmod.client.gui.perks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkTier;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class PerkNodeWidget extends AbstractWidget {
    private static final int NODE_SIZE = 24;

    private final Perk perk;
    private final boolean unlocked;
    private final boolean canUnlock;
    private final Consumer<Perk> onClick;

    public PerkNodeWidget(int x, int y, Perk perk, boolean unlocked, boolean canUnlock, Consumer<Perk> onClick) {
        super(x, y,
                perk.tier == PerkTier.TRANSCENDENCE ? NODE_SIZE + 8 : NODE_SIZE,
                perk.tier == PerkTier.TRANSCENDENCE ? NODE_SIZE + 8 : NODE_SIZE,
                Component.literal(perk.name));
        this.perk = perk;
        this.unlocked = unlocked;
        this.canUnlock = canUnlock;
        this.onClick = onClick;
    }

    public Perk getPerk() { return perk; }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int borderColor = switch (perk.tier) {
            case CORE -> 0xFF808080;
            case ACTIVE -> 0xFF00AA00;
            case SYNERGY -> 0xFF5555FF;
            case SITUATIONAL -> 0xFFFFAA00;
            case MASTERY -> 0xFFAA00AA;
            case TRANSCENDENCE -> 0xFFD4FF00;
        };

        int fillColor = unlocked ? 0xFF333333 : (canUnlock ? 0xFF555555 : 0xFF222222);
        int size = getWidth();

        graphics.fill(getX(), getY(), getX() + size, getY() + size, borderColor);
        graphics.fill(getX() + 2, getY() + 2, getX() + size - 2, getY() + size - 2, fillColor);

        Font font = Minecraft.getInstance().font;
        String label = perk.name.substring(0, Math.min(2, perk.name.length()));
        graphics.drawCenteredString(font, label, getX() + size / 2, getY() + size / 2 - 4, 0xFFFFFFFF);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (canUnlock) onClick.accept(perk);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
