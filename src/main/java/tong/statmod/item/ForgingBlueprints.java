package tong.statmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import tong.statmod.STATMod;

import java.util.List;
import java.util.function.Supplier;

/**
 * Mission M5 — Phase β (Blueprints).
 *
 * <p>4 blueprints débloquables qui déterminent quelles classes d'armes le joueur peut
 * forger. Chacun a un gating FORGING (et parfois ERUDITION) appliqué via
 * {@link tong.statmod.integration.overgeared.OvergearedRecipeGate}.
 *
 * <p>Gate :
 * <ul>
 *     <li>{@code blueprint_universal_blade} — gratuit, débloque rough_blade_*, rough_dagger_blade_*</li>
 *     <li>{@code blueprint_universal_pole} — FORGING 8, débloque rough_spear_tip_*, rough_axe_head_*</li>
 *     <li>{@code blueprint_runic_blade} — FORGING 35 + ERUDITION 15, débloque rough_*_arcane, rough_*_mithril</li>
 *     <li>{@code blueprint_legendary} — FORGING 65, débloque rough_*_orichalcum/_adamantite/_hihiirokane</li>
 * </ul>
 */
public final class ForgingBlueprints {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, STATMod.MODID);

    public static final Supplier<Item> BLUEPRINT_UNIVERSAL_BLADE =
            ITEMS.register("blueprint_universal_blade",
                    () -> new Item(new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> BLUEPRINT_UNIVERSAL_POLE =
            ITEMS.register("blueprint_universal_pole",
                    () -> new Item(new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> BLUEPRINT_RUNIC_BLADE =
            ITEMS.register("blueprint_runic_blade",
                    () -> new Item(new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> BLUEPRINT_LEGENDARY =
            ITEMS.register("blueprint_legendary",
                    () -> new Item(new Item.Properties().stacksTo(1)));

    public static List<Supplier<Item>> all() {
        return List.of(
                BLUEPRINT_UNIVERSAL_BLADE,
                BLUEPRINT_UNIVERSAL_POLE,
                BLUEPRINT_RUNIC_BLADE,
                BLUEPRINT_LEGENDARY
        );
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private ForgingBlueprints() {}
}
