package tong.statmod.client.hud;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.client.hud.overlays.GlobalLevelOverlay;
import tong.statmod.client.hud.overlays.SurvivalOverlay;

@Mod.EventBusSubscriber(modid = STATMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class HUDManager {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        // Register new custom overlays
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "statmod_survival", SurvivalOverlay.INSTANCE);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "statmod_global_level", GlobalLevelOverlay.INSTANCE);
    }

    // Hide vanilla health and food bars
    @Mod.EventBusSubscriber(modid = STATMod.MODID, value = Dist.CLIENT)
    public static class VanillaBarHider {
        @SubscribeEvent
        public static void hideVanillaBars(RenderGuiOverlayEvent.Pre event) {
            if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id()) ||
                event.getOverlay().id().equals(VanillaGuiOverlay.FOOD_LEVEL.id())) {
                event.setCanceled(true);
            }
        }
    }
}
