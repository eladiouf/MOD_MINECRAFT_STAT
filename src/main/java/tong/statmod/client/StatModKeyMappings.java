package tong.statmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import yesman.epicfight.client.input.EpicFightKeyMappings;

/**
 * Key mappings for STAT Mod skills.
 * CLASS_ARTS key activates the weapon innate skill in the custom slot.
 */
@Mod.EventBusSubscriber(modid = STATMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class StatModKeyMappings {
    public static final KeyMapping CLASS_ARTS_SKILL = new KeyMapping(
        "key.statmod.class_arts",
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_V, // Default: V key
        "key.categories.statmod"
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CLASS_ARTS_SKILL);
    }
}
