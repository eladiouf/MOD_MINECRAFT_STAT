package tong.statmod.world;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class RandomEvents {
    private static final Random RANDOM = new Random();
    private static boolean bonusXpActive = false;
    private static long bonusXpEndTime = 0;
    private static long lastCheckTime = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;

        long now = System.currentTimeMillis();

        if (now - lastCheckTime > 3600000) {
            lastCheckTime = now;
            if (RANDOM.nextFloat() < 0.10f && !bonusXpActive) {
                startBonusEvent(server);
            }
        }

        if (bonusXpActive && now > bonusXpEndTime) {
            endBonusEvent(server);
        }
    }

    private static void startBonusEvent(MinecraftServer server) {
        bonusXpActive = true;
        bonusXpEndTime = System.currentTimeMillis() + 300000;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(
                "§6🌟 Bonus XP Event! §eAll XP doubled for 5 minutes!"));
        }
        STATMod.LOGGER.info("Random bonus XP event started");
    }

    private static void endBonusEvent(MinecraftServer server) {
        bonusXpActive = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(
                "§c🌟 Bonus XP event ended."));
        }
    }

    public static boolean isBonusXpActive() { return bonusXpActive; }
}
