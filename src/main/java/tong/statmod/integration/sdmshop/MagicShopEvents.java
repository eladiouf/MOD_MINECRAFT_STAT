package tong.statmod.integration.sdmshop;

import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class MagicShopEvents {
    private MagicShopEvents() {
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        ModList mods = ModList.get();
        if (!mods.isLoaded("sdmshop") || !mods.isLoaded("sdmeconomy")
                || !mods.isLoaded("irons_spellbooks")) {
            StatMod.LOGGER.error("[Shop] Required shop stack is incomplete; catalog not generated");
            return;
        }
        var server = event.getServer();
        server.execute(() -> {
            try {
                MagicShopGenerator.regenerate(server);
            } catch (Exception exception) {
                StatMod.LOGGER.error("[Shop] Catalog generation failed", exception);
            }
        });
    }
}
