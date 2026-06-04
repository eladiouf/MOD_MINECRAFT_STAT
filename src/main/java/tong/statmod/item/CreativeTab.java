package tong.statmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import tong.statmod.STATMod;

public class CreativeTab {
    private static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, STATMod.MODID);

    public static final RegistryObject<CreativeModeTab> STAT_TAB = TABS.register("stat_tab",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.statmod"))
            .icon(() -> new ItemStack(Items.NETHER_STAR))
            .displayItems((params, output) -> {
                output.accept(ModItems.STAT_SCROLL.get());
                output.accept(ModItems.PERK_TOME.get());
                output.accept(ModItems.MASTERY_CRYSTAL.get());
            })
            .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
