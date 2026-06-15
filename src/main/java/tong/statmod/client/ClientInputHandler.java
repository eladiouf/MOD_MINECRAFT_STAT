package tong.statmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import tong.statmod.STATMod;
import tong.statmod.client.gui.PerkScreen;

@EventBusSubscriber(modid = STATMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ClientInputHandler {
    private ClientInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (ClientSetup.TOGGLE_STATS.consumeClick()) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new StatTabScreen());
        }
        while (ClientSetup.OPEN_PERKS.consumeClick()) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new PerkScreen());
        }
    }
}
