package tong.statmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import tong.statmod.StatMod;

@Mod.EventBusSubscriber(modid = StatMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ClientKeyMappings {
    public static final KeyMapping OPEN_STATS = new KeyMapping(
            "key.statmod.open_stats",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.statmod");

    public static final KeyMapping OPEN_SPELL_BINDING = new KeyMapping(
            "key.statmod.open_spell_binding",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.statmod");

    private ClientKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STATS);
        event.register(OPEN_SPELL_BINDING);
    }
}
