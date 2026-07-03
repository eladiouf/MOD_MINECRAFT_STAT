package tong.statmod.integration.puffish;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PuffishScreenCenteringTest {

    /**
     * L'offset est un int (contrainte de {@code applyChangesWithLimits}) : quand le centre du
     * tree tombe sur une demi-fraction, le centrage pixel-exact est mathématiquement impossible.
     * L'invariant atteignable — et suffisant visuellement — est un écart ≤ 0.5 px en float.
     */
    private static final double HALF_PIXEL = 0.5 + 1e-6;

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

        double renderedCenterX = 1280 / 2.0 + offset.x() + (bounds.minX + bounds.maxX) / 2.0;
        double renderedCenterY = 720 / 2.0 + offset.y() + (bounds.minY + bounds.maxY) / 2.0;
        double contentCenterX = (17 + (1280 - 17)) / 2.0;
        double contentCenterY = (54 + (720 - 17)) / 2.0;

        assertTrue(Math.abs(renderedCenterX - contentCenterX) <= HALF_PIXEL,
                "écart X " + (renderedCenterX - contentCenterX));
        assertTrue(Math.abs(renderedCenterY - contentCenterY) <= HALF_PIXEL,
                "écart Y " + (renderedCenterY - contentCenterY));
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

        double renderedCenterX = 420 / 2.0 + offset.x() + ((bounds.minX + bounds.maxX) / 2.0) * 0.75;
        double renderedCenterY = 320 / 2.0 + offset.y() + ((bounds.minY + bounds.maxY) / 2.0) * 0.75;
        double contentCenterX = (17 + (420 - 17)) / 2.0;
        double contentCenterY = (62 + (320 - 17)) / 2.0;

        assertTrue(Math.abs(renderedCenterX - contentCenterX) <= HALF_PIXEL,
                "écart X " + (renderedCenterX - contentCenterX));
        assertTrue(Math.abs(renderedCenterY - contentCenterY) <= HALF_PIXEL,
                "écart Y " + (renderedCenterY - contentCenterY));
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
