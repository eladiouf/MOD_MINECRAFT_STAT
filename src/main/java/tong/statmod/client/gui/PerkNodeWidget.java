package tong.statmod.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.client.ClientPerkCache;
import tong.statmod.client.ClientStatCache;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkTier;
import tong.statmod.network.UnlockPerkPayload;

@OnlyIn(Dist.CLIENT)
public class PerkNodeWidget {
    private static final int WIDTH = 90;
    private static final int HEIGHT = 26;

    private final Perk perk;
    private final int x;
    private final int y;

    public PerkNodeWidget(Perk perk, int x, int y) {
        this.perk = perk;
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return WIDTH; }
    public int getHeight() { return HEIGHT; }
    public Perk getPerk() { return perk; }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    public boolean tryClick() {
        if (perk == null) return false;
        boolean unlocked = ClientPerkCache.isUnlocked(perk);
        if (unlocked) return false;
        int statLevel = ClientStatCache.getLevel(perk.stat.index);
        int points = ClientPerkCache.getPointsForStat(perk.stat.index);
        if (statLevel < perk.tier.requiredStatLevel) return false;
        if (points < perk.tier.cost) return false;
        PacketDistributor.sendToServer(new UnlockPerkPayload(perk.id));
        return true;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, Font font) {
        if (perk == null) return;

        boolean unlocked = ClientPerkCache.isUnlocked(perk);
        int statLevel = ClientStatCache.getLevel(perk.stat.index);
        int points = ClientPerkCache.getPointsForStat(perk.stat.index);
        boolean meetsLevel = statLevel >= perk.tier.requiredStatLevel;
        boolean canAfford = points >= perk.tier.cost;
        boolean hovered = isMouseOver(mouseX, mouseY);

        int borderColor = tierColor(perk.tier);
        int fillColor;
        if (unlocked) {
            fillColor = 0xFF2A2A2A;
        } else if (meetsLevel && canAfford) {
            fillColor = 0xFF1A1A2A;
        } else {
            fillColor = 0xFF111111;
        }

        if (hovered) {
            fillColor = 0xFF2A2A3A;
        }

        graphics.fill(x, y, x + WIDTH, y + HEIGHT, fillColor);
        graphics.fill(x, y, x + WIDTH, y + 1, borderColor);
        graphics.fill(x, y, x + 1, y + HEIGHT, borderColor);
        graphics.fill(x + WIDTH - 1, y, x + WIDTH, y + HEIGHT, borderColor);
        graphics.fill(x, y + HEIGHT - 1, x + WIDTH, y + HEIGHT, borderColor);

        int textColor;
        if (unlocked) {
            textColor = 0xFFFFFFFF;
        } else if (meetsLevel && canAfford) {
            textColor = 0xFF55FF55;
        } else {
            textColor = 0xFF666666;
        }

        String display = perk.name;
        while (!display.isEmpty() && font.width(display) > WIDTH - 6) {
            display = display.substring(0, display.length() - 1);
        }
        graphics.drawString(font, display, x + (WIDTH - font.width(display)) / 2, y + (HEIGHT - 9) / 2, textColor);

        if (hovered) {
            int tooltipX = mouseX + 8;
            int tooltipY = mouseY - 12;

            String tierName = perk.tier.name();
            String costText = "Cost: " + perk.tier.cost + " pt" + (perk.tier.cost > 1 ? "s" : "");
            String reqText = "Req: " + perk.tier.requiredStatLevel;
            String pointsText = "Points: " + points;

            int tw = Math.max(
                Math.max(font.width(perk.name), font.width(perk.description)),
                Math.max(font.width(tierName), font.width(costText + " | " + reqText + " | " + pointsText))
            ) + 10;
            int th = 52;

            if (tooltipX + tw > graphics.guiWidth()) tooltipX = mouseX - tw - 8;
            if (tooltipY < 0) tooltipY = mouseY + 12;
            if (tooltipY + th > graphics.guiHeight()) tooltipY = graphics.guiHeight() - th - 4;

            graphics.fill(tooltipX, tooltipY, tooltipX + tw, tooltipY + th, 0xCC222222);
            graphics.fill(tooltipX, tooltipY, tooltipX + tw, tooltipY + 1, borderColor);
            graphics.fill(tooltipX, tooltipY, tooltipX + 1, tooltipY + th, borderColor);
            graphics.fill(tooltipX + tw - 1, tooltipY, tooltipX + tw, tooltipY + th, borderColor);
            graphics.fill(tooltipX, tooltipY + th - 1, tooltipX + tw, tooltipY + th, borderColor);

            int ly = tooltipY + 4;
            graphics.drawString(font, Component.literal("\u00a7f" + perk.name + "\u00a7r"), tooltipX + 5, ly, 0xFFFFFFFF);
            ly += 10;
            graphics.drawString(font, Component.literal("\u00a77" + perk.description + "\u00a7r"), tooltipX + 5, ly, 0xFFBBBBBB);
            ly += 10;
            graphics.drawString(font, Component.literal("\u00a7e" + tierName + "\u00a7r  \u00a7a" + costText + "\u00a7r  \u00a7b" + reqText + "\u00a7r  \u00a7d" + pointsText + "\u00a7r"), tooltipX + 5, ly, 0xFFCCCCCC);
            ly += 10;

            if (perk.synergyStat != null) {
                graphics.drawString(font, Component.literal("\u00a75Synergy: " + perk.synergyStat.displayName + "\u00a7r"), tooltipX + 5, ly, 0xFFAA55FF);
                ly += 10;
            }

            if (!meetsLevel) {
                graphics.drawString(font, Component.literal("\u00a7cStat level too low (" + statLevel + "/" + perk.tier.requiredStatLevel + ")\u00a7r"), tooltipX + 5, ly, 0xFFFF5555);
            } else if (!canAfford) {
                graphics.drawString(font, Component.literal("\u00a7cNot enough perk points (" + points + "/" + perk.tier.cost + ")\u00a7r"), tooltipX + 5, ly, 0xFFFF5555);
            } else if (!unlocked) {
                graphics.drawString(font, Component.literal("\u00a7aClick to unlock!\u00a7r"), tooltipX + 5, ly, 0xFF55FF55);
            }
        }
    }

    public static int tierColor(PerkTier tier) {
        return switch (tier) {
            case CORE -> 0xFF808080;
            case ACTIVE -> 0xFF55FF55;
            case SYNERGY -> 0xFF5555FF;
            case SITUATIONAL -> 0xFFFFFF55;
            case MASTERY -> 0xFFAA00AA;
            case TRANSCENDENCE -> 0xFFFFAA00;
        };
    }
}
