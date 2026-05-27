package tong.statmod.stats;

import tong.statmod.STATMod;

public class StatRegistry {
    public static void init() {
        STATMod.LOGGER.info("Registered {} stats for STAT Mod", StatType.values().length);
    }
}
