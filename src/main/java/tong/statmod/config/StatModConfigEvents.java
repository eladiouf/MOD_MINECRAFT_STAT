package tong.statmod.config;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import tong.statmod.StatMod;
import tong.statmod.effects.PlayerAttributeEffects;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class StatModConfigEvents {
    private StatModConfigEvents() {
    }

    @SubscribeEvent
    public static void reload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != StatModServerConfig.SPEC) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        server.execute(() -> server.getPlayerList().getPlayers()
                .forEach(PlayerAttributeEffects::refresh));
    }
}
