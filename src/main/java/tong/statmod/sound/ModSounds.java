package tong.statmod.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tong.statmod.STATMod;

public class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, STATMod.MODID);

    public static final RegistryObject<SoundEvent> LEVEL_UP = register("level_up");
    public static final RegistryObject<SoundEvent> MILESTONE = register("milestone");
    public static final RegistryObject<SoundEvent> PERK_UNLOCK = register("perk_unlock");
    public static final RegistryObject<SoundEvent> SKILL_CAST = register("skill_cast");
    public static final RegistryObject<SoundEvent> FATIGUE_WARNING = register("fatigue_warning");
    public static final RegistryObject<SoundEvent> THIRST_WARNING = register("thirst_warning");
    public static final RegistryObject<SoundEvent> WEAPON_LEVEL_UP = register("weapon_level_up");
    public static final RegistryObject<SoundEvent> DEATH_RESET = register("death_reset");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(STATMod.MODID, name)));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
