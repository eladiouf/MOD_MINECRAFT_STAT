package yesman.epicfight.api.client.event.types;

import net.minecraft.client.Camera;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.event.CancelableEvent;

/**
 * Compatibility alias for mods compiled against the older Epic Fight camera event package.
 */
public abstract class BuildCameraTransform extends yesman.epicfight.api.client.event.types.camera.BuildCameraTransform {
    protected BuildCameraTransform(EpicFightCameraAPI cameraAPI, Camera camera, float partialTick) {
        super(cameraAPI, camera, partialTick);
    }

    public static final class Pre extends BuildCameraTransform implements CancelableEvent {
        private boolean vanillaCameraSetupCanceled;

        public Pre(EpicFightCameraAPI cameraAPI, Camera camera, float partialTick) {
            super(cameraAPI, camera, partialTick);
        }

        public void setVanillaCameraSetupCanceled(boolean canceled) {
            this.vanillaCameraSetupCanceled = canceled;
        }

        public boolean isVanillaCameraSetupCanceled() {
            return this.vanillaCameraSetupCanceled;
        }
    }

    public static final class Post extends BuildCameraTransform {
        public Post(EpicFightCameraAPI cameraAPI, Camera camera, float partialTick) {
            super(cameraAPI, camera, partialTick);
        }
    }
}
