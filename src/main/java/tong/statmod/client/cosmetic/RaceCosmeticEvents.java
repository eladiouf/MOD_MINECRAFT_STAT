package tong.statmod.client.cosmetic;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import tong.statmod.STATMod;
import yesman.epicfight.api.client.event.types.registry.RegisterPatchedRenderersEvent;
import yesman.epicfight.client.renderer.LayerRenderer;
import yesman.epicfight.client.renderer.patched.entity.PatchedEntityRenderer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

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

    @SuppressWarnings({"rawtypes", "unchecked"})
    @SubscribeEvent
    public static void onRegisterEpicFightPlayerLayers(RegisterPatchedRenderersEvent.ModifyEntity event) {
        PatchedEntityRenderer renderer = event.get(EntityType.PLAYER);
        if (renderer instanceof LayerRenderer<?, ?, ?> layerRenderer) {
            ((LayerRenderer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>, net.minecraft.client.model.PlayerModel<AbstractClientPlayer>>) layerRenderer)
                    .addCustomLayer(new EpicFightRaceCosmeticLayer());
        }
    }
}
