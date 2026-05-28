package tong.statmod.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class DayLengthHandler {
    public static final float TARGET_DAY_MINUTES = 48.0f;
    private static final float VANILLA_DAY_MINUTES = 20.0f;
    public static final float TIME_MULTIPLIER = VANILLA_DAY_MINUTES / TARGET_DAY_MINUTES;

    private static float fractionalTime = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        if (server == null) return;

        fractionalTime += TIME_MULTIPLIER;
        if (fractionalTime >= 1.0f) {
            fractionalTime -= 1.0f;
            // Keep vanilla's +1 this tick (time advances by 1 game tick)
        } else {
            // Undo vanilla's +1 (no net time advance this tick)
            for (ServerLevel level : server.getAllLevels()) {
                level.setDayTime(level.getDayTime() - 1);
            }
        }
    }
}
