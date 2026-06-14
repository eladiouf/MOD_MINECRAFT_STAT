package tong.statmod.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import tong.statmod.STATMod;
import tong.statmod.client.gui.PerkScreen;
import tong.statmod.client.gui.StatsOverviewScreen;

@EventBusSubscriber(modid = STATMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ClientInputHandler {
    private ClientInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (ClientSetup.OPEN_PERKS.consumeClick()) {
            PerkScreen.open();
        }
        while (ClientSetup.OPEN_STATS.consumeClick()) {
            StatsOverviewScreen.open();
        }
    }
}
