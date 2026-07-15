package tong.statmod.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.client.notice.ClientProgressNotices;
import tong.statmod.client.stats.StatsOverviewScreen;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID, value = Dist.CLIENT)
public final class ClientInputEvents {
    private ClientInputEvents() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ClientProgressNotices.tick();
        Minecraft minecraft = Minecraft.getInstance();
        while (ClientKeyMappings.OPEN_STATS.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new StatsOverviewScreen());
            }
        }
    }
}
