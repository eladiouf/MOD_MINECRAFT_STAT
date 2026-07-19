package tong.statmod.event;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;
import tong.statmod.capability.StatCapabilities;
import tong.statmod.progression.xp.PlayerXpState;
import tong.statmod.progression.xp.XpAction;
import tong.statmod.progression.xp.XpAwardService;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class ExplorationXpEvents {
    private ExplorationXpEvents() {
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        player.getCapability(StatCapabilities.PLAYER_XP_STATE).ifPresent(state ->
                handleTick(player, state));
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        event.getEntity().getCapability(StatCapabilities.PLAYER_XP_STATE)
                .ifPresent(PlayerXpState::clearFall);
    }

    @SubscribeEvent
    public static void teleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(StatCapabilities.PLAYER_XP_STATE)
                    .ifPresent(PlayerXpState::clearFall);
        }
    }

    private static void handleTick(ServerPlayer player, PlayerXpState state) {
        long gameTime = player.serverLevel().getGameTime();
        if (!XpAwardService.isEligible(player)
                || player.isCreative() || player.isSpectator()) {
            state.clearFall();
            return;
        }

        if (player.isFallFlying()) {
            state.clearFall();
        } else if (!player.onGround()) {
            state.observeAirborne(player.fallDistance);
        } else {
            state.finishLanding().ifPresent(distance -> {
                if (state.tryAgility(gameTime)) {
                    XpAwardService.award(player,
                            List.of(XpAction.landing(distance)), gameTime);
                }
            });
        }

        if (gameTime % 20 != 0) {
            return;
        }
        player.serverLevel().getBiome(player.blockPosition()).unwrapKey()
                .map(key -> key.location())
                .filter(id -> !state.hasDiscoveredBiome(id))
                .ifPresent(id -> awardBiome(player, state, id, gameTime));
    }

    private static void awardBiome(
            ServerPlayer player, PlayerXpState state, ResourceLocation id, long gameTime) {
        if (XpAwardService.award(player, List.of(XpAction.biome()), gameTime)) {
            state.markBiomeDiscovered(id);
        }
    }
}
