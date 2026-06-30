package tong.statmod.integration.epicfight;

import tong.statmod.client.cosmetic.RaceCosmeticProfile;

public final class EpicFightRaceCameraHelper {
    private static final float DWARF_CAMERA_HEIGHT = 1.6f;

    private EpicFightRaceCameraHelper() {}

    public static float verticalOffset(String raceId, float scaleFactor) {
        if (RaceCosmeticProfile.resolve(raceId) != RaceCosmeticProfile.DWARF) {
            return 0.0f;
        }
        if (scaleFactor >= 1.0f) {
            return 0.0f;
        }
        return (1.0f - scaleFactor) * DWARF_CAMERA_HEIGHT;
    }
}
