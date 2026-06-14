package tong.statmod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tong.statmod.stats.StatCommands;
import tong.statmod.storage.ModAttachments;

@Mod(STATMod.MODID)
public class STATMod {
    public static final String MODID = "statmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(STATMod.class);

    public STATMod(IEventBus modBus) {
        ModAttachments.register(modBus);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event ->
                StatCommands.register(event.getDispatcher()));
        LOGGER.info("STAT Mod initialized on NeoForge 1.21.1");
    }
}
