package tong.statmod.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.capability.PlayerStatsProvider;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.command.StatsCommands;
import tong.statmod.network.StatNetwork;
import tong.statmod.stats.PlayerStats;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class PlayerStatsEvents {
    private static final ResourceLocation CAPABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(StatMod.MOD_ID, "player_stats");

    private PlayerStatsEvents() {
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerStatsProvider provider = new PlayerStatsProvider();
            event.addCapability(CAPABILITY_ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(StatCapabilities.PLAYER_STATS).ifPresent(source ->
                event.getEntity().getCapability(StatCapabilities.PLAYER_STATS).ifPresent(target ->
                        copyStats(source, target)));
        event.getOriginal().invalidateCaps();
    }

    public static void copyStats(PlayerStats source, PlayerStats target) {
        target.copyFrom(source);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.getEntity());
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        sync(event.getEntity());
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.getEntity());
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        StatsCommands.register(event.getDispatcher());
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            StatNetwork.sendSnapshot(serverPlayer);
        }
    }

    @Mod.EventBusSubscriber(modid = StatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            StatCapabilities.register(event);
        }
    }
}
