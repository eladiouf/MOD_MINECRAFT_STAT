package tong.statmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
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
        LOGGER.info("STAT Mod chargé !");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        StatsCommands.register(event.getServer().getCommands().getDispatcher());
        LOGGER.info("STAT Mod prêt sur le serveur");
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
