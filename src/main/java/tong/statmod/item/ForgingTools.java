package tong.statmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import tong.statmod.STATMod;

import java.util.List;
import java.util.function.Supplier;

public final class ForgingTools {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, STATMod.MODID);

    public static final Supplier<Item> BASIC_FORGE_TONGS =
            ITEMS.register("basic_forge_tongs", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> BASIC_SMITHING_HAMMER =
            ITEMS.register("basic_smithing_hammer", () -> new Item(new Item.Properties().stacksTo(1)));

    public static List<Supplier<Item>> all() {
        return List.of(BASIC_FORGE_TONGS, BASIC_SMITHING_HAMMER);
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private ForgingTools() {}
}
