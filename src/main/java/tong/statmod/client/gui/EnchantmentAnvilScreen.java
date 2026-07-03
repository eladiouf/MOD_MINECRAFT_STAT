package tong.statmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import tong.statmod.STATMod;
import tong.statmod.forge.EnchantmentAnvilRecipeCatalog;
import tong.statmod.menu.EnchantmentAnvilMenu;

import java.util.List;

@OnlyIn(Dist.CLIENT)
/**
 * Layout matches {@link tong.statmod.menu.EnchantmentAnvilMenu}:
 * base `17`, catalyst `44`, catalyst `71`, support `98`, output `134`, all on y `38`.
 */
public class EnchantmentAnvilScreen extends AbstractContainerScreen<EnchantmentAnvilMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "textures/gui/enchantment_anvil.png");

    private static final int VOLT = 0xFFFFAA00;
    private static final int VERDANT = 0xFF7AE582;
    private static final int EMBER = 0xFFFFC857;
    private static final int ASH = 0xFFB4B4B4;
    private static final int GRAY_500 = 0xFF808080;
    private static final int SUPPORT_SLOT_X = 98;
    private static final int SUPPORT_SLOT_Y = 38;
    private static final int SLOT_SIZE = 16;

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
        ForgeStationScreenDecor.renderSlotFrame(graphics, leftPos, topPos, 17, 38, ForgeStationScreenDecor.SlotPalette.SILVER);
        ForgeStationScreenDecor.renderSlotFrame(graphics, leftPos, topPos, 44, 38, ForgeStationScreenDecor.SlotPalette.SILVER);
        ForgeStationScreenDecor.renderSlotFrame(graphics, leftPos, topPos, 71, 38, ForgeStationScreenDecor.SlotPalette.SILVER);
        ForgeStationScreenDecor.renderSlotFrame(graphics, leftPos, topPos, 98, 38, ForgeStationScreenDecor.SlotPalette.BRONZE);
        ForgeStationScreenDecor.renderSlotFrame(graphics, leftPos, topPos, 134, 38, ForgeStationScreenDecor.SlotPalette.GOLD);
        ForgeStationScreenDecor.renderPlayerInventorySlots(graphics, leftPos, topPos);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, VOLT, false);
        Component feedback = feedbackMessage();
        if (feedback != null) {
            graphics.drawString(font, feedback, 8, 18, feedbackColor(), false);
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GRAY_500, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderExpectedSupportGhost(graphics);
        renderSupportTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private Component feedbackMessage() {
        if (menu.hasCraftableResult()) {
            return Component.translatable("statmod.enchantment_anvil.feedback.ready");
        }

        EnchantmentAnvilRecipeCatalog.InputFeedback feedback = menu.getInputFeedback();
        ItemStack expectedSupport = menu.getExpectedSupportStack();
        return switch (feedback.state()) {
            case MISSING_SUPPORT -> Component.translatable(
                    "statmod.enchantment_anvil.feedback.need_support",
                    expectedSupport.getHoverName());
            case WRONG_SUPPORT -> Component.translatable(
                    "statmod.enchantment_anvil.feedback.wrong_support",
                    expectedSupport.getHoverName());
            case INVALID_RECIPE -> Component.translatable("statmod.enchantment_anvil.feedback.invalid_recipe");
            default -> null;
        };
    }

    private int feedbackColor() {
        if (menu.hasCraftableResult()) {
            return VERDANT;
        }
        return switch (menu.getInputFeedback().state()) {
            case MISSING_SUPPORT, WRONG_SUPPORT -> EMBER;
            case INVALID_RECIPE -> ASH;
            default -> GRAY_500;
        };
    }

    private void renderExpectedSupportGhost(GuiGraphics graphics) {
        if (!shouldRenderExpectedSupportGhost()) {
            return;
        }

        ItemStack expectedSupport = menu.getExpectedSupportStack();
        if (expectedSupport.isEmpty()) {
            return;
        }

        graphics.renderFakeItem(expectedSupport, leftPos + SUPPORT_SLOT_X, topPos + SUPPORT_SLOT_Y);
    }

    private boolean shouldRenderExpectedSupportGhost() {
        return switch (menu.getInputFeedback().state()) {
            case MISSING_SUPPORT, WRONG_SUPPORT -> true;
            default -> false;
        };
    }

    private void renderSupportTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isHovering(SUPPORT_SLOT_X, SUPPORT_SLOT_Y, SLOT_SIZE, SLOT_SIZE, mouseX, mouseY)) {
            return;
        }

        EnchantmentAnvilRecipeCatalog.InputFeedback feedback = menu.getInputFeedback();
        ItemStack expectedSupport = menu.getExpectedSupportStack();
        if (expectedSupport.isEmpty()) {
            return;
        }

        List<Component> tooltip = switch (feedback.state()) {
            case MISSING_SUPPORT -> List.of(
                    Component.translatable("statmod.enchantment_anvil.tooltip.support_slot"),
                    Component.translatable("statmod.enchantment_anvil.feedback.need_support", expectedSupport.getHoverName()));
            case WRONG_SUPPORT -> List.of(
                    Component.translatable("statmod.enchantment_anvil.tooltip.support_slot"),
                    Component.translatable("statmod.enchantment_anvil.feedback.wrong_support", expectedSupport.getHoverName()));
            default -> List.of();
        };

        if (!tooltip.isEmpty()) {
            graphics.renderTooltip(font, tooltip, ItemStack.EMPTY.getTooltipImage(), mouseX, mouseY);
        }
    }
}
