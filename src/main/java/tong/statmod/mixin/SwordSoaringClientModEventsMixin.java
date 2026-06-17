package tong.statmod.mixin;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.p1nero.ss.client.SwordSoaringCameraManager;
import net.p1nero.ss.entity.SwordSoaringEntities;
import net.p1nero.ss.entity.sword.gate_of_babylon.client.BabylonRenderer;
import net.p1nero.ss.entity.sword.screen_sword.client.ScreenSwordRenderer;
import net.p1nero.ss.entity.sword.wan.client.WanRenderer;
import net.p1nero.ss.entity.sword.fly_sword.client.FlySwordRenderer;
import net.p1nero.ss.entity.vatansever.client.VatanseverRenderer;
import net.p1nero.ss.entity.vatansever_storm.client.VatanseverStormRenderer;
import net.p1nero.ss.events.ClientModEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;

@Mixin(value = ClientModEvents.class, remap = false)
public abstract class SwordSoaringClientModEventsMixin {
    @Inject(method = "onClientSetup", at = @At("HEAD"), cancellable = true)
    private static void statmod$bridgeModernEpicFightClientHooks(FMLClientSetupEvent event, CallbackInfo ci) {
        EntityRenderers.register(SwordSoaringEntities.WAN_ENTITY.get(), WanRenderer::new);
        EntityRenderers.register(SwordSoaringEntities.BABYLON.get(), BabylonRenderer::new);
        EntityRenderers.register(SwordSoaringEntities.FLY_SWORD.get(), FlySwordRenderer::new);
        EntityRenderers.register(SwordSoaringEntities.SCREEN_SWORD.get(), ScreenSwordRenderer::new);
        EntityRenderers.register(SwordSoaringEntities.VATANSEVER.get(), VatanseverRenderer::new);
        EntityRenderers.register(SwordSoaringEntities.VATANSEVER_STORM.get(), VatanseverStormRenderer::new);
        EpicFightClientEventHooks.Camera.BUILD_TRANSFORM_POST.registerEvent(cameraEvent ->
                SwordSoaringCameraManager.onEpicFightCameraSetupEnd(
                        new yesman.epicfight.api.client.event.types.BuildCameraTransform.Post(
                                cameraEvent.getCameraApi(),
                                cameraEvent.getCamera(),
                                cameraEvent.getPartialTick()
                        )
                ));
        ci.cancel();
    }
}
