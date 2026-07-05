package tong.statmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import tong.statmod.STATMod;

import java.util.List;
import java.util.function.Supplier;

/**
 * Form-specific assembly parts used to differentiate weapon recipes.
 */
public final class ForgingFormComponents {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, STATMod.MODID);

    public static final Supplier<Item> KATANA_TSUBA =
            ITEMS.register("katana_tsuba", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> RAPIER_GUARD =
            ITEMS.register("rapier_guard", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> CLAYMORE_POMMEL =
            ITEMS.register("claymore_pommel", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> HALBERD_SOCKET =
            ITEMS.register("halberd_socket", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> WARHAMMER_CORE =
            ITEMS.register("warhammer_core", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> STAFF_FOCUS =
            ITEMS.register("staff_focus", () -> new Item(new Item.Properties()));

    public static List<Supplier<Item>> all() {
        return List.of(
                KATANA_TSUBA,
                RAPIER_GUARD,
                CLAYMORE_POMMEL,
                HALBERD_SOCKET,
                WARHAMMER_CORE,
                STAFF_FOCUS
        );
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private ForgingFormComponents() {}
}
