package tong.statmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import tong.statmod.STATMod;

import java.util.List;
import java.util.function.Supplier;

/**
 * Mission M5 — Phase β (Grips).
 *
 * <p>4 grips qui complètent l'assemblage final d'une arme à partir d'un
 * {@code rough_<class>_<material>}. Chaque grip donne un bonus différent qui propage à
 * l'arme finale (voir spec §"Grips").
 */
public final class ForgingGrips {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, STATMod.MODID);

    /** Manche en bois — neutre, accessible. */
    public static final Supplier<Item> WOODEN_GRIP =
            ITEMS.register("wooden_grip", () -> new Item(new Item.Properties()));

    /** Manche enveloppé de cuir — +10% durability sur arme finale. */
    public static final Supplier<Item> LEATHER_WRAP =
            ITEMS.register("leather_wrap", () -> new Item(new Item.Properties()));

    /** Manche enveloppé fil de fer — +5% damage sur arme finale. */
    public static final Supplier<Item> WIRE_WRAP =
            ITEMS.register("wire_wrap", () -> new Item(new Item.Properties()));

    /** Manche runique (cuir + amethyst) — débloque slot enchant runic en Phase ε. */
    public static final Supplier<Item> RUNIC_GRIP =
            ITEMS.register("runic_grip", () -> new Item(new Item.Properties()));

    public static List<Supplier<Item>> all() {
        return List.of(WOODEN_GRIP, LEATHER_WRAP, WIRE_WRAP, RUNIC_GRIP);
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private ForgingGrips() {}
}
