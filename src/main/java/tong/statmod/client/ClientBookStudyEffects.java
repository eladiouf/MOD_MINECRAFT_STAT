package tong.statmod.client;

import net.minecraft.client.Minecraft;
import tong.statmod.network.BookStudyCompletionMessage;

public final class ClientBookStudyEffects {
    private ClientBookStudyEffects() {
    }

    public static void show(BookStudyCompletionMessage message) {
        if (message != null && message.valid()) {
            Minecraft.getInstance().gameRenderer.displayItemActivation(message.book());
        }
    }
}
