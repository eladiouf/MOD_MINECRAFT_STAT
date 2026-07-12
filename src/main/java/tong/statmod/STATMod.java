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
import tong.statmod.integration.parcool.ParcoolCompat;
import tong.statmod.integration.ironspells.IronSpellsCompat;
import tong.statmod.integration.ironspells.bridge.TensuraSpellLevelModifier;
import tong.statmod.integration.ironspells.bridge.TensuraSpellWrapperRegistry;
import tong.statmod.integration.puffish.PuffishSkillsCompat;
import tong.statmod.integration.tensura.MagiculeScalingHandler;
import tong.statmod.integration.tensura.RacePhysicalEffects;
import tong.statmod.integration.tensura.SummonScalingHandler;
import tong.statmod.integration.tensura.TempBuffLifecycleHandler;
import tong.statmod.integration.tensura.TensuraEpHandler;
import tong.statmod.integration.tensura.TensuraRaceHandler;
import tong.statmod.integration.overgeared.OvergearedCompat;
import tong.statmod.block.ForgingBlocks;
import tong.statmod.block.entity.ModBlockEntities;
import tong.statmod.dungeon.DungeonBlocks;
import tong.statmod.dungeon.DungeonBossHandler;
import tong.statmod.dungeon.DungeonMobSpawner;
import tong.statmod.dungeon.DungeonProtectionHandler;
import tong.statmod.dungeon.DungeonSpawnGuard;
import tong.statmod.item.ForgingBlueprints;
import tong.statmod.item.ForgingFormComponents;
import tong.statmod.item.ForgingGrips;
import tong.statmod.item.ForgingIntermediates;
import tong.statmod.item.ForgingMaterials;
import tong.statmod.item.ForgingTools;
import tong.statmod.item.ModItems;
import tong.statmod.item.RuneEssence;
import tong.statmod.item.RuneShards;
import tong.statmod.menu.ModMenuTypes;
import tong.statmod.loot.ModLootModifiers;
import tong.statmod.network.SyncLifecycleHandler;
import tong.statmod.progression.CombatXPHandler;
import tong.statmod.progression.NonCombatXPHandler;
import tong.statmod.sound.ModSounds;
import tong.statmod.stamina.StaminaEvents;
import tong.statmod.stats.ModAttributes;
import tong.statmod.stats.CraftingSupportEffectHandler;
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
        ModAttributes.register(modBus);
        ModItems.register(modBus);
        ForgingMaterials.register(modBus);
        ForgingIntermediates.register(modBus);
        ForgingGrips.register(modBus);
        ForgingFormComponents.register(modBus);
        ForgingTools.register(modBus);
        ForgingBlueprints.register(modBus);
        ForgingBlocks.register(modBus);
        DungeonBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenuTypes.register(modBus);
        RuneEssence.register(modBus);
        RuneShards.register(modBus);
        ModSounds.register(modBus);
        ModLootModifiers.register(modBus);
        modBus.register(tong.statmod.network.NetworkHandler.class);
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            modBus.register(tong.statmod.client.ClientSetup.class);
            modBus.register(tong.statmod.client.cosmetic.RaceCosmeticEvents.class);
            NeoForge.EVENT_BUS.register(tong.statmod.client.ClientInputHandler.class);
        }
        boolean tensuraLoaded = ModList.get().isLoaded("tensura");
        boolean epicFightLoaded = ModList.get().isLoaded("epicfight");
        boolean parcoolLoaded = ModList.get().isLoaded("parcool");
        boolean overgearedLoaded = ModList.get().isLoaded("overgeared");
        boolean puffishLoaded = ModList.get().isLoaded("puffish_skills");
        boolean ironSpellsLoaded = ModList.get().isLoaded("irons_spellbooks");
        if (ironSpellsLoaded && tensuraLoaded) {
            TensuraSpellWrapperRegistry.register(modBus);
        }
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event ->
                StatCommands.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.register(CombatXPHandler.class);
        NeoForge.EVENT_BUS.register(NonCombatXPHandler.class);
        NeoForge.EVENT_BUS.register(StatAttributeHandler.class);
        NeoForge.EVENT_BUS.register(CraftingSupportEffectHandler.class);
        NeoForge.EVENT_BUS.register(StaminaEvents.class);
        NeoForge.EVENT_BUS.register(SyncLifecycleHandler.class);
        NeoForge.EVENT_BUS.register(SoulLevelSyncHandler.class);
        NeoForge.EVENT_BUS.register(OverworldTimeController.class);
        NeoForge.EVENT_BUS.register(SleepRecoveryHandler.class);
        NeoForge.EVENT_BUS.register(DungeonBossHandler.class);
        NeoForge.EVENT_BUS.register(DungeonMobSpawner.class);
        NeoForge.EVENT_BUS.register(DungeonSpawnGuard.class);
        NeoForge.EVENT_BUS.register(DungeonProtectionHandler.class);
        NeoForge.EVENT_BUS.register(tong.statmod.economy.MagicBanker.class);
        NeoForge.EVENT_BUS.register(tong.statmod.economy.VillageBankerSpawner.class);
        if (tensuraLoaded) {
            TensuraEventSubscriber.register();
            NeoForge.EVENT_BUS.register(MagiculeScalingHandler.class);
            NeoForge.EVENT_BUS.register(SummonScalingHandler.class);
            NeoForge.EVENT_BUS.register(RacePhysicalEffects.class);
            NeoForge.EVENT_BUS.register(TempBuffLifecycleHandler.class);
            TensuraEpHandler.init();
            TensuraRaceHandler.init();
        }
        if (ironSpellsLoaded && tensuraLoaded) {
            NeoForge.EVENT_BUS.register(TensuraSpellLevelModifier.class);
        }
        if (epicFightLoaded) {
            EpicFightCompat.init();
        }
        if (parcoolLoaded) {
            ParcoolCompat.init();
        }
        if (overgearedLoaded) {
            OvergearedCompat.init();
        }
        if (puffishLoaded) {
            PuffishSkillsCompat.init();
        }
        if (ironSpellsLoaded) {
            IronSpellsCompat.init();
        }
        if (ModList.get().isLoaded("playerrevive")) {
            tong.statmod.integration.playerrevive.PlayerReviveIntegration.init();
        }
        if (ModList.get().isLoaded("sdmshop")) {
            NeoForge.EVENT_BUS.register(tong.statmod.integration.sdm.SDMShopNPCBridge.class);
            NeoForge.EVENT_BUS.register(tong.statmod.integration.sdm.SDMShopDatabaseInitializer.class);
        }
        LOGGER.info("STAT Mod initialized on NeoForge 1.21.1");
    }
}
