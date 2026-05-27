package tong.statmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
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
import tong.statmod.network.NetworkHandler;
import tong.statmod.network.SyncAllStatsPacket;
import tong.statmod.skills.SkillUnlockRegistry;
import tong.statmod.stats.StatRegistry;

@Mod(STATMod.MODID)
public class STATMod
{
    public static final String MODID = "statmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(STATMod.MODID);

    public STATMod(FMLJavaModLoadingContext context)
    {
        context.getModEventBus().addListener(this::commonSetup);
        NetworkHandler.register();
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        StatRegistry.init();
        EpicFightCompat.init();
        SkillUnlockRegistry.init();
        initEpicFightData();
        LOGGER.info("STAT Mod chargé !");
    }

    private static void initEpicFightData() {
        try {
            Class<?> cls = Class.forName("yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");
            java.lang.reflect.Method init = cls.getDeclaredMethod("initLivingEntityDataAccessor");
            init.setAccessible(true);
            init.invoke(null);
            LOGGER.info("Epic Fight LivingEntityPatch data initialized via STAT Mod");
        } catch (Exception e) {
            LOGGER.debug("Could not init Epic Fight data (not loaded?): {}", e.getMessage());
        }
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

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            callCreateSyncedEntityData(living);
        }
    }

    private static void callCreateSyncedEntityData(LivingEntity living) {
        try {
            Class<?> cls = Class.forName("yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");
            java.lang.reflect.Method create = cls.getDeclaredMethod("createSyncedEntityData", LivingEntity.class);
            create.setAccessible(true);
            create.invoke(null, living);
        } catch (Exception e) {
            // Epic Fight not loaded
        }
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
            }
        }
    }
}
