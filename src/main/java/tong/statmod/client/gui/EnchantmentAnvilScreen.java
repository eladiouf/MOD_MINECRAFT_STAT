package tong.statmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import tong.statmod.STATMod;
import tong.statmod.menu.EnchantmentAnvilMenu;

@OnlyIn(Dist.CLIENT)
/**
 * Layout matches {@link tong.statmod.menu.EnchantmentAnvilMenu}:
 * base `17`, catalyst `44`, catalyst `71`, support `98`, output `134`, all on y `38`.
 */
public class EnchantmentAnvilScreen extends AbstractContainerScreen<EnchantmentAnvilMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "textures/gui/enchantment_anvil.png");

    private static final int VOLT = 0xFFFFAA00;
    private static final int GRAY_500 = 0xFF808080;

    private static final int BG_TEX_W = 1291;
    private static final int BG_TEX_H = 1218;

    public EnchantmentAnvilScreen(EnchantmentAnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, imageWidth, imageHeight,
                0, 0, BG_TEX_W, BG_TEX_H, BG_TEX_W, BG_TEX_H);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, VOLT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GRAY_500, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
