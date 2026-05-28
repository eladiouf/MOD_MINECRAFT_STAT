package tong.statmod.world.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tong.statmod.STATMod;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, STATMod.MODID);

    public static final RegistryObject<MobEffect> ADRENALINE = EFFECTS.register("adrenaline", AdrenalineEffect::new);

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
