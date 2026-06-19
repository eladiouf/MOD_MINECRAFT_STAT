package tong.statmod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
final class PerkFeedbackToast {
    private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId();
    private static Consumer<ToastPayload> sinkForTests;

    private PerkFeedbackToast() {}

    static void show(PerkNodeWidget.ClickResult result) {
        ToastPayload payload = payloadFor(result);
        if (payload == null) {
            return;
        }

        if (sinkForTests != null) {
            sinkForTests.accept(payload);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        SystemToast.addOrUpdate(minecraft.getToasts(), TOAST_ID, payload.title(), payload.message());
    }

    static void setSinkForTests(Consumer<ToastPayload> sink) {
        sinkForTests = sink;
    }

    static void resetSinkForTests() {
        sinkForTests = null;
    }

    private static ToastPayload payloadFor(PerkNodeWidget.ClickResult result) {
        return switch (result) {
            case LEVEL_TOO_LOW -> new ToastPayload(Component.literal("Perk unavailable"), Component.literal("Stat level too low"));
            case NOT_ENOUGH_POINTS -> new ToastPayload(Component.literal("Perk unavailable"), Component.literal("Not enough perk points"));
            case ALREADY_UNLOCKED -> new ToastPayload(Component.literal("Perk unavailable"), Component.literal("Perk already unlocked"));
            case PERK_MISSING -> new ToastPayload(Component.literal("Perk unavailable"), Component.literal("Invalid perk"));
            case UNLOCK_SENT -> null;
        };
    }

    record ToastPayload(Component title, Component message) {}
}
