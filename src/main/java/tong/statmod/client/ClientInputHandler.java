package tong.statmod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.STATMod;
import tong.statmod.integration.puffish.PuffishSkillsCompat;
import tong.statmod.network.OpenVirtualInscriptionPayload;

@EventBusSubscriber(modid = STATMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class ClientInputHandler {
    private ClientInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (ClientSetup.TOGGLE_STATS.consumeClick()) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new StatTabScreen());
        }
        while (ClientSetup.OPEN_PERKS.consumeClick()) {
            PerkUiRouter.openFromClient(PuffishSkillsCompat.isLoaded());
        }
        while (ClientSetup.OPEN_SPELL_CODEX.consumeClick()) {
            PacketDistributor.sendToServer(new OpenVirtualInscriptionPayload());
        }
    }
}
