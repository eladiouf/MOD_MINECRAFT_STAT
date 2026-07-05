package tong.statmod.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.STATMod;
import tong.statmod.integration.puffish.PuffishSkillsCompat;
import tong.statmod.network.OpenVirtualInscriptionPayload;

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
