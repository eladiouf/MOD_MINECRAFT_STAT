package tong.statmod.integration.puffish;

public final class PuffishScreenCentering {
    private PuffishScreenCentering() {}

    public static ScreenOffset centerOnContent(int screenWidth,
                                               int screenHeight,
                                               int paddingLeft,
                                               int paddingTop,
                                               int paddingRight,
                                               int paddingBottom,
                                               float scale,
                                               int minX,
                                               int maxX,
                                               int minY,
                                               int maxY) {
        float treeCenterX = (minX + maxX) / 2.0f;
        float treeCenterY = (minY + maxY) / 2.0f;
        float contentCenterX = (paddingLeft + (screenWidth - paddingRight)) / 2.0f;
        float contentCenterY = (paddingTop + (screenHeight - paddingBottom)) / 2.0f;

        int offsetX = Math.round(contentCenterX - screenWidth / 2.0f - treeCenterX * scale);
        int offsetY = Math.round(contentCenterY - screenHeight / 2.0f - treeCenterY * scale);
        return new ScreenOffset(offsetX, offsetY);
    }

    public record ScreenOffset(int x, int y) {}
}
