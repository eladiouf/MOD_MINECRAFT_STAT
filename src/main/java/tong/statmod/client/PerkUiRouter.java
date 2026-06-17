package tong.statmod.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import tong.statmod.client.gui.PerkScreen;
import tong.statmod.network.OpenPerkTreePayload;

public final class PerkUiRouter {
    private PerkUiRouter() {}

    public static boolean shouldUsePuffish(boolean puffishLoaded) {
        return puffishLoaded;
    }

    public static void openFromClient(boolean puffishLoaded) {
        if (shouldUsePuffish(puffishLoaded)) {
            PacketDistributor.sendToServer(new OpenPerkTreePayload());
            return;
        }
        Minecraft.getInstance().setScreen(new PerkScreen());
    }
}
