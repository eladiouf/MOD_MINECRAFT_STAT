package tong.statmod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(StatMod.MOD_ID)
public final class StatMod {
    public static final String MOD_ID = "statmod";
    public static final String MOD_NAME = "STAT Mod";

    private static final Logger LOGGER = LogUtils.getLogger();

    public StatMod() {
        LOGGER.info("Initializing {} for Forge 1.20.1", MOD_NAME);
    }
}
