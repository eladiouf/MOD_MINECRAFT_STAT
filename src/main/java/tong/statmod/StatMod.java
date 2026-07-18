package tong.statmod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import tong.statmod.config.StatModServerConfig;
import tong.statmod.dungeon.DungeonBlocks;
import tong.statmod.entity.AdventurerEntities;
import tong.statmod.network.StatNetwork;
import tong.statmod.sound.ModSounds;

@Mod(StatMod.MOD_ID)
public final class StatMod {
    public static final String MOD_ID = "statmod";
    public static final String MOD_NAME = "STAT Mod";

    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public StatMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, StatModServerConfig.SPEC);
        StatNetwork.register();
        ModSounds.register(modEventBus);
        DungeonBlocks.register(modEventBus);
        AdventurerEntities.register(modEventBus);
        LOGGER.info("Initializing {} for Forge 1.20.1", MOD_NAME);
    }
}
