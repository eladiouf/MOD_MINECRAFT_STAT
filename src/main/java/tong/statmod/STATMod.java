package tong.statmod;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraft.server.level.ServerPlayer;
import tong.statmod.advancement.StatAdvancementTrigger;
import tong.statmod.anticheat.ServerValidator;
import tong.statmod.world.effect.AdrenalineBrewingRecipe;
import tong.statmod.world.effect.ModPotions;
import net.minecraft.SharedConstants;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.versions.forge.ForgeVersion;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tong.statmod.capability.PlayerStats;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.command.StatsCommands;
import tong.statmod.integration.EpicFightCompat;
import tong.statmod.integration.EpicParcoolCompat;
import tong.statmod.sound.ModSounds;
import tong.statmod.item.CreativeTab;
import tong.statmod.item.ModItems;
import tong.statmod.loot.StatLootModifier;
import tong.statmod.network.NetworkHandler;
import tong.statmod.skills.SkillRegistry;
import tong.statmod.skills.SkillRequirementRegistry;
import tong.statmod.skills.SkillUnlockRegistry;
import tong.statmod.skills.StatModSkillCategories;
import tong.statmod.skills.StatModSkillSlots;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.skill.SkillCategory;
import tong.statmod.api.PluginManager;
import tong.statmod.compat.CompatibilityChecker;
import tong.statmod.perks.Perk;
import tong.statmod.stats.StatType;
import tong.statmod.stats.StatRegistry;
import tong.statmod.world.effect.ModEffects;
import tong.statmod.network.BatchSyncPacket;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.config.ModConfig;

@Mod(STATMod.MODID)
public class STATMod
{
    public static final String MODID = "statmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(STATMod.MODID);

    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, STATMod.MODID);
    private static final RegistryObject<Codec<StatLootModifier>> STAT_LOOT =
        LOOT_MODIFIERS.register("stat_loot", () -> StatLootModifier.CODEC);

    public STATMod(FMLJavaModLoadingContext context)
    {
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        var bus = context.getModEventBus();

        // Register custom skill categories and slots with Epic Fight (must be early)
        SkillCategory.ENUM_MANAGER.registerEnumCls(STATMod.MODID, StatModSkillCategories.class);
        SkillSlot.ENUM_MANAGER.registerEnumCls(STATMod.MODID, StatModSkillSlots.class);

        bus.addListener(this::commonSetup);
        ModEffects.register(bus);
        ModPotions.register(bus);
        ModSounds.register(bus);
        ModItems.register(bus);
        CreativeTab.register(bus);
        LOOT_MODIFIERS.register(bus);
        NetworkHandler.register();
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        CriteriaTriggers.register(StatAdvancementTrigger.INSTANCE);
        StatRegistry.init();
        EpicFightCompat.init();
        EpicParcoolCompat.init();
        SkillUnlockRegistry.init();
        SkillRequirementRegistry.init();
        PluginManager.loadPlugins();
        CompatibilityChecker.check();
        event.enqueueWork(() -> {
            BrewingRecipeRegistry.addRecipe(new AdrenalineBrewingRecipe(Potions.AWKWARD, Items.SUGAR, ModPotions.ADRENALINE.get()));
            BrewingRecipeRegistry.addRecipe(new AdrenalineBrewingRecipe(ModPotions.ADRENALINE.get(), Items.REDSTONE, ModPotions.LONG_ADRENALINE.get()));
            BrewingRecipeRegistry.addRecipe(new AdrenalineBrewingRecipe(ModPotions.ADRENALINE.get(), Items.GLOWSTONE_DUST, ModPotions.STRONG_ADRENALINE.get()));
        });
        LOGGER.info("=== STAT Mod Diagnostic ===");
        LOGGER.info("  Minecraft: {}", SharedConstants.getCurrentVersion().getName());
        LOGGER.info("  Forge: {}", ForgeVersion.getVersion());
        LOGGER.info("  Stats: {} registered", StatType.values().length);
        LOGGER.info("  Skills: 60 registered via SkillBuildEvent");
        LOGGER.info("  Perks: {} defined", Perk.values().length);
        LOGGER.info("  Commands: /statmod [list|get|set|xp|reset|backup|restore|preset|profile|benchmark|top|respec|party]");
        LOGGER.info("  Config: {} values configurable", 30);
        LOGGER.info("  Network: 8 packet types registered");
        LOGGER.info("  Plugin API: {} plugins loaded", PluginManager.getPlugins().size());
        LOGGER.info("==============================");
        LOGGER.info("STAT Mod loaded!");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("STAT Mod ready on server");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
        StatsCommands.register(event.getDispatcher());
    }

    @Mod.EventBusSubscriber(modid = STATMod.MODID)
    public static class LoginHandler {
        @SubscribeEvent
        public static void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                ServerValidator.validateAllStats(serverPlayer);
                int[] levels = new int[PlayerStats.STAT_COUNT];
                int[] xp = new int[PlayerStats.STAT_COUNT];
                CapabilityHelper.withStats(serverPlayer, stats -> {
                    for (int i = 0; i < PlayerStats.STAT_COUNT; i++) {
                        levels[i] = stats.getLevel(i);
                        xp[i] = stats.getXp(i);
                    }
                });
                float[] fatigue = {0};
                int[] maxFatigue = {500};
                CapabilityHelper.withFatigue(serverPlayer, f -> {
                    fatigue[0] = f.getFatigue();
                    maxFatigue[0] = f.getMaxFatigue();
                });
                float[] thirst = {100};
                CapabilityHelper.withThirst(serverPlayer, t -> thirst[0] = t.getThirst());
                float[] mana = {0};
                CapabilityHelper.withStats(serverPlayer, s -> mana[0] = s.getMana());
                int[][] perkIds = {new int[0]};
                int[] perkPoints = {0};
                CapabilityHelper.withPerks(serverPlayer, perks -> {
                    perkIds[0] = perks.getUnlockedPerks().stream().mapToInt(i -> i).toArray();
                    perkPoints[0] = perks.getAvailablePoints();
                });
                NetworkHandler.sendToPlayer(new BatchSyncPacket(levels, xp,
                    fatigue[0], maxFatigue[0], thirst[0], mana[0], perkIds[0], perkPoints[0]), serverPlayer);
            }
        }
    }
}
