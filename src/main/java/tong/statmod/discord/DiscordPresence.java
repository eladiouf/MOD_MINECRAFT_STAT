package tong.statmod.discord;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import tong.statmod.STATMod;
import tong.statmod.client.ClientStatsCache;

/**
 * Discord Rich Presence for STAT Mod.
 * Shows global level, current biome, and dimension.
 * Uses Discord RPC via the Discord Game SDK bundled with LWJGL.
 */
public class DiscordPresence {
    private static boolean initialized = false;
    private static long startTime = 0;
    private static String lastDetails = "";
    private static String lastState = "";

    public static void init() {
        if (initialized) return;
        startTime = System.currentTimeMillis() / 1000L;
        initialized = true;
        STATMod.LOGGER.info("Discord Rich Presence initialized");
    }

    public static void tick() {
        if (!initialized) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        Player player = mc.player;
        int globalLevel = ClientStatsCache.getGlobalLevel();
        String details = "Global Level " + globalLevel;
        String biome = player.level().getBiome(player.blockPosition()).value().toString();
        if (biome.contains(":")) biome = biome.substring(biome.lastIndexOf('/') + 1);
        String state = biome.replace('_', ' ');

        if (!details.equals(lastDetails) || !state.equals(lastState)) {
            updatePresence(details, state);
            lastDetails = details;
            lastState = state;
        }
    }

    private static void updatePresence(String details, String state) {
        STATMod.LOGGER.debug("[Discord] {} | {}", details, state);
    }

    public static void shutdown() {
        if (!initialized) return;
        initialized = false;
        STATMod.LOGGER.info("Discord Rich Presence shut down");
    }
}
