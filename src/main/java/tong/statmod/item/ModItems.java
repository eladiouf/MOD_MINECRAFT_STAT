package tong.statmod.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tong.statmod.STATMod;

public class ModItems {
    private static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, STATMod.MODID);

    public static final RegistryObject<Item> STAT_SCROLL = ITEMS.register("stat_scroll",
        () -> new StatScrollItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> PERK_TOME = ITEMS.register("perk_tome",
        () -> new PerkTomeItem(new Item.Properties().stacksTo(8)));
    public static final RegistryObject<Item> MASTERY_CRYSTAL = ITEMS.register("mastery_crystal",
        () -> new MasteryCrystalItem(new Item.Properties().stacksTo(32)));
    public static final RegistryObject<Item> WELCOME_BOOK = ITEMS.register("welcome_book",
        () -> new WelcomeBookItem());
    public static final RegistryObject<Item> RESPEC_STONE = ITEMS.register("respec_stone",
        () -> new RespecStoneItem());

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
