package tong.statmod.client;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Affichage de mana STAT MOD, indépendant du HUD d'Iron's Spellbooks.
 *
 * <p>Nécessaire car Tensura remplace le HUD vanilla ({@code vanillaHud = false}) et la
 * barre de mana native d'Iron's (ancrée sur des éléments vanilla) devient invisible dans
 * cette stack. Cet overlay lit directement {@link ClientMagicData} — alimenté en continu
 * par la sync forcée serveur (voir {@code IronSpellAttributeBridge}) — et ne dépend
 * d'aucune couche vanilla : barre + pourcentage en haut à gauche, valeurs précises en bas
 * à droite au-dessus de la hotbar.
 *
 * <p>Classe chargée uniquement si Iron's Spellbooks est présent (garde dans
 * {@code ClientSetup}) — elle référence des classes d'Iron's.
 */
@OnlyIn(Dist.CLIENT)
public final class ManaHudOverlay {

    private static final int BAR_W = 110;
    private static final int BAR_H = 8;
    private static final int BAR_X = 4;
    private static final int BAR_Y = 4;

    private ManaHudOverlay() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH,
                ResourceLocation.fromNamespaceAndPath("statmod", "mana_bar_top_left"),
                ManaHudOverlay::renderTopLeft);

        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath("statmod", "mana_numbers_bottom_right"),
                ManaHudOverlay::renderBottomRight);
    }

    private static void renderTopLeft(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float mana = ClientMagicData.getPlayerMana();
        float maxMana = (float) mc.player.getAttributeValue(AttributeRegistry.MAX_MANA);
        if (maxMana <= 0) return;
        Font font = mc.font;

        int pct = (int) Math.round(mana / maxMana * 100);

        graphics.fill(BAR_X, BAR_Y, BAR_X + BAR_W, BAR_Y + BAR_H, 0xCC1A1A2E);

        if (pct > 0) {
            int fillW = Math.max(1, (BAR_W - 2) * Math.min(pct, 100) / 100);
            int color = pct > 66 ? 0xFF4080FF : pct > 33 ? 0xFFFFA040 : 0xFFFF4040;
            graphics.fill(BAR_X + 1, BAR_Y + 1, BAR_X + 1 + fillW, BAR_Y + BAR_H - 1, color);
        }

        String pctText = pct + "%";
        graphics.drawString(font, pctText,
                BAR_X + BAR_W / 2 - font.width(pctText) / 2,
                BAR_Y + 1, 0xFFFFFFFF, true);
    }

    private static void renderBottomRight(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float mana = ClientMagicData.getPlayerMana();
        float maxMana = (float) mc.player.getAttributeValue(AttributeRegistry.MAX_MANA);
        if (maxMana <= 0) return;
        Font font = mc.font;
        String text = Math.round(mana) + " / " + Math.round(maxMana);
        int x = graphics.guiWidth() - font.width(text) - 8;
        int y = graphics.guiHeight() - 42;
        graphics.fill(x - 3, y - 1, x + font.width(text) + 3, y + font.lineHeight + 1, 0x80000000);
        graphics.drawString(font, text, x, y, 0xFF55FFFF, false);
    }
}
