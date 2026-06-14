package tong.statmod.client.gui.perks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import tong.statmod.client.ClientPerkCache;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkTier;
import tong.statmod.stats.StatType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class TalentTreePanel extends AbstractWidget {
    private final StatType stat;
    private final List<PerkNodeWidget> nodes = new ArrayList<>();
    private final Consumer<Perk> onUnlockRequest;

    public TalentTreePanel(int x, int y, int width, int height, StatType stat, Consumer<Perk> onUnlockRequest) {
        super(x, y, width, height, Component.literal(stat.displayName));
        this.stat = stat;
        this.onUnlockRequest = onUnlockRequest;
        initNodes();
    }

    private void initNodes() {
        nodes.clear();
        PerkTier[] tiers = PerkTier.values();
        int nodeSpacing = 26;
        int startY = getY() + 20;

        for (int i = 0; i < tiers.length; i++) {
            Perk perk = Perk.byStatAndTier(stat, tiers[i]);
            if (perk == null) continue;

            int nx = getX() + (getWidth() / 2) - 12;
            int ny = startY + i * nodeSpacing;
            boolean unlocked = ClientPerkCache.isUnlocked(perk);
            boolean canUnlock = !unlocked && hasEnoughPoints(perk);

            PerkNodeWidget widget = new PerkNodeWidget(nx, ny, perk, unlocked, canUnlock, onUnlockRequest);
            nodes.add(widget);
        }
    }

    private boolean hasEnoughPoints(Perk perk) {
        return ClientPerkCache.getPointsForStat(stat.index) >= perk.tier.cost;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF1A1A1A);
        graphics.drawString(Minecraft.getInstance().font, stat.displayName, getX() + 5, getY() + 5, 0xFFFFFFFF);

        for (PerkNodeWidget node : nodes) {
            node.render(graphics, mouseX, mouseY, partialTick);
        }

        int points = ClientPerkCache.getPointsForStat(stat.index);
        String ptsText = points + " pts";
        graphics.drawString(Minecraft.getInstance().font, ptsText,
                getX() + getWidth() - 30, getY() + 5, 0xFFD4FF00);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (PerkNodeWidget node : nodes) {
            if (node.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
