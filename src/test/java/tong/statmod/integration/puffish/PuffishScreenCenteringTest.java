package tong.statmod.integration.puffish;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PuffishScreenCenteringTest {
    @Test
    void centersUnifiedPerkTreeInsideNormalContentViewport() {
        Bounds bounds = parseBounds(PuffishFamilyTreeBuilder.unifiedCategoryFiles().skillsJson());
        PuffishScreenCentering.ScreenOffset offset = PuffishScreenCentering.centerOnContent(
                1280, 720,
                17, 54, 17, 17,
                1.0f,
                bounds.minX, bounds.maxX,
                bounds.minY, bounds.maxY
        );

        int renderedCenterX = Math.round(1280 / 2.0f + offset.x() + (bounds.minX + bounds.maxX) / 2.0f);
        int renderedCenterY = Math.round(720 / 2.0f + offset.y() + (bounds.minY + bounds.maxY) / 2.0f);

        assertEquals(Math.round((17 + (1280 - 17)) / 2.0f), renderedCenterX);
        assertEquals(Math.round((54 + (720 - 17)) / 2.0f), renderedCenterY);
    }

    @Test
    void centersUnifiedPerkTreeInsideSmallViewportVariant() {
        Bounds bounds = parseBounds(PuffishFamilyTreeBuilder.unifiedCategoryFiles().skillsJson());
        PuffishScreenCentering.ScreenOffset offset = PuffishScreenCentering.centerOnContent(
                420, 320,
                17, 62, 17, 17,
                0.75f,
                bounds.minX, bounds.maxX,
                bounds.minY, bounds.maxY
        );

        int renderedCenterX = Math.round(420 / 2.0f + offset.x() + ((bounds.minX + bounds.maxX) / 2.0f) * 0.75f);
        int renderedCenterY = Math.round(320 / 2.0f + offset.y() + ((bounds.minY + bounds.maxY) / 2.0f) * 0.75f);

        assertEquals(Math.round((17 + (420 - 17)) / 2.0f), renderedCenterX);
        assertEquals(Math.round((62 + (320 - 17)) / 2.0f), renderedCenterY);
    }

    private static Bounds parseBounds(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (var entry : root.entrySet()) {
            JsonObject node = entry.getValue().getAsJsonObject();
            int x = node.get("x").getAsInt();
            int y = node.get("y").getAsInt();
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        return new Bounds(minX, maxX, minY, maxY);
    }

    private record Bounds(int minX, int maxX, int minY, int maxY) {}
}
