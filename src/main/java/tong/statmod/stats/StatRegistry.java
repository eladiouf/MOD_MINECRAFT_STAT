package tong.statmod.stats;

import tong.statmod.Config;
import tong.statmod.STATMod;

public class StatRegistry {
    public static void init() {
        STATMod.LOGGER.info("Registered {} stats across {} categories",
            StatType.values().length, StatCategory.values().length);
        for (StatCategory cat : StatCategory.values()) {
            int count = 0;
            for (StatType s : StatType.values()) {
                if (s.category == cat) count++;
            }
            STATMod.LOGGER.debug("  {}: {} stats", cat.name(), count);
        }
        STATMod.LOGGER.info("Stat scaling formulas initialized");
        STATMod.LOGGER.info("  XP curve: (level+1)*{} — Level 100 requires {} XP",
            Config.xpPerLevelMultiplier, (100 + 1) * Config.xpPerLevelMultiplier);
        STATMod.LOGGER.info("  Damage bonus range: {}% - {}%",
            Math.round(StatCalculator.getDamageBonus(1) * 100),
            Math.round(StatCalculator.getDamageBonus(100) * 100));
    }
}
