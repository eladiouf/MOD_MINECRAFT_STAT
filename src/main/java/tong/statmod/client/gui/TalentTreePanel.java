package tong.statmod.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import tong.statmod.client.ClientPerkCache;
import tong.statmod.client.ClientStatCache;
import tong.statmod.integration.RaceEffectApplier;
import tong.statmod.integration.SkillPerkGate;
import tong.statmod.perks.Perk;
import tong.statmod.perks.PerkTier;
import tong.statmod.stats.StatFamily;
import tong.statmod.stats.StatType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class TalentTreePanel {
    static final int NODE_W = 88;
    static final int NODE_H = 24;
    private static final int NODE_GAP = 8;
    private static final int COL_GAP = 10;
    private static final int SECTION_HEADER_H = 18;
    private static final int STAT_HEADER_H = 12;

    static final int PERKS_PER_STAT = PerkTier.values().length;
    static final int STAT_COL_H = PERKS_PER_STAT * (NODE_H + NODE_GAP);

    private final int x;
    private final int y;
    private final int panelWidth;
    private final int panelHeight;
    private final int statsPerRow;
    private final int colW;

    private final List<StatFamily> familyOrder = new ArrayList<>();
    private final Map<StatType, List<Perk>> statPerks = new LinkedHashMap<>();
    private final List<Slot> slots = new ArrayList<>();
    private Perk selectedPerk;
    private int totalContentH;
    private int scrollOffset;

    private record Slot(Perk perk, int rx, int ry) {}

    public TalentTreePanel(List<StatType> allStats, int x, int y, int panelWidth, int panelHeight) {
        this.x = x;
        this.y = y;
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.colW = NODE_W + COL_GAP;
        this.statsPerRow = Math.max(1, (panelWidth - 20) / colW);

        for (StatType stat : allStats) {
            if (!familyOrder.contains(stat.family)) familyOrder.add(stat.family);
            List<Perk> list = new ArrayList<>();
            for (PerkTier tier : PerkTier.values()) {
                Perk p = Perk.byStatAndTier(stat, tier);
                if (p != null) list.add(p);
            }
            statPerks.put(stat, list);
        }

        rebuildSlots();
        this.scrollOffset = 0;
    }

    private void rebuildSlots() {
        slots.clear();
        int cx = x + 10;
        int cy = y + 10;
        totalContentH = 10;

        for (StatFamily family : familyOrder) {
            cy += SECTION_HEADER_H;
            totalContentH += SECTION_HEADER_H;
            int col = 0;
            for (Map.Entry<StatType, List<Perk>> entry : statPerks.entrySet()) {
                if (entry.getKey().family != family) continue;
                int sx = cx + (col % statsPerRow) * colW;
                int sy = cy + (col / statsPerRow) * (STAT_COL_H + 10);
                int row = 0;
                for (Perk perk : entry.getValue()) {
                    slots.add(new Slot(perk, sx, sy + row * (NODE_H + NODE_GAP)));
                    row++;
                }
                col++;
            }
            int cols = (int) statPerks.keySet().stream().filter(s -> s.family == family).count();
            int rows = (cols + statsPerRow - 1) / statsPerRow;
            int sectionH = rows * (STAT_COL_H + 10) + 6;
            cy += sectionH;
            totalContentH += sectionH;
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, Font font) {
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xCC111111);
        graphics.enableScissor(x + 1, y + 1, x + panelWidth - 1, y + panelHeight - 1);

        int maxScroll = Math.max(0, totalContentH - panelHeight + 20);
        scrollOffset = Math.max(-maxScroll, Math.min(0, scrollOffset));

        int cx = x + 10;
        int sy = y + scrollOffset + 10;

        Map<StatType, Integer> sectionStartY = new LinkedHashMap<>();

        for (StatFamily family : familyOrder) {
            int headerY = sy;
            if (headerY + SECTION_HEADER_H > y && headerY < y + panelHeight) {
                graphics.fill(cx, headerY, x + panelWidth - 10, headerY + SECTION_HEADER_H, familyColor(family));
                graphics.drawString(font, Component.literal("\u00a7l\u00a7f" + family.displayName + "\u00a7r"),
                        cx + 6, headerY + 4, 0xFFFFFFFF);
            }
            sy += SECTION_HEADER_H;

            int col = 0;
            for (Map.Entry<StatType, List<Perk>> entry : statPerks.entrySet()) {
                if (entry.getKey().family != family) continue;
                StatType stat = entry.getKey();
                int sx = cx + (col % statsPerRow) * colW;
                int baseY = sy + (col / statsPerRow) * (STAT_COL_H + 10);

                if (!sectionStartY.containsKey(stat)) {
                    sectionStartY.put(stat, baseY);
                }

                var player = Minecraft.getInstance().player;
                int statLevel = player != null
                        ? RaceEffectApplier.getEffectiveLevel(player, stat.index)
                        : ClientStatCache.getLevel(stat.index);

                int points = ClientPerkCache.getPointsForStat(stat.index);

                String label = stat.displayName + " Lv." + statLevel;
                int labelW = font.width(label);
                if (baseY - STAT_HEADER_H > y && baseY - STAT_HEADER_H < y + panelHeight) {
                    graphics.drawString(font, Component.literal("\u00a77" + label + "\u00a7r"),
                            sx + (NODE_W - labelW) / 2, baseY - STAT_HEADER_H, 0xFFAAAAAA);
                }

                int row = 0;
                for (Perk perk : entry.getValue()) {
                    int ny = baseY + row * (NODE_H + NODE_GAP);
                    if (ny + NODE_H > y && ny < y + panelHeight) {
                        boolean hovered = mouseX >= sx && mouseX < sx + NODE_W && mouseY >= ny && mouseY < ny + NODE_H;
                        PerkNodePresentation presentation = resolvePresentation(perk, statLevel, points, player);
                        boolean selected = perk == selectedPerk;

                        renderNode(graphics, font, sx, ny, perk, presentation, hovered, selected);
                    }
                    row++;
                }
                col++;
            }
            int cols = (int) statPerks.keySet().stream().filter(s -> s.family == family).count();
            int rows = (cols + statsPerRow - 1) / statsPerRow;
            sy += rows * (STAT_COL_H + 10) + 6;
        }

        graphics.disableScissor();
    }

    private void renderNode(GuiGraphics graphics, Font font, int nx, int ny, Perk perk,
                            PerkNodePresentation presentation, boolean hovered, boolean selected) {
        int borderColor = PerkNodeWidget.borderColor(perk.tier, selected);
        int fillColor = PerkNodeWidget.fillColor(presentation.state(), hovered, selected);
        int textColor = PerkNodeWidget.textColor(presentation.state());

        graphics.fill(nx, ny, nx + NODE_W, ny + NODE_H, fillColor);
        graphics.fill(nx, ny, nx + NODE_W, ny + 1, borderColor);
        graphics.fill(nx, ny, nx + 1, ny + NODE_H, borderColor);
        graphics.fill(nx + NODE_W - 1, ny, nx + NODE_W, ny + NODE_H, borderColor);
        graphics.fill(nx, ny + NODE_H - 1, nx + NODE_W, ny + NODE_H, borderColor);
        if (selected) {
            graphics.fill(nx + 2, ny + 2, nx + NODE_W - 2, ny + 3, 0xFFF4E7B3);
        }

        String display = perk.name;
        while (!display.isEmpty() && font.width(display) > NODE_W - 6) {
            display = display.substring(0, display.length() - 1);
        }
        graphics.drawString(font, display, nx + (NODE_W - font.width(display)) / 2, ny + (NODE_H - 9) / 2, textColor);

        if (hovered) {
            renderTooltip(graphics, font, nx, ny, perk, presentation);
        }
    }

    private void renderTooltip(GuiGraphics graphics, Font font, int nx, int ny, Perk perk,
                               PerkNodePresentation presentation) {
        int mouseX = nx + NODE_W / 2;
        int mouseY = ny + NODE_H / 2;
        int tooltipX = mouseX + 8;
        int tooltipY = mouseY - 12;

        List<Component> tooltip = presentation.tooltipLines(
                Component.literal(perk.name),
                Component.literal(perk.description),
                Component.literal(perk.tier.name() + " • " + perk.stat.displayName),
                Component.literal("Cost: " + perk.tier.cost + " point" + (perk.tier.cost > 1 ? "s" : "")));

        int tw = 0;
        for (Component line : tooltip) {
            tw = Math.max(tw, font.width(line));
        }
        tw += 10;
        int th = 8 + tooltip.size() * 10;

        if (tooltipX + tw > graphics.guiWidth()) tooltipX = mouseX - tw - 8;
        if (tooltipY < 0) tooltipY = mouseY + 12;
        if (tooltipY + th > graphics.guiHeight()) tooltipY = graphics.guiHeight() - th - 4;

        int borderColor = PerkNodeWidget.tierColor(perk.tier);
        graphics.fill(tooltipX, tooltipY, tooltipX + tw, tooltipY + th, 0xCC222222);
        graphics.fill(tooltipX, tooltipY, tooltipX + tw, tooltipY + 1, borderColor);
        graphics.fill(tooltipX, tooltipY, tooltipX + 1, tooltipY + th, borderColor);
        graphics.fill(tooltipX + tw - 1, tooltipY, tooltipX + tw, tooltipY + th, borderColor);
        graphics.fill(tooltipX, tooltipY + th - 1, tooltipX + tw, tooltipY + th, borderColor);

        int ly = tooltipY + 4;
        for (Component line : tooltip) {
            graphics.drawString(font, line, tooltipX + 5, ly, 0xFFFFFFFF);
            ly += 10;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (mouseX < x || mouseX >= x + panelWidth || mouseY < y || mouseY >= y + panelHeight) return false;

        for (Slot slot : slots) {
            int sx = slot.rx;
            int sy = slot.ry + scrollOffset;
            if (mouseX >= sx && mouseX < sx + NODE_W && mouseY >= sy && mouseY < sy + NODE_H) {
                Perk perk = slot.perk;
                if (perk == null) return false;
                selectedPerk = perk;
                boolean unlocked = ClientPerkCache.isUnlocked(perk);
                if (unlocked) {
                    PerkFeedbackToast.show(PerkNodeWidget.ClickResult.ALREADY_UNLOCKED);
                    return true;
                }
                var player = Minecraft.getInstance().player;
                int statLevel = player != null
                        ? RaceEffectApplier.getEffectiveLevel(player, perk.stat.index)
                        : ClientStatCache.getLevel(perk.stat.index);
                int points = ClientPerkCache.getPointsForStat(perk.stat.index);
                PerkNodePresentation presentation = resolvePresentation(perk, statLevel, points, player);
                if (statLevel < perk.tier.requiredStatLevel) {
                    PerkFeedbackToast.show(PerkNodeWidget.ClickResult.LEVEL_TOO_LOW);
                    return true;
                }
                if (points < perk.tier.cost) {
                    PerkFeedbackToast.show(PerkNodeWidget.ClickResult.NOT_ENOUGH_POINTS);
                    return true;
                }
                if (presentation.state() == PerkNodeVisualState.LOCKED_PREREQ || presentation.state() == PerkNodeVisualState.LOCKED_MIXED || presentation.state() == PerkNodeVisualState.LOCKED_STAT) {
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                            new tong.statmod.network.UnlockPerkPayload(perk.id));
                    return true;
                }
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                        new tong.statmod.network.UnlockPerkPayload(perk.id));
                PerkFeedbackToast.show(PerkNodeWidget.ClickResult.UNLOCK_SENT);
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (mouseX < x || mouseX >= x + panelWidth || mouseY < y || mouseY >= y + panelHeight) return false;
        scrollOffset += (int) scrollY * 12;
        int maxScroll = Math.max(0, totalContentH - panelHeight + 20);
        scrollOffset = Math.max(-maxScroll, Math.min(0, scrollOffset));
        return true;
    }

    private static int familyColor(StatFamily family) {
        return switch (family) {
            case FRONTLINE_PHYSICAL_COMBAT -> 0xCC553322;
            case RANGED_HUNT_CONTROL -> 0xCC224455;
            case MAGICAL_CORE -> 0xCC332255;
            case ELEMENTAL_SPECIALIZATION -> 0xCC225533;
            case MENTAL_PRESSURE_RESILIENCE -> 0xCC442244;
            case CRAFTING_SUPPORT -> 0xCC445522;
        };
    }

    private static PerkNodePresentation resolvePresentation(Perk perk, int statLevel, int points, net.minecraft.world.entity.player.Player player) {
        int synergyLevel = perk.synergyStat != null
                ? (player != null
                ? RaceEffectApplier.getEffectiveLevel(player, perk.synergyStat.index)
                : ClientStatCache.getLevel(perk.synergyStat.index))
                : 0;
        List<String> externalRequirements = externalRequirements(perk);
        boolean meetsExternalRequirements = externalRequirements.isEmpty()
                || (player != null && SkillPerkGate.canUnlock(player, perk));
        return PerkNodePresentation.resolve(
                perk,
                ClientPerkCache.isUnlocked(perk),
                statLevel,
                points,
                synergyLevel,
                meetsExternalRequirements,
                externalRequirements);
    }

    private static List<String> externalRequirements(Perk perk) {
        List<String> requirements = new ArrayList<>();
        String race = SkillPerkGate.requiredRace(perk.id);
        if (race != null) {
            requirements.add(humanizeRequirement(race) + " race");
        }
        for (String skill : SkillPerkGate.requiredSkills(perk.id)) {
            requirements.add(humanizeRequirement(skill));
        }
        return requirements;
    }

    private static String humanizeRequirement(String raw) {
        String path = raw;
        int separator = path.indexOf(':');
        if (separator >= 0 && separator + 1 < path.length()) {
            path = path.substring(separator + 1);
        }

        String[] words = path.split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.toString();
    }
}
