package tong.statmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.client.ClientStatsCache;
import tong.statmod.client.texture.StatIconRenderer;
import tong.statmod.client.texture.TextureCache;
import tong.statmod.stats.StatCalculator;
import tong.statmod.stats.StatType;

import static tong.statmod.client.texture.TextureCache.drawInkText;

public class StatWidget extends AbstractWidget {
    private static final int WIDGET_HEIGHT = 28;
    private static final int CARD_COLOR = 0xFFD4C494;
    private static final int BORDER_COLOR = 0xFFA0724A;
    private static final int TEXT_COLOR = 0xFF3A1A00;
    private static final int LEVEL_COLOR = 0xFF8B4513;
    private static final int XP_BG_COLOR = 0xFFB8965A;
    private static final int XP_TEXT_COLOR = 0xFF6B4C1E;

    private final StatType stat;

    public StatWidget(StatType stat, int x, int y) {
        super(x, y, 220, WIDGET_HEIGHT, Component.literal(stat.displayName));
        this.stat = stat;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int level = ClientStatsCache.getLevel(stat);
        int xp = ClientStatsCache.getXp(stat);
        int needed = tong.statmod.stats.StatCalculator.getXpForNextLevel(level);

        int right = getX() + 220;
        int bottom = getY() + WIDGET_HEIGHT;

        // Card background
        graphics.fill(getX(), getY(), right, bottom, CARD_COLOR);
        // Card border
        graphics.fill(getX(), getY(), right, getY() + 1, BORDER_COLOR);
        graphics.fill(getX(), bottom - 1, right, bottom, BORDER_COLOR);
        graphics.fill(getX(), getY(), getX() + 1, bottom, BORDER_COLOR);
        graphics.fill(right - 1, getY(), right, bottom, BORDER_COLOR);

        var font = Minecraft.getInstance().font;

        // Icon (sprite atlas)
        StatIconRenderer.renderIcon(graphics, stat.name(), getX() + 4, getY() + 5);

        // Name + Level
        int levelColor;
        if (level >= 100) levelColor = 0xFF55FF55;
        else if (level >= 50) levelColor = 0xFFFFAA00;
        else levelColor = TEXT_COLOR;
        drawInkText(graphics, font, stat.displayName, getX() + 24, getY() + 4, TEXT_COLOR);
        drawInkText(graphics, font, "Niv. " + level, getX() + 160, getY() + 4, levelColor);

        // XP bar
        int barX = getX() + 24;
        int barY = getY() + 18;
        int barWidth = 140;
        int barHeight = 4;

        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, XP_BG_COLOR);

        ResourceLocation xpFillTex = TextureCache.get("xp_bar_fill.png");
        if (level >= 100) {
            graphics.blit(xpFillTex, barX, barY, barWidth, barHeight, 0, 0, 64, 17, 64, 17);
            String maxText = "MAX";
            drawInkText(graphics, font, maxText, barX + barWidth - font.width(maxText), barY - 1, XP_TEXT_COLOR);
        } else {
            int filled = (int) ((float) xp / needed * barWidth);
            if (filled > 0) {
                graphics.blit(xpFillTex, barX, barY, filled, barHeight, 0, 0, 64, 17, 64, 17);
            }
            // XP percentage next to bar
            String pctText = (int)(((float) xp / needed) * 100) + "%";
            drawInkText(graphics, font, pctText, barX + barWidth + 2, barY - 2, XP_TEXT_COLOR);
        }

        // XP text right-aligned below bar
        String xpText = level < 100 ? xp + " / " + needed + " XP" : "";
        if (!xpText.isEmpty()) {
            drawInkText(graphics, font, xpText, barX + barWidth - font.width(xpText), barY + 5, XP_TEXT_COLOR);
        }

        // Tooltip on hover
        if (mouseX >= getX() && mouseX <= getX() + 220 && mouseY >= getY() && mouseY <= getY() + WIDGET_HEIGHT) {
            String effect = getStatEffectDescription(stat);
            if (level >= 100) {
                effect += " \u00a7a(MAXED)";
            }
            graphics.renderTooltip(font, Component.literal(effect), mouseX, mouseY);
        }
    }

    @SuppressWarnings("SameReturnValue")
    private static String getStatEffectDescription(StatType stat) {
        int lvl = ClientStatsCache.getLevel(stat);
        return switch (stat) {
            case BRUTE_FORCE -> {
                float bf = StatCalculator.getDamageBonus(lvl) * 100;
                float bfMax = StatCalculator.getDamageBonus(100) * 100;
                yield "§6Dégâts mains: §e+" + String.format("%.1f", bf) + "%§7 (max +" + String.format("%.0f", bfMax) + "%)" +
                    "\n§7Gagnez de l'XP en tuant des monstres à l'épée/hache";
            }
            case BLADE_TECHNIQUE -> {
                float bt = StatCalculator.getBladeDamageBonus(lvl) * 100;
                float btMax = StatCalculator.getBladeDamageBonus(100) * 100;
                yield "§6Dégâts armes légères: §e+" + String.format("%.1f", bt) + "%§7 (max +" + String.format("%.0f", btMax) + "%)" +
                    "\n§7Gagnez de l'XP avec des combos et touches nettes";
            }
            case RAPIDITE -> {
                float rs = StatCalculator.getAttackSpeedBonus(lvl) * 100;
                float rsMax = StatCalculator.getAttackSpeedBonus(100) * 100;
                yield "§6Vitesse d'attaque: §e+" + String.format("%.1f", rs) + "%§7 (max +" + String.format("%.0f", rsMax) + "%)" +
                    "\n§7Gagnez de l'XP en enchaînant les coups rapides";
            }
            case AGILITY -> {
                float ag = StatCalculator.getMoveSpeedBonus(lvl) * 100;
                float agMax = StatCalculator.getMoveSpeedBonus(100) * 100;
                yield "§6Vitesse de déplacement: §e+" + String.format("%.1f", ag) + "%§7 (max +" + String.format("%.0f", agMax) + "%)" +
                    "\n§7Gagnez de l'XP en combattant sans bouger (agilité d'esquive)";
            }
            case PHYSICAL_RESISTANCE -> {
                float pr = StatCalculator.getDamageReduction(lvl) * 100;
                float prMax = StatCalculator.getDamageReduction(100) * 100;
                yield "§6Résistance physique: §e-" + String.format("%.1f", pr) + "%§7 reçus (max -" + String.format("%.0f", prMax) + "%)" +
                    "\n§7Gagnez de l'XP en prenant des dégâts";
            }
            case PHYSICAL_ENDURANCE -> {
                float pe = StatCalculator.getEnduranceHearts(lvl);
                float peMax = StatCalculator.getEnduranceHearts(100);
                yield "§6Cœurs bonus: §e+" + String.format("%.1f", pe) + "❤§7 (max +" + String.format("%.0f", peMax) + "❤)" +
                    "\n§7Régénération naturelle améliorée§7" +
                    "\n§7Gagnez de l'XP en bloquant les attaques";
            }
            case PRECISION -> {
                float pc = StatCalculator.getCritChance(lvl) * 100;
                float pcMax = StatCalculator.getCritChance(100) * 100;
                yield "§6Chance de crit: §e+" + String.format("%.1f", pc) + "%§7 (max +" + String.format("%.0f", pcMax) + "%)" +
                    "\n§7Dégâts supplémentaires à l'arc" +
                    "\n§7Gagnez de l'XP avec des tirs à distance";
            }
            case ARCANE_POWER -> {
                float ap = StatCalculator.getMagicDamageBonus(lvl) * 100;
                float apMax = StatCalculator.getMagicDamageBonus(100) * 100;
                yield "§6Dégâts magiques: §e+" + String.format("%.1f", ap) + "%§7 (max +" + String.format("%.0f", apMax) + "%)" +
                    "\n§7Gagnez de l'XP en lançant des sorts";
            }
            case WATER_AFFINITY -> {
                float ws = StatCalculator.getSwimSpeedBonus(lvl) * 100;
                float wsMax = StatCalculator.getSwimSpeedBonus(100) * 100;
                float wd = StatCalculator.getUnderwaterDamageBonus(lvl) * 100;
                float wdMax = StatCalculator.getUnderwaterDamageBonus(100) * 100;
                yield "§6Nage: §e+" + String.format("%.1f", ws) + "%§7 (max +" + String.format("%.0f", wsMax) + "%)" +
                    "\n§6Dégâts sous l'eau: §e+" + String.format("%.1f", wd) + "%§7 (max +" + String.format("%.0f", wdMax) + "%)" +
                    "\n§6Oxygène: §e+15s§7 sous l'eau" +
                    "\n§7Gagnez de l'XP en nageant";
            }
            case EARTH_AFFINITY -> {
                float em = StatCalculator.getMiningSpeedBonus(lvl) * 100;
                float emMax = StatCalculator.getMiningSpeedBonus(100) * 100;
                yield "§6Vitesse de minage: §e+" + String.format("%.1f", em) + "%§7 (max +" + String.format("%.0f", emMax) + "%)" +
                    "\n§7Gagnez de l'XP en minant";
            }
            case FIRE_AFFINITY -> {
                float fd = StatCalculator.getFireDamageBonus(lvl) * 100;
                float fdMax = StatCalculator.getFireDamageBonus(100) * 100;
                yield "§6Dégâts de feu: §e+" + String.format("%.1f", fd) + "%§7 (max +" + String.format("%.0f", fdMax) + "%)" +
                    "\n§6Résistance au feu: §e!" + String.format("%.1f", fd * 0.5) + "%§7 dégâts reçus en moins" +
                    "\n§7Gagnez de l'XP en étant en feu / au Nether";
            }
            case AIR_AFFINITY -> {
                float aj = StatCalculator.getJumpBonus(lvl) * 100;
                float ajMax = StatCalculator.getJumpBonus(100) * 100;
                yield "§6Saut: §e+" + String.format("%.1f", aj) + "%§7 (max +" + String.format("%.0f", ajMax) + "%)" +
                    "\n§6Chute: §e-" + String.format("%.1f", aj) + "%§7 dégâts de chute (max -" + String.format("%.0f", ajMax) + "%)" +
                    "\n§7Gagnez de l'XP en sautant";
            }
            case MAGIC_RESISTANCE -> {
                float mr = StatCalculator.getMagicReduction(lvl) * 100;
                float mrMax = StatCalculator.getMagicReduction(100) * 100;
                yield "§6Résistance magique: §e-" + String.format("%.1f", mr) + "%§7 (max -" + String.format("%.0f", mrMax) + "%)" +
                    "\n§7Gagnez de l'XP en subissant des dégâts magiques";
            }
            case CASTING_SPEED -> {
                float cs = StatCalculator.getItemUseSpeed(lvl) * 100;
                float csMax = StatCalculator.getItemUseSpeed(100) * 100;
                yield "§6Vitesse d'incantation: §e+" + String.format("%.1f", cs) + "%§7 (max +" + String.format("%.0f", csMax) + "%)" +
                    "\n§7Gagnez de l'XP en utilisant des objets/armes";
            }
            case MANA_POOL -> {
                int mb = StatCalculator.getManaBonus(lvl);
                int mbMax = StatCalculator.getManaBonus(100);
                yield "§6Mana max: §e+" + mb + "§7 (max +" + mbMax + ")" +
                    "\n§6Régénération de mana bonus" +
                    "\n§6Bouclier anti-magie: §e" + String.format("%.0f", Math.min(lvl * 2.0, 200)) + "§7 dégâts magiques absorbés" +
                    "\n§6Sprint boost: §e+10%§7 vitesse quand mana > 50%" +
                    "\n§7Gagnez de l'XP en utilisant de la magie";
            }
            case ERUDITION -> {
                float xp = StatCalculator.getXpBonus(lvl) * 100;
                float xpMax = StatCalculator.getXpBonus(100) * 100;
                yield "§6Bonus XP: §e+" + String.format("%.1f", xp) + "%§7 orbes (max +" + String.format("%.0f", xpMax) + "%)" +
                    "\n§7Gagnez de l'XP en craftant des objets enchantés";
            }
            case TRACKING -> {
                float tk = StatCalculator.getLuckBonus(lvl) * 100;
                float tkMax = StatCalculator.getLuckBonus(100) * 100;
                yield "§6Chance: §e+" + String.format("%.1f", tk) + "%§7 (max +" + String.format("%.0f", tkMax) + "%)" +
                    "\n§7Gagnez de l'XP en tuant des monstres/boss";
            }
            case KEEN_SENSES -> {
                float dr = StatCalculator.getDetectionRadius(lvl);
                float drMax = StatCalculator.getDetectionRadius(100);
                float dc = StatCalculator.getDodgeChance(lvl) * 100;
                float dcMax = StatCalculator.getDodgeChance(100) * 100;
                float cx = StatCalculator.getCombatXpRegenBoost(lvl);
                float cxMax = StatCalculator.getCombatXpRegenBoost(100);
                yield "§6Détection: §e" + String.format("%.1f", dr) + "§7 blocks (max " + String.format("%.0f", drMax) + ")" +
                    "\n§6Esquive: §e+" + String.format("%.1f", dc) + "%§7 (max +" + String.format("%.0f", dcMax) + "%)" +
                    "\n§6XP combat: §e×" + String.format("%.2f", cx) + "§7 (max ×" + String.format("%.2f", cxMax) + ")" +
                    "\n§7Gagnez de l'XP en explorant de nouvelles dimensions";
            }
            case FORGING -> {
                float td = StatCalculator.getToolDamageBonus(lvl) * 100;
                float tdMax = StatCalculator.getToolDamageBonus(100) * 100;
                float dd = StatCalculator.getDoubleDropChance(lvl) * 100;
                float ddMax = StatCalculator.getDoubleDropChance(100) * 100;
                yield "§6Dégâts outils: §e+" + String.format("%.1f", td) + "%§7 (max +" + String.format("%.0f", tdMax) + "%)" +
                    "\n§6Double drop: §e+" + String.format("%.1f", dd) + "%§7 (max +" + String.format("%.0f", ddMax) + "%)" +
                    "\n§6Vitesse minage: bonus" +
                    "\n§7Gagnez de l'XP en minant en profondeur et en craftant";
            }
            case COOKING -> {
                float fb = (StatCalculator.getFoodBuffDuration(lvl) - 1.0f) * 100;
                float fbMax = (StatCalculator.getFoodBuffDuration(100) - 1.0f) * 100;
                yield "§6Durée buffs nourriture: §e+" + String.format("%.1f", fb) + "%§7 (max +" + String.format("%.0f", fbMax) + "%)" +
                    "\n§6Nutrition extra: §e+" + Math.max(1, (int) (Math.sqrt(lvl) * 0.6)) + "§7" +
                    "\n§7Gagnez de l'XP en faisant cuire des aliments";
            }
            case ALCHEMY -> {
                float pd = StatCalculator.getPotionDurationBonus(lvl) * 100;
                float pdMax = StatCalculator.getPotionDurationBonus(100) * 100;
                yield "§6Durée potions: §e+" + String.format("%.1f", pd) + "%§7 (max +" + String.format("%.0f", pdMax) + "%)" +
                    "\n§6Régénération bonus en buvant des potions" +
                    "\n§7Gagnez de l'XP en brassant et buvant des potions";
            }
            case INTIMIDATION -> {
                float fr = StatCalculator.getFearRange(lvl);
                float frMax = StatCalculator.getFearRange(100);
                yield "§6Portée de peur: §e" + String.format("%.1f", fr) + "§7 blocks (max " + String.format("%.0f", frMax) + ")" +
                    "\n§6Affaiblit les monstres alentour en touchant une cible" +
                    "\n§7Gagnez de l'XP en tuant des boss";
            }
            case WILLPOWER -> {
                float sd = StatCalculator.getStatusDurationReduction(lvl) * 100;
                float sdMax = StatCalculator.getStatusDurationReduction(100) * 100;
                yield "§6Réduction d'effets négatifs: §e-" + String.format("%.1f", sd) + "%§7 (max -" + String.format("%.0f", sdMax) + "%)" +
                    "\n§6Récupération fatigue bonus" +
                    "\n§7Gagnez de l'XP en survivant à des situations critiques";
            }
        };
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
