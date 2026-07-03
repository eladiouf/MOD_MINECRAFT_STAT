package tong.statmod.menu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import tong.statmod.STATMod;

import java.util.function.Supplier;

public final class ModMenuTypes {

    private ModMenuTypes() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, STATMod.MODID);

    public static final Supplier<MenuType<InfusionForgeMenu>> INFUSION_FORGE_MENU =
            MENUS.register("infusion_forge",
                    () -> new MenuType<>(InfusionForgeMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<EnchantmentAnvilMenu>> ENCHANTMENT_ANVIL_MENU =
            MENUS.register("enchantment_anvil",
                    () -> new MenuType<>(EnchantmentAnvilMenu::new, FeatureFlags.VANILLA_SET));

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
