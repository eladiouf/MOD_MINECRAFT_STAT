package tong.statmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import tong.statmod.STATMod;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Mission M5 — Phase β (Intermediates).
 *
 * <p>84 nouveaux items {@code rough_<class>_<material>} — un par croisement de classe d'arme
 * (6 : blade / axe_head / spear_tip / bow_limb / staff_core / dagger_blade) et de matériau
 * (14 : les mêmes que {@link ForgingMaterials}).
 *
 * <p>Produit par smithing anvil Overgeared depuis le {@code heated_<material>_ingot}
 * correspondant. Consommé en Phase γ par la recette d'assemblage qui forge l'arme finale
 * du mod tiers.
 *
 * <p>Référence : {@code docs/superpowers/specs/2026-06-30-overgeared-universal-forge-design.md}
 * et {@code .tmp-onivo-audit/decisions/STAT-DEC-OVERGEARED-EXPANSION.md} §Phase β.
 */
public final class ForgingIntermediates {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, STATMod.MODID);

    private static final Map<String, Supplier<Item>> BY_ID = new LinkedHashMap<>();

    static {
        for (String id : ForgingIntermediateIds.allIds()) {
            BY_ID.put(id, ITEMS.register(id, () -> new Item(new Item.Properties())));
        }
    }

    /** Retourne le supplier pour {@code rough_<class>_<material>} ou null si inconnu. */
    public static Supplier<Item> byId(String name) {
        return BY_ID.get(name);
    }

    public static Collection<Supplier<Item>> all() {
        return BY_ID.values();
    }

    public static Map<String, Supplier<Item>> asMap() {
        return Map.copyOf(BY_ID);
    }

    public static int count() {
        return BY_ID.size();
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private ForgingIntermediates() {}
}
