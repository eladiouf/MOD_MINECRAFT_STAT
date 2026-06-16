package tong.statmod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tong.statmod.config.Config;
import tong.statmod.integration.SoulLevelSyncHandler;
import tong.statmod.integration.TensuraEventSubscriber;
import tong.statmod.integration.epicfight.EpicFightCompat;
import tong.statmod.integration.mahou.MahouCompat;
import tong.statmod.integration.parcool.ParcoolCompat;
import tong.statmod.integration.tensura.MagiculeScalingHandler;
import tong.statmod.integration.tensura.SummonScalingHandler;
import tong.statmod.integration.tensura.TensuraCraftQualityHandler;
import tong.statmod.integration.tensura.TensuraEpHandler;
import tong.statmod.integration.tensura.TensuraRaceHandler;
import tong.statmod.integration.overgeared.OvergearedCompat;
import tong.statmod.item.ModItems;
import tong.statmod.loot.ModLootModifiers;
import tong.statmod.progression.CombatXPHandler;
import tong.statmod.progression.NonCombatXPHandler;
import tong.statmod.sound.ModSounds;
import tong.statmod.stamina.StaminaEvents;
import tong.statmod.stats.StatAttributeHandler;
import tong.statmod.stats.StatCommands;
import tong.statmod.storage.ModAttachments;
import tong.statmod.time.OverworldTimeController;
import tong.statmod.time.SleepRecoveryHandler;

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
        NeoForge.EVENT_BUS.register(StaminaEvents.class);
        NeoForge.EVENT_BUS.register(SoulLevelSyncHandler.class);
        NeoForge.EVENT_BUS.register(OverworldTimeController.class);
        NeoForge.EVENT_BUS.register(SleepRecoveryHandler.class);
        if (ModList.get().isLoaded("tensura")) {
            TensuraEventSubscriber.register();
            NeoForge.EVENT_BUS.register(MagiculeScalingHandler.class);
            NeoForge.EVENT_BUS.register(TensuraCraftQualityHandler.class);
            NeoForge.EVENT_BUS.register(SummonScalingHandler.class);
        }
        TensuraEpHandler.init();
        TensuraRaceHandler.init();
        EpicFightCompat.init();
        MahouCompat.init();
        ParcoolCompat.init();
        OvergearedCompat.init();
        LOGGER.info("STAT Mod initialized on NeoForge 1.21.1");
    }
}
