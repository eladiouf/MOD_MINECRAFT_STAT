package tong.statmod.integration.epicfight;

import net.minecraft.client.Camera;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import tong.statmod.STATMod;
import tong.statmod.integration.PlayerDataBridge;
import tong.statmod.integration.tensura.RacePhysicalEffects;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.camera.BuildCameraTransform;
import yesman.epicfight.api.event.subscription.DefaultEventSubscription;

import java.lang.reflect.Method;

@OnlyIn(Dist.CLIENT)
public final class EpicFightClientCompat {
    private static boolean initialized = false;
    private static Method cameraSetPosition;
    private static boolean reflectionFailed = false;

    private EpicFightClientCompat() {}

    public static void init() {
        if (initialized || !ModList.get().isLoaded("epicfight")) {
            return;
        }
        initialized = true;
        EpicFightClientEventHooks.Camera.BUILD_TRANSFORM_POST.registerEvent(
                new DefaultEventSubscription<BuildCameraTransform.Post>() {
                    @Override
                    public void fire(BuildCameraTransform.Post event) {
                        onBuildTransformPost(event);
                    }
                }
        );
    }

    private static void onBuildTransformPost(BuildCameraTransform.Post event) {
        if (!event.getCameraApi().isTPSMode()) {
            return;
        }

        Camera camera = event.getCamera();
        if (camera == null || !camera.isDetached()) {
            return;
        }

        if (!(camera.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }

        String raceId;
        try {
            raceId = PlayerDataBridge.getRaceId(player);
        } catch (Throwable ignored) {
            return;
        }

        float yOffset = EpicFightRaceCameraHelper.verticalOffset(
                raceId,
                RacePhysicalEffects.getScaleFactor(player)
        );
        if (yOffset <= 0.0f) {
            return;
        }

        offsetCamera(camera, yOffset);
    }

    private static void offsetCamera(Camera camera, float yOffset) {
        if (reflectionFailed) {
            return;
        }

        try {
            if (cameraSetPosition == null) {
                cameraSetPosition = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
                cameraSetPosition.setAccessible(true);
            }

            Vec3 position = camera.getPosition();
            cameraSetPosition.invoke(camera, position.add(0.0D, yOffset, 0.0D));
        } catch (ReflectiveOperationException e) {
            reflectionFailed = true;
            STATMod.LOGGER.warn("Failed to adjust Epic Fight combat camera height for scaled races", e);
        }
    }
}
