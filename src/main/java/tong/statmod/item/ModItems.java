package tong.statmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import tong.statmod.STATMod;
import tong.statmod.integration.elementals.ElementalBranch;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, STATMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, STATMod.MODID);

    public static final Supplier<Item> PERK_TOME = ITEMS.register("perk_tome",
            () -> new PerkTomeItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> RESEPC_STONE = ITEMS.register("respec_stone",
            () -> new RespecStoneItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> BLOOD_GRIMOIRE = ITEMS.register("blood_grimoire",
            () -> new ElementalGrimoireItem(ElementalBranch.BLOOD, new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> LIGHTNING_GRIMOIRE = ITEMS.register("lightning_grimoire",
            () -> new ElementalGrimoireItem(ElementalBranch.LIGHTNING, new Item.Properties().stacksTo(1)));

    public static final Supplier<CreativeModeTab> STAT_MOD_TAB = TABS.register("stat_mod",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.statmod"))
                    .icon(() -> new ItemStack(PERK_TOME.get()))
                    .displayItems((params, output) -> {
                        output.accept(PERK_TOME.get());
                        output.accept(RESEPC_STONE.get());
                        output.accept(BLOOD_GRIMOIRE.get());
                        output.accept(LIGHTNING_GRIMOIRE.get());
                    })
                    .build());

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
