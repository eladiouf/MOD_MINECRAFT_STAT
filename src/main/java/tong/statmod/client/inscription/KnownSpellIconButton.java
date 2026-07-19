package tong.statmod.client.inscription;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public final class KnownSpellIconButton extends AbstractButton {
    public static final int SIZE = 20;
    private static final int ICON_SIZE = 16;

    private final Runnable action;
    private AbstractSpell spell;
    private int learnedLevel = 1;
    private boolean selected;
    private boolean bound;

    public KnownSpellIconButton(int x, int y, Runnable action) {
        super(x, y, SIZE, SIZE, Component.empty());
        this.action = action;
    }

    public void setSpell(AbstractSpell spell, int learnedLevel, Player player) {
        this.spell = spell;
        this.learnedLevel = Math.max(1, learnedLevel);
        visible = spell != null;
        setTooltip(spell == null ? null : Tooltip.create(buildTooltip(spell, player)));
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setBound(boolean bound) {
        this.bound = bound;
    }

    @Override
    public void onPress() {
        if (action != null) {
            action.run();
        }
    }

    @Override
    protected void renderWidget(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int frame = !active ? 0xFF555555
                : selected ? 0xFFFFD24A
                : bound ? 0xFF55E060
                : isHovered() ? 0xFFAAAAAA
                : 0xFF333333;
        graphics.fill(getX(), getY(), getX() + SIZE, getY() + SIZE,
                bound ? 0xFF0E2410 : 0xFF101010);
        graphics.renderOutline(getX(), getY(), SIZE, SIZE, frame);
        if (spell != null) {
            graphics.blit(spell.getSpellIconResource(),
                    getX() + 2, getY() + 2, 0, 0,
                    ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            if (bound) {
                graphics.fill(getX() + SIZE - 4, getY() + 1,
                        getX() + SIZE - 1, getY() + 4, 0xFF55E060);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    private Component buildTooltip(AbstractSpell spell, Player player) {
        MutableComponent text = (player == null
                ? Component.translatable(spell.getSpellName())
                : spell.getDisplayName(player)).copy();
        text.append("\n").append(Component.translatable(
                "statmod.spell.tooltip.learned_level", learnedLevel));
        if (spell.getSchoolType() != null && spell.getSchoolType().getId() != null) {
            text.append("\n").append(Component.translatable(
                    "statmod.spell.tooltip.school",
                    spell.getSchoolType().getId().getPath()));
        }
        text.append("\n").append(Component.translatable(
                "statmod.spell.tooltip.rarity",
                spell.getRarity(learnedLevel).getDisplayName().getString()));
        text.append("\n").append(Component.translatable(
                "statmod.spell.tooltip.mana", spell.getManaCost(learnedLevel)));
        text.append("\n").append(Component.translatable(
                "statmod.spell.tooltip.cooldown",
                String.format(java.util.Locale.ROOT, "%.1fs", spell.getSpellCooldown() / 20.0F)));
        if (bound) {
            text.append("\n").append(Component.translatable("statmod.spell.tooltip.bound"));
        }
        if (player != null) {
            List<MutableComponent> unique = spell.getUniqueInfo(learnedLevel, player);
            if (unique != null) {
                unique.forEach(line -> text.append("\n").append(line));
            }
        }
        return text;
    }
}
