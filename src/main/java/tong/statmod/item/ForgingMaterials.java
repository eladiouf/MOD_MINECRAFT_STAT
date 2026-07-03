package tong.statmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import tong.statmod.STATMod;

import java.util.List;
import java.util.function.Supplier;

/**
 * Mission M5 — Phase α (Materials Foundation).
 *
 * <p>Registry des 14 nouveaux items {@code heated_<material>_ingot} qui étendent Overgeared
 * à tous les métaux du modpack (vanilla manquants + magistuarmory + Iron's Spellbooks +
 * Tensura). Chaque heated item est produit par {@code minecraft:blasting} depuis l'ingot
 * froid correspondant, sous condition de stat (voir {@link
 * tong.statmod.integration.overgeared.OvergearedRecipeGate#canHeat}).
 *
 * <p>Référence : {@code docs/superpowers/specs/2026-06-30-overgeared-universal-forge-design.md}
 * et {@code .tmp-onivo-audit/decisions/STAT-DEC-OVERGEARED-EXPANSION.md}.
 */
public final class ForgingMaterials {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, STATMod.MODID);

    // Vanilla manquants chez Overgeared natif
    public static final Supplier<Item> HEATED_GOLD_INGOT =
            ITEMS.register("heated_gold_ingot", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HEATED_DIAMOND =
            ITEMS.register("heated_diamond", () -> new Item(new Item.Properties()));

    // magistuarmory (Epic Knights)
    public static final Supplier<Item> HEATED_TIN_INGOT =
            ITEMS.register("heated_tin_ingot", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HEATED_BRONZE_INGOT =
            ITEMS.register("heated_bronze_ingot", () -> new Item(new Item.Properties()));

    // Iron's Spellbooks
    public static final Supplier<Item> HEATED_PYRIUM_INGOT =
            ITEMS.register("heated_pyrium_ingot", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HEATED_ARCANE_INGOT =
            ITEMS.register("heated_arcane_ingot", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HEATED_MITHRIL_INGOT =
            ITEMS.register("heated_mithril_ingot", () -> new Item(new Item.Properties()));

    // Tensura
    public static final Supplier<Item> HEATED_LOW_MAGISTEEL_INGOT =
            ITEMS.register("heated_low_magisteel_ingot", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HEATED_MAGISTEEL_INGOT =
            ITEMS.register("heated_magisteel_ingot", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HEATED_PURE_MAGISTEEL_INGOT =
            ITEMS.register("heated_pure_magisteel_ingot", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HEATED_HIGH_MAGISTEEL_INGOT =
            ITEMS.register("heated_high_magisteel_ingot", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HEATED_ORICHALCUM_INGOT =
            ITEMS.register("heated_orichalcum_ingot", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HEATED_ADAMANTITE_INGOT =
            ITEMS.register("heated_adamantite_ingot", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HEATED_HIHIIROKANE_INGOT =
            ITEMS.register("heated_hihiirokane_ingot", () -> new Item(new Item.Properties()));

    /**
     * Liste ordonnée par tier croissant — utile pour les tests et l'affichage dans
     * l'onglet créatif.
     */
    public static List<Supplier<Item>> all() {
        return List.of(
                HEATED_GOLD_INGOT,
                HEATED_TIN_INGOT,
                HEATED_BRONZE_INGOT,
                HEATED_DIAMOND,
                HEATED_PYRIUM_INGOT,
                HEATED_ARCANE_INGOT,
                HEATED_MITHRIL_INGOT,
                HEATED_LOW_MAGISTEEL_INGOT,
                HEATED_MAGISTEEL_INGOT,
                HEATED_PURE_MAGISTEEL_INGOT,
                HEATED_HIGH_MAGISTEEL_INGOT,
                HEATED_ORICHALCUM_INGOT,
                HEATED_ADAMANTITE_INGOT,
                HEATED_HIHIIROKANE_INGOT
        );
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private ForgingMaterials() {}
}
