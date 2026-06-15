package tong.statmod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tong.statmod.config.Config;
import tong.statmod.integration.SoulLevelSyncHandler;
import tong.statmod.integration.TensuraEventSubscriber;
import tong.statmod.item.ModItems;
import tong.statmod.loot.ModLootModifiers;
import tong.statmod.progression.CombatXPHandler;
import tong.statmod.progression.NonCombatXPHandler;
import tong.statmod.sound.ModSounds;
import tong.statmod.stats.StatAttributeHandler;
import tong.statmod.stats.StatCommands;
import tong.statmod.storage.ModAttachments;

@Mod(STATMod.MODID)
public class STATMod {
    public static final String MODID = "statmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(STATMod.class);

    public STATMod(IEventBus modBus) {
        ModContainer container = ModLoadingContext.get().getActiveContainer();
        container.registerConfig(ModConfig.Type.COMMON, Config.getSpec());
        ModAttachments.register(modBus);
        ModItems.register(modBus);
        ModSounds.register(modBus);
        ModLootModifiers.register(modBus);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event ->
                StatCommands.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.register(CombatXPHandler.class);
        NeoForge.EVENT_BUS.register(NonCombatXPHandler.class);
        NeoForge.EVENT_BUS.register(StatAttributeHandler.class);
        NeoForge.EVENT_BUS.register(SoulLevelSyncHandler.class);
        TensuraEventSubscriber.register();
        LOGGER.info("STAT Mod initialized on NeoForge 1.21.1");
    }
}
