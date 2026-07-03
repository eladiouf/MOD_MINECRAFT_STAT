package tong.statmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;

final class ForgeStationScreenDecor {
    private static final int SLOT_PADDING = 1;
    private static final int SLOT_OUTER_SIZE = 18;
    private static final int SLOT_EDGE = 0xFF151515;
    private static final int SLOT_INNER = 0xFF8B8B8B;
    private static final int SLOT_CORE = 0xFF373737;
    private static final int PLAYER_SLOT_START_X = 8;
    private static final int PLAYER_ROWS_START_Y = 84;
    private static final int HOTBAR_START_Y = 142;
    private static final int SLOT_SPACING = 18;

    private ForgeStationScreenDecor() {
    }

    static void renderSlotFrame(GuiGraphics graphics, int leftPos, int topPos, int slotX, int slotY, SlotPalette palette) {
        int x0 = leftPos + slotX - SLOT_PADDING;
        int y0 = topPos + slotY - SLOT_PADDING;
        int x1 = x0 + SLOT_OUTER_SIZE;
        int y1 = y0 + SLOT_OUTER_SIZE;

        graphics.fill(x0, y0, x1, y1, SLOT_EDGE);
        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, SLOT_INNER);
        graphics.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, palette.fill());
        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, palette.borderLight());
        graphics.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 1, palette.borderLight());
        graphics.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, palette.shadow());
        graphics.fill(x1 - 2, y0 + 1, x1 - 1, y1 - 1, palette.shadow());
        graphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, SLOT_CORE);
    }

    static void renderPlayerInventorySlots(GuiGraphics graphics, int leftPos, int topPos) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                renderSlotFrame(graphics, leftPos, topPos,
                        PLAYER_SLOT_START_X + col * SLOT_SPACING,
                        PLAYER_ROWS_START_Y + row * SLOT_SPACING,
                        SlotPalette.SLATE);
            }
        }

        for (int col = 0; col < 9; col++) {
            renderSlotFrame(graphics, leftPos, topPos,
                    PLAYER_SLOT_START_X + col * SLOT_SPACING,
                    HOTBAR_START_Y,
                    SlotPalette.SLATE);
        }
    }

    enum SlotPalette {
        SLATE(0xFFFFFFFF, 0xFF8B8B8B, 0xFF373737, 0xFF000000),
        SILVER(0xFFF0E8FF, 0xFF8B8B8B, 0xFF2F223E, 0xFF120D18),
        BRONZE(0xFFFFE4BC, 0xFF8B8B8B, 0xFF3F2B14, 0xFF1E1409),
        GOLD(0xFFFFFFC8, 0xFF8B8B8B, 0xFF43310D, 0xFF241906);

        private final int borderLight;
        private final int borderDark;
        private final int fill;
        private final int shadow;

        SlotPalette(int borderLight, int borderDark, int fill, int shadow) {
            this.borderLight = borderLight;
            this.borderDark = borderDark;
            this.fill = fill;
            this.shadow = shadow;
        }

        int borderLight() {
            return borderLight;
        }

        int borderDark() {
            return borderDark;
        }

        int fill() {
            return fill;
        }

        int shadow() {
            return shadow;
        }
    }
}
