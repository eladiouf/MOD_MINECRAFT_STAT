package tong.statmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.lwjgl.glfw.GLFW;
import tong.statmod.STATMod;
import tong.statmod.client.gui.EnchantmentAnvilScreen;
import tong.statmod.client.gui.InfusionForgeScreen;
import tong.statmod.integration.epicfight.EpicFightClientCompat;
import tong.statmod.menu.ModMenuTypes;

public final class ClientSetup {
    private ClientSetup() {}

    public static final KeyMapping TOGGLE_STATS = new KeyMapping(
            "key.statmod.toggle_stats",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.statmod");

    public static final KeyMapping OPEN_PERKS = new KeyMapping(
            "key.statmod.open_perks",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.statmod");

    public static final KeyMapping OPEN_SPELL_CODEX = new KeyMapping(
            "key.statmod.open_spell_codex",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.statmod");

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_STATS);
        event.register(OPEN_PERKS);
        event.register(OPEN_SPELL_CODEX);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        StatHudOverlay.register(event);
        DungeonHudOverlay.register(event);
        // Mana HUD maison : la barre native d'Iron's est invisible avec Tensura qui
        // remplace le HUD vanilla. Garde de chargement : la classe référence Iron's.
        if (net.neoforged.fml.ModList.get().isLoaded("irons_spellbooks")) {
            ManaHudOverlay.register(event);
        }
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.INFUSION_FORGE_MENU.get(), InfusionForgeScreen::new);
        event.register(ModMenuTypes.ENCHANTMENT_ANVIL_MENU.get(), EnchantmentAnvilScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            NeoForge.EVENT_BUS.register(ClientCacheLifecycle.class);
            EpicFightClientCompat.init();
        });
    }
}
