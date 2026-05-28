package tong.statmod;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraft.server.level.ServerPlayer;
import tong.statmod.world.effect.AdrenalineBrewingRecipe;
import tong.statmod.world.effect.ModPotions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tong.statmod.capability.PlayerStats;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.command.StatsCommands;
import tong.statmod.integration.EpicFightCompat;
import tong.statmod.integration.EpicParcoolCompat;
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.SyncAllStatsPacket;
import tong.statmod.skills.SkillRegistry;
import tong.statmod.skills.SkillRequirementRegistry;
import tong.statmod.skills.SkillUnlockRegistry;
import tong.statmod.skills.StatModSkillCategories;
import tong.statmod.skills.StatModSkillSlots;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.skill.SkillCategory;
import tong.statmod.stats.StatRegistry;
import tong.statmod.world.effect.ModEffects;
import tong.statmod.fatigue.FatigueProvider;
import tong.statmod.network.FatiguePacket;
import tong.statmod.network.ThirstPacket;
import tong.statmod.network.SyncPerksPacket;
import tong.statmod.perks.PerkProvider;
import tong.statmod.world.thirst.ThirstProvider;
import net.minecraftforge.fml.config.ModConfig;

@Mod(STATMod.MODID)
public class STATMod
{
    public static final String MODID = "statmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(STATMod.MODID);

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
        NetworkHandler.register();
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        StatRegistry.init();
        EpicFightCompat.init();
        EpicParcoolCompat.init();
        SkillUnlockRegistry.init();
        SkillRequirementRegistry.init();
        event.enqueueWork(() -> {
            BrewingRecipeRegistry.addRecipe(new AdrenalineBrewingRecipe(Potions.AWKWARD, Items.SUGAR, ModPotions.ADRENALINE.get()));
            BrewingRecipeRegistry.addRecipe(new AdrenalineBrewingRecipe(ModPotions.ADRENALINE.get(), Items.REDSTONE, ModPotions.LONG_ADRENALINE.get()));
            BrewingRecipeRegistry.addRecipe(new AdrenalineBrewingRecipe(ModPotions.ADRENALINE.get(), Items.GLOWSTONE_DUST, ModPotions.STRONG_ADRENALINE.get()));
        });
        LOGGER.info("STAT Mod chargé !");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("STAT Mod prêt sur le serveur");
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
                serverPlayer.getCapability(PlayerStatsProvider.PLAYER_STATS).ifPresent(stats -> {
                    int[] levels = new int[PlayerStats.STAT_COUNT];
                    int[] xp = new int[PlayerStats.STAT_COUNT];
                    for (int i = 0; i < PlayerStats.STAT_COUNT; i++) {
                        levels[i] = stats.getLevel(i);
                        xp[i] = stats.getXp(i);
                    }
                    NetworkHandler.sendToPlayer(new SyncAllStatsPacket(levels, xp), serverPlayer);
                });
                serverPlayer.getCapability(FatigueProvider.FATIGUE).ifPresent(fatigue ->
                    NetworkHandler.sendToPlayer(new FatiguePacket(fatigue.getFatigue(), fatigue.getMaxFatigue()), serverPlayer));
                serverPlayer.getCapability(ThirstProvider.THIRST).ifPresent(thirst ->
                    NetworkHandler.sendToPlayer(new ThirstPacket(thirst.getThirst()), serverPlayer));
                serverPlayer.getCapability(PerkProvider.PERKS).ifPresent(perks -> {
                    int[] ids = perks.getUnlockedPerks().stream().mapToInt(i -> i).toArray();
                    NetworkHandler.sendToPlayer(new SyncPerksPacket(ids, perks.getAvailablePoints()), serverPlayer);
                });
            }
        }
    }
}
