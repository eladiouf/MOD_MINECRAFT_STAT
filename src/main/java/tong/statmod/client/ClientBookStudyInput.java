package tong.statmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import tong.statmod.network.BookStudyInputMessage.Action;
import tong.statmod.network.StatNetwork;

public final class ClientBookStudyInput {
    static final int HEARTBEAT_INTERVAL = 5;

    private static InteractionHand activeHand;
    private static int heartbeatTicks;

    private ClientBookStudyInput() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null
                || minecraft.getConnection() == null) {
            resetWithoutPacket();
            return;
        }
        InteractionHand desiredHand = desiredHand(minecraft);
        if (desiredHand != activeHand) {
            if (activeHand != null) {
                StatNetwork.sendBookStudyInput(Action.RELEASE, activeHand);
            }
            activeHand = desiredHand;
            heartbeatTicks = 0;
            if (activeHand != null) {
                StatNetwork.sendBookStudyInput(Action.BEGIN, activeHand);
            }
            return;
        }
        if (activeHand != null && ++heartbeatTicks >= HEARTBEAT_INTERVAL) {
            heartbeatTicks = 0;
            StatNetwork.sendBookStudyInput(Action.HEARTBEAT, activeHand);
        }
    }

    private static InteractionHand desiredHand(Minecraft minecraft) {
        if (minecraft.screen != null || !minecraft.options.keyUse.isDown()) {
            return null;
        }
        if (minecraft.player.getMainHandItem().is(Items.ENCHANTED_BOOK)) {
            return InteractionHand.MAIN_HAND;
        }
        if (minecraft.player.getOffhandItem().is(Items.ENCHANTED_BOOK)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static void resetWithoutPacket() {
        activeHand = null;
        heartbeatTicks = 0;
    }
}
