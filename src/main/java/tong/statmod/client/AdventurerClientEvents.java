package tong.statmod.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.entity.AdventurerEntities;

/** Enregistre le renderer de l'aventurier (côté client uniquement). */
@Mod.EventBusSubscriber(modid = StatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AdventurerClientEvents {

    private AdventurerClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AdventurerEntities.ADVENTURER.get(), AdventurerRenderer::new);
    }
}
