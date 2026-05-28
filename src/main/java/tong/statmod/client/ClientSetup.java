package tong.statmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import tong.statmod.STATMod;
import tong.statmod.client.gui.CharacterScreen;
import tong.statmod.client.gui.PerkScreen;

@Mod.EventBusSubscriber(modid = STATMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    public static final KeyMapping OPEN_STATS_KEY = new KeyMapping(
        "key.statmod.open_stats", InputConstants.KEY_P, "key.categories.statmod");
    public static final KeyMapping OPEN_PERKS_KEY = new KeyMapping(
        "key.statmod.open_perks", InputConstants.KEY_O, "key.categories.statmod");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(ClientEventHandler.class);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STATS_KEY);
        event.register(OPEN_PERKS_KEY);
    }

    @Mod.EventBusSubscriber(modid = STATMod.MODID, value = Dist.CLIENT)
    public static class ClientEventHandler {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (OPEN_STATS_KEY.consumeClick()) {
                Minecraft.getInstance().setScreen(new CharacterScreen());
            }
            if (OPEN_PERKS_KEY.consumeClick()) {
                Minecraft.getInstance().setScreen(new PerkScreen());
            }
        }
    }
}
