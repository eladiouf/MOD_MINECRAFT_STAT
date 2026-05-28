package tong.statmod.world.effect;

import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tong.statmod.STATMod;

public class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
        DeferredRegister.create(ForgeRegistries.POTIONS, STATMod.MODID);

    public static final RegistryObject<Potion> ADRENALINE = POTIONS.register("adrenaline",
        () -> new Potion(new net.minecraft.world.effect.MobEffectInstance(ModEffects.ADRENALINE.get(), 900)));
    public static final RegistryObject<Potion> LONG_ADRENALINE = POTIONS.register("long_adrenaline",
        () -> new Potion(new net.minecraft.world.effect.MobEffectInstance(ModEffects.ADRENALINE.get(), 1800)));
    public static final RegistryObject<Potion> STRONG_ADRENALINE = POTIONS.register("strong_adrenaline",
        () -> new Potion(new net.minecraft.world.effect.MobEffectInstance(ModEffects.ADRENALINE.get(), 400, 1)));

    public static void register(IEventBus bus) {
        POTIONS.register(bus);
    }
}
