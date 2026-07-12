package tong.statmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import tong.statmod.STATMod;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, STATMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, STATMod.MODID);

    public static final Supplier<Item> PERK_TOME = ITEMS.register("perk_tome",
            () -> new PerkTomeItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> RESPEC_STONE = ITEMS.register("respec_stone",
            () -> new RespecStoneItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> DUNGEON_BEACON = ITEMS.register("dungeon_beacon",
            () -> new DungeonBeaconItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // Armes élémentaires
    public static final Supplier<Item> AQUATIC_STAFF = ITEMS.register("aquatic_staff",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final Supplier<Item> EARTHEN_HAMMER = ITEMS.register("earthen_hammer",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final Supplier<Item> PYRO_DAGGER = ITEMS.register("pyro_dagger",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final Supplier<Item> AERO_BOW = ITEMS.register("aero_bow",
            () -> new BowItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // Monnaie physique FDP_cfa — pièces et billets échangeables chez le Banquier magique.
    public static final Supplier<Item> FDP_COIN_50 = money("fdp_coin_50");
    public static final Supplier<Item> FDP_COIN_100 = money("fdp_coin_100");
    public static final Supplier<Item> FDP_COIN_200 = money("fdp_coin_200");
    public static final Supplier<Item> FDP_COIN_500 = money("fdp_coin_500");
    public static final Supplier<Item> FDP_NOTE_1000 = money("fdp_note_1000");
    public static final Supplier<Item> FDP_NOTE_2000 = money("fdp_note_2000");
    public static final Supplier<Item> FDP_NOTE_5000 = money("fdp_note_5000");
    public static final Supplier<Item> FDP_NOTE_10000 = money("fdp_note_10000");

    public static final Supplier<CreativeModeTab> STAT_MOD_TAB = TABS.register("stat_mod",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.statmod"))
                    .icon(() -> new ItemStack(PERK_TOME.get()))
                    .displayItems((params, output) -> {
                        output.accept(PERK_TOME.get());
                        output.accept(RESPEC_STONE.get());
                        output.accept(DUNGEON_BEACON.get());
                        output.accept(AQUATIC_STAFF.get());
                        output.accept(EARTHEN_HAMMER.get());
                        output.accept(PYRO_DAGGER.get());
                        output.accept(AERO_BOW.get());
                        for (var denomination : tong.statmod.economy.FdpDenomination.ascending()) {
                            output.accept(denomination.item());
                        }
                        // Mission M5 Phase α — heated materials
                        for (Supplier<Item> heated : ForgingMaterials.all()) {
                            output.accept(heated.get());
                        }
                        // Mission M5 Phase β — intermediates + grips + blueprints
                        for (Supplier<Item> bp : ForgingBlueprints.all()) {
                            output.accept(bp.get());
                        }
                        for (Supplier<Item> grip : ForgingGrips.all()) {
                            output.accept(grip.get());
                        }
                        for (Supplier<Item> component : ForgingFormComponents.all()) {
                            output.accept(component.get());
                        }
                        for (Supplier<Item> tool : ForgingTools.all()) {
                            output.accept(tool.get());
                        }
                        for (Supplier<Item> rough : ForgingIntermediates.all()) {
                            output.accept(rough.get());
                        }
                        // Mission M5 Phase δ — magic forge block + rune essences
                        for (Supplier<Item> blk : tong.statmod.block.ForgingBlocks.blockItems()) {
                            output.accept(blk.get());
                        }
                        for (Supplier<Item> essence : tong.statmod.item.RuneEssence.all()) {
                            output.accept(essence.get());
                        }
                        // M6 — Dungeon blocks
                        output.accept(tong.statmod.dungeon.DungeonBlocks.DUNGEON_PORTAL_ITEM.get());
                        output.accept(tong.statmod.dungeon.DungeonBlocks.RETURN_BEACON_ITEM.get());
                        output.accept(tong.statmod.dungeon.DungeonBlocks.NEXT_FLOOR_TELEPORTER_ITEM.get());
                        output.accept(tong.statmod.dungeon.DungeonBlocks.BOSS_ALTAR_ITEM.get());
                    })
                    .build());

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }

    private static Supplier<Item> money(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
    }
}
