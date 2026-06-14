package tong.statmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import tong.statmod.STATMod;

@EventBusSubscriber(modid = STATMod.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    public static final KeyMapping OPEN_PERKS = new KeyMapping(
            "key.statmod.open_perks",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.statmod");

    public static final KeyMapping OPEN_STATS = new KeyMapping(
            "key.statmod.open_stats",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.statmod");

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_PERKS);
        event.register(OPEN_STATS);
    }
}
