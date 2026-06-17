package tong.statmod.integration.epicfight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EpicFightLegacyCameraCompatTest {
    @Test
    void exposesLegacyBuildCameraTransformPostAlias() throws ClassNotFoundException {
        Class<?> alias = Class.forName("yesman.epicfight.api.client.event.types.BuildCameraTransform$Post");
        assertTrue(yesman.epicfight.api.client.event.types.camera.BuildCameraTransform.class.isAssignableFrom(alias));
    }
}
