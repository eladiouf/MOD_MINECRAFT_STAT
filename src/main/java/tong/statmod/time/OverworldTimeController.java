package tong.statmod.time;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OverworldTimeController {
    public static final double DAY_SECONDS = 32.0 * 60.0;
    public static final double NIGHT_SECONDS = 16.0 * 60.0;
    private static final double DAY_TICKS = 12000.0;
    private static final double NIGHT_TICKS = 12000.0;
    private static final Map<String, Double> EXACT_TIME = new ConcurrentHashMap<>();

    private OverworldTimeController() {}

    public static double dayTicksPerSecond() {
        return DAY_TICKS / DAY_SECONDS;
    }

    public static double nightTicksPerSecond() {
        return NIGHT_TICKS / NIGHT_SECONDS;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }

        String key = level.dimension().location().toString();
        double exact = EXACT_TIME.computeIfAbsent(key, ignored -> (double) level.getDayTime());
        long actual = level.getDayTime();

        // Respect external time changes (sleep, /time set, commands)
        if (Math.abs((long) exact - actual) > 50L) {
            exact = actual;
        }

        boolean day = Math.floorMod((long) exact, 24000L) < 12000L;
        exact += (day ? dayTicksPerSecond() : nightTicksPerSecond()) / 20.0d;
        EXACT_TIME.put(key, exact);
        level.setDayTime((long) Math.floor(exact));
    }
}
