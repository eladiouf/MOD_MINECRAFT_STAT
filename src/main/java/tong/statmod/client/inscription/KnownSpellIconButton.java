package tong.statmod.client.inscription;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Icon-only square button for a known spell. Renders the spell icon (16x16),
 * highlights on hover/selection, and exposes a multi-line tooltip with the
 * spell's display name, school, rarity, mana cost and cooldown.
 */
public final class KnownSpellIconButton extends AbstractButton {
    public static final int SIZE = 20;
    private static final int ICON_SIZE = 16;

    private final Runnable onPress;
    private AbstractSpell spell;
    private int spellLevel = 1;
    private boolean selected;
    private boolean bound;

    public KnownSpellIconButton(int x, int y, Runnable onPress) {
        super(x, y, SIZE, SIZE, Component.empty());
        this.onPress = onPress;
    }

    public void setSpell(AbstractSpell spell, int level, Player player) {
        this.spell = spell;
        this.spellLevel = Math.max(1, level);
        if (spell == null) {
            this.setTooltip(null);
            this.visible = false;
            return;
        }
        this.visible = true;
        this.setTooltip(Tooltip.create(buildTooltip(spell, this.spellLevel, player, this.bound)));
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /** Marque le sort comme déjà inscrit dans le grimoire actif. */
    public void setBound(boolean bound) {
        this.bound = bound;
    }

    @Override
    public void onPress() {
        if (this.onPress != null) this.onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Order of priority: selected (Volt) > bound (green) > hovered > active > inactive.
        int frameColor = !this.active ? 0xFF555555
                : this.selected ? 0xFFFFD24A
                : this.bound ? 0xFF55E060
                : this.isHovered() ? 0xFFAAAAAA
                : 0xFF333333;
        int bgColor = this.bound ? 0xFF0E2410 : 0xFF101010;
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + SIZE, this.getY() + SIZE, bgColor);
        guiGraphics.renderOutline(this.getX(), this.getY(), SIZE, SIZE, frameColor);
        if (this.spell != null) {
            int iconX = this.getX() + (SIZE - ICON_SIZE) / 2;
            int iconY = this.getY() + (SIZE - ICON_SIZE) / 2;
            guiGraphics.blit(this.spell.getSpellIconResource(), iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            if (this.bound) {
                // Small filled dot in the top-right corner marks the "déjà inscrit" state.
                int dotX = this.getX() + SIZE - 4;
                int dotY = this.getY() + 1;
                guiGraphics.fill(dotX, dotY, dotX + 3, dotY + 3, 0xFF55E060);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    private static Component buildTooltip(AbstractSpell spell, int level, Player player, boolean bound) {
        MutableComponent root = (player != null ? spell.getDisplayName(player) : Component.translatable(spell.getSpellName())).copy();
        if (bound) {
            root.append(Component.literal("\n"));
            root.append(Component.translatable("statmod.spell.tooltip.bound")
                    .withStyle(style -> style.withColor(0xFF55E060).withItalic(true)));
        }
        String school = String.valueOf(spell.getSchoolType().getId().getPath());
        root.append(Component.literal("\n"));
        root.append(Component.translatable("statmod.spell.tooltip.school", capitalize(school)));
        root.append(Component.literal("\n"));
        root.append(Component.translatable("statmod.spell.tooltip.rarity", spell.getRarity(level).getDisplayName().getString()));
        root.append(Component.literal("\n"));
        root.append(Component.translatable("statmod.spell.tooltip.mana", spell.getManaCost(level)));
        root.append(Component.literal("\n"));
        int cooldownTicks = spell.getSpellCooldown();
        root.append(Component.translatable("statmod.spell.tooltip.cooldown", String.format("%.1fs", cooldownTicks / 20.0f)));
        if (player != null) {
            List<MutableComponent> uniqueInfo = spell.getUniqueInfo(level, player);
            if (uniqueInfo != null) {
                for (MutableComponent line : uniqueInfo) {
                    root.append(Component.literal("\n"));
                    root.append(line);
                }
            }
        }
        return root;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
