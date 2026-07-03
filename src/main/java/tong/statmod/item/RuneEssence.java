package tong.statmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import tong.statmod.STATMod;

import java.util.List;
import java.util.function.Supplier;

/**
 * Mission M5 — Phase δ (Magic Forge).
 *
 * <p>Consommable d'infusion magique. Combiné à un {@code rough_<class>_<material>} sur
 * l'infusion_forge, il produit une variante magic-infused de l'arme cible (recettes
 * Phase δ). Trois variantes par école d'affinité — un rune essence pour chaque tier
 * majeur de magic material.
 */
public final class RuneEssence {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, STATMod.MODID);

    /** Essence d'arcane brut — débloque les recettes infusion arcane/runic. */
    public static final Supplier<Item> RUNE_ESSENCE_ARCANE =
            ITEMS.register("rune_essence_arcane",
                    () -> new Item(new Item.Properties()));

    /** Essence pyrium — débloque les recettes infusion fire/pyromancy. */
    public static final Supplier<Item> RUNE_ESSENCE_PYRIUM =
            ITEMS.register("rune_essence_pyrium",
                    () -> new Item(new Item.Properties()));

    /** Essence mithril — débloque les recettes infusion ice/cold/holy. */
    public static final Supplier<Item> RUNE_ESSENCE_MITHRIL =
            ITEMS.register("rune_essence_mithril",
                    () -> new Item(new Item.Properties()));

    public static List<Supplier<Item>> all() {
        return List.of(RUNE_ESSENCE_ARCANE, RUNE_ESSENCE_PYRIUM, RUNE_ESSENCE_MITHRIL);
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private RuneEssence() {}
}
