package tong.statmod.client.cosmetic;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import tong.statmod.STATMod;

@EventBusSubscriber(modid = STATMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class RaceCosmeticEvents {
    private RaceCosmeticEvents() {}

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RaceCosmeticModels.ELF_EARS,       RaceCosmeticModels::elfEars);
        event.registerLayerDefinition(RaceCosmeticModels.DWARF_BEARD,    RaceCosmeticModels::dwarfBeard);
        event.registerLayerDefinition(RaceCosmeticModels.BEASTFOLK_EARS, RaceCosmeticModels::beastfolkEars);
        event.registerLayerDefinition(RaceCosmeticModels.BEASTFOLK_TAIL, RaceCosmeticModels::beastfolkTail);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skinModel : event.getSkins()) {
            EntityRenderer<?> renderer = event.getSkin(skinModel);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                playerRenderer.addLayer(new RaceCosmeticLayer(playerRenderer, event.getEntityModels()));
            }
        }
    }

}
